package com.aws.proserve.korea.event.generator.threading;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.AwsEventGenerator.Configs;
import com.aws.proserve.korea.event.generator.AwsEventGeneratorVersionInfo;
import com.aws.proserve.korea.event.generator.utils.AwsEventGeneratorFileUtils;
import com.aws.proserve.korea.event.generator.utils.ConsoleConstants;
import com.aws.proserve.korea.event.generator.utils.ConsoleUtils;
import com.aws.proserve.korea.event.generator.utils.ConversionUtils;
import com.aws.proserve.korea.event.generator.utils.LoggerUtils;
import com.aws.proserve.korea.event.generator.view.ConsoleView;

public class MonitorThread implements Runnable {

	// [2018-10-18] Kim, Sang Hyoun: ActiveTasksThreadPoolExecutor
//	private ThreadPoolExecutor executor;
	private ActiveTasksThreadPoolExecutor executor;
	
	private int delayMillis;
	
//	private boolean run = true;

	private ConsoleView statisticsView;

	private boolean paused;

	private Date startTime;

	private boolean stopped = false;
	
	public MonitorThread(
		ActiveTasksThreadPoolExecutor executor,
		int delayMillis,
		ConsoleView statisticsView
	) {
		this.executor = executor;
		this.delayMillis = delayMillis;
		this.statisticsView = statisticsView;
	}
	
//	public void shutdown() {
//		this.run = false;
//	}
	
