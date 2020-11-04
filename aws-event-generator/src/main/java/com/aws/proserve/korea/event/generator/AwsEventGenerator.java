package com.aws.proserve.korea.event.generator;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
//import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aws.proserve.korea.event.generator.boosting.BoostParams;
import com.aws.proserve.korea.event.generator.configuration.AwsEventGeneratorConfigurationUtils;
import com.aws.proserve.korea.event.generator.configuration.BasicConfigurationUtils;
import com.aws.proserve.korea.event.generator.exception.SomethingWrongException;
import com.aws.proserve.korea.event.generator.messagefile.MessageFile;
import com.aws.proserve.korea.event.generator.metric.AwsEventGeneratorMetric;
import com.aws.proserve.korea.event.generator.performance.BoostState;
import com.aws.proserve.korea.event.generator.performance.DailyBoostPeriodsProvider;
import com.aws.proserve.korea.event.generator.threading.ActiveTasksThreadPoolExecutor;
import com.aws.proserve.korea.event.generator.threading.AwsEventGeneratorTask;
import com.aws.proserve.korea.event.generator.threading.DailyTransitionerJob;
import com.aws.proserve.korea.event.generator.threading.MonitorThread;
import com.aws.proserve.korea.event.generator.threading.RejectedExecutionHandlerImpl;
import com.aws.proserve.korea.event.generator.threading.Result;
import com.aws.proserve.korea.event.generator.threading.UserInputHandler;
import com.aws.proserve.korea.event.generator.utils.AwsEventGeneratorFileUtils;
import com.aws.proserve.korea.event.generator.utils.AwsEventGeneratorUtils;
import com.aws.proserve.korea.event.generator.utils.ConversionUtils;
import com.aws.proserve.korea.event.generator.utils.LoggerUtils;
import com.aws.proserve.korea.event.generator.view.ConsoleView;
import com.aws.proserve.korea.event.generator.view.StatisticsView;


public class AwsEventGenerator {

	public class Configs {
		public static final String CONFIG_KEY_SYSLOG_SERVER = "Syslog_Server";
		public static final String CONFIG_DEFAULT_VALUE_SYSLOG_SERVER = "localhost";
		
		public static final String CONFIG_KEY_SYSLOG_PORT = "Syslog_Port";
		public static final int CONFIG_DEFAULT_VALUE_SYSLOG_PORT = 514;
		
		public static final String CONFIG_KEY_SYSLOG_MESSAGE_HOST_NAME = "Syslog_Message_Host_Name";
		public static final String CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_HOST_NAME = "Event_Generator_Host";
		public static final String CONFIG_KEY_SYSLOG_MESSAGE_APP_NAME = "Syslog_Message_App_Name";
		public static final String CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_APP_NAME = "Event_Generator";
		public static final String CONFIG_KEY_SYSLOG_MESSAGE_FACILITY = "Syslog_Message_Facility";
		public static final String CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_FACILITY = "USER";
		public static final String CONFIG_KEY_SYSLOG_MESSAGE_SEVERITY = "Syslog_Message_Severity";
		public static final String CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_SEVERITY = "INFORMATIONAL";
		
		public static final String CONFIG_KEY_MESSAGE_FILES_BUNDLE_FILE_PATH = "Message_Files_Bundle_Path";
		public static final String CONFIG_DEFAULT_VALUE_MESSAGE_FILES_BUNDLE_FILE_PATH = "message_files_bundle.txt";

		public static final String CONFIG_KEY_NEW_LINE = "New_Line";
		
		public static final String CONFIG_KEY_MESSAGE_FILES_BUNDLE_DELIMITER = "Message_Files_Bundle_Delimiter";
		public static final String CONFIG_DEFAULT_VALUE_MESSAGE_FILES_BUNDLE_DELIMITER = ",";
		
		public static final String CONFIG_KEY_MESSAGE_FILES_BUNDLE_COMMENT_LINE_CHAR = "Message_Files_Bundle_Comment_Line_Char";
		public static final String CONFIG_DEFAULT_VALUE_MESSAGE_FILES_BUNDLE_COMMENT_LINE_CHAR = "#";
		
		public static final String CONFIG_KEY_RESULT_OUTPUT_FILE_DELIMITER = "Result_Output_File_Delimiter";
		public static final String CONFIG_DEFAULT_VALUE_RESULT_OUTPUT_FILE_DELIMITER = ",";
		
		public static final String CONFIG_KEY_CACHE_MESSAGE = "Cache_Message";
		public static final String CONFIG_DEFAULT_VALUE_CACHE_MESSAGE = "true";
		
		public static final String CONFIG_KEY_RESULT_OUTPUT_FILE_PATH = "Result_Output_File_Path";
		public static final String CONFIG_DEFAULT_VALUE_RESULT_OUTPUT_FILE_PATH = "result_output.txt";
		
		public static final String CONFIG_KEY_MAX_THREAD_COUNT = "Max_Thread_Count";
		public static final String CONFIG_DEFAULT_VALUE_MAX_THREAD_COUNT = "100";
		
		public static final String CONFIG_KEY_USE_MONITOR_THREAD = "Use_Monitor_Thread";
		public static final String CONFIG_DEFAULT_VALUE_USE_MONITOR_THREAD = "true";
		
		public static final String CONFIG_KEY_REPLACE_TEXT_DELIMIETER = "Replace_Text_Delimiter";
		// [2018-10-30] Kim, Sang Hyoun: Using short GUID - http://www.shortguid.com/#/guid/uid-64
//		public static final String CONFIG_DEFAULT_VALUE_REPLACE_TEXT_DELIMIETER = "\\|";
		public static final String CONFIG_DEFAULT_VALUE_REPLACE_TEXT_DELIMIETER = "{Cw0GBA0NAwQ}";
		
		private static final String CONFIG_KEY_DAILY_PEAK_BOOST_PERIODS = "Daily_Peak_Boost_Periods";
		private static final String CONFIG_DEFAULT_VALUE_DAILY_PEAK_BOOST_PERIODS = "";
		
		private static final String CONFIG_KEY_DAILY_OVER_BOOST_PERIODS = "Daily_Peak_Boost_Periods";
		private static final String CONFIG_DEFAULT_VALUE_DAILY_OVER_BOOST_PERIODS = "";

		private static final String CONFIG_KEY_BOOST_WAIT_COEFFICIENT = "Boost_Wait_Coefficient";
		private static final double CONFIG_DEFAULT_VALUE_BOOST_WAIT_COEFFICIENT = 1.0;
		
		private static final String CONFIG_KEY_BOOST_BASELINED_EPS_THRESHOLD = "Boost_Baselined_EPS_Threshold";
		private static final double CONFIG_DEFAULT_VALUE_BOOST_BASELINED_EPS_THRESHOLD = 50.0;
		
		public static final String CONFIG_KEY_DAILY_SENT_COUNT_LIMIT = "Daily_Sent_Count_Limit";
		public static final long CONFIG_DEFAULT_VALUE_DAILY_SENT_COUNT_LIMIT = 805306368l;
		public static final String CONFIG_KEY_DAILY_SENT_BYTES_RAW_LIMIT = "Daily_Sent_Bytes_Raw_Limit";
		public static final long CONFIG_DEFAULT_VALUE_DAILY_SENT_BYTES_RAW_LIMIT = 322122547200l;
		public static final String CONFIG_KEY_DAILY_SENT_BYTES_ENVELOPED_LIMIT = "Daily_Sent_Bytes_Enveloped_Limit";
		public static final long CONFIG_DEFAULT_VALUE_DAILY_SENT_BYTES_ENVELOPED_LIMIT = 322122547200l;
		
		public static final String CONFIG_KEY_DAILY_TRANSITION_CRON_EXPRESSION = "Daily_Transition_Cron_Expression";
		public static final String CONFIG_DEFAULT_VALUE_DAILY_TRANSITION_CRON_EXPRESSION = "59 59 23 * * ? *";
	
		private static final String CONFIG_KEY_BOOST_FACTOR = "Boost_Factor";
		private static final double CONFIG_DEFAULT_VALUE_BOOST_FACTOR = 2.0d;
		
		private static final String CONFIG_KEY_SORT_BY_EPS = "Sort_By_ESP";
		private static final boolean CONFIG_DEFAULT_VALUE_SORT_BY_EPS = false;
		private static final String CONFIG_KEY_QUOTE_CHAR = "Quote_Char";
		private static final String CONFIG_DEFAULT_VALUE_QUOTE_CHAR = "\"";
		
		
		public String getConfigFilePath() {
			return AwsEventGenerator.getInstance().getConfigFilePath();
		}
		
		public String getMessageFilesBundlePath() {
			// [2018-10-16] Kim, Sang Hyoun
			// Hack to resolve NPE raising with uninitialized properties configuration.
			if (propsConfiguration != null) {
				return getPropertiesConfiguration().getString(
					CONFIG_KEY_MESSAGE_FILES_BUNDLE_FILE_PATH,
					CONFIG_DEFAULT_VALUE_MESSAGE_FILES_BUNDLE_FILE_PATH
				);
			} else {
				return CONFIG_DEFAULT_VALUE_MESSAGE_FILES_BUNDLE_FILE_PATH;
			}
		}

		public String getNewLine() {
//			return getPropertiesConfiguration().getString(
//				CONFIG_KEY_NEW_LINE,
//				AwsEventGeneratorFileUtils.NEW_LINE
//			);
			
			try {
				return getPropertiesConfiguration().getString(CONFIG_KEY_NEW_LINE);
			} catch (Exception e) {
				return null;
			}
		}

		public String getMessageFilesBundleParamDelimiter() {
			return getPropertiesConfiguration().getString(
				CONFIG_KEY_MESSAGE_FILES_BUNDLE_DELIMITER,
				CONFIG_DEFAULT_VALUE_MESSAGE_FILES_BUNDLE_DELIMITER
			);
		}
		
