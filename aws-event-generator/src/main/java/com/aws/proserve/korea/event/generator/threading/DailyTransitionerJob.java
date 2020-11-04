package com.aws.proserve.korea.event.generator.threading;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.utils.LoggerUtils;
import com.aws.proserve.korea.event.generator.view.OverallInfo;
import com.aws.proserve.korea.event.generator.view.StatisticsView;


public class DailyTransitionerJob implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// Request all tasks to suspend.
		AwsEventGenerator.getInstance().requestSuspend();
		
		this.saveStatistics();
		
		// Reset daily metric.
		AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().resetDailyMetrics();
		// Reset all daily metrics from tasks, too.
		AwsEventGeneratorTask.resetDailyMetricsOfAllTasks();
		
		// Reset daily limit touch flag.
		AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
			.setDailyLimitTouched(false);
		
		// Resume tasks.
		AwsEventGenerator.getInstance().requestResume();
		
		if (LoggerUtils.getLogger().isInfoEnabled()) {
			// This job simply prints out its job name and the
	        // date and time that it is running
	        JobKey jobKey = context.getJobDetail().getKey();
			LoggerUtils.getLogger().info(
				"Daily transition cron job successfully executed: " + jobKey + " at " + new Date()
			);
		}
	}

	private void saveStatistics() {
		Calendar calNow = Calendar.getInstance();
		String filePath = String.format(
			"daily_report_%04d%02d%02d-%02d%02d%02d-%03d.txt",
			calNow.get(Calendar.YEAR),
			calNow.get(Calendar.MONTH) + 1,
			calNow.get(Calendar.DAY_OF_MONTH),
			calNow.get(Calendar.HOUR_OF_DAY),
			calNow.get(Calendar.MINUTE),
			calNow.get(Calendar.SECOND),
			calNow.get(Calendar.MILLISECOND)
		);
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(filePath));
			
			// Print Header.
			out.write(
				String.format(
					StatisticsView.SENDER_TASK_DAILY_HEADER_FORMAT,
					"TID",
					"MSGFILE",
					"SERVER",
					"PORT",
					"EVENTS",
					"/DAY",
					"BYTES",
					"/DAY",
					"STATE"
				)
			);
			out.flush();
			
			// It's good to update the overall info at this moment.
			long overallSentCount = 0;
			long overallSentBytesRaw = 0;
			long overallSentBytesEnveloped = 0;
			long overallDailySentCount = 0;
			long overallDailySentBytesRaw = 0;
			long overallDailySentBytesEnveloped = 0;
			OverallInfo overallInfo = AwsEventGenerator.getInstance().getStatisticsView().getOverallInfo();
			// Iterate all tasks and save the daily metrics.
			int idx = 0;
			for (Runnable runnable:
				AwsEventGenerator.getInstance().getThreading().getExecutorPool().getActiveTasksSet()
			) {
				if (runnable != null &&
					runnable instanceof AwsEventGeneratorTask
				) {
					AwsEventGeneratorTask senderTask = (AwsEventGeneratorTask)runnable;
					synchronized (senderTask.getMessageFile()) {
						overallSentCount += senderTask.getSentCount();
						overallSentBytesRaw += senderTask.getSentBytesRaw();
						overallSentBytesEnveloped += senderTask.getSentBytesEnveloped();
						
						overallDailySentCount += senderTask.getDailySentCount();
						overallDailySentBytesRaw = senderTask.getDailySentBytesRaw();
						overallDailySentBytesEnveloped += senderTask.getDailySentBytesEnveloped();
						
						// Wait for each task to release its lock on message file object.
						// Then write the current status to file.
						String line = senderTask.makeFormattedStatisticsLine();
						out.write(line);
						out.flush();
						
						idx++;
					}
				}
			}
			
			overallInfo.setSentCount(overallSentCount);
			overallInfo.setSentBytesRaw(overallSentBytesRaw);
			overallInfo.setSentBytesEnveloped(overallSentBytesEnveloped);
			
			// [2018-11-08] Sang Hyoun: Update view perspective of daily metrics as well.
			overallInfo.setDailySentCount(overallDailySentCount);
			overallInfo.setDailySentBytesRaw(overallDailySentBytesRaw);
			overallInfo.setDailySentBytesEnveloped(overallDailySentBytesEnveloped);
			
			// Demarcation line.
			out.write(
				String.format("%s%n", StringUtils.repeat("=", 160))
			);
			out.flush();
			
			// Also write the overall info.
			out.write(overallInfo.makeFormattedStatisticsLine(idx));
			out.flush();
		} catch (Throwable t) {
			LoggerUtils.getLogger().warn(
				"[ERROR]: Daily report file save error. " + t.getLocalizedMessage()
			);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					LoggerUtils.getLogger().warn(
						"[ERROR]: Daily report file close failed. " + e.getLocalizedMessage()
					);
				}
			}
		}
	}

}
