package com.aws.proserve.korea.event.generator.messagefile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
//import com.amazonaws.xray.AWSXRay;
//import com.amazonaws.xray.entities.Segment;
//import com.amazonaws.xray.entities.Subsegment;
//import com.amazonaws.xray.entities.TraceID;
import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.boosting.BoostParams;
import com.aws.proserve.korea.event.generator.exception.DeviceEventException;
import com.aws.proserve.korea.event.generator.performance.BoostState;
import com.aws.proserve.korea.event.generator.performance.DailyBoostPeriod;
import com.aws.proserve.korea.event.generator.performance.DailyBoostPeriodsProvider;
import com.aws.proserve.korea.event.generator.threading.AwsEventGeneratorTask;
import com.aws.proserve.korea.event.generator.utils.DebugUtils;
import com.aws.proserve.korea.event.generator.utils.LoggerUtils;
import com.aws.proserve.korea.event.generator.utils.RegexHelper;
import com.aws.proserve.korea.network.syslog.Facility;
import com.aws.proserve.korea.network.syslog.MessageFormat;
import com.aws.proserve.korea.network.syslog.Severity;
import com.aws.proserve.korea.network.syslog.sender.AbstractSyslogMessageSender;
import com.aws.proserve.korea.network.syslog.sender.NullSyslogMessageSender;
import com.aws.proserve.korea.network.syslog.sender.UdpSyslogMessageSender;


public class MessageFile implements Cloneable {

	private static final CharSequence REPLACEMENT_HEADER_DATETIME = "%DATETIME:";
	private static final int REPLACEMENT_HEADER_DATETIME_LEN = REPLACEMENT_HEADER_DATETIME.length();

	// [2018-10-12] Kim, Sang Hyoun: Timestamp on instantiation.
	private Date startDttm = new Date();
	
	private long batchNumber = 0;
	private int slotId = -1;
	
	String messageFilePath;
	
	String replaceSourcesText;
	String replaceTargetsText;
	private String[] replaceSources = null;
	private String[] replaceTargets = null;
	
	String syslogServer;
	int syslogPort = -1;
	int initialSendIntervalMillis = 100;
	int targetSendIntervalMillis = 1;
	int warmingUpPeriodSec = 60;
	boolean isRepeat = true;
	private boolean isCacheMessage;
	private String syslogMessageHostName;
	private String syslogAppName;
	private String syslogFacility;
	private String syslogSeverity;
	private boolean syslogPrependPRIPart = true;
	private boolean syslogPrependHeaderTimestamp = false;
	private boolean syslogPrependHeaderHostname = false;
	private boolean syslogPrependHeaderAppName = false;
	private DailyBoostPeriodsProvider dailyBoostPeriodsProvider = null;
	
	// To handle invalid (blank) line.
	boolean isInvalid = false;

	private BufferedReader messageFileReader;

	private ArrayList<String> messageCache;
	private ArrayList<String> replacedLinesCache = new ArrayList<String>();

	private AbstractSyslogMessageSender syslogSender;
//	private AbstractKafkaSender kafkaSender;

	private final AtomicLong dilatedWarmingUpPeriodMillis = new AtomicLong();

	private long lastElapsedTimeNanos = 0;

	private long lastBaseElapsedTimeNanos = 0;
	
	private AwsEventGeneratorTask AwsEventGeneratorTask = null;

	private int lineCount = 0;

	private double currentBaselinedEPS = 0;

	private boolean boosted;

	private double boostBaselinedEPSThreshold;

	private boolean boostedByUser = false;
	private double baselinedEPSByUser = 100.0d;
	private double boostFactorByUser = 1.0d;

	/*
	 * All other accumulated metrics are defined in AbstractSyslogMessageSender.
	 */
	private AtomicLong dailySentCount = new AtomicLong(0);
	private AtomicLong dailySentBytesRaw = new AtomicLong(0);
	private AtomicLong dailySentBytesEnveloped = new AtomicLong(0);

	private boolean touchedDailyLimit = false;
	private Date resetDate = null;

	private BoostParams boostParams = null;

	private RegexHelper regexHelper = new RegexHelper();
	private String messageFileName;
	
	// TODO: Handle case when Kafka is not configured - NullKafkaSender.
	private KafkaProducer<String, String> kafkaProducer;
	private String kafkaBootstrapServers;
	private String kafkaTopic;
	private String kafkaClientIdPrefix;
	private boolean kafkaProducerDisabled = false;
	private long instanceDttm;

	public MessageFile() {
		this(true);
	}

	public MessageFile(boolean isInvalid) {
		super();
		this.isInvalid = isInvalid;
	}

