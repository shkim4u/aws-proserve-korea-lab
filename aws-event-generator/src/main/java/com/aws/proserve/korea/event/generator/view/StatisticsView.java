package com.aws.proserve.korea.event.generator.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.threading.AwsEventGeneratorTask;
import com.aws.proserve.korea.event.generator.utils.FormatUtils;

public class StatisticsView extends AbstractConsoleView {

	public static final String SENDER_TASK_DAILY_HEADER_FORMAT = "%10s %-40.40s %-20.20s %5s " +
//		"%14s %14s %10s %16s %16s %13s %16s %16s %13s " +
		"%14s %14s %16s %16s " +
		"%10s%n";
	
	public static final String SENDER_TASK_DAILY_STATS_FORMAT = "%10s %-40.40s %-20.20s %5d " +
//			"%14s %14s %10s %16s %16s %13s %16s %16s %13s " +
			"%14s %14s %16s %16s " +
			"%10s%n";
	
	public static final String PRINT_HEADER_LONG = "%10s %-40.40s %-20.20s %5s " +
		"%14s %14s %10s %16s %16s %13s %16s %16s %13s " +
		"%10s%n";
	
	public static final String PRINT_HEADER_SHORT = "%10s %-40.40s %-20.20s %5s " +
		"%14s %14s %10s %16s %16s %13s " +
		"%10s%n";

	private static final String PRINT_HEADER_SHORT_NO_STATE = "%10s %-40.40s %-20.20s %5s " +
		"%14s %14s %10s %16s %16s %13s%n";
	
	private static final String PRINT_HEADER_SHORT_NO_STATE_NO_TID_SHORT_FILEPATH_NO_BPS = "%-20.20s %-20.20s %5s " +
		"%14s %14s %10s %16s %16s%n";
	
	public static final String PRINT_VIEW_LONG = "%10s %-40.40s %-20.20s %5d " +
		"%14s %14s %10s %16s %16s %13s %16s %16s %13s " +
		"%10s%n";
		
	public static final String PRINT_VIEW_SHORT = "%10s %-40.40s %-20.20s %5d " +
		"%14s %14s %10s %16s %16s %13s " +
		"%10s%n";
	
	public static final String PRINT_VIEW_SHORT_NO_STATE_NO_TID_SHORT_FILEPATH_NO_BPS = "%-20.20s %-20.20s %5d " +
		"%14s %14s %10s %16s %16s%n";
	
	public static final String PRINT_VIEW_SHORT_NO_STATE = "%10s %-40.40s %-20.20s %5d " +
		"%14s %14s %10s %16s %16s %13s%n";

	private List<AwsEventGeneratorTaskInfo> deviceEventSenderTaskInfoList = new ArrayList<AwsEventGeneratorTaskInfo>();
	
	private Map<Long, AwsEventGeneratorTask> deviceEventSenderTaskMap =
		new HashMap<Long, AwsEventGeneratorTask>();

	private OverallInfo overallInfo =
		new OverallInfo(deviceEventSenderTaskInfoList);

	private boolean sortByEPS;

	private int dailyProgressBarRepeat = 0;
	
	public StatisticsView(Integer width, boolean sortByEPS) {
		super(width);
		this.sortByEPS = sortByEPS;
	}

	@Override
	public void printView() throws Exception {
		printHeader();
		
		// Scan for new submitted task.
		scanForNewAwsEventGeneratorTasks();
		
		updateAwsEventGeneratorTasks(deviceEventSenderTaskInfoList);
		
		if (this.sortByEPS) {
			Collections.sort(
				deviceEventSenderTaskInfoList, 
				AwsEventGeneratorTaskInfo.EPS_COMPARATOR
			);
		} else {
			Collections.sort(
				deviceEventSenderTaskInfoList,
				AwsEventGeneratorTaskInfo.TID_COMPARATOR
			);
		}
		
		for (AwsEventGeneratorTaskInfo deviceEventSenderTaskInfo:
			deviceEventSenderTaskInfoList
		) {
			if (deviceEventSenderTaskInfo.getState() ==
				AwsEventGeneratorTaskInfoState.ATTACHED
			) {
				printAwsEventGeneratorTaskInfo(deviceEventSenderTaskInfo);
			} else if (
				deviceEventSenderTaskInfo.getState() ==
				AwsEventGeneratorTaskInfoState.NOT_RUNNING
			) {
				System.out.printf(
					"%5d %-15.15s [ERROR: Could not fetch telemetries (Task NOT RUNNING?)] %n",
					deviceEventSenderTaskInfo.getId(),
					deviceEventSenderTaskInfo.getDisplayName()
				);
			}
		}
		
		printOverall();
		
		printDailyLimitStatus();
		
		printFooter();
	}

