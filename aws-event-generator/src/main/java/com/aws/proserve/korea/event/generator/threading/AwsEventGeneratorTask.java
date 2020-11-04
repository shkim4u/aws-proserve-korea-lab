package com.aws.proserve.korea.event.generator.threading;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.exception.DeviceEventException;
import com.aws.proserve.korea.event.generator.messagefile.MessageFile;
import com.aws.proserve.korea.event.generator.performance.BoostState;
import com.aws.proserve.korea.event.generator.utils.FormatUtils;
import com.aws.proserve.korea.event.generator.utils.LoggerUtils;
import com.aws.proserve.korea.event.generator.view.AwsEventGeneratorTaskInfo;
import com.aws.proserve.korea.event.generator.view.StatisticsView;

public class AwsEventGeneratorTask implements Runnable {

	// Message file info object parsed from each line of message files bundle file.
	MessageFile messageFile;
	private boolean running = false;
	private Long threadId;
	
	private Date upTime;
//	private Date lastSentTime;
	
	private String threadName;

	public AwsEventGeneratorTask() {
		super();
	}

	public AwsEventGeneratorTask(MessageFile messageFile) {
		super();
		this.messageFile = messageFile;
		this.messageFile.setAwsEventGeneratorTask(this);
		this.messageFile.prepare();
	}
	
