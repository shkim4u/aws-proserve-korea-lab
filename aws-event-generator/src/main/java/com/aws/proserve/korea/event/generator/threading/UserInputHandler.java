package com.aws.proserve.korea.event.generator.threading;

import java.io.IOException;

import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.utils.ConsoleUtils;
import com.aws.proserve.korea.event.generator.utils.LoggerUtils;

public class UserInputHandler implements Runnable {
	
	private ActiveTasksThreadPoolExecutor executor;
	
	private boolean run = true;

	private MonitorThread monitor;

//	private ConsoleView consoleView;

	public UserInputHandler(
		ActiveTasksThreadPoolExecutor executor,
		MonitorThread monitor
//		ConsoleView consoleView
	) {
		this.executor = executor;
		this.monitor = monitor;
	}

	@Override
	public void run() {
		try {
			if (!handleInput('q')) {
//				System.exit(0);
				
				// [2018-10-24] Kim, Sang Hyoun: Stop the program.
				AwsEventGenerator.getInstance().stop();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean handleInput(char returnChar) throws IOException {
		boolean ret = true;
		try {
			char ch = 'h';
			do {
				try {
					ch = ConsoleUtils.readChar('h');
	
					switch (ch) {
					case 'p':
						monitor.togglePause();
						break;
						
					case 's':
						AwsEventGenerator.getInstance().toggleSuspend();
						break;
	
					case '1':
						monitor.setDelayMillis(1000);
						monitor.requestNotifyAll();
						break;
						
					case '2':
						monitor.setDelayMillis(3000);
						monitor.requestNotifyAll();
						break;
						
					case '3':
						monitor.setDelayMillis(5000);
						monitor.requestNotifyAll();
						break;
						
					case '4':
						monitor.setDelayMillis(10000);
						monitor.requestNotifyAll();
						break;
						
					case '5':
						monitor.setDelayMillis(20000);
						monitor.requestNotifyAll();
						break;
	
					// [2018-10-30] SH: Attache more device message file.
					case 'm':
						monitor.requestPause();
//						this.waitSilently();
						synchronized (monitor) {
							monitor.promptAndAttachMessageFilesBundle();
							handleInput('c');
						}
						monitor.requestResume();
						break;
						
					// Clone task.
					case 'o':
						monitor.requestPause();
//						this.waitSilently();
						synchronized (monitor) {
							monitor.promptAndCloneTasks();
							handleInput('c');
						}
						monitor.requestResume();
						break;
						
					case 'a':
						monitor.requestPause();
//						this.waitSilently();
						synchronized (monitor) {
							monitor.promptAndConfigureBoostMode(true);
							handleInput('c');
						}
						monitor.requestResume();
						break;
						
					case 'd':
						monitor.requestPause();
//						this.waitSilently();
						synchronized (monitor) {
							monitor.promptAndConfigureBoostMode(false);
							handleInput('c');
						}
						monitor.requestResume();
						break;
						
					// Modify daily limits.
					case 'l':
						monitor.requestPause();
//						this.waitSilently();
						synchronized (monitor) {
							monitor.promptAndModifyDailyLimits();
							handleInput('c');
						}
						monitor.requestResume();
						break;
						
//					case 'r':
//						monitor.requestPause();
//						this.waitSilently();
//						monitor.promptAndReportCurrentMetrics();
						
					case 'h':
	//					consoleView.pause();
	//					consoleView.showHelp();
	//					consoleView.resume();
						
						monitor.requestPause();
						// Wait for monitor thread to refresh first.
//						this.waitSilently();
						synchronized (AwsEventGenerator
							.getInstance().getThreading().getMonitor()) {
							monitor.showHelp();
							handleInput('c');
						}
						monitor.requestResume();
						break;
	
					case 'q':
						ret = false;
						break;
		
					default:
						break;
					}
	
					if (ret == false) {
						break;
					}
				} catch (Throwable t) {
					LoggerUtils.getLogger().warn(
						"[ERROR] handleInput: " + t.getLocalizedMessage()
					);
				}
			} while (ch != returnChar);
		} finally {
		}
		
		return ret;
	}

	private synchronized void waitSilently() {
		try {
			wait();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void shutdown() {
		this.run = false;
	}

	public synchronized void requestNotifyAll() {
		notifyAll();
	}

}