	private void printFooter() {
//		System.out.printf(
//			" [\'h\': help] %n"
//		);
	}

	private void printOverall() {
		overallInfo.update();
		printAwsEventGeneratorTaskInfo(overallInfo);
	}
	
	/**
	 * Should be called after printOverall() is executed.
	 */
	private void printDailyLimitStatus() {
		System.out.printf(
//			"%-13.13s (%s/%s | %s/%s | %s/%s) %s%n",
			"%-13.13s (events limit: %s/%s, bytes limit: %s/%s) %s%n",
			(AwsEventGenerator.getInstance().isDailyLimitTouched() ?
				"daily limited" :
				"daily running"
			),
			FormatUtils.dfNoDecimal.format(
				overallInfo.getDailySentCount()
			),
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getDailySentCountLimitFormatted(),
			FormatUtils.dfNoDecimal.format(
				overallInfo.getDailySentBytesEnveloped()
			),
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getDailySentBytesEnvelopedLimitFormatted(),
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getResetDateFromatted()
		);
	}

	private String showProgressBar() {
		String dailyProgressBar = StringUtils.repeat("==", ++dailyProgressBarRepeat );
		if (dailyProgressBarRepeat >= 20) {
			dailyProgressBarRepeat = 0;
		}
		return dailyProgressBar;
	}

	private void printAwsEventGeneratorTaskInfo(
		AwsEventGeneratorTaskInfo deviceEventSenderTaskInfo
	) {
//		System.out.printf(
//			"%5s %-30.30s %-20.20s %5s " +
//			"%10s %10s %10s %10s %10s%n",
//			"TID", "MSGFILE", "SERVER", "PORT",
//			"EVENTS", "EPS", "BYTES(RAW)", "BPS(RAW)", "BYTES(ENVLP)", "BPS(ENVLP)"
//		);
		
		System.out.printf(
			PRINT_VIEW_SHORT_NO_STATE_NO_TID_SHORT_FILEPATH_NO_BPS,
//			deviceEventSenderTaskInfo.getId(),
//			deviceEventSenderTaskInfo.getRawId(),
			StringUtils.abbreviateMiddle(
//				deviceEventSenderTaskInfo.getMessageFilePath(),
				deviceEventSenderTaskInfo.getMessageFileName(),
				"[...]",
				20
			),
			StringUtils.abbreviateMiddle(
				deviceEventSenderTaskInfo.getSyslogServer(),
				"[...]",
				20
			),
			deviceEventSenderTaskInfo.getSyslogPort(),
//			"(N/A)",
			// Marker.
			FormatUtils.dfNoDecimal.format(deviceEventSenderTaskInfo.getSentCount()),
			FormatUtils.dfNoDecimal.format(deviceEventSenderTaskInfo.getDailySentCount()),
			FormatUtils.dfOneDecimal.format(deviceEventSenderTaskInfo.getEPS()),
//			FormatUtils.dfNoDecimal.format(deviceEventSenderTaskInfo.getSentBytesRaw()),
//			FormatUtils.dfNoDecimal.format(deviceEventSenderTaskInfo.getDailySentBytesRaw()),
////			FileUtils.byteCountToDisplaySize(deviceEventSenderTaskInfo.getSentBytesRaw()),
//			FormatUtils.dfOneDecimal.format(deviceEventSenderTaskInfo.getBPSRaw()),
			FormatUtils.dfNoDecimal.format(deviceEventSenderTaskInfo.getSentBytesEnveloped()),
			FormatUtils.dfNoDecimal.format(deviceEventSenderTaskInfo.getDailySentBytesEnveloped())
//			FileUtils.byteCountToDisplaySize(deviceEventSenderTaskInfo.getSentBytesEnveloped()),
			
//			FormatUtils.dfOneDecimal.format(deviceEventSenderTaskInfo.getBPSEnveloped())
			
//			deviceEventSenderTaskInfo.getBoostState()
		);	
	}

	private void scanForNewAwsEventGeneratorTasks() {
		Map<Long, AwsEventGeneratorTask> senderTasks = AwsEventGeneratorTask.
			getNewAwsEventGeneratorTasks(deviceEventSenderTaskMap);
		
		Set<Entry<Long, AwsEventGeneratorTask>> set = senderTasks.entrySet();
		for (Entry<Long, AwsEventGeneratorTask> entry: set) {
			AwsEventGeneratorTask senderTask = entry.getValue();
			long tid = senderTask.getThreadId();
			if (!deviceEventSenderTaskMap.containsKey(tid)) {
				AwsEventGeneratorTaskInfo taskInfo = AwsEventGeneratorTask.processNewTask(
					tid,
					senderTask
				);
				deviceEventSenderTaskInfoList.add(taskInfo);
			}
		}
		
		deviceEventSenderTaskMap = senderTasks;
	}

