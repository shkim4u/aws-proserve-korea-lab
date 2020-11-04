package com.aws.proserve.korea.event.generator.view;

import java.util.Comparator;

import com.aws.proserve.korea.event.generator.performance.BoostState;
import com.aws.proserve.korea.event.generator.threading.AwsEventGeneratorTask;


/**
 * AwsEventGeneratorTaskInfo retrieves or updates the metrics for a 
 * specific AwsEventGeneratorTask.
 * 
 * 
 * @author Kim, Sang Hyoun
 *
 */
public class AwsEventGeneratorTaskInfo {

	private static final class EPSComparator implements
		Comparator<AwsEventGeneratorTaskInfo> {

		@Override
		public int compare(AwsEventGeneratorTaskInfo o1, AwsEventGeneratorTaskInfo o2) {
			return Double.valueOf(o2.getEPS()).compareTo(
				Double.valueOf(o1.getEPS())
			);
		}
	}

	private static final class TIDComparator implements
		Comparator<AwsEventGeneratorTaskInfo> {

		@Override
		public int compare(AwsEventGeneratorTaskInfo o1, AwsEventGeneratorTaskInfo o2) {
			return Long.valueOf(o1.getTid()).compareTo(
				Long.valueOf(o2.getTid())
			);
		}
		
	}
	
	public static final Comparator<AwsEventGeneratorTaskInfo> EPS_COMPARATOR =
		new EPSComparator();
	public static final Comparator<AwsEventGeneratorTaskInfo> TID_COMPARATOR =
		new TIDComparator();

	private AwsEventGeneratorTaskInfoState state = AwsEventGeneratorTaskInfoState.INIT;
	// [2018-10-18] Kim, Sang Hyoun: For future use.
//	private AwsEventGeneratorTaskInfoType type = AwsEventGeneratorTaskInfoType.INDIVIDUAL;
	
	private AwsEventGeneratorTask senderTask = null;
	
	protected String rawId;
	
//	private long lastUpTime = -1;
	protected long upTime = -1;
	protected long lastSentTime = -1;

	protected long sentCount = 0;
	protected long sentBytesEnveloped = 0;
	protected long sentBytesRaw = 0;
	protected long deltaSentCount = 0;
	protected long deltaSentBytesEnveloped = 0;
	protected long deltaSentBytesRaw = 0;
	
//	private long deltaUpTime;
	protected long deltaSentTime;

	protected double eps = 0.0d;
	protected double bpsEnveloped = 0.0d;
	protected double bpsRaw = 0.0d;

	protected BoostState boostState;

	protected long dailySentCount;

	protected long dailySentBytesRaw;

	protected long dailySentBytesEnveloped;
	
	private long tid;

	public AwsEventGeneratorTaskInfo(
		AwsEventGeneratorTask senderTask,
		long tid,
		String rawId
	) {
		this.state = AwsEventGeneratorTaskInfoState.ATTACHED;
		this.senderTask = senderTask;
		this.tid = tid;
		this.rawId = rawId;
//		this.type = (senderTask != null ? AwsEventGeneratorTaskInfoType.INDIVIDUAL :
//			AwsEventGeneratorTaskInfoType.OVERALL
//		);
		
		this.upTime = senderTask.getUpTimeAsLong();
		
		update();
	}

	public AwsEventGeneratorTaskInfo() {
		this.state = AwsEventGeneratorTaskInfoState.ATTACHED;
	}

	public void update() {
		if (state != AwsEventGeneratorTaskInfoState.ATTACHED) {
			return;
		}
		
		if (senderTask == null) {
			state = AwsEventGeneratorTaskInfoState.DETACHED;
			return;
		}
		
		// Update internal state.
		updateInternal();
	}

	public long getTid() {
		return this.tid;
	}
	
	protected void updateInternal() {
		long sentCount = senderTask.getSentCount();
		deltaSentCount = sentCount - this.sentCount;
		this.sentCount = sentCount;
		
		this.dailySentCount = senderTask.getDailySentCount();

		long sentBytesRaw = senderTask.getSentBytesRaw();
		deltaSentBytesRaw = sentBytesRaw - this.sentBytesRaw;
		this.sentBytesRaw = sentBytesRaw;

		this.dailySentBytesRaw = senderTask.getDailySentBytesRaw();
		
		long sentBytesEnveloped = senderTask.getSentBytesEnveloped();
		deltaSentBytesEnveloped = sentBytesEnveloped - this.sentBytesEnveloped;
		this.sentBytesEnveloped = sentBytesEnveloped;
		
		this.dailySentBytesEnveloped = senderTask.getDailySentBytesEnveloped();
		
		// Always update up time?
//		upTime = senderTask.getUpTimeAsLong();
		
		// Sent time.
		long lastSentTime = senderTask.getLastSentTimeMillis();
		if (this.lastSentTime > 0) {
			deltaSentTime = lastSentTime - this.lastSentTime;
			
			eps = calcLoad(deltaSentTime, deltaSentCount);
			bpsRaw = calcLoad(deltaSentTime, deltaSentBytesRaw);
			bpsEnveloped = calcLoad(deltaSentTime, deltaSentBytesEnveloped);
		}
		this.lastSentTime = lastSentTime;
		
		// [2018-11-05] SH: Daily peak boost period mode.
		this.boostState = senderTask.getBoostState();
	}