	public void run() {
		setStartTime(new Date());
		
		while (AwsEventGenerator.getInstance().isRunning()) {
			LoggerUtils.getLogger().debug(
				String.format(
					"[Monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
					this.executor.getPoolSize(),
					this.executor.getCorePoolSize(),
					this.executor.getActiveCount(),
					this.executor.getCompletedTaskCount(),
					this.executor.getTaskCount(),
					this.executor.isShutdown(),
					this.executor.isTerminated()
				)
			);
			
//			try {
//				Thread.sleep(delayMillis);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			
			try {
				showView();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

//			// [2014-09-01] Kim, Sean
//			// To die more elegantly.
//			synchronized (this) {
//				try {
//					this.wait(delayMillis);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
		}
	}

	public Date getStartTime() {
		return startTime;
	}
	
	private void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	private void showView() throws Exception {
		System.setOut(
			new PrintStream(
				new BufferedOutputStream(
					new FileOutputStream(FileDescriptor.out)
				),
				false
			)
		);
		
//		while (!statisticsView.shouldExit()) {
		try {
			while (AwsEventGenerator.getInstance().isRunning()) {
	//			synchronized (this) {
	//				if (!paused) {
	//					refreshView(view);
	//				}
	//				// [2018-10-23] Kim, Sang Hyoun: Wait with notify.
	////				view.sleep(delayMillis);
	//				waitAwhile();
					
					refreshView();
					waitOrPause();
	//			}
			}
		} finally {
			// Exited the view loop, but need to wait for tasks and refresh view
			// once again.
			AwsEventGenerator.getInstance().awaitTasks();
			refreshView();
		}
	}
	
	private synchronized void refreshView() throws Exception {
		clearTerminal();
		printTopBar();
		this.statisticsView.printView();
		printBottomBar();
		System.out.flush();		
	}

	private void printBottomBar() {
		// [2018-11-22] SH: Just return when in background mode.
		if (AwsEventGenerator.getInstance().isBackground()) return;
		
		if (AwsEventGenerator.getInstance().isShowExtendedMenus()) {
			System.out.printf(
				" ['p': " + (!this.paused ? "pause" : "resume") + " refresh]" +
				" [1: 1s, 2: 3s, 3: 5s, 4: 10s, 5: 20s]%n" +
				" ['s': " + (!AwsEventGenerator.getInstance().isSuspended() ? "suspend" : "restart") + " sending]%n" +
				" ['m': attach device (m)essages bundle file]%n" +
				" ['o': cl(o)ne existing task(s)]%n" +
				" ['a': (a)ctivate peak boosting] ['d': (d)eactivate peak boosting]%n" +
	//			" ['h': help]" + 
				" ['l': modify daily (l)imits]%n" +	// ['v': (v)iew daily limits status]%n" +
				" ['q': (q)uit]%n"
			);
		} else {
			System.out.printf(
				" ['p': " + (!this.paused ? "pause" : "resume") + " refresh]" +
				" [1: 1s, 2: 3s, 3: 5s, 4: 10s, 5: 20s]%n" +
				" ['s': " + (!AwsEventGenerator.getInstance().isSuspended() ? "suspend" : "restart") + " sending]%n" +
	//			" ['h': help]" + 
				" ['q': (q)uit]%n"
			);
		}
	}

//	private void waitAwhile() throws InterruptedException {
//		wait(delayMillis);
//	}
	
	private void waitOrPause() throws InterruptedException {
		// Double-check-and-lock on paused state.
		if (paused) {
			synchronized (this) {
				if (paused) {
					// Finally refresh once again before waiting to show screen accordingly.
					try {
						refreshView();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					// Notify user input handler that it is now safe to go its works. 
//					AwsEventGenerator.getInstance()
//						.getUserInputHandler().requestNotifyAll();
					wait();
				}
			}
		} else {
			synchronized (this) {
				if (!paused) {
					wait(delayMillis);
				}
			}
		}
	}

	public void requestPause() {
		synchronized (this) {
			this.paused = true;
			this.notifyAll();
			
//			try {
//				refreshView(this.statisticsView);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}
	
	public void requestResume() {
		synchronized (this) {
			this.paused = false;
			this.notifyAll();
		}
	}
	
	public void togglePause() {
		synchronized (this) {
			this.paused = !this.paused;
			this.notifyAll();
		}
	}
	
	public void showHelp() {
		clearTerminal();
	
//		  Z,B,E,e   Global: 'Z' colors; 'B' bold; 'E'/'e' summary/task memory scale
//		  l,t,m     Toggle Summary: 'l' load avg; 't' task/cpu stats; 'm' memory info
//		  0,1,2,3,I Toggle: '0' zeros; '1/2/3' cpus or numa node views; 'I' Irix mode
//		  f,F,X     Fields: 'f'/'F' add/remove/order/sort; 'X' increase fixed-width
//
//		  L,&,<,> . Locate: 'L'/'&' find/again; Move sort column: '<'/'>' left/right
//		  R,H,V,J . Toggle: 'R' Sort; 'H' Threads; 'V' Forest view; 'J' Num justify
//		  c,i,S,j . Toggle: 'c' Cmd name/line; 'i' Idle; 'S' Time; 'j' Str justify
//		  x,y     . Toggle highlights: 'x' sort field; 'y' running tasks
//		  z,b     . Toggle: 'z' color/mono; 'b' bold/reverse (only if 'x' or 'y')
//		  u,U,o,O . Filter by: 'u'/'U' effective/any user; 'o'/'O' other criteria
//		  n,#,^O  . Set: 'n'/'#' max tasks displayed; Show: Ctrl+'O' other filter(s)
//		  C,...   . Toggle scroll coordinates msg for: up,down,left,right,home,end
//
//		  k,r       Manipulate tasks: 'k' kill; 'r' renice
//		  d or s    Set update interval
//		  W,Y       Write configuration file 'W'; Inspect other output 'Y'
//		  q         Quit
//		          ( commands shown with '.' require a visible task display window ) 
//		Press 'h' or '?' for help with Windows,
//		Type 'q' or <Esc> to continue 
		
		StringBuffer sb = new StringBuffer();
		sb.append("Help for Interactive Commands - " + 
			AwsEventGeneratorVersionInfo.getProgramShortName() + " " + 
			AwsEventGeneratorVersionInfo.getVersion() + AwsEventGeneratorFileUtils.NEW_LINE
		);
		sb.append("  1\tDelay 1.0 sec" + AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  2\tDelay 3.0 secs" + AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  3\tDelay 5.0 secs" + AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  4\tDelay 10.0 secs" + AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  5\tDelay 20.0 secs" + AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append(AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  p\tToggle: 'p' stop/resume refresh" + AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("Type 'c' to continue ");

		System.out.print(sb.toString());
		
		System.out.flush();
	}
	
	private void printTopBar() {
//		System.out.printf(" AwsEventGenerator %s - %8tT, %6s, %16s, %2d cpus, %15.15s",
//		System.out.printf(" AwsEventGenerator %s - %8tT, %6s, %16s, %2d cpus, %9d freemem, %9d totalmem",
		
		Date nowDttm = new Date();
//		String duration = DurationFormatUtils.formatDurationHMS(
//			nowDttm.getTime() - startTime.getTime()
//		);
		String duration = DurationFormatUtils.formatDuration(
			nowDttm.getTime() - startTime.getTime(),
			"**H:mm:ss**",
			true
		);
		
		System.out.printf(
//			" %s %s - %8tT up %10s, %6s, %2d cpus, " +
//			"%15.15s, %9d/%9d(mem), %-13.13s(%s/%s | %s/%s | %s/%s) %s",
			" %s %s - %8tT up %10s, %6s, %2d cpus, " +
			"%15.15s, %9d/%9d(mem)",
			AwsEventGeneratorVersionInfo.getProgramShortName(),
			AwsEventGeneratorVersionInfo.getVersion(),
			nowDttm,
			duration,
			SystemUtils.OS_ARCH,
//			SystemUtils.JAVA_RUNTIME_NAME,
			Runtime.getRuntime().availableProcessors(),
			SystemUtils.OS_NAME + " " + SystemUtils.OS_VERSION,
			Runtime.getRuntime().freeMemory(),
			Runtime.getRuntime().totalMemory()
//			(AwsEventGenerator.getInstance().isDailyLimitTouched() ?
//				"daily limited" :
//				"daily running"
//			),
//			FormatUtils.dfNoDecimal.format(
//				AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getDailySentCount()
//			),
//			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getDailySentCountLimitFormatted(),
//			FormatUtils.dfNoDecimal.format(
//				AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getDailySentBytesRaw()
//			),
//			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getDailySentBytesRawLimitFormatted(),
//			FormatUtils.dfNoDecimal.format(
//				AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getDailySentBytesEnveloped()
//			),
//			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getDailySentBytesEnvelopedLimitFormatted(),
//			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric().getResetDateFromatted()
		);

		System.out.println();
	}

	private void clearTerminal() {
		if (System.getProperty("os.name").contains("Windows")) {
			// hack
			System.out.printf("%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n");
//			System.out.printf("%n");
		} else if (System.getProperty("AwsEventGenerator.altClear") != null) {
			System.out.print('\f');
		} else {
			System.out.print(ConsoleConstants.CLEAR_TERMINAL_ANSI_CMD);
		}
	}

	public int getDelayMillis() {
		return delayMillis;
	}

	public void setDelayMillis(int delayMillis) {
		this.delayMillis = delayMillis;
	}

	public void requestRefresh() {
		try {
			refreshView();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void stop() {
		this.stopped  = true;
		notifyAll();
	}

	public synchronized void requestNotifyAll() {
		notifyAll();
	}

	public void promptAndAttachMessageFilesBundle() {
		// Clear terminal first.
		clearTerminal();
		
		// Restore console terminal to default state.
		// This is needed to take terminal echo on and others effective.
		ConsoleUtils.restoreTerminalSilently();
		
		String cwd = AwsEventGeneratorFileUtils.getCurrentWorkingDirectoryAbsolutePath();
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("Attach device mssage file." +
			AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  Current working directory: [" + cwd + "]" +
			AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  Relative or absolute path acceptable." +
			AwsEventGeneratorFileUtils.NEW_LINE);
		sb.append("  eg) ../devices/message_files_bundle2.txt, /home/user/message_files_bundle2.txt or C:\\User\\message_files_bundle2.txt" +
			AwsEventGeneratorFileUtils.NEW_LINE
		);
//		sb.append("Please type message file and press ENTER (Just press ENTER to return): ");
//		
		System.out.print(sb.toString());
		System.out.flush();
		
		String filePath = "";
		boolean fileExist = false;
//		Scanner scanner = null;
//		try {
//			scanner = new Scanner(System.in);
//			line = scanner.nextLine();
//		} finally {
//			scanner.close();
//		}
		
		try {
			do {
				System.out.print("Please type message file and press ENTER (Just press ENTER to return): ");
				System.out.flush();

				try {
					filePath = ConsoleUtils.readLine(false);
				} catch (IOException e) {
					LoggerUtils.getLogger().warn(
						"ConsoleUtils.readLine() error: " + e.getLocalizedMessage()
					);
					System.out.println("ERROR: " + e.getLocalizedMessage());
//					System.out.println("Exiting.");
					System.out.flush();
				}
				if (StringUtils.isBlank(filePath)) {
					break;
				}
				
				// Check existence of the file.
				fileExist = new File(filePath).exists();
				if (!fileExist) {
					System.out.println("File [" + filePath + "] not found!");
					System.out.flush();
				}
			} while (!fileExist);
		} finally {
			ConsoleUtils.standbyTerminalSilently();
		}
		
		if (StringUtils.isNotBlank(filePath)) {
			// Submit task with input message file path.
			try {
				AwsEventGenerator.getInstance().attachMessageFilesBundle(filePath);
				
				System.out.println("Message File [" + filePath + "] attached and tasks successfully submitted!");
				System.out.flush();
				
//				try {
//					Thread.sleep(300);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			} catch (IOException e) {
				LoggerUtils.getLogger().error(
					"[ERROR] Attaching message file [{}]. [{}]",
					filePath,
					e.getLocalizedMessage()
				);
				
				System.out.println("ERROR ATTACHING: " + e.getLocalizedMessage());
//				System.out.println("Exiting.");
				System.out.flush();
			}
		}
		
		System.out.print("Type 'c' to continue ");
		System.out.flush();
	}
	
	public void promptAndConfigureBoostMode(boolean activate) {
		// Clear terminal first.
		clearTerminal();
		
		// Restore console terminal to default state.
		// This is needed to take terminal echo on and others effective.
		ConsoleUtils.restoreTerminalSilently();
		
		StringBuffer sb = new StringBuffer();
		sb.append(
			String.format(
				"%s boost mode for sending tasks.",
				(activate ? "Activate" : "Deactivate")
			) +	AwsEventGeneratorFileUtils.NEW_LINE
		);
		sb.append("  Current sending tasks:" +
			AwsEventGeneratorFileUtils.NEW_LINE);
		// Header.
		sb.append(
			String.format(
				"%10s %-45.45s %-30.30s %5s %10s%n",
				"TID",
				"MSGFILE",
				"SERVER",
				"PORT",
				"STATE"
			)
		);
		// Iterate all the tasks and print infos.
		for (Runnable runnable:
			AwsEventGenerator.getInstance()
				.getThreading().getExecutorPool().getActiveTasksSet()
		) {
			if (runnable != null &&
				runnable instanceof AwsEventGeneratorTask
			) {
				AwsEventGeneratorTask senderTask = (AwsEventGeneratorTask)runnable;
				sb.append(
					String.format(
						"%10s %-45.45s %-30.30s %5d %10s%n",
						senderTask.getThreadId(),		// Equiv. RawID of TaskInfo.
						StringUtils.abbreviateMiddle(
							senderTask.getMessageFile().getMessageFilePath(),
							"[.]",
							45
						),
						StringUtils.abbreviateMiddle(
							senderTask.getMessageFile().getSyslogServer(),
							"[.]",
							30
						),
						senderTask.getMessageFile().getSyslogPort(),
						senderTask.getMessageFile().getBoostState()
					)
				);
						
			}
		}
		
		System.out.print(sb.toString());
		System.out.flush();
		
		String selection = "";
		boolean canceled = false;
		boolean taskExist = false;
		boolean allTasks = false;
		double baselinedEPS = 0.0d;
		String baselinedEPSStr = "";
		double boostFactor = 0.0d;
		String boostFactorStr = "";
		try {
			do {
				// Step 1: Take input for task(s).
				System.out.print("Type TID (one) or 'a' for all tasks. Press ENTER to return: ");
				System.out.flush();
				
				try {
					selection = ConsoleUtils.readLine(false);
				} catch (IOException e) {
					LoggerUtils.getLogger().warn(
						"ConsoleUtils.readLine() error: " + e.getLocalizedMessage()
					);
					System.out.println("ERROR: " + e.getLocalizedMessage());
//					System.out.println("Exiting.");
					System.out.flush();
				}
				if (StringUtils.isBlank(selection)) {
					canceled = true;
					break;
				}
				
				// Check existence of the selected TID.
				if (StringUtils.equalsIgnoreCase(selection, "a")) {
					allTasks = true;
					taskExist = true;
				} else {
					long tid = Long.parseLong(selection);
					allTasks = false;
					taskExist = (AwsEventGeneratorTask.exist(tid) != null);
					if (!taskExist) {
						System.out.println(
							String.format(
								"Task [%s] not found!",
								selection
							)
						);
						System.out.flush();
					}
				}
			} while (!taskExist);
			
			if (!canceled && activate) {
				do {
					// Step 3: Baselined EPS in case there is no real 
					// baselined EPS is not gathered yet.
					System.out.print("Baselined EPS(>=100.0) (Numeric with decimal point allowed, e.g. 500.0). Press ENTER to return: ");
					System.out.flush();
					
//					// Display suggested value.
//					ConsoleUtils.diaplaySuggestedInput("500");
					
					try {
						baselinedEPSStr = ConsoleUtils.readLine(false);
					} catch (IOException e) {
						LoggerUtils.getLogger().warn(
							"ConsoleUtils.readLine() error: " + e.getLocalizedMessage()
						);
						System.out.println("ERROR: " + e.getLocalizedMessage());
//						System.out.println("Exiting.");
						System.out.flush();
					}
					
					if (StringUtils.isBlank(baselinedEPSStr)) {
						canceled = true;
						break;
					}
					
					try {
						baselinedEPS = Double.parseDouble(baselinedEPSStr);
					} catch (NumberFormatException e) {
						System.out.println(
							String.format(
								"Baselined EPS [%s] not valid!",
								baselinedEPSStr
							)
						);
						System.out.flush();
					}
					
					if (baselinedEPS <= 100.0d) {
						String.format(
							"Baselined EPS must be larger than 100.0!"
						);
						System.out.flush();
					}
				} while (!(baselinedEPS > 100.0d));
			}
			
			if (!canceled && activate) {
				do {
					// Step 3: Boost factor.
					System.out.print("Boost Factor(>=1.0) (Numeric with decimal point allowed, e.g. 2.0). Press ENTER to return: ");
					System.out.flush();
					
					try {
						boostFactorStr = ConsoleUtils.readLine(false);
					} catch (IOException e) {
						LoggerUtils.getLogger().warn(
							"ConsoleUtils.readLine() error: " + e.getLocalizedMessage()
						);
						System.out.println("ERROR: " + e.getLocalizedMessage());
//						System.out.println("Exiting.");
						System.out.flush();
					}
					
					if (StringUtils.isBlank(boostFactorStr)) {
						canceled = true;
						break;
					}
					
					try {
						boostFactor = Double.parseDouble(boostFactorStr);
					} catch (NumberFormatException e) {
						System.out.println(
							String.format(
								"Boost factor [%s] not valid!",
								boostFactorStr
							)
						);
						System.out.flush();
					}
					
					if (boostFactor <= 1.0d) {
						String.format(
							"Boost factor must be larger than 1.0!"
						);
						System.out.flush();
					}
				} while (!(boostFactor > 1.0d));
			}
		} finally {
			ConsoleUtils.standbyTerminalSilently();
		}
		
		if (!canceled) {
			if (activate) {
				AwsEventGeneratorTask.activateBoostFor(
					selection,
					baselinedEPS,
					boostFactor
				);
				
				System.out.println(
					String.format(
						"Tasks [%s] have been successfully activated for boost.",
						(allTasks ? "ALL" : selection)
					)
				);
				System.out.flush();
			} else {
				AwsEventGeneratorTask.deactivateBoostFor(selection);
				
				System.out.println(
					String.format(
						"Tasks [%s] have been successfully deactivated from boost.",
						(allTasks ? "ALL" : selection)
					)
				);
				System.out.flush();
			}
		}
		
		System.out.print("Type 'c' to continue ");
		System.out.flush();
	}
	
	public void promptAndModifyDailyLimits() {
		// Clear terminal first.
		clearTerminal();
		
		// Restore console terminal to default state.
		// This is needed to take terminal echo on and others effective.
		ConsoleUtils.restoreTerminalSilently();
		
		StringBuffer sb = new StringBuffer();
		sb.append(
			String.format(
				"Modify daily limits for sending tasks."
			) + AwsEventGeneratorFileUtils.NEW_LINE
		);
		sb.append("  Current daily limits:" +
			AwsEventGeneratorFileUtils.NEW_LINE
		);
		// Header.
		sb.append(
			String.format(
				"%-45.45s %-15.15s%n",
				"DAILY_LIMITS_NAME",
				"VALUE"
			)
		);
		
		boolean isDailyLimitsModified = AwsEventGenerator.getInstance()
			.getAwsEventGeneratorMetric().isModified();
		// Values.
		sb.append(
			String.format(
				"%-45.45s %-15.15s%n",
				Configs.CONFIG_KEY_DAILY_SENT_COUNT_LIMIT,
				(isDailyLimitsModified ?
					AwsEventGenerator.getInstance()
						.getAwsEventGeneratorMetric().getDailySentBytesRawLimitStr() :
					AwsEventGenerator.getInstance()
						.getConfiguration()
						.getDailySentCountLimitDisplayString()
				)
			)
		);
		sb.append(
			String.format(
				"%-45.45s %-15.15s%n",
				Configs.CONFIG_KEY_DAILY_SENT_BYTES_RAW_LIMIT,
				(isDailyLimitsModified ?
					AwsEventGenerator.getInstance()
						.getAwsEventGeneratorMetric().getDailySentBytesRawLimitStr() :
					AwsEventGenerator.getInstance()
						.getConfiguration()
						.getDailySentBytesRawLimitDisplayString()
				)
			)
		);
		sb.append(
			String.format(
				"%-45.45s %-15.15s%n",
				Configs.CONFIG_KEY_DAILY_SENT_BYTES_ENVELOPED_LIMIT,
				(isDailyLimitsModified ?
					AwsEventGenerator.getInstance()
						.getAwsEventGeneratorMetric().getDailySentBytesEnvelopedLimitStr() :
					AwsEventGenerator.getInstance()
						.getConfiguration()
						.getDailySentBytesEnvelopedLimitDisplayString()
				)
			)
		);
		
		System.out.print(sb.toString());
		System.out.flush();
		
		String dailySentCountLimitStr = "";
		long dailySentCountLimit = 0;
		String dailySentBytesRawLimitStr = "";
		long dailySentBytesRawLimit = 0;
		String dailySentBytesEnvelopedLimitStr = "";
		long dailySentBytesEnvelopedLimit = 0;
		boolean canceled = false;
		boolean inputValid = true;
		
		try {
			do {
				// Step 1: Daily sent count limit.
				System.out.print("Type number for daily event count limit (0 for no limit). Press ENTER to return: ");
				System.out.flush();
				
				dailySentCountLimitStr = ConsoleUtils.readLineSilently(false);
				if (StringUtils.isBlank(dailySentCountLimitStr)) {
					canceled = true;
					break;
				}
				
				// Check if the input is number.
				try {
					dailySentCountLimit = Long.parseLong(dailySentCountLimitStr);
				} catch (NumberFormatException e) {
					inputValid = false;
					System.out.println(
						String.format(
							"Input [%s] is not a number!",
							dailySentCountLimitStr
						)
					);
					System.out.flush();
				}
				
//				if (StringUtils.isNumeric(dailySentCountLimitStr)) {
//					inputValid = false;
//					System.out.println(
//						String.format(
//							"Input [%s] is not a number!",
//							dailySentCountLimitStr
//						)
//					);
//					System.out.flush();
//				}
			} while (!inputValid);
			
			if (!canceled) {
				do {
					// Step 2: Daily sent bytes RAW limit.
					System.out.print("Type number for daily sent bytes RAW limit (eg. 10000, 1MiB, 300GiB, 0 for no limit). Press ENTER to return: ");
					System.out.flush();
					
					dailySentBytesRawLimitStr = ConsoleUtils.readLineSilently(false);
					if (StringUtils.isBlank(dailySentBytesRawLimitStr)) {
						canceled = true;
						break;
					}
					
					// Check if the input is valid.
					try {
						if (StringUtils.equalsIgnoreCase(
							dailySentBytesRawLimitStr.trim(),
							"0"
						)) {
							dailySentBytesRawLimit = 0l;
						} else {
							dailySentBytesRawLimit = ConversionUtils
								.parseAny(dailySentBytesRawLimitStr);
						}
					} catch (Exception e) {
						inputValid = false;
						
						System.out.println(
							String.format(
								"Input [%s] is not valid!",
								dailySentBytesRawLimitStr
							)
						);
						System.out.flush();
					}
				} while (!inputValid);
			}
				
			if (!canceled) {
				do {
					// Step 3: Daily sent bytes ENVELOPED limit.
					System.out.print("Type number for daily sent bytes ENVELOPED limit (eg. 10000, 1MiB, 300GiB, 0 for no limit). Press ENTER to return: ");
					System.out.flush();
					
					dailySentBytesEnvelopedLimitStr = ConsoleUtils.readLineSilently(false);
					if (StringUtils.isBlank(dailySentBytesEnvelopedLimitStr)) {
						canceled = true;
						break;
					}
					
					// Check if the input is valid.
					try {
						
						if (StringUtils.equalsIgnoreCase(
							dailySentBytesEnvelopedLimitStr.trim(),
							"0"
						)) {
							dailySentBytesEnvelopedLimit = 0l;
						} else {
							dailySentBytesEnvelopedLimit = ConversionUtils
								.parseAny(dailySentBytesEnvelopedLimitStr);
						}
					} catch (Exception e) {
						inputValid = false;
						
						System.out.println(
							String.format(
								"Input [%s] is not valid!",
								dailySentBytesEnvelopedLimitStr
							)
						);
						System.out.flush();
					}
				} while (!inputValid);
			}
		} finally {
			ConsoleUtils.standbyTerminalSilently();
		}
		
		if (!canceled) {
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
				.setDailySentCountLimit(dailySentCountLimit);
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
				.setDailySentCountLimitStr(dailySentCountLimitStr);
			
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
				.setDailySentBytesRawLimit(dailySentBytesRawLimit);
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
				.setDailySentBytesRawLimitStr(dailySentBytesRawLimitStr);
			
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
				.setDailySentBytesEnvelopedLimit(dailySentBytesEnvelopedLimit);
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
				.setDailySentBytesEnvelopedLimitStr(dailySentBytesEnvelopedLimitStr);
			
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
				.setModified(true);
			
			AwsEventGenerator.getInstance().getAwsEventGeneratorMetric()
				.setDailyLimitTouched(false);
			
			// Resume all tasks.
			AwsEventGenerator.getInstance().requestResume();
			
			System.out.println(
				String.format(
					"Daily limits have been successfully modified."
				)
			);
			System.out.flush();
		}
		
		System.out.print("Type 'c' to continue ");
		System.out.flush();
	}

	public void promptAndCloneTasks() {

		// Clear terminal first.
		clearTerminal();
		
		// Restore console terminal to default state.
		// This is needed to take terminal echo on and others effective.
		ConsoleUtils.restoreTerminalSilently();
		
		StringBuffer sb = new StringBuffer();
		sb.append(
			String.format(
				"Clone sending task(s) to new one(s)."
			) +	AwsEventGeneratorFileUtils.NEW_LINE
		);
		sb.append("  Current sending tasks:" +
			AwsEventGeneratorFileUtils.NEW_LINE);
		// Header.
		sb.append(
			String.format(
				"%10s %-45.45s %-30.30s %5s %10s%n",
				"TID",
				"MSGFILE",
				"SERVER",
				"PORT",
				"STATE"
			)
		);
		// Iterate all the tasks and print infos.
		for (Runnable runnable:
			AwsEventGenerator.getInstance()
				.getThreading().getExecutorPool().getActiveTasksSet()
		) {
			if (runnable != null &&
				runnable instanceof AwsEventGeneratorTask
			) {
				AwsEventGeneratorTask senderTask = (AwsEventGeneratorTask)runnable;
				sb.append(
					String.format(
						"%10s %-45.45s %-30.30s %5d %10s%n",
						senderTask.getThreadId(),		// Equiv. RawID of TaskInfo.
						StringUtils.abbreviateMiddle(
							senderTask.getMessageFile().getMessageFilePath(),
							"[.]",
							45
						),
						StringUtils.abbreviateMiddle(
							senderTask.getMessageFile().getSyslogServer(),
							"[.]",
							30
						),
						senderTask.getMessageFile().getSyslogPort(),
						senderTask.getMessageFile().getBoostState()
					)
				);
						
			}
		}
		
		System.out.print(sb.toString());
		System.out.flush();
		
		String selection = "";
		boolean canceled = false;
		boolean taskExist = false;
		boolean allTasks = false;
		try {
			do {
				// Step 1: Take input for task(s).
				System.out.print("Type TID (one) or 'a' for all tasks. Press ENTER to return: ");
				System.out.flush();
				
				try {
					selection = ConsoleUtils.readLine(false);
				} catch (IOException e) {
					LoggerUtils.getLogger().warn(
						"ConsoleUtils.readLine() error: " + e.getLocalizedMessage()
					);
					System.out.println("ERROR: " + e.getLocalizedMessage());
//					System.out.println("Exiting.");
					System.out.flush();
				}
				if (StringUtils.isBlank(selection)) {
					canceled = true;
					break;
				}
				
				// Check existence of the selected TID.
				if (StringUtils.equalsIgnoreCase(selection, "a")) {
					allTasks = true;
					taskExist = true;
				} else {
					long tid = Long.parseLong(selection);
					allTasks = false;
					taskExist = (AwsEventGeneratorTask.exist(tid) != null);
					if (!taskExist) {
						System.out.println(
							String.format(
								"Task [%s] not found!",
								selection
							)
						);
						System.out.flush();
					}
				}
			} while (!taskExist);
			
		} finally {
			ConsoleUtils.standbyTerminalSilently();
		}
		
		if (!canceled) {
			try {
				// Clone the selected task(s).
				AwsEventGeneratorTask.cloneTasksFor(selection);
				
				LoggerUtils.getLogger().info(
					String.format(
						"[SUCCESS] Task cloned for %s",
						selection
					)
				);
				
				System.out.println(
					String.format(
						"Task(s) [%s] have been successfully cloned to new task(s).",
						(allTasks ? "ALL" : selection)
					)
				);
				System.out.flush();
			} catch (CloneNotSupportedException e) {
				LoggerUtils.getLogger().error(
					String.format(
						"[ERROR] Cloning for %s. %s",
						selection,
						e.getLocalizedMessage()
					)
				);
				
				System.out.println("ERROR CLONING: " + e.getLocalizedMessage());
				System.out.flush();
			}
		}
		
		System.out.print("Type 'c' to continue ");
		System.out.flush();
	
	}
}