	public MessageFile(
		long batchNumber,
		int slotId,
		String messageFilePath,
		String replaceSourcesText,
		String replaceTargetsText,
		String syslogServer,
		int syslogPort,
		int initialSendIntervalMillis,
		int targetSendIntervalMillis,
		int warmingUpPeriodSec,
		boolean isRepeat,
		boolean isCacheMessage,
		String syslogMessageHostName,
		String syslogAppName,
		String syslogFacility,
		String syslogSeverity,
		boolean syslogPrependPRIPart,
		boolean syslogPrependHeaderTimestamp,
		boolean syslogPrependHeaderHostname,
		boolean syslogPrependHeaderAppName,
		DailyBoostPeriodsProvider dailyPeakBoostPeriodsProvider,
		double boostBaselinedEPSThreshold,
		BoostParams boostParams,
		String kafkaBootstrapServers,
		String kafkaTopic,
		String kafkaClientIdPrefix
	) {
		super();
		
		this.instanceDttm = System.currentTimeMillis();
		
		this.batchNumber = batchNumber;
		this.slotId = slotId;
		this.messageFilePath = messageFilePath;
		// [2018-11-22] SH: Message file name to be short on display.
		if (this.messageFilePath != null) {
			this.messageFileName = FilenameUtils.getName(this.messageFilePath);
		}
		
		// Replace source pattern and its split texts.
		if (!StringUtils.isBlank(replaceSourcesText)) {
			this.replaceSources = StringUtils.splitByWholeSeparatorPreserveAllTokens(
				replaceSourcesText,
				AwsEventGenerator.getInstance().getConfiguration().getReplaceTextDelimiter()
			);
		}
		this.replaceSourcesText = replaceSourcesText;

		// Replace target pattern and its split texts.
		if (!StringUtils.isBlank(replaceTargetsText)) {
			this.replaceTargets = StringUtils.splitByWholeSeparatorPreserveAllTokens(
				replaceTargetsText,
				AwsEventGenerator.getInstance().getConfiguration().getReplaceTextDelimiter()
			);
		}
		this.replaceTargetsText = replaceTargetsText;
		
		if (StringUtils.isNotBlank(syslogServer)) {
			this.syslogServer = syslogServer;
		} else {
			// Set as the global value from configuration file.
			this.syslogServer = AwsEventGenerator.getInstance().getConfiguration().getSyslogServer();
		}
		
		if (syslogPort > 0) {
			this.syslogPort = syslogPort;
		} else {
			this.syslogPort = AwsEventGenerator.getInstance().getConfiguration().getSyslogPort();
		}
		this.initialSendIntervalMillis = initialSendIntervalMillis;
		this.targetSendIntervalMillis = targetSendIntervalMillis;
		this.warmingUpPeriodSec = warmingUpPeriodSec;
		this.isRepeat = isRepeat;
		this.isCacheMessage = isCacheMessage;
		this.syslogMessageHostName = syslogMessageHostName;
		this.syslogAppName = syslogAppName;
		this.syslogFacility = syslogFacility;
		this.syslogSeverity = syslogSeverity;
		this.syslogPrependPRIPart = syslogPrependPRIPart;
		this.syslogPrependHeaderTimestamp = syslogPrependHeaderTimestamp;
		this.syslogPrependHeaderHostname = syslogPrependHeaderHostname;
		this.syslogPrependHeaderAppName = syslogPrependHeaderAppName;
		this.dailyBoostPeriodsProvider = dailyPeakBoostPeriodsProvider;
		this.boostBaselinedEPSThreshold = boostBaselinedEPSThreshold;
		this.boostParams  = boostParams;
		
		// [2020-09-19] KSH: Kafka.
		this.kafkaBootstrapServers = kafkaBootstrapServers;
		this.kafkaTopic = kafkaTopic;
		this.kafkaClientIdPrefix = kafkaClientIdPrefix;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		MessageFile aMessageFile = (MessageFile)super.clone();
		
		// Attributes to deep copy.
		aMessageFile.startDttm = new Date();
		if (this.dailyBoostPeriodsProvider != null) {
			aMessageFile.dailyBoostPeriodsProvider = 
				(DailyBoostPeriodsProvider)this.dailyBoostPeriodsProvider.clone();
		}
		if (this.boostParams != null) {
			aMessageFile.boostParams = (BoostParams)this.boostParams.clone();
		}
		
		return aMessageFile;
	}
	
	public long getBatchNumber() {
		return batchNumber;
	}

	public void setBatchNumber(long batchNumber) {
		this.batchNumber = batchNumber;
	}

	public int getSlotId() {
		return slotId;
	}

	public void setSlotId(int slotId) {
		this.slotId = slotId;
	}

	public String getMessageFilePath() {
		return messageFilePath;
	}

	public void setMessageFilePath(String messageFilePath) {
		this.messageFilePath = messageFilePath;
	}

	public String getMessageFileName() {
		return messageFileName;
	}
	
	public String getReplaceSourcesText() {
		return replaceSourcesText;
	}

	public void setReplaceSourcesText(String replaceSourcesText) {
		this.replaceSourcesText = replaceSourcesText;
	}

	public String getReplaceTargetsText() {
		return replaceTargetsText;
	}

	public void setReplaceTargetsText(String replaceTargetsText) {
		this.replaceTargetsText = replaceTargetsText;
	}

	public boolean isInvalid() {
		return isInvalid;
	}
//
//	public void setEmpty(boolean isEmpty) {
//		this.isEmpty = isEmpty;
//	}

	@Override
	public String toString() {
		return "MessageFile [batchNumber=" + batchNumber + ", slotId=" + slotId + ", messageFilePath="
			+ messageFilePath + ", replaceSourcesRegex=" + replaceSourcesText + ", replaceTargetsRegex="
			+ replaceTargetsText + ", isInvalid=" + isInvalid + "]";
	}
	
	public long getLineNumber() {
//		return (
//			slotId == -1 ?
//			-1 :
//			(batchNumber * SyslogSenderMain.getInstance().getConfiguration().getMaxSlots()) +
//			slotId + 1
//		);
		
		return slotId;
	}
	
	public boolean validate() throws DeviceEventException {
		if (isInvalid) {
			throw new DeviceEventException(
				String.format(
					"MessageFile is invalid possibly because message files bundle line is invalid or blank: "
						+ "(BatchNo = %d, SlotId = %d, LineNo = %d",
					getBatchNumber(),
					getSlotId(),
					getLineNumber()
				)
			);
		}
		
		// [2018-11-05] SH: Check message file lines.
		File file = new File(this.getMessageFilePath());
		LineIterator lineIterator;
		try {
			lineIterator = FileUtils.lineIterator(file);
		} catch (IOException e) {
			throw new DeviceEventException(e.getLocalizedMessage());
		}
		int lines = 0;
		try {
			while (lineIterator.hasNext()) {
				lines++;
				lineIterator.nextLine();
			}
		} finally {
			LineIterator.closeQuietly(lineIterator);
		}
		
		this.lineCount = lines;
		if (this.lineCount == 0) {
			throw new DeviceEventException(
				String.format(
					"MessageFile [%s] is empty: No more to do and exiting the task.",
					this.getMessageFilePath()
				)
			);
		}

		LoggerUtils.getLogger().info(
			String.format(
				"Message file [%s] validated with [%d] lines. Starting processing...",
				this.getMessageFilePath(),
				this.lineCount
			)
		);
		
		return true;
	}

