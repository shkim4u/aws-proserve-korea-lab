package com.aws.proserve.korea.event.generator.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.aws.proserve.korea.event.generator.AwsEventGenerator.Configs;

public class AwsEventGeneratorConfigurationUtils {

	private static Integer envPeakBoostFactor = null;

	public static Properties defaultConfigs() {
		/*
		 * Common configurations in case that there is no config file yet.
		 */
		Properties configProps = new Properties();
		
		//# Target server.
//		configProps.setProperty(Configs.CONFIG_KEY_SYSLOG_SERVER, "esm611.microfocus.skytapdns.com");
		configProps.setProperty(Configs.CONFIG_KEY_SYSLOG_SERVER, "localhost");
		
		//# Port.
		configProps.setProperty(Configs.CONFIG_KEY_SYSLOG_PORT, "514");
		
		//# Message files bundle file.
		configProps.setProperty(Configs.CONFIG_KEY_MESSAGE_FILES_BUNDLE_FILE_PATH, "message_files_bundle.txt");
		
//		//# Max Slots.
//		configProps.setProperty(Configs.CONFIG_KEY_CONCURRENT_SLOTS, "10");
		
		// Delimiter in message files bundle file.
		configProps.setProperty(
			Configs.CONFIG_KEY_MESSAGE_FILES_BUNDLE_DELIMITER,
			Configs.CONFIG_DEFAULT_VALUE_MESSAGE_FILES_BUNDLE_DELIMITER
		);
		
		// Cache event messages?
		configProps.setProperty(
			Configs.CONFIG_KEY_CACHE_MESSAGE,
			Configs.CONFIG_DEFAULT_VALUE_CACHE_MESSAGE
		);
		
		// Result output file path.
		configProps.setProperty(
			Configs.CONFIG_KEY_RESULT_OUTPUT_FILE_PATH,
			Configs.CONFIG_DEFAULT_VALUE_RESULT_OUTPUT_FILE_PATH
		);
		
		// Thread count.
		configProps.setProperty(
			Configs.CONFIG_KEY_MAX_THREAD_COUNT,
			Configs.CONFIG_DEFAULT_VALUE_MAX_THREAD_COUNT
		);
		
//		// Verbose.
//		configProps.setProperty(
//			Configs.CONFIG_KEY_VERBOSE,
//			Configs.CONFIG_DEFAULT_VALUE_VERBOSE
//		);
		
		// Replace text delimiter.
		configProps.setProperty(
			Configs.CONFIG_KEY_REPLACE_TEXT_DELIMIETER,
			Configs.CONFIG_DEFAULT_VALUE_REPLACE_TEXT_DELIMIETER
		);
		
		// Use monitor thread.
		configProps.setProperty(
			Configs.CONFIG_KEY_USE_MONITOR_THREAD,
			Configs.CONFIG_DEFAULT_VALUE_USE_MONITOR_THREAD
		);
		
		// Syslog message host name.
		configProps.setProperty(
			Configs.CONFIG_KEY_SYSLOG_MESSAGE_HOST_NAME,
			Configs.CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_HOST_NAME
		);
		
		// Syslog message app name.
		configProps.setProperty(
			Configs.CONFIG_KEY_SYSLOG_MESSAGE_APP_NAME,
			Configs.CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_APP_NAME
		);
		
		// Syslog message facility.
		configProps.setProperty(
			Configs.CONFIG_KEY_SYSLOG_MESSAGE_FACILITY,
			Configs.CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_FACILITY
		);
				
		// Syslog message severity.
		configProps.setProperty(
			Configs.CONFIG_KEY_SYSLOG_MESSAGE_SEVERITY,
			Configs.CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_SEVERITY
		);
		
		// Daily limit.
		configProps.setProperty(
			Configs.CONFIG_KEY_DAILY_SENT_COUNT_LIMIT,
			Long.toString(Configs.CONFIG_DEFAULT_VALUE_DAILY_SENT_COUNT_LIMIT, 10)
		);
		configProps.setProperty(
			Configs.CONFIG_KEY_DAILY_SENT_BYTES_RAW_LIMIT,
			Long.toString(Configs.CONFIG_DEFAULT_VALUE_DAILY_SENT_BYTES_RAW_LIMIT, 10)
		);
		configProps.setProperty(
			Configs.CONFIG_KEY_DAILY_SENT_BYTES_ENVELOPED_LIMIT,
			Long.toString(Configs.CONFIG_DEFAULT_VALUE_DAILY_SENT_BYTES_ENVELOPED_LIMIT, 10)
		);
		
		// Daily transition cron expression.
		configProps.setProperty(
			Configs.CONFIG_KEY_DAILY_TRANSITION_CRON_EXPRESSION,
			Configs.CONFIG_DEFAULT_VALUE_DAILY_TRANSITION_CRON_EXPRESSION
		);
				
		return configProps;
	}

	public static void saveSampleMessageFilesBundleFile(String filePath) throws IOException {
//		String messageFilesBundleFilePath = getMessageFilesBundleParam().getMessageFilesBundleFilePath();
		String messageFilesBundleFilePath = filePath;
		File messageFilesBundleFile = new File(messageFilesBundleFilePath);
		if (!messageFilesBundleFile.exists()) {
			Collection<String> lines = new ArrayList<String>();
			
			lines.add("#messageFilePath,replaceSourcesText,replaceTargetsText,"
				+ "syslogServer,syslogPort,"
				+ "initialSendIntervalMillis,targetSendIntervalMillis,warmingUpPeriodSec,"
				+ "isRepeat,isCacheMessage,"
				+ "syslogMessageHostName,syslogAppName,syslogFacility,syslogSeverity,"
				+ "syslogPrependPRIPart,syslogPrependHeaderTimestamp,syslogPrependHeaderHostname,syslogPrependHeaderAppName,dailyPeakBoostPeriods,boostParams"
			);
			lines.add("syslog-ASA.txt,,,"
				+ "esm611.microfocus.skytapdns.com,1514,"
				+ "100,1,60,"
				+ "true,true,"
				+ "Event_Generator_Host,Event_Generator,USER,INFORMATIONAL,"
				+ "false,false,false,false,,"
			);
			
			FileUtils.writeLines(messageFilesBundleFile, lines, true);
		}
	}

//	public static boolean isPeakBoost() {
//		
//		
//		if (System.getProperty("AwsEventGenerator.peak_boost") != null) {
//			envPeakBoostFactor = NumberUtils.toInt(
//				System.getProperty("AwsEventGenerator.peak_boost"),
//				2
//			);
//		}
//		
//		return (envPeakBoostFactor != null);
//	}

	public static Integer envPeakBoostFactor() {
		if (envPeakBoostFactor == null) {
			if (System.getProperty("AwsEventGenerator.peak_boost") != null) {
				envPeakBoostFactor = NumberUtils.toInt(
					System.getProperty("AwsEventGenerator.peak_boost"),
					2
				);
			} else {
				envPeakBoostFactor = -1;
			}
		}
		
		return envPeakBoostFactor;
	}

	public static boolean isPeakBoost() {
		return (envPeakBoostFactor() > 0);
	}
}