		public String getResultOutputFileDelimiter() {
			return getPropertiesConfiguration().getString(
				CONFIG_KEY_RESULT_OUTPUT_FILE_DELIMITER,
				CONFIG_DEFAULT_VALUE_RESULT_OUTPUT_FILE_DELIMITER
			);
		}

		public String discoverNewLineChar() {
			String newLine = getNewLine();

			if (newLine == null) {
				if (isFileWindows()) {
					newLine = "\r\n";
				} else if (isFileUnix()) {
					newLine = "\n";
				} else if (isFileMac()) {
					newLine = "\r";
				} else {
					newLine = AwsEventGeneratorFileUtils.NEW_LINE;
				}
			}

			if (newLine == null) {
				newLine = AwsEventGeneratorFileUtils.NEW_LINE;
			}

			return newLine;
		}

		private boolean isFileWindows() {
			String filePath = this.getConfigFilePath();
			return AwsEventGeneratorFileUtils.isWindowsFormat(filePath);
		}

		private boolean isFileUnix() {
			String filePath = this.getConfigFilePath();
			return AwsEventGeneratorFileUtils.isUnixFormat(filePath);
		}

		private boolean isFileMac() {
			String filePath = this.getConfigFilePath();
			return AwsEventGeneratorFileUtils.isMacFormat(filePath);
		}

		/**
		 * Return global syslog port to send message to.
		 * @return
		 */
		public int getSyslogPort() {
			return getPropertiesConfiguration().getInt(
				CONFIG_KEY_SYSLOG_PORT,
				CONFIG_DEFAULT_VALUE_SYSLOG_PORT
			);
		}

		public String getSyslogServer() {
			return getPropertiesConfiguration().getString(CONFIG_KEY_SYSLOG_SERVER);
		}

		public String getMessageFilesBundleCommentLineChar() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_MESSAGE_FILES_BUNDLE_COMMENT_LINE_CHAR,
				Configs.CONFIG_DEFAULT_VALUE_MESSAGE_FILES_BUNDLE_COMMENT_LINE_CHAR
			);
		}
		
		public boolean isCacheMessage() {
			return getPropertiesConfiguration().getBoolean(
				Configs.CONFIG_KEY_CACHE_MESSAGE,
				true
			);
		}

		public String getResultOutputFilePath() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_RESULT_OUTPUT_FILE_PATH,
				Configs.CONFIG_DEFAULT_VALUE_RESULT_OUTPUT_FILE_PATH
			);
		}
		
		public int getThreadCount() {
			return getPropertiesConfiguration().getInt(
				Configs.CONFIG_KEY_MAX_THREAD_COUNT,
				-1
			);
		}
		
		public boolean isUseMonitorThread() {
			return getPropertiesConfiguration().getBoolean(
				Configs.CONFIG_KEY_USE_MONITOR_THREAD,
				true
			);
		}
		
		public boolean isVerbose() {
//			return getPropertiesConfiguration().getBoolean(
//				Configs.CONFIG_KEY_VERBOSE,
//				false
//			);
			
			// [2018-10-16] Kim, Sang Hyoun: Verbose moved into command params.
			return commandParams.isVerbose();
		}

		public String getReplaceTextDelimiter() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_REPLACE_TEXT_DELIMIETER,
				Configs.CONFIG_DEFAULT_VALUE_REPLACE_TEXT_DELIMIETER
			);
		}

		public String getSyslogMessageHostName() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_SYSLOG_MESSAGE_HOST_NAME,
				Configs.CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_HOST_NAME
			);
		}
		
		public String getSyslogMessageAppName() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_SYSLOG_MESSAGE_APP_NAME,
				Configs.CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_APP_NAME
			);
		}

		public String getSyslogMessageFacility() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_SYSLOG_MESSAGE_FACILITY,
				Configs.CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_FACILITY
			);
		}

		public String getSyslogMessageSeverity() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_SYSLOG_MESSAGE_SEVERITY,
				Configs.CONFIG_DEFAULT_VALUE_SYSLOG_MESSAGE_SEVERITY
			);
		}

		public String getDailyPeakBoostPeriods() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_DAILY_PEAK_BOOST_PERIODS,
				Configs.CONFIG_DEFAULT_VALUE_DAILY_PEAK_BOOST_PERIODS
			);
		}
		
		public String getDailyOverBoostPeriods() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_DAILY_OVER_BOOST_PERIODS,
				Configs.CONFIG_DEFAULT_VALUE_DAILY_OVER_BOOST_PERIODS
			);
		}

		public double getBoostWaitCoefficient() {
			return getPropertiesConfiguration().getDouble(
				Configs.CONFIG_KEY_BOOST_WAIT_COEFFICIENT,
				Configs.CONFIG_DEFAULT_VALUE_BOOST_WAIT_COEFFICIENT
			);
		}

		public double getBoostBaselinedEPSThreshold() {
			return getPropertiesConfiguration().getDouble(
				Configs.CONFIG_KEY_BOOST_BASELINED_EPS_THRESHOLD,
				Configs.CONFIG_DEFAULT_VALUE_BOOST_BASELINED_EPS_THRESHOLD
			);
		}

		public long getDailySentCountLimit() {
			return getPropertiesConfiguration().getLong(
				Configs.CONFIG_KEY_DAILY_SENT_COUNT_LIMIT,
				0
			);
		}

		public long getDailySentBytesRawLimit() {
			String sentBytesRawLimitStr = getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_DAILY_SENT_BYTES_RAW_LIMIT,
				"0"
			);
			
			if (StringUtils.isNumeric(sentBytesRawLimitStr)) {
				return NumberUtils.toLong(sentBytesRawLimitStr);
			} else {
				try {
					return ConversionUtils.parseAny(sentBytesRawLimitStr);
				} catch (Exception e) {
					LoggerUtils.getLogger().warn(
						String.format(
							"Config [%s] has invalid value [%s].",
							Configs.CONFIG_KEY_DAILY_SENT_BYTES_RAW_LIMIT,
							sentBytesRawLimitStr
						)
					);
					
					// Return 0 upon any error.
					return 0;
				}
			}
		}

		public long getDailySentBytesEnvelopedLimit() {
			String sentBytesRawEnvelopedStr = getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_DAILY_SENT_BYTES_ENVELOPED_LIMIT,
				"0"
			);
			
			if (StringUtils.isNumeric(sentBytesRawEnvelopedStr)) {
				return NumberUtils.toLong(sentBytesRawEnvelopedStr);
			} else {
				try {
					return ConversionUtils.parseAny(sentBytesRawEnvelopedStr);
				} catch (Exception e) {
					LoggerUtils.getLogger().warn(
						String.format(
							"Config [%s] has invalid value [%s].",
							Configs.CONFIG_KEY_DAILY_SENT_BYTES_ENVELOPED_LIMIT,
							sentBytesRawEnvelopedStr
						)
					);
					
					// Return 0 upon any error.
					return 0;
				}
			}
		}

		public String getDailySentCountLimitDisplayString() {
			String sentCountStr = getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_DAILY_SENT_COUNT_LIMIT,
				"0 (no limit or not set)"
			);
			
			return sentCountStr;
		}

		public String getDailySentBytesRawLimitDisplayString() {
			String sentBytesRawRawStr = getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_DAILY_SENT_BYTES_RAW_LIMIT,
				"0 (no limit or not set)"
			);
			
			return sentBytesRawRawStr;
		}

		public String getDailySentBytesEnvelopedLimitDisplayString() {
			String sentBytesRawEnvelopedStr = getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_DAILY_SENT_BYTES_ENVELOPED_LIMIT,
				"0 (no limit or not set)"
			);
			
			return sentBytesRawEnvelopedStr;
		}

		public String getDailyTransitionCronExpression() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_DAILY_TRANSITION_CRON_EXPRESSION,
				Configs.CONFIG_DEFAULT_VALUE_DAILY_TRANSITION_CRON_EXPRESSION
			);
		}

		public double getBoostFactor() {
			return getPropertiesConfiguration().getDouble(
				Configs.CONFIG_KEY_BOOST_FACTOR,
				Configs.CONFIG_DEFAULT_VALUE_BOOST_FACTOR
			);
		}

		public boolean isSortByEPS() {
			return getPropertiesConfiguration().getBoolean(
				Configs.CONFIG_KEY_SORT_BY_EPS,
				Configs.CONFIG_DEFAULT_VALUE_SORT_BY_EPS
			);
		}

		public char getQuoteChar() {
			return getPropertiesConfiguration().getString(
				Configs.CONFIG_KEY_QUOTE_CHAR,
				Configs.CONFIG_DEFAULT_VALUE_QUOTE_CHAR
			).charAt(0);
		}


	}
	
	public class CommandParams {
		public boolean isVerbose() {
			return verbose;
		}

		public void setVerbose(boolean isVerbose) {
			verbose = true;
		}
	}
	