	public void sendAllMessages() throws IOException {
		openSyslogSenderIfNeeded();
		// [2020-09-19] KSH: Support for Kafka producer.
		openKafkaProducerIfNeeded();
		
//		if (1 == 0) {
//			Subsegment subsegment = AWSXRay.beginSubsegment("# Test");
//			
//			subsegment.setNamespace("Test");
//			
//			AWSXRay.endSubsegment();
//			
//			return;
//		}
		
		
		// [2020-09-20] KSH: AWS X-Ray Subsegment.
		// Wrap in Segment.
//		String segmentName = "AWS Event Generator Thread - " + this.messageFileName;
//		Segment segment = AWSXRay.beginSegment(segmentName);
//		
////		segment.setNamespace("remote");
////		segment.setOrigin("Test");
//		segment.putMetadata("message_file_path", this.messageFilePath);
//		
//		// Experimental.
//		TraceID traceId = TraceID.fromString(AwsEventGenerator.getInstance().getXRayTraceId());
//		String parentIdStr = AwsEventGenerator.getInstance().getXRayParentId();
//		segment.setTraceId(traceId);
//		segment.setParentId(parentIdStr);
		
		try {
			int iter = 0;
			do {
				try {
					prepareDataSources();
		
					String line = null;
					if (LoggerUtils.isVerbose) {
						LoggerUtils.verbose(
							"Message File [{}] Iteration [{}]: Started.",
							this.getMessageFilePath(),
							iter
						);
					}
					
					// [2018-11-01] SH: Wait ahead of sending loop if in peak boost,
					// instead of doing so in the loop.
					if ((boosted = checkIfNeedBoost())) {
						try {
							this.waitAwhileForBoost();
						} catch (InterruptedException e) {
							continue;
						}
					}
					
					// [2018-11-01] SH: Track nano time for peak boost up.
					// FIXME: Remove if we have performance impact.
					long startTimeNanos = System.nanoTime();
					int eventPos = 0;
					
					for (;
						AwsEventGenerator.getInstance().isRunning() &&
							(line = readLine(eventPos)) != null;
						eventPos++
					) {
						// [2018-11-12] Sang Hyoun: Deliver pos to take cache effective.
						
						// SENDING MESSAGE!
//						Subsegment subsegment = AWSXRay.beginSubsegment("## SendingMessage - " + iter + "." + eventPos);
//						subsegment.setNamespace("remote");
//						
////						Segment segment = AWSXRay.beginSegment("# sendMessage - " + iter + "." + eventPos,
////							TraceID.fromString(AwsEventGenerator.getInstance().getXRayTraceId()),
////							AwsEventGenerator.getInstance().getXRayParentId()
////						);
						
						
						String replacedLine = "";
						try {
							replacedLine = sendMessage(line, iter, eventPos);
						} finally {
//							AWSXRay.endSubsegment();
////							AWSXRay.endSegment();
						}
						
						LoggerUtils.verbose(
							"Message Sent [{},{}-{}]: " + replacedLine,
							this.getMessageFilePath(),
							iter,
							eventPos
						);
						
						// Sleep for a while at somewhere between initial interval, target interval,
						// and by warming-up period.
						// [2018-11-01] SH: Do not wait if it's on peak boost period.
//						if (!inPeakBoostPeriod) {
						if (!(boosted = checkIfNeedBoost())) {
							try {
								waitOrSuspend();
							} catch (InterruptedException e) {
								// Interrupted, so break the loop.
								break;
							}
						}
					}
					
					// FIXME: Remove if we have performance impact.
					lastElapsedTimeNanos = System.nanoTime() - startTimeNanos;
					if (DebugUtils.isDebug()) {
						DebugUtils.debugln(
							"Message File %s Iteration %d: Ended. %d nano secs elapsed",
							this.getMessageFilePath(),
							iter,
							lastElapsedTimeNanos
						);
					}
					if (LoggerUtils.isVerbose) {
						LoggerUtils.verbose(
							"Message File [{}] Iteration [{}]: Ended. [{}] nano secs elapsed",
							this.getMessageFilePath(),
							iter,
							lastElapsedTimeNanos
						);
					}
					
//					if (!inPeakBoostPeriod) {
					if (!(boosted = checkIfNeedBoost())) {
						this.currentBaselinedEPS = 
							calcLoadNanos(lastElapsedTimeNanos, eventPos);
//						setCurrentBaselinedEPS(
//							calcLoadNanos(lastElapsedTimeNanos, eventCount)
//						);
					}
				} catch (IOException e) {
					LoggerUtils.getLogger().warn(e.getLocalizedMessage());
					
					throw e;
				} finally {
					ensureCloseMessageFile();
				}
				
				iter++;
			} while (
				AwsEventGenerator.getInstance().isRunning() &&
				this.isRepeat()
			);
		}
		finally {
//			 [2020-09-20] KSH: AWS X-Ray.
//			AWSXRay.endSegment();
			
			ensureCleanupMessageCache();
			ensureCleanupRepalcedLinesCache();
			try {
				syslogSender.close();
			} catch (Exception e) {
				e.printStackTrace();
			}	
			
			// Close Kafka producer.
			try {
				if (kafkaProducer != null) {
					kafkaProducer.close(Duration.ofMillis(5000));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void openKafkaProducerIfNeeded() {
		if (kafkaProducer == null && !kafkaProducerDisabled) {
			Properties props = new Properties();
			props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.getKafkaBootstrapServers());
			props.put(ProducerConfig.CLIENT_ID_CONFIG, this.getKafkaClientId());
			props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
			props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
			
			// TODO: Interceptor for X-Ray.
			props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, "com.aws.proserve.korea.network.syslog.sender.KafkaProducerInterceptor");
			
			try {
				kafkaProducer = new KafkaProducer<String, String>(props);
			} catch (Exception e) {
				kafkaProducerDisabled  = true;
				
				// Disable Kafka sending.
				// TODO: Retry once in a while.
				e.printStackTrace();
			}
		}
	}

	private String getKafkaClientIdPrefix() {
		return this.kafkaClientIdPrefix;
	}
	
	private String getKafkaClientId() {
		return this.kafkaClientIdPrefix + Thread.currentThread().getName();
	}

	private String getKafkaBootstrapServers() {
		return kafkaBootstrapServers;
	}

	private void ensureCleanupRepalcedLinesCache() {
		if (this.replacedLinesCache != null) {
			this.replacedLinesCache.clear();
			this.replacedLinesCache = null;
		}
	}

	private boolean checkIfNeedBoost() {
		// True only when
		// 1. (
		//		Configured in messages files bundle ||
		//		Manually set by user ||
		//		Currently in peak boost period
		// ) &&
		// 2. The basedlined EPS is over threshold in config.properties.
		boolean peakBoostNeeded = (
			(
				this.isBoostedByConfig() ||
				this.boostedByUser ||
				dailyBoostPeriodsProvider.inPeakBoostPeriod()
			)
//			&&
			// [2018-11-07] No need to check baselined EPS any more.
			// Baselined EPS is fallen back to config, user input, or daily
			// peak boost period settings when needed.
//			this.currentBaselinedEPS >= this.boostBaselinedEPSThreshold
//				AwsEventGenerator.getInstance().getConfiguration().getDailyPeakBoostBaselinedEPSThreshold()
		);
		
		if (this.boosted != peakBoostNeeded) {
			// Peak boost state change.
			LoggerUtils.getLogger().info(
				String.format(
					"MessageFile: DailyPeakBoost state changed [%s] -> [%s]",
					(this.boosted ? "BOOSTED" : "BASELINED"),
					(peakBoostNeeded ? "BOOSTED" : "BASELINED")
				)
			);
		}
		
		return peakBoostNeeded;
	}

	private boolean isBoostedByConfig() {
		if (this.boostParams == null) {
			return false;
		}
		
		return boostParams.isBoosted();
	}
	
	public void setBoostedByConfig(boolean boosted) {
		if (this.boostParams != null) {
			this.boostParams.setBoosted(boosted);
		}
	}

	private double calcLoadNanos(long deltaTimeNanos, int deltaCount) {
		if (deltaTimeNanos <= 0) {
			return 0.0d;
		}
		
		return (double)(
			(double)deltaCount / ((double)deltaTimeNanos / 1000000000.d)
		);
	}

	private void waitAwhileForBoost() throws InterruptedException {
		long boostWaitTimeMillis = this.getCurrentBoostWaitTimeMillis();
		if (DebugUtils.isDebug()) {
			DebugUtils.debugln(
				"boostWaitTimeMillis: %d",
				boostWaitTimeMillis
			);
		}
		
		if (LoggerUtils.getLogger().isDebugEnabled()) {
			LoggerUtils.getLogger().debug(
				"boostWaitTimeMillis: {}",
				boostWaitTimeMillis
			);
		}
		
		if (boostWaitTimeMillis == 0) {
			// Something wrong.
			// Reset the flag.
			this.boosted = false;
			return;
		}
		
		waitOrSuspend(boostWaitTimeMillis);
	}

	private long getCurrentBoostWaitTimeMillis() {
		/*
		 * Calculation to get millis to wait.
		 * - 1. Target EPS (A): Current Baselined EPS x Boost Factor
		 * - 2. How many iterations of file needed (B): Frequency (F) x Lines of Message File
		 * - Our goal here is to get Frequency (F).
		 * - Let A = B, then derive F.
		 * - 3. A = B
		 * - 4. F = (Current Baselined EPS x Boost Factor) / (Lines of Message File)
		 * - 5. Period T (sec) = 1 / F = (Lines of Message File) / (Current Baselined EPS x Boost Factor)
		 * - 6. So, time to wait (millis) = (Lines of Message File) x 1000 / (Current Baselined EPS x Boost Factor)
		 * - 7. Considering some latency in operation clocks, "Wait Coefficient" can be used.
		 * - 8. Conclusion: e x (Lines of Message File) x 1000 / (Current Baselined EPS x Boost Factor)
		 *      Where "e" is wait coefficient (around 0.8).
		 */
		
		int lineCount = this.getLineCount();
		if (lineCount <= 0) {
			return 0;
		}
		
//		double currentBaselinedEPS = this.getCurrentBaselinedEPS();
		
//		if (currentBaselinedEPS < this.getBoostBaselinedEPSThreshold()) {
//			return 0;
//		}
		
		// Prioritized boost factor and baselined EPS.
		double baselinedEPS = this.getCurrentBaselinedEPS();
		double boostFactor = 0.0d;
		do {
			// [2018-11-09] Sang Hyoun
			// Priority 0: Over-boost.
			if (this.getDailyBoostPeriodsProvider()
				.inOverBoostPeriod()
			) {
				baselinedEPS = NumberUtils.max(
					baselinedEPS,
					this.getDailyBoostPeriodsProvider()
						.getCurrentOverBoostBaselinedEPS(),
					this.getBoostBaselinedEPSThreshold()
				);
				
				boostFactor = this.getDailyBoostPeriodsProvider()
					.getCurrentOverBoostFactor();
				break;
			}
			
			// Priority 1: User input.
			if (this.boostedByUser) {
				baselinedEPS = NumberUtils.max(
					baselinedEPS,
					this.getBaselinedEPSByUser(),
					this.getBoostBaselinedEPSThreshold()
				);
				boostFactor = this.getBoostFactorByUser();
				break;
			}
			
			// Priority 2: Config in message files bundle.
			if (this.boostParams.isBoosted()) {
				baselinedEPS = NumberUtils.max(
					baselinedEPS,
					this.boostParams.getBaselinedEPS(),
					this.getBoostBaselinedEPSThreshold()
				);
				boostFactor = this.boostParams.getBoostFactor();
				break;
			}
			
			// Priority 3: Daily peak boost periods.
			if (this.getDailyBoostPeriodsProvider().inPeakBoostPeriod()) {
				baselinedEPS = NumberUtils.max(
					baselinedEPS,
					this.getDailyBoostPeriodsProvider()
						.getCurrentPeakBoostBaselinedEPS(),
					this.getBoostBaselinedEPSThreshold()
				);
				boostFactor = this.getDailyBoostPeriodsProvider()
					.getCurrentPeakBoostFactor();
				break;
			}
			
			// Something else, then use default from config.
			baselinedEPS = this.getBoostBaselinedEPSThreshold();
			boostFactor = AwsEventGenerator.getInstance()
				.getConfiguration().getBoostFactor();
			break;
		} while (true);
		
////		double boostFactor = 0.0d;
//		if (this.boostedByUser) {
//			boostFactor = this.getBoostFactorByUser();
//		} else {
//			boostFactor = this.getDailyPeakBoostPeriodsProvider().getCurrentPeakBoostFactor();
//		}
		
		double waitCoefficient = AwsEventGenerator.getInstance().getConfiguration().getBoostWaitCoefficient();
		
		long ret = (long)(
			((double)waitCoefficient * (double)lineCount * 1000.) /
			(boostFactor * (double)baselinedEPS)
		);
		
		// Finally, filter cases where it should wait abnormally long.
//		if (ret >= 30000) {
//			return 0;
//		}
		
		return ret;
	}

	private double getBoostBaselinedEPSThreshold() {
		return this.boostBaselinedEPSThreshold;
	}

	private double getCurrentBaselinedEPS() {
		return this.currentBaselinedEPS ;
	}

//	private void setCurrentBaselinedEPS(double currentBaselinedEPS) {
//		this.currentBaselinedEPS = currentBaselinedEPS;
//	}
	
	private int getLineCount() {
		return this.lineCount ;
	}

	private String sendMessage(String line, int iter, int eventPos) throws IOException {
		String replacedLine = null;
		try {
//			// [2018-11-12] Sang Hyoun: Read from cache first.
//			if (this.replacedLinesCache.size() > eventPos) {
//				replacedLine = this.replacedLinesCache.get(eventPos);
//			} else {
				replacedLine = replaceMessagePatterns(line);
//				// Add to cache.
//				this.replacedLinesCache.add(eventPos, replacedLine);
//			}
		} catch (Throwable t) {
			LoggerUtils.getLogger().warn(
				String.format(
					"[ERROR] While replacing %s. %s",
					line,
					t.getLocalizedMessage()
				)
			);
			
			return line;
		}
		// Send a Syslog message
		syslogSender.sendMessage(replacedLine);
		
		// [2020-09-19] KSH: Send to Kafka.
//		kafkaSender.sendMessage(replacedLine);
		if (kafkaProducer != null) {
			Segment segment = AWSXRay.beginSegment(
				"AwsEventGenerator send"
			);
			segment.setNamespace("AWS::XRay::EventGenerator");
			
			try {
				final ProducerRecord<String, String> record = new ProducerRecord<String, String>(
					this.getKafkaTopic(),
					this.getKafkaClientId() + "-" + this.instanceDttm + ":" + iter + ":" + eventPos,
					replacedLine
				);
				
				kafkaProducer.send(
					record,
					(metadata, exception) -> {
		                if (metadata != null) {
		                    System.out.println("Record: -> " + record.key() + " | " + record.value());
		                }
		                else {
		                    System.out.println("Error Sending Record -> " + record.value());
		                }
					}
				);
			} finally {
				AWSXRay.endSegment();
			}
		}
		
		// Update metric.
		AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
			.addDeltas(
				this.syslogSender.getLastSentCount(),
				this.syslogSender.getLastSentBytesRaw(),
				this.syslogSender.getLastSentBytesEnvoloped()
			);
		this.addDeltas(
			this.syslogSender.getLastSentCount(),
			this.syslogSender.getLastSentBytesRaw(),
			this.syslogSender.getLastSentBytesEnvoloped()
		);

		// [2018-11-05] SH: Check daily limit.
		// If it touches daily limit again either event count or bytes,
		// then it will request all other tasks to suspend and suspend itself.
		// Related wait() lock will be release at program stop or 
		// Quartz triggers daily transition at midnight.
		if (AwsEventGenerator.getInstance()
			.getAwsEventGeneratorMetric().checkDailyLimitTouched()
		) {
//			AwsEventGenerator.getInstance().setDailyLimitTouched(true);
			AwsEventGenerator.getInstance().requestSuspend();
			// Also notify to monitor to refresh screen.
//			AwsEventGenerator.getInstance().getThreading().getMonitor().requestRefresh();
		}
		
		return replacedLine;
	}

	private String getKafkaTopic() {
		return this.kafkaTopic;
	}

	private void addDeltas(long lastSentCount, long lastSentBytesRaw, long lastSentBytesEnvoloped) {
		this.dailySentCount.addAndGet(lastSentCount);
		this.dailySentBytesRaw.addAndGet(lastSentBytesRaw);
		this.dailySentBytesEnveloped.addAndGet(lastSentBytesEnvoloped);
	}

	private void waitOrSuspend() throws InterruptedException {
		waitOrSuspend(0);
	}
	
	private void waitOrSuspend(long timeout) throws InterruptedException {
		// Double-check-and-lock against "suspended" state in main app.
		do {
			if (AwsEventGenerator.getInstance().isSuspended()) {
				// [2018-11-01] SH: Change lock to avoid contention.
//				synchronized (AwsEventGenerator.getInstance()) {
				synchronized (this) {
					if (AwsEventGenerator.getInstance().isSuspended()) {
						// Calculate time to prolong warming-up period.
						long millisBefore = System.currentTimeMillis();
						try {
//							AwsEventGenerator.getInstance().wait();
							wait();
						} finally {
							dilatedWarmingUpPeriodMillis.addAndGet(System.currentTimeMillis() - millisBefore);
						}
					}
				}
			} else {
				// [2018-11-01] SH: Change lock to avoid contention.
//				synchronized (AwsEventGenerator.getInstance()) {
				synchronized (this) {
					if (!AwsEventGenerator.getInstance().isSuspended()) {
						// TODO: Boost up!
//						AwsEventGenerator.getInstance().wait(
//							0,
//							1000
//						);
//						AwsEventGenerator.getInstance().wait(
//							getCurrentSendIntervalMillis() / 1000,
//							1000
//						);
						
//						AwsEventGenerator.getInstance().wait(
//							getCurrentSendIntervalMillis()
//						);
						wait(
							timeout <= 0 ?
							getCurrentSendIntervalMillis() :
							timeout
						);
					}
				}
			}
		} while (AwsEventGenerator.getInstance().isSuspended() &&
			AwsEventGenerator.getInstance().isRunning()
		);
	}

	private String replaceMessagePatterns(String line) {
		if (StringUtils.isBlank(line)) {
			return line;
		}
		
		if (ArrayUtils.isEmpty(this.replaceSources) || ArrayUtils.isEmpty(this.replaceTargets)) {
			return line;
		}
		
		// [2018-10-30] Kim, Sang Hyoun: Using RegExUtils from Commons lang3.
//		try {
//			String ret = StringUtils.replaceEachRepeatedly(line, replaceSources, replaceTargets);
//			return ret;
//		} catch (Exception e) {
//			LoggerUtils.getLogger().warn("replaceMessagePatterns() failed. Message line not changed!");
//			return line;
//		}
		
		for (int i = 0; i < this.replaceSources.length; i++) {
			String replaceSourceRegex = this.replaceSources[i];
			// [2018-11-09] Sang Hyoun: Replace with special case including datetime.
			String replacement = getReplacementConsideringSpecialTokens(this.replaceTargets[i]);

			// Find compiled pattern from the cache.
			Pattern pattern = null;
			try {
				pattern = regexHelper.getCachedPattern(replaceSourceRegex);
			} catch (Throwable t) {
				
			}
			if (pattern == null) {
				// Use string.
				line = RegExUtils.replaceAll(
					line,
					replaceSourceRegex,
					replacement
				);
			} else {
				line = RegExUtils.replaceAll(
					line,
					pattern,
					replacement
				);
			}
		}
		
		return line;
	}

	private String getReplacementConsideringSpecialTokens(String replacement) {
		if (replacement == null) return null;
		
		/*
Date and Time Pattern	Result
"yyyy.MM.dd G 'at' HH:mm:ss z"	2001.07.04 AD at 12:08:56 PDT
"EEE, MMM d, ''yy"	Wed, Jul 4, '01
"h:mm a"	12:08 PM
"hh 'o''clock' a, zzzz"	12 o'clock PM, Pacific Daylight Time
"K:mm a, z"	0:08 PM, PDT
"yyyyy.MMMMM.dd GGG hh:mm aaa"	02001.July.04 AD 12:08 PM
"EEE, d MMM yyyy HH:mm:ss Z"	Wed, 4 Jul 2001 12:08:56 -0700
"yyMMddHHmmssZ"	010704120856-0700
"yyyy-MM-dd'T'HH:mm:ss.SSSZ"	2001-07-04T12:08:56.235-0700
"yyyy-MM-dd'T'HH:mm:ss.SSSXXX"	2001-07-04T12:08:56.235-07:00
"YYYY-'W'ww-u"	2001-W27-3

CheckPoint-FW: MMM d HH:mm:ss
		 */
		
		try {
//			// Token 1: %DATETIME:{datetimeformat}%
//			if (StringUtils.startsWith(replacement, "%DATETIME:")) {
//				int colonIdx = StringUtils.indexOf(replacement, ':');
//				int rightPercentIdx = StringUtils.lastIndexOf(replacement, '%');
//				
//				if (colonIdx < 0 || rightPercentIdx < 1 || rightPercentIdx <= colonIdx) {
//					// Weird case.
//					return replacement;
//				}
//				
//				// Get format.
//				String format = StringUtils.mid(
//					replacement,
//					colonIdx + 1,
//					rightPercentIdx - colonIdx - 1	// Drop last %.
//				);
//				long currentTimeMillis = System.currentTimeMillis();
//				
//				String ret = DateFormatUtils.format(currentTimeMillis, format);
//				
//				return ret;
//			} else {
//				return replacement;
//			}
			
			int idx = 0, lastIdx = 0, lastAppendIdx = 0;
			StringBuilder sb = new StringBuilder();
//			int headerlen = "%DATETIME:".length();
			do {
				idx = StringUtils.indexOf(replacement, REPLACEMENT_HEADER_DATETIME, lastIdx);
				if (idx >= 0) {
					lastIdx = idx + 1;
					
					sb.append(replacement.substring(lastAppendIdx, idx));
					
					// Find '%' right behind "%DATETIME:".
					int percentIdx = StringUtils.indexOf(replacement, "%", idx + 1);
					lastAppendIdx = percentIdx + 1;
					// Get format.
					String format = StringUtils.mid(
						replacement,
						idx + REPLACEMENT_HEADER_DATETIME_LEN,
						percentIdx - (idx + REPLACEMENT_HEADER_DATETIME_LEN)	// Drop last %.
					);
					long currentTimeMillis = System.currentTimeMillis();
					
					String ret = DateFormatUtils.format(currentTimeMillis, format);
					
					sb.append(ret);
				}
				
			} while (idx >= 0);
			sb.append(replacement.substring(lastAppendIdx));
			
			return sb.toString();
//			System.out.println("sb: " + sb.toString());
		} catch (Throwable t) {
			LoggerUtils.getLogger().warn(
				String.format(
					"[ERROR] Getting replacement for special token %s. %s",
					replacement,
					t.getLocalizedMessage()
				)
			);
			
			return replacement;
		}
	}

	private void ensureCleanupMessageCache() {
		if (messageCache != null) {
			messageCache.clear();
			messageCache = null;
		}

	}

	private void ensureCloseMessageFile() {
		if (messageFileReader != null) {
			try {
				messageFileReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			messageFileReader = null;
		}
	}

	private String readLine(int pos) throws IOException {
		String line = null;
		
		// Try to get a line from cache first.
		if (isCacheMessage()) {
			// [2018-11-01] SH: Do not raise exception.
//			try {
//				line = messageCache.get(pos);
//			} catch (IndexOutOfBoundsException e) {
//				// Not pinned (cached) yet.
//				line = messageFileReader.readLine();
//				
//				// Add to cache.
//				messageCache.add(pos, line);
//			}
			
			// Check position.
			if (messageCache.size() > pos) {
				line = messageCache.get(pos);
			} else {
				line = messageFileReader.readLine();
				// Add to cache.
				messageCache.add(pos, line);
			}
			
		} else {
			line = messageFileReader.readLine();
		}
		
//		// Fall back to file read.
//		if (line == null) {
//			line = messageFileReader.readLine();
//			
//			if (isCacheMessage()) {
//				// Add to cache.
//				messageCache.add(pos, line);
//			}
//		}
		
		return line;
	}

	private void prepareDataSources() throws IOException {
//		if (messageFileReader == null) {
//			messageFileReader = new BufferedReader(
//				new FileReader(getMessageFilePath())
//			);
//		}
		
		// Initialize message cache when specified in configuration.
//		if (AwsEventGenerator.getInstance().getConfiguration().isCacheMessage() &&
		
		if (!isCacheMessage()) {
			// Open file anyhow in this case.
			openMessageFileIfNeeded();
		} else {
			if (messageCache == null) {
				// Cache not initialized yet.
				messageCache = new ArrayList<String>();
				
				// In this case, we have to open the file as well for first read.
				openMessageFileIfNeeded();
			}
		}
		
//		if (isCacheMessage() && messageCache == null) {
//			messageCache = new ArrayList<String>();
//		}
	}

	private void openMessageFileIfNeeded() throws FileNotFoundException {
		if (messageFileReader == null) {
			messageFileReader = new BufferedReader(
				new FileReader(getMessageFilePath())
			);
		}		
	}

	private long getCurrentSendIntervalMillis() {
		Date currentDttm = new Date();
		long elapsed = currentDttm.getTime() - startDttm.getTime();
		
		if (elapsed <= 0) {
			return initialSendIntervalMillis;
		}
		
		long adjustedWarmingUpPeriodMillis = (warmingUpPeriodSec * 1000) - dilatedWarmingUpPeriodMillis.get();
//		if (elapsed >= warmingUpPeriodSec * 1000) {
		if (elapsed >= adjustedWarmingUpPeriodMillis) {
			return targetSendIntervalMillis;
		}
		
		// Note: The return value will be bottom of each incremental step of
		// elapsed / warmingUpPeriodSec * 1000
		int intervalGapMillis = initialSendIntervalMillis - targetSendIntervalMillis;
		if (intervalGapMillis <= 0) {
			return targetSendIntervalMillis;
		}
		
//		double intervalGapFraction = ((double)elapsed / ((double)warmingUpPeriodSec * 1000.d));
		double intervalGapFraction = ((double)elapsed / (double)adjustedWarmingUpPeriodMillis);
		long subtractIntervalMillis = (long) ((double)intervalGapMillis * intervalGapFraction);
		long ret = (long) (
			initialSendIntervalMillis - subtractIntervalMillis
		);
		
		if (DebugUtils.isDebug()) {
			DebugUtils.debugln("initialSendIntervalMillis = %d, "
				+ "targetSendIntervalMillis = %d, "
				+ "intervalGapFraction = %f, "
				+ "subtractIntervalMillis = %d, " 
				+ "currentSendIntervalMillis = %d",
				initialSendIntervalMillis,
				targetSendIntervalMillis,
				intervalGapFraction,
				subtractIntervalMillis,
				ret
			);
		}
		
		if (LoggerUtils.getLogger().isDebugEnabled()) {
			String msg = String.format("initialSendIntervalMillis = %d, "
				+ "targetSendIntervalMillis = %d, "
				+ "intervalGapFraction = %f, "
				+ "subtractIntervalMillis = %d, " 
				+ "currentSendIntervalMillis = %d",
				initialSendIntervalMillis,
				targetSendIntervalMillis,
				intervalGapFraction,
				subtractIntervalMillis,
				ret
			);
			LoggerUtils.getLogger().debug(msg);
		}
		
		return ret;
	}

	public String getSyslogServer() {
		return syslogServer;
	}

	public void setSyslogServer(String syslogServer) {
		this.syslogServer = syslogServer;
	}

	public int getSyslogPort() {
		return syslogPort;
	}

	public void setSyslogPort(int syslogPort) {
		this.syslogPort = syslogPort;
	}

	public int getInitialSendIntervalMillis() {
		return initialSendIntervalMillis;
	}

	public void setInitialSendIntervalMillis(int initialSendIntervalMillis) {
		this.initialSendIntervalMillis = initialSendIntervalMillis;
	}

	public int getTargetSendIntervalMillis() {
		return targetSendIntervalMillis;
	}

	public void setTargetSendIntervalMillis(int targetSendIntervalMillis) {
		this.targetSendIntervalMillis = targetSendIntervalMillis;
	}

	public int getWarmingUpPeriodSec() {
		return warmingUpPeriodSec;
	}

	public void setWarmingUpPeriodSec(int warmingUpPeriodSec) {
		this.warmingUpPeriodSec = warmingUpPeriodSec;
	}

	public boolean isRepeat() {
		return isRepeat;
	}

	public void setRepeat(boolean isRepeat) {
		this.isRepeat = isRepeat;
	}

	public boolean isCacheMessage() {
		return isCacheMessage;
	}

	public void setCacheMessage(boolean isCacheMessage) {
		this.isCacheMessage = isCacheMessage;
	}

	public void prepare() {
		openSyslogSenderIfNeeded();
	}

	private void openSyslogSenderIfNeeded() {
		if (syslogSender == null) {
			// [2018-10-19] Kim, Sang Hyoun: Utilized consolidated Syslog utility source.
			// [2020-09-18] KSH: NullSyslogMessageSender for server address is in ["N/A", "n/a", "Null", "null", "false", "Disabled"].
			if (isSyslogServerEnabled()) {
				syslogSender = new UdpSyslogMessageSender();
			} else {
				syslogSender = new NullSyslogMessageSender();
			}
		}
		
		if (syslogSender instanceof UdpSyslogMessageSender) {
			((UdpSyslogMessageSender)syslogSender).setAllDefaults(
	//			InetAddress.getLocalHost().getHostAddress(),
				this.getSyslogMessageHostName(),
//				"Event_Generator",
				this.getSyslogAppName(),
//				Facility.USER,
				Facility.fromLabel(this.getSyslogFacility()),
//				Severity.INFORMATIONAL
				Severity.fromLabel(this.getSyslogSeverity())
			);
			((UdpSyslogMessageSender)syslogSender).setSyslogServerHostname(getSyslogServer());
			// S udp usually uses port 514 as per https://tools.ietf.org/html/rfc3164#page-5
			((UdpSyslogMessageSender)syslogSender).setSyslogServerPort(getSyslogPort());
			((UdpSyslogMessageSender)syslogSender).setMessageFormat(MessageFormat.RFC_3164); // optional, default is RFC 3164
		}
		
		// [2018-10-25] Kim, Sang Hyoun: Tweak PRI, Header part (Timestamp, Hostname, and AppName)
		// to cover cases with "almost fulfilled" syslog data only without PRI part.
		syslogSender.setPrependPRIPart(this.syslogPrependPRIPart);
		syslogSender.setPrependHeaderTimestamp(this.syslogPrependHeaderTimestamp);
		syslogSender.setPrependHeaderHostname(this.syslogPrependHeaderHostname);
		syslogSender.setPrependHeaderAppName(this.syslogPrependHeaderAppName);
	}

	private boolean isSyslogServerEnabled() {
		return !StringUtils.equalsAnyIgnoreCase(syslogServer, "N/A", "DISABLED", "FALSE");
	}

	public AbstractSyslogMessageSender getSyslogSender() {
		return syslogSender;
	}

	public String getSyslogMessageHostName() {
		return syslogMessageHostName;
	}

	public void setSyslogMessageHostName(String syslogMessageHostName) {
		this.syslogMessageHostName = syslogMessageHostName;
	}

	public String getSyslogAppName() {
		return syslogAppName;
	}

	public void setSyslogAppName(String syslogAppName) {
		this.syslogAppName = syslogAppName;
	}

	public String getSyslogFacility() {
		return syslogFacility;
	}

	public void setSyslogFacility(String syslogFacility) {
		this.syslogFacility = syslogFacility;
	}

	public String getSyslogSeverity() {
		return syslogSeverity;
	}

	public void setSyslogSeverity(String syslogSeverity) {
		this.syslogSeverity = syslogSeverity;
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

	public AwsEventGeneratorTask getAwsEventGeneratorTask() {
		return AwsEventGeneratorTask;
	}

	public void setAwsEventGeneratorTask(AwsEventGeneratorTask AwsEventGeneratorTask) {
		this.AwsEventGeneratorTask = AwsEventGeneratorTask;
	}

	public DailyBoostPeriodsProvider getDailyBoostPeriodsProvider() {
		return dailyBoostPeriodsProvider;
	}

	public void setDailyPeakBoostPeriodsProvider(DailyBoostPeriodsProvider dailyPeakBoostPeriodsProvider) {
		this.dailyBoostPeriodsProvider = dailyPeakBoostPeriodsProvider;
	}

	public BoostState getBoostState() {
		// Do not refresh each time. Just return as remembered at sending.
//		return this.getDailyPeakBoostPeriodsProvider().getDailyPeakBoostPeriodState();
		return (this.boosted ?
			BoostState.BOOSTED :
			BoostState.BASELINED
		);
	}

	public boolean isBoostedByUser() {
		return boostedByUser;
	}

	public void setBoostedByUser(boolean boostedByUser) {
		this.boostedByUser = boostedByUser;
	}

	public double getBoostFactorByUser() {
		return this.boostFactorByUser ;
	}
	
	public void setBoostFactorByUser(double boostFactorByUser) {
		this.boostFactorByUser = boostFactorByUser;
	}

	public void resetDailyMetrics() {
		this.dailySentCount.set(0);
		this.dailySentBytesRaw.set(0);
		this.dailySentBytesEnveloped.set(0);
		
		// Reserved flag and date.
		this.touchedDailyLimit = false;
		this.resetDate = new Date();
	}

	public AtomicLong getDailySentCount() {
		return dailySentCount;
	}

	public AtomicLong getDailySentBytesRaw() {
		return dailySentBytesRaw;
	}

	public AtomicLong getDailySentBytesEnveloped() {
		return dailySentBytesEnveloped;
	}

	public BoostParams getBoostParams() {
		return boostParams;
	}

	public double getBaselinedEPSByUser() {
		return baselinedEPSByUser;
	}

	public void setBaselinedEPSByUser(double baselinedEPSByUser) {
		this.baselinedEPSByUser = baselinedEPSByUser;
	}

	public static MessageFile copyFrom(MessageFile messageFile) {
		if (messageFile == null) {
			return new MessageFile(false);
		}
		
		// Deep copy.
		List<DailyBoostPeriod> dailyPeakBoostPeriodsList = new ArrayList<DailyBoostPeriod>();
		if (messageFile.getDailyBoostPeriodsProvider() != null &&
			messageFile.getDailyBoostPeriodsProvider().getDailyPeakBoostPeriodsList() != null
		) {
			for (DailyBoostPeriod period: messageFile.getDailyBoostPeriodsProvider()
				.getDailyPeakBoostPeriodsList()) {
				dailyPeakBoostPeriodsList.add(
					new DailyBoostPeriod(
						period.getBoostStartTimeStr(),
						period.getBoostEndTimeStr(),
						period.getPeakBoostBaselinedEPS(),
						period.getBoostFactor()
					)
				);
			}
		}
		
		List<DailyBoostPeriod> dailyOverBoostPeriodsList = new ArrayList<DailyBoostPeriod>();
		if (messageFile.getDailyBoostPeriodsProvider() != null &&
			messageFile.getDailyBoostPeriodsProvider().getDailyOverBoostPeriodsList() != null
		) {
			for (DailyBoostPeriod period: messageFile.getDailyBoostPeriodsProvider()
				.getDailyOverBoostPeriodsList()) {
				dailyOverBoostPeriodsList.add(
					new DailyBoostPeriod(
						period.getBoostStartTimeStr(),
						period.getBoostEndTimeStr(),
						period.getPeakBoostBaselinedEPS(),
						period.getBoostFactor()
					)
				);
			}
		}
		
		DailyBoostPeriodsProvider newProvider = new DailyBoostPeriodsProvider();
		newProvider.setDailyPeakBoostPeriodsList(dailyPeakBoostPeriodsList);
		newProvider.setDailyOverBoostPeriodsList(dailyOverBoostPeriodsList);
		
		BoostParams newBoostParams = null;
		if (messageFile.getBoostParams() != null) {
			newBoostParams = new BoostParams(
				messageFile.getBoostParams().getBaselinedEPS(),
				messageFile.getBoostParams().getBoostFactor()
			);
		}
		
		return new MessageFile(
			messageFile.getBatchNumber(),
			messageFile.getSlotId(),
			messageFile.getMessageFilePath(),
			messageFile.getReplaceSourcesText(),
			messageFile.getReplaceTargetsText(),
			messageFile.getSyslogServer(),
			messageFile.getSyslogPort(),
			messageFile.getInitialSendIntervalMillis(),
			messageFile.getTargetSendIntervalMillis(),
			messageFile.getWarmingUpPeriodSec(),
			messageFile.isRepeat(),
			messageFile.isCacheMessage(),
			messageFile.getSyslogMessageHostName(),
			messageFile.getSyslogAppName(),
			messageFile.getSyslogFacility(),
			messageFile.getSyslogSeverity(),
			messageFile.isSyslogPrependPRIPart(),
			messageFile.isSyslogPrependHeaderTimestamp(),
			messageFile.isSyslogPrependHeaderHostname(),
			messageFile.isSyslogPrependHeaderAppName(),
//			(DailyPeakBoostPeriodsProvider)messageFile.getDailyPeakBoostPeriodsProvider().clone(),
			newProvider,
			messageFile.getBoostBaselinedEPSThreshold(),
//			(BoostParams)messageFile.getBoostParams().clone()
			newBoostParams,
			messageFile.getKafkaBootstrapServers(),
			messageFile.getKafkaTopic(),
			messageFile.getKafkaClientIdPrefix()
		);
	}

}
