package com.aws.proserve.korea.network.syslog.sender;


import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


import com.aws.proserve.korea.network.syslog.Facility;
import com.aws.proserve.korea.network.syslog.MessageFormat;
import com.aws.proserve.korea.network.syslog.Severity;
import com.aws.proserve.korea.network.syslog.SyslogMessage;
import com.aws.proserve.korea.network.syslog.utils.InternalLogger;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public abstract class AbstractSyslogMessageSender implements SyslogMessageSender {
    protected final static Charset UTF_8 = Charset.forName("UTF-8");
    protected final InternalLogger logger = InternalLogger.getLogger(getClass());
    // default values
    protected String defaultAppName;
    protected Facility defaultFacility = Facility.USER;
    protected String defaultMessageHostname;
    protected Severity defaultSeverity = Severity.INFORMATIONAL;
    // remote syslog server config
    /**
     * Format of messages accepted by the remote syslog server
     * ({@link com.aws.proserve.korea.network.syslog.MessageFormat#RFC_3164 RFC_3164} or
     * {@link com.aws.proserve.korea.network.syslog.MessageFormat#RFC_5424 RFC_5424})
     */
    protected MessageFormat messageFormat = DEFAULT_SYSLOG_MESSAGE_FORMAT;
    // statistics
    protected final AtomicLong sentCounter = new AtomicLong();
    protected final AtomicLong sendDurationInNanosCounter = new AtomicLong();
    protected final AtomicInteger sendErrorCounter = new AtomicInteger();

    protected final AtomicLong sentBytesRaw = new AtomicLong();
    protected final AtomicLong sentBytesEnveloped = new AtomicLong();
    
    protected long lastSentCount = 0;
    protected long lastSentBytesRaw = 0;
    protected long lastSentBytesEnveloped = 0;
    
	private long lastSentTimeMillis = -1;
	
	private boolean syslogPrependPRIPart;
	private boolean syslogPrependHeaderTimestamp;
	private boolean syslogPrependHeaderHostname;
	private boolean syslogPrependHeaderAppName;

    /**
     * Send the given text message
     *
     * @param message
     * @throws java.io.IOException
     */
    @Override
	public void sendMessage(CharArrayWriter message) throws IOException {

		SyslogMessage syslogMessage = new SyslogMessage()
			.withAppName(defaultAppName)
			.withFacility(defaultFacility)
			.withHostname(defaultMessageHostname)
			.withSeverity(defaultSeverity)
			.withMsg(message)
			.withPrependPRIPart(this.syslogPrependPRIPart)
			.withPrependHeaderTimestamp(this.syslogPrependHeaderTimestamp)
			.withPrependHeaderHostname(this.syslogPrependHeaderHostname)
			.withPrependHeaderAppName(this.syslogPrependHeaderAppName);
		sendMessage(syslogMessage);

		// [2018-10-22] Kim, Sang Hyoun: Calculate sent bytes raw.
		sentBytesRaw.addAndGet(message.size());
		lastSentCount = 1;
		lastSentBytesRaw = message.size();
		lastSentTimeMillis = System.currentTimeMillis();
	}

    @Override
    public void sendMessage(CharSequence message) throws IOException {
        CharArrayWriter writer = new CharArrayWriter();
        writer.append(message);
        sendMessage(writer);
    }

    /**
     * Send the given {@link com.aws.proserve.korea.network.syslog.SyslogMessage}.
     *
     * @param message the message to send
     * @throws IOException
     */
    public abstract void sendMessage(SyslogMessage message) throws IOException;

    public String getDefaultAppName() {
        return defaultAppName;
    }

    public Facility getDefaultFacility() {
        return defaultFacility;
    }

    public MessageFormat getMessageFormat() {
        return messageFormat;
    }

    public String getDefaultMessageHostname() {
        return defaultMessageHostname;
    }

    public long getSentCount() {
        return sentCounter.get();
    }
    
    // [2018-10-22] Kim, Sang Hyoun: Sent bytes.
	public long getSentBytesEnvoloped() {
		return sentBytesEnveloped.get();
	}
	
	public long getSentBytesRaw() {
		return sentBytesRaw.get();
	}

    /**
     * Human readable view of {@link #getSendDurationInNanos()}
     *
     * @return total duration spent sending syslog messages
     */
    public long getSendDurationInMillis() {
        return TimeUnit.MILLISECONDS.convert(getSendDurationInNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Human readable view of {@link #getSendDurationInNanos()}
     *
     * @return total duration spent sending syslog messages
     */
    public long getSendDurationInNanos() {
        return sendDurationInNanosCounter.get();
    }

    public int getSendErrorCount() {
        return sendErrorCounter.get();
    }

    public Severity getDefaultSeverity() {
        return defaultSeverity;
    }

    public void setDefaultAppName(String defaultAppName) {
        this.defaultAppName = defaultAppName;
    }

    public void setDefaultMessageHostname(String defaultHostname) {
        this.defaultMessageHostname = defaultHostname;
    }

    public void setDefaultFacility(Facility defaultFacility) {
        this.defaultFacility = defaultFacility;
    }

    public void setMessageFormat(MessageFormat messageFormat) {
        this.messageFormat = messageFormat;
    }

    public void setDefaultSeverity(Severity defaultSeverity) {
        this.defaultSeverity = defaultSeverity;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "defaultAppName='" + defaultAppName + '\'' +
                ", defaultFacility=" + defaultFacility +
                ", defaultMessageHostname='" + defaultMessageHostname + '\'' +
                ", defaultSeverity=" + defaultSeverity +
                ", messageFormat=" + messageFormat +
                ", sentCounter=" + sentCounter +
                ", sentBytesEnveloped=" + sentBytesEnveloped +
                ", sendDurationInNanosCounter=" + sendDurationInNanosCounter +
                ", sendErrorCounter=" + sendErrorCounter +
                '}';
    }
    
    public void setAllDefaults(
    	String defaultMessageHostname,
    	String defaultAppName,
    	Facility defaultFacility,
    	Severity defaultSeverity
//    	String serverAddress,				// Reserved.
//    	int serverPort						// Reserved.
    ) {
    	if (defaultMessageHostname != null) {
    		setDefaultMessageHostname(defaultMessageHostname);
    	}
		setDefaultAppName(defaultAppName);
		setDefaultFacility(defaultFacility);
		setDefaultSeverity(defaultSeverity);
		
//		// If set, try to get bound address.
//		if (tryToCheckBoundAddress) {
//			try {
//				Socket socket = new Socket();
//				socket.connect(new InetSocketAddress("google.com", 80));
//				System.out.println(socket.getLocalAddress());
//			}
//		}
    }

	public void close() {
		
	}

	public long getLastSentTimeMillis() {
		return lastSentTimeMillis;
	}

	public void setPrependPRIPart(boolean syslogPrependPRIPart) {
		this.syslogPrependPRIPart = syslogPrependPRIPart;
	}

	public void setPrependHeaderTimestamp(boolean syslogPrependHeaderTimestamp) {
		this.syslogPrependHeaderTimestamp = syslogPrependHeaderTimestamp;
	}

	public void setPrependHeaderHostname(boolean syslogPrependHeaderHostname) {
		this.syslogPrependHeaderHostname = syslogPrependHeaderHostname;
	}

	public void setPrependHeaderAppName(boolean syslogPrependHeaderAppName) {
		this.syslogPrependHeaderAppName = syslogPrependHeaderAppName;
	}

	public boolean isSyslogPrependPRIPart() {
		return syslogPrependPRIPart;
	}

	public void setSyslogPrependPRIPart(boolean syslogPrependPRIPart) {
		this.syslogPrependPRIPart = syslogPrependPRIPart;
	}

	public boolean isSyslogPrependHeaderTimestamp() {
		return syslogPrependHeaderTimestamp;
	}

	public void setSyslogPrependHeaderTimestamp(boolean syslogPrependHeaderTimestamp) {
		this.syslogPrependHeaderTimestamp = syslogPrependHeaderTimestamp;
	}

	public boolean isSyslogPrependHeaderHostname() {
		return syslogPrependHeaderHostname;
	}

	public void setSyslogPrependHeaderHostname(boolean syslogPrependHeaderHostname) {
		this.syslogPrependHeaderHostname = syslogPrependHeaderHostname;
	}

	public boolean isSyslogPrependHeaderAppName() {
		return syslogPrependHeaderAppName;
	}

	public void setSyslogPrependHeaderAppName(boolean syslogPrependHeaderAppName) {
		this.syslogPrependHeaderAppName = syslogPrependHeaderAppName;
	}

	public long getLastSentCount() {
		return this.lastSentCount;
	}

	public long getLastSentBytesRaw() {
		return this.lastSentBytesRaw;
	}

	public long getLastSentBytesEnvoloped() {
		return this.lastSentBytesEnveloped;
	}

}