	@Override
	public void run() {
		// Set running flag on.
		setRunning(true);

		// Set thread ID and name.
		setThreadId(Thread.currentThread().getId());
		setThreadName(Thread.currentThread().getName());
		
		// Set last up time.
		setUpTime(new Date());
		
		// Update.
		update();
		
		// [2014-09-13] Notify the main thread waiting for this worker thread re-runs well.
		synchronized (this) {
			this.notify();
		}
		
		Result result = new Result();
		int slotId = -1;
		boolean success = true;
		String resultMessage = "SUCCESS";
		try {
			if (messageFile == null) {
				throw new DeviceEventException("Message file info is null for processing.");
			}
			
			// Validate message file info.
			messageFile.validate();
			
			/*
			 * Loop the message file and send message texts from here.
			 */
			// Step 1. Open message file.
			messageFile.sendAllMessages();
		} catch (Exception e) {
			LoggerUtils.getLogger().warn(e.getLocalizedMessage(), e);
			success = false;
			resultMessage = String.format(
				"%s, %s",
				e.getLocalizedMessage(),
				messageFile.getMessageFilePath()
			);
		} finally {
			// [2018-11-06] Sang Hyoun: Collect result to report.
			result.setAttachedTaskID(this.threadId);
			result.setSyslogServer(this.messageFile.getSyslogServer());
			result.setSyslogPort(this.messageFile.getSyslogPort());
			result.setStartDate(this.upTime);
			result.setEndDate(new Date());
			result.setAccumulatedSentCount(messageFile.getSyslogSender().getSentCount());
			result.setDailySentCount(messageFile.getDailySentCount().get());
			result.setAccumulatedSentBytesRaw(messageFile.getSyslogSender().getSentBytesRaw());
			result.setDailySentBytesRaw(messageFile.getDailySentBytesRaw().get());
			result.setAccumulatedSentBytesEnveloped(messageFile.getSyslogSender().getSentBytesEnvoloped());
			result.setDailySentBytesEnveloped(messageFile.getDailySentBytesEnveloped().get());
			
			setResult(result, success, resultMessage);

			// Add to result output sink.
			// messageFile == null implies no input provide, and 
			// therefore we don't need to add to sink list.
			try {
				if (messageFile != null) {
					AwsEventGenerator.getInstance().getResultSink().drain(result);
				}
				
				synchronized (LoggerUtils.getLogger()) {
					LoggerUtils.getLogger().info(
						"Slot [{}], Message File [{}]: Sending message file complete - [{}]",
						slotId,
						messageFile.getMessageFilePath(),
						result.getResultMessage()
					);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			
			setRunning(false);
		}
	}

	public void setUpTime(Date upTime) {
		this.upTime = upTime;
	}

	public Date getUpTime() {
		return upTime;
	}
	
	public long getUpTimeAsLong() {
		return (upTime == null ? 0 : upTime.getTime());
	}
	
	private void update() {
		
	}

	private void setResult(
		Result result,
		boolean success,
		String resultMessage
	) {
		if (result == null) return;
		
		// Add to result output.
		int pos = (messageFile == null ? -1 : messageFile.getSlotId());
		String messageFilePath = (messageFile == null ? null : messageFile.getMessageFilePath());
		String delimiter = AwsEventGenerator.getInstance().getConfiguration().getResultOutputFileDelimiter();
		String newLineChar = AwsEventGenerator.getInstance().getConfiguration().discoverNewLineChar();
		
		result.setPos(pos);
		result.setMessageFilePath(messageFilePath);
		result.setSuccess(success);
		result.setResultMessage(resultMessage);
		result.setDelimiter(delimiter);
		result.setNewLineChar(newLineChar);
	}
	
	private void updateStats(boolean success) {
		AwsEventGenerator.getInstance().updateStats(success);
	}


	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running  = running;
	}

	public MessageFile getMessageFile() {
		return messageFile;
	}

	public void setMessageFile(MessageFile messageFile) {
		this.messageFile = messageFile;
	}

	public static Map<Long, AwsEventGeneratorTask> getNewAwsEventGeneratorTasks(
		Map<Long, AwsEventGeneratorTask> existingAwsEventGeneratorTaskMap
	) {
		Map<Long, AwsEventGeneratorTask> map = new HashMap<Long, AwsEventGeneratorTask>(existingAwsEventGeneratorTaskMap);
		getMonitoredTasks(map, existingAwsEventGeneratorTaskMap);
		return map;
	}

	public static void getMonitoredTasks(
		Map<Long, AwsEventGeneratorTask> map,
		Map<Long, AwsEventGeneratorTask> existingAwsEventGeneratorTaskMap
	) {
//		while (true) {
//			int activeCount = threading.executorPool.getActiveCount();
//			if (activeCount == 0) break;
//			
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//				
//				return;
//			}
//		}
		
		// Get all active tasks.
		Set<Runnable> runnables = AwsEventGenerator.getInstance().getThreading().
			getExecutorPool().getActiveTasksSet();
		for (Runnable runnable: runnables) {
			if (runnable != null) {
				if (runnable instanceof AwsEventGeneratorTask) {
					AwsEventGeneratorTask task = (AwsEventGeneratorTask)runnable;
					Long tid = task.getThreadId();
					if (!existingAwsEventGeneratorTaskMap.containsKey(tid)) {
						map.put(tid, task);
					}
				}
			}
		}
	}

	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}

	public static AwsEventGeneratorTaskInfo processNewTask(
		long tid,
		AwsEventGeneratorTask senderTask
	) {
		if (senderTask == null) {
			LoggerUtils.getLogger().warn(
				"Sender task null (TID={})",
				tid
			);
			return AwsEventGeneratorTaskInfo.createDeadTask(tid, senderTask);
		}
		
		return AwsEventGeneratorTaskInfo.attachToTask(tid, senderTask);
	}

//	public Date getLastSentTime() {
//		return lastSentTime;
//	}
//
//	public void setLastSentTime(Date lastSentTime) {
//		this.lastSentTime = lastSentTime;
//	}
//	
//	public long getLastSentTimeAsLong() {
//		return (lastSentTime == null ? 0 : lastSentTime.getTime());
//	}
	
	public long getLastSentTimeMillis() {
		return this.messageFile.getSyslogSender().getLastSentTimeMillis();
	}

