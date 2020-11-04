package com.aws.proserve.korea.network.syslog.sender;

import java.io.CharArrayWriter;
import java.io.IOException;

import com.aws.proserve.korea.network.syslog.SyslogMessage;

public class NullSyslogMessageSender extends AbstractSyslogMessageSender {

	@Override
	public void sendMessage(SyslogMessage message) throws IOException {
		// Do nothing. This is a null sender.
	}

	@Override
	public void sendMessage(CharArrayWriter message) throws IOException {
		// Do nothing. This is a null sender.
	}

	@Override
	public void sendMessage(CharSequence message) throws IOException {
		// Do nothing. This is a null sender.
	}

	
}