	private void updateAwsEventGeneratorTasks(
		List<AwsEventGeneratorTaskInfo> deviceEventSenderTaskInfoList
	) {
		long overallSentCount = 0;
		long overallSentBytesRaw = 0;
		long overallSentBytesEnveloped = 0;
		long overallDailySentCount = 0;
		long overallDailySentBytesRaw = 0;
		long overallDailySentBytesEnveloped = 0;
		for (AwsEventGeneratorTaskInfo deviceEventSenderTaskInfo: deviceEventSenderTaskInfoList) {
			deviceEventSenderTaskInfo.update();
			
			overallSentCount += deviceEventSenderTaskInfo.getSentCount();
			overallSentBytesRaw += deviceEventSenderTaskInfo.getSentBytesRaw();
			overallSentBytesEnveloped += deviceEventSenderTaskInfo.getSentBytesEnveloped();
			
			overallDailySentCount += deviceEventSenderTaskInfo.getDailySentCount();
			overallDailySentBytesRaw = deviceEventSenderTaskInfo.getDailySentBytesRaw();
			overallDailySentBytesEnveloped += deviceEventSenderTaskInfo.getDailySentBytesEnveloped();
			
//			// Add deltas.
//			overallInfo.addDeltas(
//				deviceEventSenderTaskInfo.getDeltaSentCount(),
//				deviceEventSenderTaskInfo.getDeltaSentBytesRaw(),
//				deviceEventSenderTaskInfo.getDeltaSentBytesEnveloped()
//			);
		}
		
		overallInfo.setSentCount(overallSentCount);
		overallInfo.setSentBytesRaw(overallSentBytesRaw);
		overallInfo.setSentBytesEnveloped(overallSentBytesEnveloped);
		
		// [2018-11-08] Sang Hyoun: Update view perspective of daily metrics as well.
		overallInfo.setDailySentCount(overallDailySentCount);
		overallInfo.setDailySentBytesRaw(overallDailySentBytesRaw);
		overallInfo.setDailySentBytesEnveloped(overallDailySentBytesEnveloped);
	}
	
//	JvmTop 0.8.0 alpha - 17:19:52, x86_64,  4 cpus,  Mac OS X 10.14, load avg 2.23
//	      https://github.com/patric-r/jvmtop
//
//	       PID MAIN-CLASS      HPCUR HPMAX NHCUR NHMAX    CPU     GC    VM USERNAME   #T DL
//	      1797 m.jvmtop.JvmTop   12m 3641m   15m   n/a 53.53%  0.00% O8U16 shkim4u1   18   
//	       957                  684m 1024m  443m   n/a  5.77%  0.00% O8U10 shkim4u1   74   
//	      1537 m.jvmtop.JvmTop   10m 3641m   21m   n/a  0.21%  0.00% O8U16 shkim4u1   28   
//	     [2J[H JvmTop 0.8.0 alpha - 17:19:54, x86_64,  4 cpus,  Mac OS X 10.14, load avg 2.13
//	      https://github.com/patric-r/jvmtop

//	System.out.printf(
//			"%5s %-15.15s %5s %5s %5s %5s %6s %6s %5s %8s %4s %2s%n",
//        	"PID", "MAIN-CLASS", "HPCUR", "HPMAX", "NHCUR", "NHMAX", "CPU", "GC",
//        	"VM", "USERNAME", "#T", "DL");

	private void printHeader() {
//		System.out.printf(
//			PRINT_HEADER_SHORT,
//			"TID", "MSGFILE", "SERVER", "PORT",
//			"EVENTS", "/DAY", "EPS", "BYTES(RAW)", "/DAY", "BPS(RAW)", "BYTES(ENVLP)", "/DAY", "BPS(ENVLP)",
//			"STATE"
//		);
		
//		System.out.printf(
//			PRINT_HEADER_SHORT_NO_STATE,
//			"TID", "MSGFILE", "SERVER", "PORT",
//			"EVENTS", "/DAY", "EPS", "BYTES", "/DAY", "BPS"
//		);
		
		System.out.printf(
			PRINT_HEADER_SHORT_NO_STATE_NO_TID_SHORT_FILEPATH_NO_BPS,
			"MSGFILE", "SERVER", "PORT",
			"EVENTS", "/DAY", "EPS", "BYTES", "/DAY"
		);
	}

	@Override
	public void pause() {
		
	}

	@Override
	public void showHelp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	public OverallInfo getOverallInfo() {
		return overallInfo;
	}
}
