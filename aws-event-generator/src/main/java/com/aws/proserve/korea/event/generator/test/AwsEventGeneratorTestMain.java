package com.aws.proserve.korea.event.generator.test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.validator.routines.RegexValidator;

import com.aws.proserve.korea.event.generator.utils.RegexHelper;

public class AwsEventGeneratorTestMain {

	public static class Test implements Cloneable {

		String message = null;
		
		Test(String message) {
			this.message = message;
		}
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			// TODO Auto-generated method stub
			return super.clone();
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
		
	}
	
	public static void main(String[] args) throws CloneNotSupportedException {
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			public void run() {
//				System.out.println("Exited!");
//			}
//		});
//		for (;;)
//			;
		
//		String[] colData = StringUtils.splitByWholeSeparatorPreserveAllTokens(
//			"192.168.0.1\\|192.168.0.2",
//			"\\|"
//		);
//		
//		for (int i = 0; i < colData.length; i++) {
//			System.out.println("colData[" + i + "]: " + colData[i]);
//		}
		
		if (1 == 0) {
			Test aTest = new Test("aTest");
			Test bTest = (Test)aTest.clone();
			
			System.out.println("aTest: " + aTest.getMessage());
			System.out.println("aTest: " + bTest.getMessage());

			bTest.setMessage("bTest");
			
			System.out.println("aTest: " + aTest.getMessage());
			System.out.println("aTest: " + bTest.getMessage());

			return;
		}
		
		if (1 == 1) {
			RegexHelper regexHelper = new RegexHelper();
			
			String replacements[] = new String[] {
				"%DATETIME:d MMM yyyy HH:mm:ss% %ASA %DATETIME:d MMM HH:mm:ss%",
				"%DATETIME:d MMM yyyy HH:mm:ss% %ASA",
				"%ASA %DATETIME:d MMM HH:mm:ss%",
				"%DATETIME:d MMM HH:mm:ss%",
				"%ASA"
			};
			for (int i = 0; i < replacements.length; i++) {
				String replacement = replacements[i];
				Pattern dttmPattern = regexHelper.getCachedPattern(replacement);
				RegexValidator regexValidator = new RegexValidator(
					new String[] {
						"(%DATETIME:.*%)"
					}
				);
				String[] groups = regexValidator.match(replacement);
	
				int idx = 0, lastIdx = 0, lastAppendIdx = 0;
				StringBuilder sb = new StringBuilder();
				int headerlen = "%DATETIME:".length();
				do {
					idx = StringUtils.indexOf(replacement, "%DATETIME:", lastIdx);
					if (idx >= 0) {
						lastIdx = idx + 1;
						
						sb.append(replacement.substring(lastAppendIdx, idx));
						
						// Find '%' right behind "%DATETIME:".
						int percentIdx = StringUtils.indexOf(replacement, "%", idx + 1);
						lastAppendIdx = percentIdx + 1;
						// Get format.
						String format = StringUtils.mid(
							replacement,
							idx + headerlen,
							percentIdx - (idx + headerlen)	// Drop last %.
						);
						long currentTimeMillis = System.currentTimeMillis();
						
						String ret = DateFormatUtils.format(currentTimeMillis, format);
						
						sb.append(ret);
					}
					
				} while (idx >= 0);
				sb.append(replacement.substring(lastAppendIdx));
				
				System.out.println("sb: " + sb.toString());
			}
			
			String log = "Sep 3 15:10:54 192.168.99.1 Checkpoint: 3Sep2007 15:10:28 accept 192.168.99.1 >eth2 rule: 9; rule_uid: {11111111-2222-3333-8A67-F54CED606693}; service_id: domain-udp; src: 200.14.120.9; dst: 192.168.99.184; proto: udp; product: VPN-1 & FireWall-1; service: 53; s_port: 32769;";

			String replaced = RegExUtils.replaceAll(
				log,
				RegexHelper.DATETIME_PATTERN_CHECKPOINT_FIREWALL_LEADING,
				"Test"
			);
			
			System.out.println(log + " => " + replaced);
			
			String messageFile = "../devices/CheckPoint-FW.txt,\"^(?:(((Jan(uary)?|Ma(r(ch)?|y)|Jul(y)?|Aug(ust)?|Oct(ober)?|Dec(ember)?)\\\\ 31)|((Jan(uary)?|Ma(r(ch)?|y)|Apr(il)?|Ju((ly?)|(ne?))|Aug(ust)?|Oct(ober)?|(Sep|Nov|Dec)(ember)?)\\\\ (0?[1-9]|([12]\\\\d)|30))|(Feb(ruary)?\\\\ (0?[1-9]|1\\\\d|2[0-8]|(29(?=,\\\\ ((1[6-9]|[2-9]\\\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00)))))))\\\\ )(?:[0-1]?[0-9]|[2][1-4]):[0-5]?[0-9]:[0-5]?[0-9]\",,172.16.100.100,504,1000,1,60,true,true,,,,,,,,,,,600|2";
			String[] cols = StringUtils.splitPreserveAllTokens(messageFile, ",\"");
			
			StringTokenizer tokenizer = new StringTokenizer(messageFile, ",");
//			tokenizer.setEmptyTokenAsNull(true);
			tokenizer.setIgnoreEmptyTokens(false);
			tokenizer.setQuoteChar('"');
			int idx = 0;
			while (tokenizer.hasNext()) {
				String token = tokenizer.next();
				System.out.println(
					String.format(
						"TOKEN[%d]: %s",
						++idx,
						token
					)
				);
			}
			
			List<String> list = tokenizer.getTokenList();
			
			
			return;
		}
		
		// [2018-11-02] DateUtils test.
		Date nowDttm = Calendar.getInstance().getTime();
		
		String peakBoostStartTimeStr = "08:30:00";
		String peakBoostEndTimeStr = "10:00:00";
		
		// Convert time string to number.
		Date startTime, endTime = null;
		long startTimeSecondsPassed, endTimeSecondsPassed = 0;
		try {
			startTime = DateUtils.parseDate(
				peakBoostStartTimeStr,
				new String[] {"HH:mm:ss"}
			);
			Calendar calStartTime = DateUtils.toCalendar(startTime);
			
			int hour = calStartTime.get(Calendar.HOUR_OF_DAY);
			int minute = calStartTime.get(Calendar.MINUTE);
			int second = calStartTime.get(Calendar.SECOND);
			
			long starTimeSecs = (3600 * hour + 60 * minute + second);
			
//			long startTimeMillis = startTime.getTime();
//			
//			ZonedDateTime now = ZonedDateTime.now();
//			ZonedDateTime midnight = now.at
//			Duration duration = Duration.between(midnight, now);
//			long secondsPassed = duration.getSeconds();
//			
//			
//			Calendar cal1 = DateUtils.toCalendar(startTime);
//			cal1.set(Calendar.HOUR_OF_DAY, 0);
//			cal1.set(Calendar.MINUTE, 0);
//			cal1.set(Calendar.SECOND, 0);
//			cal1.set(Calendar.MILLISECOND, 0);
//			long passed = startTimeMillis - cal1.getTimeInMillis();
//			long secondsPassed = passed / 1000;
			
			
			endTime = DateUtils.parseDate(
				peakBoostEndTimeStr,
				new String[] {"HH:mm:ss"}
			);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

//http://zguide.zeromq.org/java:interrupt
///*
//*
//*  Interrupt in Java
//*  Shows how to handle Ctrl-C
//*/
//
//import org.zeromq.ZMQ;
//import org.zeromq.ZMQException;
//
//public class Interrupt {
//   public static void main (String[] args) {
//      //  Prepare our context and socket
//      final ZMQ.Context context = ZMQ.context(1);
//
//      final Thread zmqThread = new Thread() {
//         @Override
//         public void run() {
//            ZMQ.Socket socket = context.socket(ZMQ.REP);
//            socket.bind("tcp://*:5555");
//
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    socket.recv (0);
//                } catch (ZMQException e) {
//                    if (e.getErrorCode () == ZMQ.Error.ETERM.getCode ()) {
//                        break;
//                    }
//                }
//            }
//
//            socket.close();
//         }
//      };
//
//      Runtime.getRuntime().addShutdownHook(new Thread() {
//         @Override
//         public void run() {
//            System.out.println("W: Interrupt received, killing server…");
//            context.term();
//            try {
//               zmqThread.interrupt();
//               zmqThread.join();
//            } catch (InterruptedException e) {
//            }
//         }
//      });
//
//      zmqThread.start();
//   }
//}
