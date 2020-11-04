package com.aws.proserve.korea.event.generator.utils;

//import org.apache.log4j.Appender;
//import org.apache.log4j.AppenderSkeleton;
//import org.apache.log4j.ConsoleAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.PatternLayout;
//import org.apache.log4j.helpers.NullEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {

	public static Logger logger = null;
	public static boolean isVerbose = false;
	
	static {
		// Configure the logger.
		try {
//			// Get root logger.
//			org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
//			
//			if (rootLogger != null) {
//				isVerbose = AwsEventGenerator.getInstance().getCommandParams().isVerbose();
////				boolean isVerbose = AwsEventGenerator.getInstance().getConfiguration().isVerbose();
//				
//				// Inspect the command line argument has "-v" (verbose) option.
//				// If so, set root logger level to "ALL".
//				if (isVerbose) {
//					rootLogger.setLevel(Level.ALL);
//				}
//				
//				// Find console appender.
//				ConsoleAppender consoleAppender = null;
//				Enumeration appenders = rootLogger.getAllAppenders();
//				while (appenders != null &&
//					!(appenders instanceof NullEnumeration) &&
//					appenders.hasMoreElements()
//				) {
//					Appender appender = (Appender)appenders.nextElement();
//					if (appender instanceof AppenderSkeleton) {
//						if (isVerbose) {
//							((AppenderSkeleton)appender).setThreshold(Level.ALL);
//						}
//					}
//					
//					if ((appender instanceof ConsoleAppender) && (consoleAppender == null)) {
//						// Let's just remember the first console appender.
//						consoleAppender = (ConsoleAppender)appender;
//					}
//					
//				}
//				
//				if (consoleAppender == null &&
//					BooleanUtils.toBoolean(System.getProperty("AwsEventGenerator.console"))
//				) {
//					// Add console appender.
//					consoleAppender = new ConsoleAppender();
//					String pattern = "[%d]: %m%n";
//					consoleAppender.setLayout(new PatternLayout(pattern));
//					
//					consoleAppender.setThreshold(
//						isVerbose ? Level.ALL : Level.INFO
//					);
//					consoleAppender.activateOptions();
//					
//					// Add to the root logger.
//					rootLogger.addAppender(consoleAppender);
//				} else {
//				}
//			}
			
			// Get slf4j logger which is mainly used in this project.
			logger = LoggerFactory.getLogger(LoggerUtils.class);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public static Logger getLogger() {
		return logger;
	}

	public static void verbose(String format, Object... arguments) {
		if (isVerbose) {
			LoggerUtils.getLogger().trace(format, arguments);
		}
	}
}
