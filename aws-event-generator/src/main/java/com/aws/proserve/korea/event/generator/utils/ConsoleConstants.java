package com.aws.proserve.korea.event.generator.utils;

public interface ConsoleConstants {

	String CLEAR_TERMINAL_ANSI_CMD = new String(
		new byte[] {
			(byte) 0x1b, (byte) 0x5b, (byte) 0x32, (byte) 0x4a,
			(byte) 0x1b, (byte) 0x5b, (byte) 0x48
		}
	);
    // constants copied from wincon.h

	/**
	 * The ReadFile or ReadConsole function returns only when a carriage return
	 * character is read. If this mode is disable, the functions return when one or
	 * more characters are available.
	 */
	public static final int ENABLE_LINE_INPUT = 2;

	/**
	 * Characters read by the ReadFile or ReadConsole function are written to the
	 * active screen buffer as they are read. This mode can be used only if the
	 * ENABLE_LINE_INPUT mode is also enabled.
	 */
	public static final int ENABLE_ECHO_INPUT = 4;

	/**
	 * CTRL+C is processed by the system and is not placed in the input buffer. If
	 * the input buffer is being read by ReadFile or ReadConsole, other control keys
	 * are processed by the system and are not returned in the ReadFile or
	 * ReadConsole buffer. If the ENABLE_LINE_INPUT mode is also enabled, backspace,
	 * carriage return, and linefeed characters are handled by the system.
	 */
	public static final int ENABLE_PROCESSED_INPUT = 1;

	/**
	 * User interactions that change the size of the console screen buffer are
	 * reported in the console's input buffee. Information about these events can be
	 * read from the input buffer by applications using theReadConsoleInput
	 * function, but not by those using ReadFile orReadConsole.
	 */
	public static final int ENABLE_WINDOW_INPUT = 8;
}
