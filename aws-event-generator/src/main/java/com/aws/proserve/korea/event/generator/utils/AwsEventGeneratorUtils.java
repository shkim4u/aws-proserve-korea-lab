package com.aws.proserve.korea.event.generator.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class AwsEventGeneratorUtils {

	public static final String NEW_LINE = System.getProperty("line.separator");
	
	public enum FileType { WINDOWS, UNIX, MAC, UNKNOWN }

    public static final char CR = '\r';
    public static final char LF = '\n';

    public static boolean isWindowsFormat(String fileName) {
    	try {
			return (FileType.WINDOWS == discover(fileName));
		} catch (IOException e) {
			return false;
		} 
    }
    
    public static boolean isUnixFormat(String fileName) {
    	try {
    		return (FileType.UNIX == discover(fileName));
    	} catch (IOException e) {
    		return false;
    	}
    }
    
    public static boolean isMacFormat(String fileName) {
    	try {
    		return (FileType.MAC == discover(fileName));
    	} catch (IOException e) {
    		return false;
    	}
    }
    
	public static FileType discover(String fileName) throws IOException {
		Reader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			FileType result = discover(reader);
			return result;
		} finally {
			reader.close();
		}
	}

	private static FileType discover(Reader reader) throws IOException {
		int c;
		while ((c = reader.read()) != -1) {
			switch (c) {
			case LF:
				return FileType.UNIX;
			case CR: {
				if (reader.read() == LF)
					return FileType.WINDOWS;
				return FileType.MAC;
			}
			default:
				continue;
			}
		}
		return FileType.UNKNOWN;
	}

//	public static void traceOutputsQuietly(List outputs, boolean append) {
//		if (outputs == null || outputs.size() == 0) return;
//		File file = new File(
//			FilenameUtils.getFullPath(FileTokenizer.getInstance().getInputFileName()) +
//			"tokenizer.trace"
//		);
//		
//		List newList = new ArrayList(outputs);
//		
//		// Add timestamp.
//		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
//		newList.add(0, "\n\n" + "[" + sdf.format(new Date()) + "]");
//		
//		try {
//			FileUtils.writeLines(file, newList, true);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	public static void traceOutputsQuietly(List outputs, boolean append, Exception e) {
//		if (outputs == null) return;
//		File file = new File(
//			FilenameUtils.getFullPath(FileTokenizer.getInstance().getInputFileName()) +
//			"tokenizer.trace"
//		);
//		
//		List newList = new ArrayList(outputs);
//		if (e != null) {
//			// Add reason.
//			newList.add(0, ExceptionUtils.getStackTrace(e));
//			newList.add(0, e.getMessage());
//		}
//		
//		// Add timestamp.
//		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
//		newList.add(0, "\n\n" + "[" + sdf.format(new Date()) + "]");
//		
//		try {
//			FileUtils.writeLines(file, newList, append);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//	}

}
