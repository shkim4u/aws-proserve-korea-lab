package com.aws.proserve.korea.event.generator.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;

import jline.Terminal;
import jline.UnixTerminal;
import jline.UnsupportedTerminal;
import jline.WindowsTerminal;

public class ConsoleUtils {
	private native static int getConsoleMode();
    private native static void setConsoleMode(final int mode);
    
	public static String readLine() throws IOException {
		return readLine(true);
	}
	
	public static String readLine(boolean useSystemConsoleFirst) throws IOException {
		if (System.console() != null && useSystemConsoleFirst) {
			DebugUtils.debugln("Using System.console()");
			return System.console().readLine();
		}
		
		DebugUtils.debugln("Using InputStreamReader(System.in)");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		return reader.readLine();
	}
	
	public static String readLine(String format, Object... args) throws IOException {
		if (System.console() != null) {
			return System.console().readLine(format, args);
		}
		LoggerUtils.getLogger().debug(String.format(format, args));
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		return reader.readLine();
	}
	
	public static char[] readPassword(String format, Object... args) throws IOException {
		if (System.console() != null)
			return System.console().readPassword(format, args);
		return readLine(format, args).toCharArray();
	}

	public static char readChar(char defaultChar) throws IOException {
		char ch = defaultChar;
		if (DebugUtils.isUnsupportedTerminal()) {
			DebugUtils.debugln("isUnsupportedTerminal");
			String input = ConsoleUtils.readLine();
			if (input != null) {
				ch = input.charAt(0);
			}
			
			// In this debug context, the new line is already there.
			// No need to LoggerUtils.getLogger().debug().
		} else {
			DebugUtils.debugln("SupportedTerminal");
//			int i = WindowsTerminal.getTerminal().readCharacter(System.in);
			int i = Terminal.getTerminal().readCharacter(System.in);
			ch = (char)i;
			
//			LoggerUtils.getLogger().debug("");
		}
		
		return ch;
	}
	
	public static void restoreTerminalSilently() {
		Terminal term = Terminal.getTerminal();
		if (term == null) return;
		
		try {
			if (term instanceof UnixTerminal) {
				DebugUtils.debugln("Restoring Unix Terminal");
				((UnixTerminal)term).restoreTerminal();
			} else if (term instanceof WindowsTerminal) {
				DebugUtils.debugln("Restoring Windows Terminal");
//				final int currentMode = getConsoleMode();
//				DebugUtils.debugln("Current console mode: " + currentMode);
//				int newMode = currentMode | (
//					ConsoleConstants.ENABLE_LINE_INPUT |
//					ConsoleConstants.ENABLE_ECHO_INPUT |
//					ConsoleConstants.ENABLE_PROCESSED_INPUT |
//					ConsoleConstants.ENABLE_WINDOW_INPUT
//				);
//				DebugUtils.debugln("New console mode: " + newMode);

				term.enableEcho();
//				setConsoleMode(newMode);
			} else if (term instanceof UnsupportedTerminal) {
				DebugUtils.debugln("Restoring Unsupported Terminal");
				Terminal.resetTerminal();
			}
		} catch (Exception e) {
		}		
	}
	
	public static void standbyTerminalSilently() {
		Terminal term = Terminal.getTerminal();
		try {
			if (term instanceof UnixTerminal) {
				Terminal.setupTerminal();
			} else if (term instanceof WindowsTerminal) {
//				final int currentMode = getConsoleMode();
//				int newMode = currentMode &	~(
//					ConsoleConstants.ENABLE_LINE_INPUT |
//					ConsoleConstants.ENABLE_ECHO_INPUT |
//					ConsoleConstants.ENABLE_PROCESSED_INPUT |
//					ConsoleConstants.ENABLE_WINDOW_INPUT
//				);
				
				term.disableEcho();
//				setConsoleMode(newMode);
			} else if (term instanceof UnsupportedTerminal) {
				Terminal.setupTerminal();
			}
		} catch (Exception e) {
			
		}
	}
	
