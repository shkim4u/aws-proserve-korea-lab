/*
 * Copyright 2010-2014, CloudBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aws.proserve.korea.network.syslog.sender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;


import org.apache.commons.io.IOUtils;

import com.aws.proserve.korea.network.syslog.SyslogMessage;
import com.aws.proserve.korea.network.syslog.utils.CachingReference;

/**
 * Syslog message sender over UDP.
 *
 * TODO optimize performances recycling the byte arrays. Note: {@link java.io.ByteArrayOutputStream}
 * can be subclassed to access to the underlying {@code byte[]}.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
//@ThreadSafe
public class UdpSyslogMessageSender extends AbstractSyslogMessageSender {
    /**
     * {@link java.net.InetAddress InetAddress} of the remote Syslog Server.
     *
     * The {@code InetAddress} is refreshed regularly to handle DNS changes (default {@link #DEFAULT_INET_ADDRESS_TTL_IN_MILLIS})
     *
     * Default value: {@link #DEFAULT_SYSLOG_HOST}
     */
    protected CachingReference<InetAddress> syslogServerHostnameReference;
    /**
     * Listen port of the remote Syslog server.
     *
     * Default: {@link #DEFAULT_SYSLOG_PORT}
     */
    protected int syslogServerPort = DEFAULT_SYSLOG_PORT;

    private DatagramSocket datagramSocket;

    public UdpSyslogMessageSender() {
    	this(true);
    }
    
    public UdpSyslogMessageSender(boolean tryToCheckOutboundAddress) {
        try {
            setSyslogServerHostname(DEFAULT_SYSLOG_HOST);
            datagramSocket = new DatagramSocket();
        } catch (IOException e) {
            throw new IllegalStateException("Exception initializing datagramSocket", e);
        }
        
		// If set, try to get bound address.
		if (tryToCheckOutboundAddress) {
			checkOutboundAddressQuietly();
		}
    }

    private void checkOutboundAddressQuietly() {
		Socket socket = null;
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress("google.com", 80));
//			System.out.println(socket.getLocalAddress());
			setDefaultMessageHostname(socket.getLocalAddress().getHostAddress());
		} catch (IOException e) {
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
     * Send the given {@link com.aws.proserve.korea.network.syslog.SyslogMessage} over UDP.
     *
     * @param message the message to send
     * @throws IOException
     */
    @Override
    public void sendMessage(SyslogMessage message) throws IOException {
		sentCounter.incrementAndGet();
        long nanosBefore = System.nanoTime();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(baos, UTF_8);
            message.toSyslogMessage(messageFormat, out);
            out.flush();

            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Send syslog message " + new String(baos.toByteArray(), UTF_8));
            }
            byte[] bytes = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, syslogServerHostnameReference.get(), syslogServerPort);
            datagramSocket.send(packet);
            // [2018-10-18] Kim, Sang Hyoun: Update sent bytes.
            sentBytesEnveloped.addAndGet(bytes.length);
            lastSentBytesEnveloped = bytes.length;
            
//            SocketAddress sockAddr = packet.getSocketAddress();
//            sockAddr.toString();
            
//            SocketAddress sockAddr = datagramSocket.getLocalSocketAddress();
//            sockAddr.toString();
        } catch (IOException e) {
            sendErrorCounter.incrementAndGet();
            throw e;
        } catch (RuntimeException e) {
            sendErrorCounter.incrementAndGet();
            throw e;
        } finally {
            sendDurationInNanosCounter.addAndGet(System.nanoTime() - nanosBefore);
        }
    }


    public void setSyslogServerHostname(final String syslogServerHostname) {
        this.syslogServerHostnameReference = new CachingReference<InetAddress>(DEFAULT_INET_ADDRESS_TTL_IN_NANOS) {
            @Override
            protected InetAddress newObject() {
                try {
                    return InetAddress.getByName(syslogServerHostname);
                } catch (UnknownHostException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    public void setSyslogServerPort(int syslogServerPort) {
        this.syslogServerPort = syslogServerPort;
    }

    public String getSyslogServerHostname() {
        InetAddress inetAddress = syslogServerHostnameReference.get();
        return inetAddress == null ? null : inetAddress.getHostName();
    }

    public int getSyslogServerPort() {
        return syslogServerPort;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "syslogServerHostname='" + this.getSyslogServerHostname() + '\'' +
                ", syslogServerPort='" + this.getSyslogServerPort() + '\'' +
                ", defaultAppName='" + defaultAppName + '\'' +
                ", defaultFacility=" + defaultFacility +
                ", defaultMessageHostname='" + defaultMessageHostname + '\'' +
                ", defaultSeverity=" + defaultSeverity +
                ", messageFormat=" + messageFormat +
                ", sentCounter=" + sentCounter +
                ", sentBytesEnvloped=" + sentBytesEnveloped +
                ", sendDurationInNanosCounter=" + sendDurationInNanosCounter +
                ", sendErrorCounter=" + sendErrorCounter +
                '}';
    }

	@Override
	public void close() {
		IOUtils.closeQuietly(datagramSocket);
		super.close();
	}
    
    
}