//	public class MessageFilesBundleParams {
//		
//		
//	}
	
	public class MessageFilesBundleParams {
		private int messageFilesBundleLineCount = 0;
		
		public String getMessageFilesBundleFilePath() {
			return AwsEventGenerator.getInstance().getConfiguration().getMessageFilesBundlePath();
		}
		
		public int getMessageFilesBundleLineCount() throws IOException {
			if (messageFilesBundleLineCount > 0)
				return messageFilesBundleLineCount;

			String messageFilesBundlePath = AwsEventGenerator.getInstance().getConfiguration().getMessageFilesBundlePath();
			if (messageFilesBundlePath == null) {
				// Bogus case.
				throw new IOException("Message files bundle file path null: [" + messageFilesBundlePath + "]");
			}

			if (!new File(messageFilesBundlePath).exists()) {
				throw new IOException(
						"The message files bundle file [" + messageFilesBundlePath + "] does not exist!");
			}

			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(messageFilesBundlePath));
				String line = null;
				while ((line = in.readLine()) != null) {
					if (!StringUtils.startsWith(line, "#")) {
						messageFilesBundleLineCount++;
					}
				}

				return messageFilesBundleLineCount;
			} finally {
				if (in != null)
					in.close();
			}
		}
		
		String[] getAllColumnValues(String paramLine, String delimiter) {
			// Split line with delimiter.
			// IMPORTANT: Change to Apache Commons Text's StringTokenizer to handle quoted string.
//			String[] colData = StringUtils.splitByWholeSeparatorPreserveAllTokens(
//				paramLine,
//				delimiter
//			);
			
			StringTokenizer tokenizer = new StringTokenizer(
				paramLine,
				delimiter
			);
			tokenizer.setIgnoreEmptyTokens(false);
			tokenizer.setQuoteChar(
				AwsEventGenerator.getInstance()
					.getConfiguration().getQuoteChar()
			);
			
			String[] colData = tokenizer.getTokenArray();
			
			return colData;
		}

		MessageFile messageFileByParamLine(
			String paramLine,
			long batchNumber,
			int slotId,
			String delimiter
		) {
			// [2018-11-07] Sang Hyoun: Treat null or empty lines.
			if (StringUtils.isBlank(paramLine)) {
				MessageFile messageFile = new MessageFile(true);
				return messageFile;
			}
			
			try {
				String[] columnValues = getAllColumnValues(
					paramLine,
					delimiter
				);
				
				int colIdx = 0;
				
				String messageFilePath = columnValues[colIdx++];
				String replaceSourcesText = columnValues[colIdx++];
				String replaceTargetsText = columnValues[colIdx++];
				String syslogServer = columnValues[colIdx++];
				
				int syslogPort = -1;
				String syslogPortStr = columnValues[colIdx++];
				if (StringUtils.isNotBlank(syslogPortStr)) {
					try {
						syslogPort = Integer.parseInt(syslogPortStr);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				String initialSendIntervalMillisStr = columnValues[colIdx++];
				int initialSendIntervalMillis = 100;
				if (StringUtils.isNotBlank(initialSendIntervalMillisStr)) {
					try {
						initialSendIntervalMillis = Integer.parseInt(initialSendIntervalMillisStr);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				String targetSendIntervalMillisStr = columnValues[colIdx++];
				int targetSendIntervalMillis = 1;
				if (StringUtils.isNotBlank(targetSendIntervalMillisStr)) {
					try {
						targetSendIntervalMillis = Integer.parseInt(targetSendIntervalMillisStr);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				String warmUpPeriodSecStr = columnValues[colIdx++];
				int warmUpPeriodSec = 60;
				if (StringUtils.isNotBlank(warmUpPeriodSecStr)) {
					try {
						warmUpPeriodSec = Integer.parseInt(warmUpPeriodSecStr);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				String isRepeatStr = columnValues[colIdx++];
				boolean isRepeat = true;
				isRepeat = BooleanUtils.toBoolean(isRepeatStr);
				
				String isCacheMessageStr = columnValues[colIdx++];
				boolean isCacheMessage = true;
				isCacheMessage = BooleanUtils.toBoolean(isCacheMessageStr);
				// Combine with the global cache setting.
				// Each message cache will be enabled only if 
				// global cache is enabled and it is set on each message line.
				isCacheMessage &= AwsEventGenerator.getInstance().getConfiguration().isCacheMessage();
				
				String syslogMessageHostName = columnValues[colIdx++];
				if (StringUtils.isBlank(syslogMessageHostName)) {
//					syslogMessageHostName = AwsEventGenerator.getInstance().getConfiguration().getSyslogMessageHostName();
					syslogMessageHostName = null;
				}
				
				String syslogAppName = columnValues[colIdx++];
				if (StringUtils.isBlank(syslogAppName)) {
					syslogAppName = null;
				}
				
				String syslogFacility = columnValues[colIdx++];
				if (StringUtils.isBlank(syslogFacility)) {
					syslogFacility = AwsEventGenerator.getInstance().getConfiguration().getSyslogMessageFacility();
				}

				String syslogSeverity = columnValues[colIdx++];
				if (StringUtils.isBlank(syslogSeverity)) {
					syslogSeverity = AwsEventGenerator.getInstance().getConfiguration().getSyslogMessageSeverity();;
				}
				
				String syslogPrependPRIPartStr = columnValues[colIdx++];
				boolean syslogPrependPRIPart = false;
				if (!StringUtils.isBlank(syslogPrependPRIPartStr)) {
					syslogPrependPRIPart = BooleanUtils.toBoolean(syslogPrependPRIPartStr);
				}
				
				String syslogPrependHeaderTimestampStr = columnValues[colIdx++];
				boolean syslogPrependHeaderTimestamp = false;
				if (!StringUtils.isBlank(syslogPrependHeaderTimestampStr)) {
					syslogPrependHeaderTimestamp = BooleanUtils.toBoolean(syslogPrependHeaderTimestampStr);
				}
				
				String syslogPrependHeaderHostnameStr = columnValues[colIdx++];
				boolean syslogPrependHeaderHostname = false;
				if (!StringUtils.isBlank(syslogPrependHeaderHostnameStr)) {
					syslogPrependHeaderHostname = BooleanUtils.toBoolean(syslogPrependHeaderHostnameStr);
				}

				String syslogPrependHeaderAppNameStr = columnValues[colIdx++];
				boolean syslogPrependHeaderAppName = false;
				if (!StringUtils.isBlank(syslogPrependHeaderAppNameStr)) {
					syslogPrependHeaderAppName = BooleanUtils.toBoolean(syslogPrependHeaderAppNameStr);
				}
				
				// [2018-11-02] SH: Daily peak boost periods provider.
				String dailyPeakBoostPeriodsString = columnValues[colIdx++];
				String dailyOverBoostPeriodsString = columnValues[colIdx++];
				DailyBoostPeriodsProvider peakBoostPeriodsProvider =
					DailyBoostPeriodsProvider.fromString(
						dailyPeakBoostPeriodsString,
						dailyOverBoostPeriodsString
					);
				assert(peakBoostPeriodsProvider != null);
				if (peakBoostPeriodsProvider == null ||
					peakBoostPeriodsProvider.getDailyPeakBoostPeriodsList() == null ||
					peakBoostPeriodsProvider.getDailyPeakBoostPeriodsList().isEmpty()
				) {
					// Fallback to global one.
					peakBoostPeriodsProvider.setDailyPeakBoostPeriodsList(
						AwsEventGenerator.getInstance()
							.getDailyPeakBoostPeriodsProvider()
							.getDailyPeakBoostPeriodsList()
						);
				}
				if (peakBoostPeriodsProvider == null ||
					peakBoostPeriodsProvider.getDailyOverBoostPeriodsList() == null ||
					peakBoostPeriodsProvider.getDailyOverBoostPeriodsList().isEmpty()
				) {
					peakBoostPeriodsProvider.setDailyOverBoostPeriodsList(
						AwsEventGenerator.getInstance()
							.getDailyPeakBoostPeriodsProvider()
							.getDailyOverBoostPeriodsList()
						);
					}
				
				// [2018-11-05] SH: Use global value for boost baselined EPS.
				double boostBaselinedEPSThreshold = AwsEventGenerator.getInstance()
					.getConfiguration().getBoostBaselinedEPSThreshold();
				
				// [2018-11-07] Sang Hyoun: Boost params from message files bundle.
				String boostParamsStr = columnValues[colIdx++];
				BoostParams boostParams = null;
				if (StringUtils.isNotBlank(boostParamsStr)) {
					boostParams = BoostParams.fromString(boostParamsStr);
				}
				
				// [2020-09-19] KSH: Kafka properties.
				String kafkaBootstrapServers = columnValues[colIdx++];
				if (StringUtils.isBlank(kafkaBootstrapServers)) {
					kafkaBootstrapServers = null;
				}
				String kafkaTopic = columnValues[colIdx++];
				if (StringUtils.isBlank(kafkaTopic)) {
					kafkaTopic = null;
				}
				String kafkaClientIdPrefix = columnValues[colIdx++];
				if (StringUtils.isBlank(kafkaClientIdPrefix)) {
					kafkaClientIdPrefix = null;
				}
					
				return new MessageFile(
					batchNumber,
					slotId,
					messageFilePath,
					replaceSourcesText,
					replaceTargetsText,
					syslogServer,
					syslogPort,
					initialSendIntervalMillis,
					targetSendIntervalMillis,
					warmUpPeriodSec,
					isRepeat,
					isCacheMessage,
					syslogMessageHostName,
					syslogAppName,
					syslogFacility,
					syslogSeverity,
					syslogPrependPRIPart,
					syslogPrependHeaderTimestamp,
					syslogPrependHeaderHostname,
					syslogPrependHeaderAppName,
					peakBoostPeriodsProvider,
					boostBaselinedEPSThreshold,
					boostParams,
					kafkaBootstrapServers,
					kafkaTopic,
					kafkaClientIdPrefix
				);
			} catch (Exception e) {
				// Upon any exception, just return one with empty info.
				e.printStackTrace();
				
				LoggerUtils.getLogger().error(
					"ERROR: " + e.getLocalizedMessage()
				);
				
				throw e;
			}
		}

		public String discoverNewLineChar() {
			String newLine = getNewLine();
			
			if (newLine == null) {
				if (isFileWindows()) {
					newLine = "\r\n";
				} else if (isFileUnix()) {
					newLine = "\n";
				} else if (isFileMac()) {
					newLine = "\r";
				} else {
					newLine = AwsEventGeneratorUtils.NEW_LINE;
				}
			}
			
			if (newLine == null) {
				newLine = AwsEventGeneratorUtils.NEW_LINE;
			}
			
			return newLine;
		}
		
		private boolean isFileWindows() {
			String filePath = this.getMessageFilesBundleFilePath();
			return AwsEventGeneratorUtils.isWindowsFormat(filePath);
		}

		private boolean isFileUnix() {
			String filePath = getMessageFilesBundleFilePath();
			return AwsEventGeneratorUtils.isUnixFormat(filePath);
		}

		private boolean isFileMac() {
			String filePath = getMessageFilesBundleFilePath();
			return AwsEventGeneratorUtils.isMacFormat(filePath);
		}

		private String getNewLine() {
			try {
				String newLine = AwsEventGenerator.getInstance().getConfiguration().getNewLine();
				return newLine;
			} catch (Exception e) {
				return null;
			}
		}
	}
	
	public interface IMessageFilesBundle {
		void open(String filePath) throws IOException;
		void close() throws IOException;
		boolean available() throws IOException;
		Object acquire() throws IOException;
		void mark(int readAheadLimit) throws IOException;
		void reset() throws IOException;
	}
	
	public class MessageFilesBundle implements IMessageFilesBundle {
		// TODO: Move codes from below to MessageFile implementer.
		BufferedReader in = null;
		
		public void open(String filePath) throws FileNotFoundException {
//			in = new BufferedReader(
//				new FileReader(AwsEventGenerator.getInstance().getConfiguration().getMessageFilesBundlePath())
//			);
			in = new BufferedReader(
				new FileReader(filePath)
			);
		}
		
		public void close() {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				in = null;
			}
		}
		
		public boolean available() throws IOException {
			if (in == null) {
				return false;
			}
			
			// Test the next line if it is EOF.
			in.mark(1024);
			try {
				if (in.readLine() == null) {
					return false;
				} else {
					return true;
				}
			} finally {
				in.reset();
			}
		}

		@Override
		public Object acquire() throws IOException {
			if (in == null) {
				return null;
			}
			
			String line = null;
			line = in.readLine();
			if (line == null) {
				return null;
			}
			
			line = line.trim();
//			if (line.startsWith(Configs.CONFIG_VALUE_MESSAGE_FILES_BUNDLE_COMMENT_LINE_CHAR)) {
			if (line.startsWith(configs.getMessageFilesBundleCommentLineChar())) {
				return new MessageFile();
			}
			
			MessageFile messageFile = messageFilesBundleParams.messageFileByParamLine(
				line,
				0,		// Batch number which will be set later.
				-1,		// Will be a Slot ID, which will be set later.
				configs.getMessageFilesBundleParamDelimiter()
			);
			
			return messageFile;
		}

		@Override
		public void mark(int readAheadLimit) throws IOException {
			if (in != null) {
				in.mark(readAheadLimit);
			}
		}

		@Override
		public void reset() throws IOException {
			if (in != null) {
				in.reset();
			}
		}
		
	}
	
	/*
	public static class Result implements Comparable<Result> {

		private Integer pos = null;
		private String messageFilePath = null;
		private boolean success = true;
		private String resultMessage = null;
		private Object data = null;
		private String delimiter = ",";
		private String newLineChar = AwsEventGeneratorUtils.NEW_LINE;
		
		public Result() {
			
		}

		public Result(Integer pos, String messageFilePath, boolean success, String resultMessage, Object data,
				String delimiter, String newLineChar) {
			super();
			this.pos = pos;
			this.messageFilePath = messageFilePath;
			this.success = success;
			this.resultMessage = resultMessage;
			this.data = data;
			this.delimiter = delimiter;
			this.newLineChar = newLineChar;
		}
		
		@Override
		public int compareTo(Result o) {
			if (o == null) {
				return 1;
			}
			
			return pos.compareTo(o.getPos());
		}

		public Integer getPos() {
			return pos;
		}

		public void setPos(Integer pos) {
			this.pos = pos;
		}

		public String getMessageFilePath() {
			return messageFilePath;
		}

		public void setMessageFilePath(String messageFilePath) {
			this.messageFilePath = messageFilePath;
		}

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public String getResultMessage() {
			return resultMessage;
		}

		public void setResultMessage(String resultMessage) {
			this.resultMessage = resultMessage;
		}

		public Object getData() {
			return data;
		}

		public void setData(Object data) {
			this.data = data;
		}

		public String getDelimiter() {
			return delimiter;
		}

		public void setDelimiter(String delimiter) {
			this.delimiter = delimiter;
		}

		public String getNewLineChar() {
			return newLineChar;
		}

		public void setNewLineChar(String newLineChar) {
			this.newLineChar = newLineChar;
		}
		
		public String makeLine() {
			return makeLine(true);
		}
		
		public String makeLine(boolean newLine) {
			StringBuffer sb = new StringBuffer();
			
			sb.append(getMessageFilePath());
			if (newLine) {
				sb.append(newLineChar);
			}
			
			return sb.toString();
		}
	}
	*/
	
	public static class ResultComparator implements Comparator<Result> {

		@Override
		public int compare(Result o1, Result o2) {
			if (o1 == null && o2 != null) {
				return -1;
			}
			
			if (o1 != null && o2 == null) {
				return 1;
			}
			
			if (o1 == null && o2 == null) {
				return 0;
			}
			
			return o1.compareTo(o2);
		}
		
	}
	
	public interface IResultSink {
		void open() throws IOException;
		void close() throws IOException;
		void drain(Object item);
		void flush() throws IOException;
		void discard();
	}
	
	public class ResultOutput implements IResultSink {
		private BufferedWriter out = null;
		private List<Result> results = new ArrayList<Result>();

		@Override
		public void open() throws IOException {
			String orgFilePath = AwsEventGenerator.getInstance()
				.getConfiguration().getResultOutputFilePath();
			// Manipulate to reflect current datetime.
			String fullPath = FilenameUtils.getFullPath(orgFilePath);
			String baseName = FilenameUtils.getBaseName(orgFilePath);
			Calendar calNow = Calendar.getInstance();
			baseName = baseName + "_" + String.format(
				"%04d%02d%02d-%02d%02d%02d",
//				calNow.getTime(),
				calNow.get(Calendar.YEAR),
				calNow.get(Calendar.MONTH) + 1,
				calNow.get(Calendar.DAY_OF_MONTH),
				calNow.get(Calendar.HOUR_OF_DAY),
				calNow.get(Calendar.MINUTE),
				calNow.get(Calendar.SECOND)
			);
			
			String filePath = fullPath + baseName + "." +
				FilenameUtils.getExtension(orgFilePath);
			
			out = new BufferedWriter(
				new FileWriter(
					filePath
				)
			);
		}

		@Override
		public void close() throws IOException {
			if (out != null) {
				out.close();
			}
		}

		@Override
		public void drain(Object item) {
			if (item != null && item instanceof Result) {
				synchronized (this) {
					results.add((Result)item);
				}
			}
		}

		@Override
		public void flush() throws IOException {
			// Write the result list to file.
			if (results != null) {
				// Sort the list with respect to position.
				Collections.sort(results, new ResultComparator());
				
				int resultCount = results.size();
				for (int i = 0; i < resultCount; i++) {
					Result result = results.get(i);
					String line = result.makeLine();
					out.write(line);
					out.flush();
				}
				
				// Clear the result list.
				results.clear();
				
				LoggerUtils.getLogger().info(
//					"[{}] [DUMPED {} RESULTS TO '{}']",
					"[DUMPED {} RESULTS TO '{}']",
//					batchNumber,
					resultCount,
					AwsEventGenerator.getInstance().getConfiguration().getResultOutputFilePath()
				);
			}
		}

		@Override
		public void discard() {
			if (results != null) {
				results.clear();
			}			
		}
	}

	public static final String XRaySegmentName = "AWS Event Generator";

	public ConsoleView statisticsView = null;
	
	public class Threading {
		public RejectedExecutionHandlerImpl rejectionHandler;
		public ThreadFactory threadFactory;
		// [2018-10-18] Kim, Sang Hyoun: Changed to custom ActiveTasksThreadPoolExecutor.
//		public ThreadPoolExecutor executorPool;
		public ActiveTasksThreadPoolExecutor executorPool;
		public MonitorThread monitor;
//		public SyslogSendTask[] workers;
		// [2018-10-08] Kim, Sang Hyoun: Change to array list.
//		public List<SyslogSendTask> workers = new ArrayList<SyslogSendTask>();
		private UserInputHandler inputHandler;
		private Scheduler scheduler;
		private JobDetail job;
		private CronTrigger trigger;
		
		public void prepareThreads() throws IOException {
			// RejectedExecutionHandler implementation.
			rejectionHandler = new RejectedExecutionHandlerImpl();
			
			// ThreadFactory.
			threadFactory = Executors.defaultThreadFactory();
			
//			int corePoolSize = 10;
//			corePoolSize = configs.getConcurrentSlots();
			
			// Check thread count.
//			if (threadCount < 0) {
//				threadCount = messageFilesBundleParams.getMessageFilesBundleLineCount();
//			}
////			if (threadCount < corePoolSize) {
////				threadCount = corePoolSize;
////			}
			int maxThreadCount = AwsEventGenerator.getInstance().getConfiguration().getThreadCount();
			if (maxThreadCount <= 0) {
				maxThreadCount = messageFilesBundleParams.getMessageFilesBundleLineCount();
				
				if (maxThreadCount <= 0) {
					throw new IOException(
						"Message files bundle file " +
						AwsEventGenerator.getInstance().getConfiguration().getMessageFilesBundlePath() + 
						" has line count less than or equal to 0 (excluding comment lines starting '#')." +
						AwsEventGeneratorFileUtils.NEW_LINE +
						"Please ensure that message files bundle file has valid bundle item."
					);
				}
				
				LoggerUtils.getLogger().info(
					"[Max_Thread_Count in config equals to or less than 0. Adjusted to {} to match with message bundle line count.]",
					maxThreadCount
				);
			} else {
				if (maxThreadCount < messageFilesBundleParams.getMessageFilesBundleLineCount()) {
					maxThreadCount = messageFilesBundleParams.getMessageFilesBundleLineCount();
					LoggerUtils.getLogger().warn(
						"[Max_Thread_Count in less than message bundle lines. Adjusted to {} to match with message bundle line count.]",
						maxThreadCount
					);	
				}
			}
			
			// Create ThreadPoolExecutor.
			// [2018-10-18] Kim, Sang Hyoun: ActiveTasksThreadPoolExecutor.
//			executorPool = new ThreadPoolExecutor(
//				threadCount,			// Initial thread pool size.
//				threadCount,			// Maximum thread pool size.
//				10,
//				TimeUnit.SECONDS,
//				new ArrayBlockingQueue<Runnable>(threadCount),
//				threading.threadFactory,
//				threading.rejectionHandler
//			);
			executorPool = new ActiveTasksThreadPoolExecutor(
//				messageFilesBundleParams.getMessageFilesBundleLineCount(),			// Initial thread pool size.
				maxThreadCount,
				maxThreadCount,			// Maximum thread pool size.
				10,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(maxThreadCount),
				threading.threadFactory,
				threading.rejectionHandler
			);			
			// Monitor thread.
//			if (useMonitorThread) {
			boolean useMonitorThread = AwsEventGenerator.getInstance().getConfiguration().isUseMonitorThread();
			if (useMonitorThread)	{
				monitor = new MonitorThread(
					threading.executorPool,
					(int)(delay * 1000),
					statisticsView 
				);
			}
			
			// Prepare tasks.
//			workers = new SyslogSendTask[threadCount];
//			for (int i = 0; i < threadCount; i++) {
//				threading.workers[i] = new SyslogSendTask();
//			}
			
			// [2018-10-23] Kim, Sang Hyoun: Thread to take user input from keyboard.
			// [2018-11-22] SH: No user input handler if running in background mode.
			if (!AwsEventGenerator.getInstance().isBackground()) {
				inputHandler = new UserInputHandler(threading.executorPool, monitor);
			}
			
			// [2018-11-06] Sang Hyoun: Cron job to get daily transitioned.
			// Grab the Scheduler instance from the Factory
			try {
				scheduler = StdSchedulerFactory.getDefaultScheduler();
//
//				// and start it off
//				scheduler.start();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void startHelperThreads() {
			boolean useMonitorThread = AwsEventGenerator.getInstance().getConfiguration().isUseMonitorThread();
			if (useMonitorThread && (monitor != null)) {
				Thread monitorThread = new Thread(monitor, "Monitor");
				monitorThread.setDaemon(true);
				monitorThread.start();
			}
			
			if (!AwsEventGenerator.getInstance().isBackground() &&
				inputHandler != null) {
				Thread inputHandlerThread = new Thread(inputHandler, "InputHandler");
				inputHandlerThread.setDaemon(true);
				inputHandlerThread.start();
			}
			
			// [2018-11-06] Sang Hyoun: Start daily transition scheduler job.
//			JobDetail jobDailyTransitioner = new JobDetail("daily-transitioner-job", "group", DailyTransitionerJob.class);
			   // job 5 will run at 10am on the 1st and 15th days of the month
			job = newJob(DailyTransitionerJob.class)
				.withIdentity("daily-transitioner-job", "group1")
				.build();
			// https://crontab.guru/every-day-at-midnight
			// https://examples.javacodegeeks.com/enterprise-java/quartz/quartz-scheduler-cron-expression-example/
			/*
			 * 4. Cron Expression Components
				Cron-Expressions are strings that are actually made up of seven sub-expressions. Each sub-expression describes the individual details of the schedule. These sub-expression are separated with white-space.
				
				The sub-expressions represent the following components:
				
				Seconds
				Minutes
				Hours
				Day-of-Month
				Month
				Day-of-Week
				Year (optional field)
				In our example, we have used wild cards * and ?.
				Wild-cards * means “every” possible value of this field whereas wild card? is used to
				specify “no specific value”.
				It is allowed for the day-of-month and day-of-week fields.
				Cron expression “0 24 23 * * ? *” means the job will fire at time 23:24:00 every day.
			 */
			String cronExpression = AwsEventGenerator.getInstance()
				.getConfiguration().getDailyTransitionCronExpression();
			trigger = newTrigger().withIdentity("daily-transitioner-trigger", "group1")
//				.withSchedule(cronSchedule("59 59 23 * * ? *"))
				.withSchedule(cronSchedule(cronExpression))
				.build();
			
			try {
				// Tell quartz to schedule the job using our trigger
				scheduler.scheduleJob(job, trigger);

				// and start it off
				scheduler.start();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/*
		public void endThreads() {
			// Shutdown the pool.
			executorPool.shutdown();
			
			try {
				executorPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (monitor != null) {
				monitor.shutdown();
				synchronized (monitor) {
					monitor.notify();
				}
			}
			
			if (inputHandler != null) {
				inputHandler.shutdown();
				synchronized (inputHandler) {
					inputHandler.notify();
				}
			}
		}
		*/
		
		public void awaitThreads() {
			try {
				if (executorPool != null) {
					executorPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (scheduler != null) {
				try {
					scheduler.shutdown();
				} catch (SchedulerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public ActiveTasksThreadPoolExecutor getExecutorPool() {
			return executorPool;
		}

		public void setExecutorPool(ActiveTasksThreadPoolExecutor executorPool) {
			this.executorPool = executorPool;
		}

		public MonitorThread getMonitor() {
			return this.monitor;
		}
	}

	static volatile AwsEventGenerator instance = null;

	String appName;

	private CommandLineParser commandParser = null;
	private Options options;

//	private static Logger logger = Logger.getLogger(AwsEventGenerator.class);
	private static Logger logger = LoggerFactory.getLogger(AwsEventGenerator.class);
	
//	private String messageFilesBundlePath;
//	private String resultOutputFilePath;
//	private int threadCount = -1;
	private boolean verbose = false;
//	private boolean useMonitorThread = false;
	
	private CommandParams commandParams;
	private Configs configs;
	private MessageFilesBundleParams messageFilesBundleParams;
	private IMessageFilesBundle messageFilesBundle;
	private IResultSink resultSink;
	
	protected String configFilePath;
	protected PropertiesConfiguration propsConfiguration;
	
	private Threading threading;

	private boolean somethingWrong = false;

	private Double delay = 1.0d;

	private boolean suspended = false;

	private boolean running = false;

	private DailyBoostPeriodsProvider dailyPeakBoostPeriodsProvider;
	
	// [2018-11-05] SH: Metric to track daily limit shooting.
	private AwsEventGeneratorMetric AwsEventGeneratorMetric =
		new AwsEventGeneratorMetric();

	private Date upTime;

	private boolean showExtendedMenus = false;

	private boolean background = false;

	private String xrayTraceId;

	private String xrayParentId;
	
//	private long batchNumber = 0;

	public AwsEventGenerator() {
		this.appName = AwsEventGenerator.class.getName();
		
		commandParams = new CommandParams();
		configs = new Configs();
		messageFilesBundleParams = new MessageFilesBundleParams();
		threading = new Threading();
		messageFilesBundle = new MessageFilesBundle();
		resultSink = new ResultOutput();
		
		setRunning(true);
	}
	
	public CommandParams getCommandParams() {
		return commandParams;
	}

	public String getConfigFilePath() {
		return this.configFilePath;
	}

	public void setCommandParams(CommandParams commandParams) {
		this.commandParams = commandParams;
	}

	/**
	 * Return the single instance of application - AwsEventGenerator.
	 * 
	 * @return AwsEventGenerator instance
	 */
	public static AwsEventGenerator getInstance() {
		// Double-check-and-lock.
		if (instance == null) {
			synchronized (AwsEventGenerator.class) {
				if (instance == null) {
					instance = new AwsEventGenerator();
				}
			}
		}

		return instance;
	}

    public static void usage() {
        System.out.println("Usage: java " + AwsEventGenerator.class.getName()
            + " [none/simple/files/dictionary [trigger mask]]");
        System.out.println("  none - no completors");
        System.out.println("  simple - a simple completor that comples "
            + "\"foo\", \"bar\", and \"baz\"");
        System.out
            .println("  files - a completor that comples " + "file names");
        System.out.println("  classes - a completor that comples "
            + "java class names");
        System.out
            .println("  trigger - a special word which causes it to assume "
                + "the next line is a password");
        System.out.println("  mask - is the character to print in place of "
            + "the actual password character");
        System.out.println("  color - colored prompt and feedback");
        System.out.println("\n  E.g - java Example simple su '*'\n"
            + "will use the simple completor with 'su' triggering\n"
            + "the use of '*' as a password mask.");
    }
	
	public static void main(String[] args) throws Throwable {
		AwsEventGenerator theApp = null;

		// [2020-09-13] KSH: AWS SIntercept the message
		// Use this.
//    	AWSXRay.beginSegment("Kafka #1-Read File");
//       	Segment document = AWSXRay.getCurrentSegment();
//       	String ParentID=document.getId();
//       	TraceID PTraceID=document.getTraceId();
//       	
//       	document.setUser("Kafka Producer");
//    	//document.setId("6226467e3f840201");
//    	//document.setTraceId(PTraceID.fromString("1-5f5c5420-21afbbf6c378491194ffbbbb"));
//    	//ParentID=document.getId();
//       	
//        
//        AWSXRay.endSegment();	          
//        AWSXRay.beginSegment("Kafka #2-Publishing",PTraceID,ParentID);
//        
//        System.out.println("Producing job completed");
//        AWSXRay.endSegment();
//        
//        if (1 == 1) {
//        	System.exit(0);
//        }
        
        // End of use this.
        
//    	AWSXRay.beginSegment("Kafka #1-Read File");
//       	Segment document = AWSXRay.getCurrentSegment();
//       	String ParentID=document.getId();
//       	TraceID PTraceID=document.getTraceId();
//		
//		AWSXRayRecorder xrayRecorder = AWSXRayRecorderBuilder.defaultRecorder();
//		Subsegment kafkaProducerInterceptorSS = xrayRecorder.beginSubsegment("KafkaProducerInterceptorSS");
//
//		// get the parentId and traceId
//		String parentId = xrayRecorder.getCurrentSegment().getId();
//		TraceID traceId = xrayRecorder.getCurrentSegment().getTraceId();
//
//		// format the trace information prior to saving
//		String traceInformation = traceId.toString() + "::" + parentId;
//
//		// create a new message object with the trace information
////		Message interceptedMessage = (Message) record.value();
//
//		// now add the message with trace information
////		String changedMessage = "intercepted-and-changed-" + interceptedMessage.getMessageText();
////		Message messageWithTraceInformation = new Message(interceptedMessage.getToUserName(), changedMessage,
////				traceInformation);
//
//		// send the changed intercepted message
////		ProducerRecord newRecord = new ProducerRecord<>(KafkaTopic, record.key(), messageWithTraceInformation);
//		kafkaProducerInterceptorSS.end();

//		return newRecord;

//		Segment segment = null;
//		Subsegment subsegment = null;
//		segment = AWSXRay.beginSegment("AWSEventGenerator-main");
//		
//		String traceId = segment.getTraceId().toString();
//		String parentId = segment.getParentId();
//
//		Map<String, Object> map1 = new HashMap<String, Object>();
//		Map<String, Object> submap1 = new HashMap<String, Object>();
//		submap1.put("TestSubKey1", "TestSubValue1");
//		map1.put("TestKey1", submap1);
//		
//		Map<String, Object> map2 = new HashMap<String, Object>();
//		map2.put("TesKey2", "TestValue2");
////		segment.setService(map);
////		segment.setNamespace("Test");
//
//		
//		subsegment = AWSXRay.beginSubsegment("TestSubsegment");
//		subsegment.setNamespace("Test");
////		subsegment.setAws(map1);
//		
//		
////		Segment segment2 = AWSXRay.beginDummySegment();
////		segment2.setAws(map1);
////		segment2.setService(map2);
////		AWSXRay.endSegment();
//		
//		
////		AWSXRayRecorderBuilder.standard().withDefaultPlugins().build().beginDummySegment();
////		AWSXRay.endSegment();
//		
//		AWSXRay.endSubsegment();
//
//		
//		AWSXRay.endSegment();
		
//		if (1 == 1) {
//			System.exit(0);
//		}
		
		try {
			theApp = AwsEventGenerator.getInstance();

//			// Experimental.
//			theApp.setXRayTraceId(traceId);
//			theApp.setXRayParentId(parentId);

			
////			subsegment = AWSXRay.beginSubsegment("AWSEventGenerator-parseArgs");
////			subsegment.setTraceId(TraceID.fromString(traceId));
////			subsegment.setParentId(parentId);
//
//			segment = AWSXRay.beginSegment("Test");
//			segment.setTraceId(TraceID.fromString(traceId));
//			segment.setParentId(parentId);
			
			// Parse arguments.
			theApp.parseArgs(args);
			
////			AWSXRay.endSubsegment();
//			
//			AWSXRay.endSegment();
			
//			if (1 == 1) {
//				return;
//			}
			
//			subsegment.end();

			// Initialize.
//			subsegment = AWSXRay.beginSubsegment("AWSEventGenerator-init");
			theApp.init();
//			AWSXRay.endSubsegment();

////			subsegment = AWSXRay.beginSubsegment("AWSEventGenerator-doAction");
//			segment = AWSXRay.beginSegment("doAction");
//			segment.setTraceId(TraceID.fromString(traceId));
//			segment.setParentId(parentId);
			
			// Do action.
			theApp.doAction();
			
//			AWSXRay.endSegment();
////			AWSXRay.endSubsegment();
		} catch (Throwable t) {
			t.printStackTrace();

			throw t;
		} finally {
			if (theApp != null) {
				try {
//					subsegment = AWSXRay.beginSubsegment("AWSEventGenerator-fini");
					theApp.fini();
//					AWSXRay.endSubsegment();
//					subsegment.end();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
//			AWSXRay.endSegment();
		}
		
//		System.out.println("Program exited!");
//		System.out.flush();
	}

	public String getXRayParentId() {
		return this.xrayParentId;
	}
	
	private void setXRayParentId(String parentId) {
		this.xrayParentId = parentId;
	}

	public String getXRayTraceId() {
		return this.xrayTraceId;
	}
	
	private void setXRayTraceId(String traceId) {
		this.xrayTraceId = traceId;
	}

	private void fini() {
		threading.awaitThreads();
	}

	private void doAction() throws IOException {
		asyncSendSyslog();
	}

	private void asyncSendSyslog() throws IOException {
//		threading.prepareThreads();
//		threading.startHelperThreads();
		
//		try {
			// Let's start with the actual works.
			asyncProcessMessageFilesBundle();
//		} finally {
////			threading.endThreads();
//			threading.awaitThreads();
//		}
	}

	private boolean asyncProcessMessageFilesBundle() throws IOException {
		long totalLines = 0;
		
		totalLines = messageFilesBundleParams.getMessageFilesBundleLineCount();
		
		// If lines count is equal or less than 0, it's empty.
		if (totalLines <= 0) {
			logger.warn("The message files bundle [" + AwsEventGenerator.getInstance().getConfiguration().getMessageFilesBundlePath() + "] file is empty!");
			
			return false;
		}
		
//		boolean next = true;
		
		// Open message files bundle file.
		
		messageFilesBundle.open(
			AwsEventGenerator.getInstance().getConfiguration().getMessageFilesBundlePath()
		);
		resultSink.open();
		
		Result result = new Result();
		boolean success = true;
		String resultMessage = "SUCCESS";
		
//		// [2020-09-20] KSH: AWS X-Ray tracing for this entire program.
//		Segment segment = AWSXRay.beginSegment(AwsEventGenerator.XRaySegmentName);
		try {
			sendMessageFiles();
		}
//		catch (Throwable t) {
//			segment.addException(t);
//		}
		finally {
			
//			// [2020-09-20] KSH: End AWS X-Ray segment for tracing.
//			AWSXRay.endSegment();
			
			try {
				messageFilesBundle.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// [2018-11-06] Sang Hyoun: Overall result.
			result.setAttachedTaskID(0);
			result.setSyslogServer("(N/A)");
			result.setSyslogPort(-1);
			result.setStartDate(this.upTime);
			result.setEndDate(new Date());
			result.setAccumulatedSentCount(
				this.getAwsEventGeneratorMetric().getAccumulatedSentCount().get()
			);
			result.setDailySentCount(
				this.getAwsEventGeneratorMetric().getDailySentCount().get()
			);
			result.setAccumulatedSentBytesRaw(
				this.getAwsEventGeneratorMetric().getAccumulatedSentBytesRaw().get()
			);
			result.setDailySentBytesRaw(
				this.getAwsEventGeneratorMetric().getDailySentBytesRaw().get()
			);
			result.setAccumulatedSentBytesEnveloped(
				this.getAwsEventGeneratorMetric().getAccumulatedSentBytesEnveloped().get()
			);
			result.setDailySentBytesEnveloped(
				this.getAwsEventGeneratorMetric().getDailySentBytesEnveloped().get()
			);
			
			result.setSuccess(success);
			result.setResultMessage(resultMessage);
			
			this.getResultSink().drain(result);
			
			try {
				resultSink.flush();
				resultSink.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// Print the total report screen.
//			printTotalCount();
		}
		
		return true;
	}

	private void sendMessageFiles() throws IOException {
		try {
//			List<MessageFile> messageFileList = null;
			MessageFile messageFile = null;
			
			try {
//				messageFileList = new ArrayList<MessageFile>();
				
				messageFilesBundle.mark(4096);
				int lc = 0;
				while (
					(messageFile = (MessageFile)messageFilesBundle.acquire()) != null
				) {
					if (!messageFile.isInvalid()) {					
						// FIXME: Remove batchNumber.
	//					messageFile.setBatchNumber(batchNumber);
						messageFile.setSlotId(lc++);
	//					messageFileList.add(messageFile);
						
						LoggerUtils.getLogger().debug(
							"[" + lc + "] MessageFile in message files bundle file: " +
								messageFile
						);
						
						// IMPORTANT: Submit actual task here!
						submitTask(messageFile, false);
					}
				}
			} finally {
				awaitTasks();
				
				// Flush the output.
				// [2018-11-06] Sang Hyoun: Moved to main loop.
//				resultSink.flush();
				
//				if (messageFileList != null) {
//					messageFileList.clear();
//					messageFileList = null;
//				}
			}
		} finally {
			// Increment batch number.
//			batchNumber++;
//			resetBatchCount();
		}
	}

	public void awaitTasks() {
		while (true) {
			int activeCount = threading.executorPool.getActiveCount();
			if (activeCount == 0) break;
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				
				return;
			}
		}
	}

	public void submitTask(MessageFile messageFile, boolean waitForCompletion) {
		if (messageFile == null) {
			// Nothing to submit. Just return.
			return;
		}
		
//		threading.workers[i].setMessageFile(messageFile);
		AwsEventGeneratorTask syslogSendTask = new AwsEventGeneratorTask(messageFile);
		
		
//		 Consumer to display a number 
////        Consumer<Subsegment> display = a -> System.out.println(a);
//		Consumer<Subsegment> display = new Consumer<Subsegment>() {
//
//			@Override
//			public void accept(Subsegment t) {
//				System.out.println(t);
//			}
//			
//		};
//		AWSXRay.createSubsegment("Task", display);
		
		threading.executorPool.execute(syslogSendTask);
		
		// Wait until the worker thread gets started with running state.
		// Double-check-and-wait.
		if (!syslogSendTask.isRunning()) {
			synchronized (syslogSendTask) {
				if (!syslogSendTask.isRunning()) {
					// It is extremely rare that the worker thread does not enter into
					// running state within 3 seconds.
					try {
						syslogSendTask.wait(3000);
					} catch (InterruptedException e) {
						LoggerUtils.getLogger().warn(
							"Submitted task for message file not running and wait error: " +
								e.getLocalizedMessage()
						);
					}
				}
			}
		}
		
		// Wait until the submitted task is completed.
		if (waitForCompletion) {
			while (true) {
				if (!syslogSendTask.isRunning()) {
					break;
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void init() throws Exception {
		printVersionInfo();
		
		printNotice();
		
		printSystemInfo();
		
		/*
		 * Check the "Common Configuration File".
		 * This will hold the common configuration values.
		 */
		StringBuffer sb = new StringBuffer();
		try {
			checkConfigFile();
		} catch (Exception e) {
			sb.append(e.getLocalizedMessage());
			sb.append(AwsEventGeneratorFileUtils.NEW_LINE);
			// Something wrong.
			somethingWrong  = true;
			LoggerUtils.getLogger().warn("Configuration file check failed. Moving on with something-wrong flag turned on: " + e.getLocalizedMessage());
		}
		
		try {
			checkMessageFilesBundleFile();
		} catch (Exception e) {
			sb.append(e.getLocalizedMessage());
			sb.append(AwsEventGeneratorFileUtils.NEW_LINE);
			// Something wrong.
			somethingWrong = true;
			LoggerUtils.getLogger().warn("Message files bundle file check failed. Something-wrong flag concluded to be set on: " + e.getLocalizedMessage());
		}
		
		if (somethingWrong) {
			throw new SomethingWrongException("Something wrong happened and program stopped for user input: " + sb.toString());
		}
		
		// [2018-11-01] SH: All normal. Do some more initialization works.
		dailyPeakBoostPeriodsProvider = DailyBoostPeriodsProvider.fromString(
			AwsEventGenerator.getInstance()
				.getConfiguration().getDailyPeakBoostPeriods(),
			AwsEventGenerator.getInstance()
				.getConfiguration().getDailyOverBoostPeriods()
		);
		
		// Daily limit.
		long dailySentCountLimit = AwsEventGenerator.getInstance()
			.getConfiguration().getDailySentCountLimit();
		long dailySentBytesRawLimit = AwsEventGenerator.getInstance()
			.getConfiguration().getDailySentBytesRawLimit();
		long dailySentBytesEnvelopedLimit = AwsEventGenerator.getInstance()
			.getConfiguration().getDailySentBytesEnvelopedLimit();
		this.AwsEventGeneratorMetric.setDailySentCountLimit(
			dailySentCountLimit
		);
		this.AwsEventGeneratorMetric.setDailySentBytesRawLimit(
			dailySentBytesRawLimit
		);
		this.AwsEventGeneratorMetric.setDailySentBytesEnvelopedLimit(
			dailySentBytesEnvelopedLimit
		);
		
		// Create view.
		this.statisticsView = new StatisticsView(
			null,
			AwsEventGenerator.getInstance()
				.getConfiguration().isSortByEPS()
		);
		
		threading.prepareThreads();
		threading.startHelperThreads();
		
		// Set up time.
		setUpTime(new Date());
	}


	private void setUpTime(Date upTime) {
		this.upTime = upTime;
	}

	private Options getOptions() {
		return options;
	}

	private void parseArgs(String[] args) {
		// Parser.
		commandParser = new BasicParser();

		// Options.
		options = new Options();

		addOptions();

		parseOptions(args);
	}

	private void addOptions() {
		// Options.
		Options options = this.getOptions();

		// Parameter file.
		options.addOption("c", "configFile", true, "Configuration file path (absolute or relative).");

//		// Result output file path.
//		options.addOption(
//			"o",
//			"resultOutputFile",
//			true,
//			"Result output file path (absolute or relative)."
//		);
//		
//		// Thread count.
//		options.addOption(
//			"t",
//			"threadCount",
//			true,
//			"Thread count."
//		);
//		
//		// Use monitor thread.
//		options.addOption(
//			"m",
//			"monitorThread",
//			false,
//			"Use monitor thread"
//		);
		
		// [2018-10-16] Kim, Sang Hyoun: Delay to refresh stats.
		options.addOption("d", "delay", true, "Delay (sec) to refresh statistics.");
		
		// [2018-11-08] Sang Hyoun: Enable/disable extended menus.
		options.addOption("m", "menu", false, "Show extended menu.");
		
		// Verbose.
		options.addOption("v", "verbose", false, "Verbose mode.");
		
		// [2018-11-22] SH: Suppress standard input for background operation.
		options.addOption("b", "background", false, "Background mode (no standard input)");
	}

	private void parseOptions(String[] args) {
		CommandLine cmdline = null;

		try {
			// Parse the program arguments.
			cmdline = commandParser.parse(options, args);

			// Config file path.
			if (cmdline.hasOption('c')) {
				configFilePath = cmdline.getOptionValue('c');
			} else {

			}

			// Fallback to the default of config file path.
			if (configFilePath == null) {
				configFilePath = "config.properties";
			}

//			// Message files bundle file.
//			if (cmdline.hasOption('m')) {
//				messageFilesBundlePath = cmdline.getOptionValue('m');
//			}
			
			// [2018-10-04] Sang Hyoun: Do not override now to acquire from configuration file later.
//			if (messageFilesBundlePath == null) {
//				messageFilesBundlePath = "message_files_bundle.txt";
//			}
			
//			// Result output file path.
//			if (cmdline.hasOption('o')) {
//				resultOutputFilePath = cmdline.getOptionValue('o');
//			} else {
//				
//			}
			
//			// Thread count.
//			if (cmdline.hasOption('t')) {
//				try {
//					threadCount = Integer.parseInt(cmdline.getOptionValue('t'));
//				} catch (Exception e) {
//					// TODO: Display warning message to show the number format is not correct or something.
//				}
//			}
////			// Set to 10 if the thread count is not valid.
////			if (threadCount <= 0) {
////				// TODO: Display warning message to coalesce the invalid thread count value to 10.
////				threadCount = 10;
////			}
			
//			// Use monitor thread.
//			if (cmdline.hasOption('m')) {
//				useMonitorThread = true;
//			}
//			
			// [2018-10-18] Kim, Sang Hyoun: Delay to refresh stats.
			if (cmdline.hasOption('d')) {
				String delayStr = cmdline.getOptionValue('d');
				delay  = (Double)Double.parseDouble(delayStr);
				if (delay < 0.1d) {
					throw new IllegalArgumentException("Delay cannot be set below 0.1");
				}
			}

			// [2018-11-08] Sang Hyoun: Show extended menus.
			if (cmdline.hasOption('m')) {
				showExtendedMenus = true;
			}
			
			// Verbose.
			if (cmdline.hasOption('v')) {
				verbose = true;
			}
			
			// [2018-11-22] SH: Background mode.
			if (cmdline.hasOption('b')) {
				background = true;
			}
		} catch (Exception e) {
			LoggerUtils.getLogger().info(e.getMessage(), e);
			printUsage(System.out);
		}
	}
	
	protected void printUsage(PrintStream out) {
		final PrintWriter writer = new PrintWriter(out);
		
		final HelpFormatter usageFormatter = new HelpFormatter();
		usageFormatter.printUsage(writer, 80, appName, options);

		writer.close();
	}

	private void checkConfigFile() throws ConfigurationException {
		// Set default configuration file path.
		// Handle the case when init configuration file is not provided.
		if (configFilePath == null) {
			configFilePath = "config.properties";
		}
		
		propsConfiguration = loadConfigurationFile(
			configFilePath,
			AwsEventGeneratorConfigurationUtils.defaultConfigs()
		);
	}
	
	private PropertiesConfiguration loadConfigurationFile(
		String filePath,
		Properties properties
	) throws ConfigurationException {
		File file = new File(filePath);
		
		// Properties configuration.
		// If the file exists, the contents will be loaded.
		// If not, set the properties below and save() will store the contents to the file.
//		PropertiesConfiguration propConf = new PropertiesConfiguration(file);
		PropertiesConfiguration propConf = new PropertiesConfiguration();
		propConf.setDelimiterParsingDisabled(true);
		propConf.setFile(file);
		propConf.load();
		propConf.setAutoSave(false);
		
		/*
		 * Sort (or not) the properties if the file has to be newly created.
		 */
		if (!file.exists()) {
			BasicConfigurationUtils.setSortedProperties(propConf, properties, true);
		
			// Save.
			propConf.save();
			
			// Throw exception to notify user that the init configuration file is missing.
			String message = String.format(
				"Common configuration file [%s] does not exist." +
					AwsEventGeneratorFileUtils.NEW_LINE +
					"This tool created file [%s] for your convenient example, " +
					"but stopped execution for user confirmation." +
					AwsEventGeneratorFileUtils.NEW_LINE +
					"Verify, modify if needed, and restart this tool.",
				filePath,
				filePath
			);

			throw new ConfigurationException(message);
		}
		
		// Throw exception and stop upon missing property.
		validateProperties(filePath, propConf);
		
		return propConf;
	}

	private void validateProperties(String filePath, PropertiesConfiguration propConf) throws ConfigurationException {
		if (propConf != null) {
			// Enumerate the mandatory items only.
			checkMissingParameter(filePath, propConf, Configs.CONFIG_KEY_SYSLOG_SERVER);
			checkMissingParameter(filePath, propConf, Configs.CONFIG_KEY_SYSLOG_PORT);
			checkMissingParameter(filePath, propConf, Configs.CONFIG_KEY_MESSAGE_FILES_BUNDLE_FILE_PATH);
		}
	}

	private void checkMissingParameter(
		String filePath,
		PropertiesConfiguration propConf,
		String configName
	) throws ConfigurationException {
		if (propConf == null) return;
		
		String configValue = propConf.getString(configName);
		if (configValue == null) {
			String message = String.format(
				"[%s]: Missing configuration '%s'",
				filePath,
				configName
			);
				
			throw new ConfigurationException(message);
		}
	}

	private void printVersionInfo() {
		// Print version information.
		StringBuffer sb = new StringBuffer();
		sb.append(
			AwsEventGeneratorUtils.NEW_LINE +
			"################################################################" +
			AwsEventGeneratorUtils.NEW_LINE
		);
		sb.append(AwsEventGeneratorVersionInfo.getProgramShortName() + AwsEventGeneratorUtils.NEW_LINE);
		sb.append(">>> Version: " + AwsEventGeneratorVersionInfo.getVersion() + AwsEventGeneratorUtils.NEW_LINE);
		sb.append("################################################################" +
			AwsEventGeneratorUtils.NEW_LINE
		);
		
		logger.info(sb.toString());
	}

	private void printNotice() {
		System.out.println();
		System.out.println("======================== Program Notice ========================");
		System.out.println("This tool is the property of Micro Focus and is provided to ");
		System.out.println("its customers or partners for convenience purposes only for them. It is not intended for ");
		System.out.println("use in end-user production installations.");
		System.out.println();
		System.out.println("DO USE THIS PROGRAM UPON YOUR OWN DISCRETION.");
		System.out.println("Terminating this program may lead to unexpected state of ");
		System.out.println("connected stauts, statistical numerics generated after starting.");
		System.out.println("========================= End of Notice ========================");
		System.out.println();
//		System.out.println();
	}

	private void printSystemInfo() {
		StringBuffer sb = new StringBuffer();
		try {
			sb.append(AwsEventGeneratorFileUtils.NEW_LINE);
			sb.append("## System:\n");
			sb.append(new java.util.Date() + AwsEventGeneratorFileUtils.NEW_LINE);

			Properties properties = System.getProperties();
			
			List keyList = Collections.list(properties.keys());
			Collections.sort(keyList);
			
			for (Iterator iter = keyList.iterator();
				iter.hasNext();
			) {
				String key = (String)iter.next();
				String value = properties.getProperty(key);
				sb.append(key + " = " + value + AwsEventGeneratorFileUtils.NEW_LINE);
			}

			Runtime runtime = Runtime.getRuntime();
			sb.append("Total memory: " + runtime.totalMemory() + ", free memory: " + runtime.freeMemory() + AwsEventGeneratorFileUtils.NEW_LINE);
			sb.append("Hostname: " + InetAddress.getLocalHost().getHostName() + AwsEventGeneratorFileUtils.NEW_LINE);
			sb.append("Available Processors: " + runtime.availableProcessors() + AwsEventGeneratorFileUtils.NEW_LINE);
			sb.append("OS Architecture: " + System.getProperty("os.arch"));

			sb.append(AwsEventGeneratorFileUtils.NEW_LINE);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		logger.debug(sb.toString());
	}

	public void updateStats(boolean success) {
		throw new RuntimeException("IMPLEMENT THIS!");
	}
	
	public Configs getConfiguration() {
		return configs;
	}

	public PropertiesConfiguration getPropertiesConfiguration() {
		return propsConfiguration;
	}
	
	// Field setters/getters from here.
	public MessageFilesBundleParams getMessageFilesBundleParam() {
		return messageFilesBundleParams;
	}

	public void setMessageFilesBundleParam(MessageFilesBundleParams messageFilesBundle) {
		this.messageFilesBundleParams = messageFilesBundle;
	}
	
//	public String getMessageFilesBundlePath() {
//		return messageFilesBundlePath;
//	}

	private void checkMessageFilesBundleFile() throws IOException {
		String messageFilesBundlePath = getMessageFilesBundleParam().getMessageFilesBundleFilePath();
		
		if (!new File(messageFilesBundlePath).exists()) {
			// [2018-10-15] Kim, Sang Hyoun
			AwsEventGeneratorConfigurationUtils.saveSampleMessageFilesBundleFile(messageFilesBundlePath);
			
			// Throw exception to notify user that the sample message files bundle file
			// is missing and created.
			String message = String.format(
				"Message files bundle file [%s] does not exist, and " +
				"sample bundle file is created [%s] for your convenience." +
				AwsEventGeneratorFileUtils.NEW_LINE +
				"But tools stopped its execution for user confirmation." +
				AwsEventGeneratorFileUtils.NEW_LINE +
				"Verify, modify if needed, and start this tool again.",
				messageFilesBundlePath,
				messageFilesBundlePath
			);
			
			throw new IOException(message);
		}
	}

//	public String getResultOutputFilePath() {
//		return resultOutputFilePath;
//	}
//
//	public void setResultOutputFilePath(String resultOutputFilePath) {
//		this.resultOutputFilePath = resultOutputFilePath;
//	}

//	private void saveSampleMessageFilesBundleFile() throws IOException {
//		String messageFilesBundleFilePath = getMessageFilesBundleParam().getMessageFilesBundleFilePath();
//		File messageFilesBundleFile = new File(messageFilesBundleFilePath);
//		if (!messageFilesBundleFile.exists()) {
//			Collection<String> lines = new ArrayList<String>();
//			
//			lines.add("#messageFilePath,replaceSourcesText,replaceTargetsText,"
//				+ "syslogServer,syslogPort,"
//				+ "initialSendIntervalMillis,targetSendIntervalMillis,warmingUpPeriodSec,"
//				+ "isRepeat,isCacheMessage,messageHostName"
//			);
//			lines.add("syslog-ASA.txt,,,"
//				+ "esm611.microfocus.skytapdns.com,1514,"
//				+ "100,1,60,"
//				+ "true,true,Event_Generator_Host"
//			);
//			
//			FileUtils.writeLines(messageFilesBundleFile, lines, true);
//		}
//	}

	public IResultSink getResultSink() {
		return resultSink;
	}

	public boolean isSomeFileMissing() {
		return somethingWrong;
	}

	public void setSomeFileMissing(boolean fileMissing) {
		this.somethingWrong = fileMissing;
	}

	public Double getDelay() {
		return delay;
	}

	public void setDelay(Double delay) {
		this.delay = delay;
	}

	public Threading getThreading() {
		return threading;
	}

	public void setThreading(Threading threading) {
		this.threading = threading;
	}

	public ConsoleView getStatisticsView() {
		return statisticsView;
	}

	public void requestSuspend() {
		synchronized (this) {
			this.suspended  = true;
			this.notifyAll();
		}
		
		// [2018-11-01] SH: Also notify to all the tasks.
		AwsEventGeneratorTask.notifyToAllTasks();
		
	}
	
	public void requestResume() {
		synchronized (this) {
			this.suspended = false;
			this.notifyAll();
		}
		
		// [2018-11-01] SH: Also notify to all the tasks.
		AwsEventGeneratorTask.notifyToAllTasks();
	}
	
	public void toggleSuspend() {
		synchronized (this) {
			this.suspended = !this.suspended;
			this.notifyAll();
			
			// Also notify to monitor to refresh screen.
			this.threading.monitor.requestRefresh();
		}
		
		// [2018-11-01] SH: Also notify to all the tasks.
		AwsEventGeneratorTask.notifyToAllTasks();
	}

	public boolean isSuspended() {
		return this.suspended;
	}

	public boolean isRunning() {
		return this.running ;
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}

	public synchronized void stop() {
		this.running = false;
		this.notifyAll();
		
		// [2018-11-01] SH: Also notify to all the tasks.
		AwsEventGeneratorTask.notifyToAllTasks();

		// Shutdown the pool.
		threading.executorPool.shutdown();

		
		// Call to monitor.
		threading.monitor.stop();
	}

	public UserInputHandler getUserInputHandler() {
		return threading.inputHandler;
	}

	public void attachMessageFilesBundle(String filePath) throws IOException {
		MessageFilesBundle bundle = new MessageFilesBundle();
		bundle.open(filePath);
		try {
			MessageFile messageFile = null;
			bundle.mark(4096);
			int lc = 0;
			while ((messageFile = (MessageFile)bundle.acquire()) != null) {
				if (!messageFile.isInvalid()) {
					messageFile.setSlotId(lc++);
					
					// Submit task.
					submitTask(messageFile, false);
					
					LoggerUtils.getLogger().info(
						"[" + lc + "] message file [{}] attached.",
						messageFile
					);
				}
			}
		} finally {
			// No need to await task to join when attaching.
			
			// But should close IO.
			if (bundle != null) {
				bundle.close();
			}
		}
	}

	public DailyBoostPeriodsProvider getDailyPeakBoostPeriodsProvider() {
		return dailyPeakBoostPeriodsProvider;
	}

	public BoostState getDailyPeakBoostPeriodState() {
		return this.dailyPeakBoostPeriodsProvider.getDailyPeakBoostPeriodState();
	}

	public AwsEventGeneratorMetric getAwsEventGeneratorMetric() {
		return AwsEventGeneratorMetric;
	}

	public void setDailyLimitTouched(boolean touchedDailyLimit) {
		this.AwsEventGeneratorMetric.setDailyLimitTouched(touchedDailyLimit);
	}
	
	public boolean isDailyLimitTouched() {
		return this.AwsEventGeneratorMetric.isDailyLimitTouched();
	}

	public boolean isShowExtendedMenus() {
		return showExtendedMenus;
	}

	public void setShowExtendedMenus(boolean showExtendedMenus) {
		this.showExtendedMenus = showExtendedMenus;
	}

	public boolean isBackground() {
		return background;
	}

	public void setBackground(boolean background) {
		this.background = background;
	}

}