	public String getDisplayName() {
		return threadName;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public long getSentCount() {
		return this.getMessageFile().getSyslogSender().getSentCount();
	}

	public long getSentBytesEnveloped() {
		return this.getMessageFile().getSyslogSender().getSentBytesEnvoloped();
	}

	public long getSentBytesRaw() {
		return this.getMessageFile().getSyslogSender().getSentBytesRaw();
	}

	public static void notifyToAllTasks() {
		for (Runnable runnable:
			AwsEventGenerator.getInstance().getThreading().getExecutorPool().getActiveTasksSet())
		{
			if (runnable instanceof AwsEventGeneratorTask) {
				AwsEventGeneratorTask task = (AwsEventGeneratorTask)runnable;
				synchronized (task.getMessageFile()) {
					task.getMessageFile().notify();
				}
			}
		}		
	}

	public BoostState getBoostState() {
		return this.messageFile.getBoostState();
	}

	public static AwsEventGeneratorTask exist(long tid) {
//		if (tid == null) return null;

		AwsEventGeneratorTask ret = null;
		// Iterate all the tasks and print infos.
		for (Runnable runnable:
			AwsEventGenerator.getInstance().getThreading().getExecutorPool().getActiveTasksSet()
		) {
			if (runnable != null &&
				runnable instanceof AwsEventGeneratorTask
			) {
				AwsEventGeneratorTask senderTask = (AwsEventGeneratorTask)runnable;
				if (senderTask.getThreadId().equals(tid)) {
					ret = senderTask;
					break;
				}
			}
		}
		
		return ret;
	}

	public static void activateBoostFor(
		String selection,
		double baselinedEPS,
		double boostFactor
	) {
		if (StringUtils.equalsIgnoreCase(selection, "a")) {
			// Iterate all the tasks and print infos.
			for (Runnable runnable:
				AwsEventGenerator.getInstance().getThreading().getExecutorPool().getActiveTasksSet()
			) {
				if (runnable != null &&
					runnable instanceof AwsEventGeneratorTask
				) {
					AwsEventGeneratorTask senderTask = (AwsEventGeneratorTask)runnable;
					senderTask.activateBoost(baselinedEPS, boostFactor);
				}
			}
		} else {
			long tid = Long.parseLong(selection);
			AwsEventGeneratorTask senderTask = exist(tid);
			if (senderTask != null) {
				senderTask.activateBoost(baselinedEPS, boostFactor);
			}
		}
		
	}

	public static void deactivateBoostFor(String selection) {
		if (StringUtils.equalsIgnoreCase(selection, "a")) {
			// Iterate all the tasks and print infos.
			for (Runnable runnable:
				AwsEventGenerator.getInstance().getThreading().getExecutorPool().getActiveTasksSet()
			) {
				if (runnable != null &&
					runnable instanceof AwsEventGeneratorTask
				) {
					AwsEventGeneratorTask senderTask = (AwsEventGeneratorTask)runnable;
					senderTask.deactivateBoost();
				}
			}
		} else {
			long tid = Long.parseLong(selection);
			AwsEventGeneratorTask senderTask = exist(tid);
			if (senderTask != null) {
				senderTask.deactivateBoost();
			}
		}
		
	}
	
	private void activateBoost(double baselinedEPS, double boostFactor) {
		this.getMessageFile().setBoostedByUser(true);
		this.getMessageFile().setBaselinedEPSByUser(baselinedEPS);
		this.getMessageFile().setBoostFactorByUser(boostFactor);
	}

	private void deactivateBoost() {
		this.getMessageFile().setBoostedByUser(false);
		this.getMessageFile().setBoostFactorByUser(1.0d);
		
		// [2018-11-08] Sang Hyoun: Deactivate boost by config as well.
		this.getMessageFile().setBoostedByConfig(false);
	}
	
	public static void resetDailyMetricsOfAllTasks() {
		// Iterate all the tasks and reset daily metrics.
		for (Runnable runnable:
			AwsEventGenerator.getInstance().getThreading().getExecutorPool().getActiveTasksSet()
		) {
			if (runnable != null &&
				runnable instanceof AwsEventGeneratorTask
			) {
				AwsEventGeneratorTask senderTask = (AwsEventGeneratorTask)runnable;
				senderTask.resetDailyMetrics();
			}
		}
	}

	private void resetDailyMetrics() {
		this.messageFile.resetDailyMetrics();
	}

	public long getDailySentCount() {
		return this.messageFile.getDailySentCount().get();
	}

	public long getDailySentBytesRaw() {
		return this.messageFile.getDailySentBytesRaw().get();
	}

	public long getDailySentBytesEnveloped() {
		return this.messageFile.getDailySentBytesEnveloped().get();
	}

	public static void cloneTasksFor(String selection) throws CloneNotSupportedException {
		if (StringUtils.equalsIgnoreCase(selection, "a")) {
			// Iterate all the tasks and print infos.
			for (Runnable runnable:
				AwsEventGenerator.getInstance().getThreading().getExecutorPool().getActiveTasksSet()
			) {
				if (runnable != null &&
					runnable instanceof AwsEventGeneratorTask
				) {
					AwsEventGeneratorTask senderTask = (AwsEventGeneratorTask)runnable;
					senderTask.cloneTask();
				}
			}
		} else {
			long tid = Long.parseLong(selection);
			AwsEventGeneratorTask senderTask = exist(tid);
			if (senderTask != null) {
				senderTask.cloneTask();
			}
		}
	}
	
//	public void cloneTask() throws CloneNotSupportedException {
//		try {
//			MessageFile aNewMessageFile = (MessageFile)this.messageFile.clone();
//			// Submit task.
//			AwsEventGenerator.getInstance()
//				.submitTask(aNewMessageFile, false);
//			
//			LoggerUtils.getLogger().info(
//				String.format(
//					"Tasks [%d] has successfully been cloned. Message file = %s",
//					this.getThreadId(),
//					aNewMessageFile.getMessageFilePath()
//				)
//			);
//		} catch (CloneNotSupportedException e) {
//			LoggerUtils.getLogger().info(
//				String.format(
//					"Tasks [%d] clone failed. %s",
//					this.getThreadId(),
//					e.getLocalizedMessage()
//				)
//			);
//			
//			throw e;
//		}
//	}
	
	public void cloneTask() {
		MessageFile newMessageFile = MessageFile.copyFrom(this.messageFile);
		// Submit task.
		AwsEventGenerator.getInstance()
			.submitTask(newMessageFile, false);
		
		LoggerUtils.getLogger().info(
			String.format(
				"Tasks [%d] has successfully been cloned. Message file = %s",
				this.getThreadId(),
				newMessageFile.getMessageFilePath()
			)
		);
	}

	public String makeFormattedStatisticsLine() {
		String ret = String.format(
			StatisticsView.SENDER_TASK_DAILY_STATS_FORMAT,
			this.getThreadId() + "",
			StringUtils.abbreviateMiddle(
				this.getMessageFile().getMessageFilePath(),
				"[...]",
				40
			),
			StringUtils.abbreviateMiddle(
				this.getMessageFile().getSyslogServer(),
				"[...]",
				20
			),
			this.getMessageFile().getSyslogPort(),
			FormatUtils.dfNoDecimal.format(this.getSentCount()),
			FormatUtils.dfNoDecimal.format(this.getDailySentCount()),
//			FormatUtils.dfNoDecimal.format(this.getSentBytesRaw()),
//			FormatUtils.dfNoDecimal.format(this.getDailySentBytesRaw()),
			FormatUtils.dfNoDecimal.format(this.getSentBytesEnveloped()),
			FormatUtils.dfNoDecimal.format(this.getDailySentBytesEnveloped()),
			this.getBoostState()
		);
		
		return ret;
	}
}
