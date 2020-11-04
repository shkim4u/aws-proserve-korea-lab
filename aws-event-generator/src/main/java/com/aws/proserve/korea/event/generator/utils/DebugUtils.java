package com.aws.proserve.korea.event.generator.utils;

import org.apache.commons.lang3.BooleanUtils;

public class DebugUtils {

	public static boolean isUnsupportedTerminal() {
		if (System.getProperty("jline.terminal") != null) {
			String terminal = System.getProperty("jline.terminal");
			if (terminal != null) {
				return "jline.UnsupportedTerminal".equalsIgnoreCase(terminal);
			}
		}
		
		return false;
	}
	
	public static void debugln(String message, Object...args) {
		if (BooleanUtils.toBoolean(
			System.getProperty("AwsEventGenerator.debug"))
		) {
			System.out.println(String.format(message, args));
			System.out.flush();
		}
	}

	public static boolean isDebug() {
		return BooleanUtils.toBoolean(System.getProperty("AwsEventGenerator.debug"));
	}

}
