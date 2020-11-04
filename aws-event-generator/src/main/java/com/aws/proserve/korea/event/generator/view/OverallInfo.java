package com.aws.proserve.korea.event.generator.view;

import java.util.List;

import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.utils.FormatUtils;

public final class OverallInfo extends AwsEventGeneratorTaskInfo {

//		private long sentCount = 0;
//		private long deltaSentCount = 0;
//		private long sentBytesRaw = 0;
//		private long deltaSentBytesRaw = 0;
//		private long sentBytesEnveloped = 0;
//		private long deltaSentBytesEnveloped = 0;

		private long lastUpdateTimeMillis = -1;
		private long deltaUpdateTimeMillis = 0;
		private long totalSentCount;
		private long totalSentBytesRaw;
		private long totalSentBytesEnveloped;
		
		private List<AwsEventGeneratorTaskInfo> deviceEventSenderTaskInfoList;
		private int numOfTotalDevices;
		
		public OverallInfo(
			List<AwsEventGeneratorTaskInfo> deviceEventSenderTaskInfoList
		) {
			super();
		
			this.deviceEventSenderTaskInfoList = deviceEventSenderTaskInfoList;
			// Set basic information as an overall info provider.
			this.setRawId("(ALL)");
		}
		
		public void addDeltas(long deltaSentCount, long deltaSentBytesRaw, long deltaSentBytesEnveloped) {
			// Sent count.
			this.sentCount += deltaSentCount;
//			this.deltaSentCount = deltaSentCount;
			
			// Sent bytes raw.
			this.sentBytesRaw += deltaSentBytesRaw;
//			this.deltaSentBytesRaw = deltaSentBytesRaw;
			
			// Sent bytes enveloped.
			this.sentBytesEnveloped += deltaSentBytesEnveloped;
//			this.deltaSentBytesEnveloped = deltaSentBytesEnveloped;
		}

		protected void updateInternal() {
			if (deviceEventSenderTaskInfoList != null) {
				numOfTotalDevices = deviceEventSenderTaskInfoList.size();
			}
			
			// SentCount updated in StatisticsView#updateAwsEventGeneratorTasks
			long sentCount = this.getSentCount();
			deltaSentCount = sentCount - this.totalSentCount;
			this.totalSentCount = sentCount;
			
			// [2018-11-08] Sang Hyoun: No need. Daily metrics already collected in StatisticsView.updateAwsEventGeneratorTasks();
//			this.dailySentCount = AwsEventGenerator.getInstance()
//				.getAwsEventGeneratorMetric().getDailySentCount().get();
			
			long sentBytesRaw = this.getSentBytesRaw();
			deltaSentBytesRaw = sentBytesRaw - this.totalSentBytesRaw;
			this.totalSentBytesRaw = sentBytesRaw;
			
//			this.dailySentBytesRaw = AwsEventGenerator.getInstance()
//				.getAwsEventGeneratorMetric().getDailySentBytesRaw().get();
			
			long sentBytesEnveloped = this.getSentBytesEnveloped();
			deltaSentBytesEnveloped = sentBytesEnveloped - this.totalSentBytesEnveloped;
			this.totalSentBytesEnveloped = sentBytesEnveloped;
			
//			this.dailySentBytesEnveloped = AwsEventGenerator.getInstance()
//				.getAwsEventGeneratorMetric().getDailySentBytesEnveloped().get();
			
			long lastUpdateTimeMillis = System.currentTimeMillis();
			if (this.lastUpdateTimeMillis  > 0) {
				deltaUpdateTimeMillis = lastUpdateTimeMillis - this.lastUpdateTimeMillis;
				
				if (numOfTotalDevices == 1) {
					AwsEventGeneratorTaskInfo desti = deviceEventSenderTaskInfoList.get(0);
					eps = desti.getEPS();
					bpsRaw = desti.getBPSRaw();
					bpsEnveloped = desti.getBPSEnveloped();
				} else {
					eps = calcLoad(deltaUpdateTimeMillis, deltaSentCount);
					bpsRaw = calcLoad(deltaUpdateTimeMillis, deltaSentBytesRaw);
					bpsEnveloped = calcLoad(deltaUpdateTimeMillis, deltaSentBytesEnveloped);
				}
			}
			this.lastUpdateTimeMillis = lastUpdateTimeMillis;
			
			// [2018-11-05] SH: Daily peak boost period mode.
			this.boostState = AwsEventGenerator
				.getInstance().getDailyPeakBoostPeriodState();
		}

		public long getLastUpdateTimeMillis() {
			return lastUpdateTimeMillis;
		}

		public long getDeltaUpdateTimeMillis() {
			return deltaUpdateTimeMillis;
		}

		@Override
		public String getDisplayName() {
			return "(OVERALL)";
		}

		@Override
		public String getMessageFilePath() {
			return "(N/A)";
		}
		
		@Override
		public String getMessageFileName() {
			return "(N/A)";
		}

		@Override
		public String getSyslogServer() {
			return "(N/A)";
		}

		@Override
		public int getSyslogPort() {
			return -1;
		}

		@Override
		public void update() {
			// Update internal state.
			updateInternal();
		}

		@Override
		public String getRawId() {
			return this.rawId + "[" + this.numOfTotalDevices + "]";
		}

		public void setDailySentCount(long dailySentCount) {
			this.dailySentCount = dailySentCount;
		}

		public void setDailySentBytesRaw(long dailySentBytesRaw) {
			this.dailySentBytesRaw = dailySentBytesRaw;
		}

		public void setDailySentBytesEnveloped(long dailySentBytesEnveloped) {
			this.dailySentBytesEnveloped = dailySentBytesEnveloped;
		}

		public String makeFormattedStatisticsLine(int count) {
			String ret = String.format(
				StatisticsView.SENDER_TASK_DAILY_STATS_FORMAT,
				"(ALL)[" + count + "]",
				"(N/A)",
				"(N/A)",
				-1,
				FormatUtils.dfNoDecimal.format(this.getSentCount()),
				FormatUtils.dfNoDecimal.format(this.getDailySentCount()),
//				FormatUtils.dfNoDecimal.format(this.getSentBytesRaw()),
//				FormatUtils.dfNoDecimal.format(this.getDailySentBytesRaw()),
				FormatUtils.dfNoDecimal.format(this.getSentBytesEnveloped()),
				FormatUtils.dfNoDecimal.format(this.getDailySentBytesEnveloped()),
				this.getBoostState()
			);
			
			return ret;
		}
	}