	protected double calcLoad(long deltaSentTimeMillis, long deltaSentCount) {
		if (deltaSentTimeMillis <= 0) {
			return 0.0d;
		}
		
		return (double)(
			(double)deltaSentCount / ((double)deltaSentTimeMillis / 1000.d)
		);
	}

	public AwsEventGeneratorTaskInfoState getState() {
		return state;
	}

	public void setState(AwsEventGeneratorTaskInfoState state) {
		this.state = state;
	}

	public AwsEventGeneratorTask getSenderTask() {
		return senderTask;
	}

	public void setSenderTask(AwsEventGeneratorTask senderTask) {
		this.senderTask = senderTask;
	}

//	public long getLastUpTime() {
//		return lastUpTime;
//	}
//
//	public void setLastUpTime(long lastUpTime) {
//		this.lastUpTime = lastUpTime;
//	}

	public long getLastSentTime() {
		return lastSentTime;
	}

	public void setLastSentTime(long lastSentTime) {
		this.lastSentTime = lastSentTime;
	}

	public long getSentCount() {
		return sentCount;
	}

	public void setSentCount(long sentCount) {
		this.sentCount = sentCount;
	}

	public long getSentBytesRaw() {
		return sentBytesRaw;
	}
	
	public void setSentBytesRaw(long sentBytesRaw) {
		this.sentBytesRaw = sentBytesRaw;
	}
	
	public long getSentBytesEnveloped() {
		return sentBytesEnveloped;
	}

	public void setSentBytesEnveloped(long sentBytesEnveloped) {
		this.sentBytesEnveloped = sentBytesEnveloped;
	}
	
	public long getDeltaSentCount() {
		return deltaSentCount;
	}

	public long getDeltaSentBytesRaw() {
		return deltaSentBytesRaw;
	}

	public long getDeltaSentBytesEnveloped() {
		return deltaSentBytesEnveloped;
	}

//	public long getDeltaUpTime() {
//		return deltaUpTime;
//	}
//
//	public void setDeltaUpTime(long deltaUpTime) {
//		this.deltaUpTime = deltaUpTime;
//	}

	public long getDeltaSentTime() {
		return deltaSentTime;
	}

	public void setDeltaSentTime(long deltaSentTime) {
		this.deltaSentTime = deltaSentTime;
	}

	public String getRawId() {
		return rawId;
	}

	public void setRawId(String rawId) {
		this.rawId = rawId;
	}

	public Long getId() {
		return senderTask.getThreadId();
	}

	public String getDisplayName() {
		return senderTask.getDisplayName();
	}
	
	public String getMessageFilePath() {
		return senderTask.getMessageFile().getMessageFilePath();
	}

	public String getMessageFileName() {
		return senderTask.getMessageFile().getMessageFileName();
	}

	public String getSyslogServer() {
		return senderTask.getMessageFile().getSyslogServer();
	}

	public int getSyslogPort() {
		return senderTask.getMessageFile().getSyslogPort();
	}

	public double getEPS() {
		return this.eps;
	}

	public double getBPSEnveloped() {
		return this.bpsEnveloped;
	}

	public double getBPSRaw() {
		return this.bpsRaw;
	}
	
	public long getUpTime() {
		return upTime;
	}

	public static AwsEventGeneratorTaskInfo createDeadTask(
		long tid, AwsEventGeneratorTask senderTask
	) {
		return createDeadTask(tid, senderTask, AwsEventGeneratorTaskInfoState.ERROR_DURING_ATTACH);
	}

	private static AwsEventGeneratorTaskInfo createDeadTask(
		long tid,
		AwsEventGeneratorTask senderTask,
		AwsEventGeneratorTaskInfoState state
	) {
		AwsEventGeneratorTaskInfo deviceEventSenderTaskInfo = 
			new AwsEventGeneratorTaskInfo();
		deviceEventSenderTaskInfo.state = state;
		deviceEventSenderTaskInfo.senderTask = senderTask;
		return deviceEventSenderTaskInfo;
	}

	public static AwsEventGeneratorTaskInfo attachToTask(
		long tid,
		AwsEventGeneratorTask senderTask
	) {
		return new AwsEventGeneratorTaskInfo(
			senderTask,
			tid,
			tid + ""
		);
	}

	public BoostState getBoostState() {
		return this.boostState;
	}

	public long getDailySentCount() {
		return this.dailySentCount;
	}

	public long getDailySentBytesRaw() {
		return this.dailySentBytesRaw;
	}

	public long getDailySentBytesEnveloped() {
		return this.dailySentBytesEnveloped;
	}


}