	public static void main(String[] args) throws IOException {
//		boolean ret = true;
//		try {
//			char ch = 'u';
//			do {
//				System.out.println();
//				System.out.print("Insert next tokens and press 'c' to continue, or press 'q' to quit: ");
//				ch = ConsoleUtils.readChar('u');
//	
//				System.out.print(ch);
//				if (ch == 'c' || ch == 'C') {
//					System.out.println();
//					ret = true;
//					break;
//				}
//				
//				if (ch == 'q' || ch == 'Q') {
//					System.out.println();
//					ret = false;
//					break;
//				}
//				
//			} while (ch != 'c' &&
//				ch != 'C' &&
//				ch != 'q' && 
//				ch != 'Q'
//			);
//		} finally {
//		}

//		System.setOut(
//			new PrintStream(
//				new BufferedOutputStream(
//					new FileOutputStream(FileDescriptor.out)
//				),
//				false
//			)
//		);

//		char ch = ConsoleUtils.readChar('h');
		
		// Reset terminal.
//		Terminal.resetTerminal();
		
//		Terminal.getTerminal().enableEcho();
		
//		char ch = ConsoleUtils.readChar('h');
		
		new ConsoleUtils().doMain();
	}
	
	public void doMain() throws IOException {
		Terminal term = Terminal.getTerminal();
		try {
			if (term instanceof UnixTerminal) {
				((UnixTerminal)term).restoreTerminal();
			} else if (term instanceof WindowsTerminal) {
				final int currentMode = getConsoleMode();
				int newMode = currentMode | (2 | 4 | 1 | 8);

				term.enableEcho();
				setConsoleMode(newMode);
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String cwd = AwsEventGeneratorFileUtils.getCurrentWorkingDirectoryAbsolutePath();
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("Attach device mssage file." + AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  Current working directory: [" +
			AwsEventGeneratorFileUtils.getCurrentWorkingDirectoryAbsolutePath() + "]" +
			AwsEventGeneratorFileUtils.NEW_LINE
		);
		sb.append("  Relative or absolute path acceptable." + AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  eg) ../devices/message_files_bundle2.txt, /home/user/message_files_bundle2.txt or C:\\User\\message_files_bundle2.txt" +
			AwsEventGeneratorFileUtils.NEW_LINE
		);
		sb.append("Please input message file and type ENTER: ");
		
		System.out.print(sb.toString());
		
		System.out.flush();
		
		String line = null;
		try {
			line = ConsoleUtils.readLine(false);
		} catch (IOException e) {
			System.out.println("Error: " + e.getLocalizedMessage());
			
			return;
		}
		
//		Scanner scanner = new Scanner(System.in);
//		line = scanner.nextLine();
		
		System.out.println("LINE: " + line);
		System.out.flush();
		
		char ch = ConsoleUtils.readChar('h');
	}
	public static String readLineSilently(boolean useSystemConsoleFirst) {
		try {
			return readLine(useSystemConsoleFirst);
		} catch (IOException e) {
			if (LoggerUtils.getLogger().isWarnEnabled()) {
				LoggerUtils.getLogger().warn(
					"ConsoleUtils.readLine() error: " + e.getLocalizedMessage()
				);
			}
			System.out.println("ERROR: " + e.getLocalizedMessage());
//			System.out.println("Exiting.");
			System.out.flush();
			
			return StringUtils.EMPTY;
		}
	}
	public static void diaplaySuggestedInput(String suggestedInput) {
		if (StringUtils.isBlank(suggestedInput)) return;
		
		final byte[] suggestedInputBytes = suggestedInput.getBytes();
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(
			suggestedInputBytes
		);

		InputStream old = System.in;
		try {
			System.setIn(inputStream);
		} finally {
			System.setIn(old);
		}
	}

}
