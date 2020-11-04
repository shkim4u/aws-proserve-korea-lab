package com.aws.proserve.korea.event.generator.threading;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.aws.proserve.korea.event.generator.utils.AwsEventGeneratorFileUtils;

public class Result implements Comparable<Result> {

	private Integer pos = null;
	private String messageFilePath = null;
	private boolean success = true;
	private String resultMessage = null;
	private Object data = null;
	private String delimiter = ",";
	private String newLineChar = AwsEventGeneratorFileUtils.NEW_LINE;
	
	private long attachedTaskID = 0;
	private long accumulatedSentCount;
	private long dailySentCount;
	private long accumulatedSentBytesRaw;
	private long dailySentBytesRaw;
	private long accumulatedSentBytesEnveloped;
	private long dailySentBytesEnveloped;
	private String syslogServer;
	private int syslogPort;
	private Date startDate;
	private Date endDate;
	
	public Result() {
		
	}
	
	public Result(Integer pos, String messageFilePath, boolean success, String resultMessage, Object data,
		String delimiter, String newLineChar) {
		super();
		this.pos = pos;
		this.messageFilePath = messageFilePath;
		this.success = success;
		this.resultMessage = resultMessage;
		this.data = data;
		this.delimiter = delimiter;
		this.newLineChar = newLineChar;
	}

	@Override
	public int compareTo(Result o) {
		if (o == null) {
			return 1;
		}
		
		// [2018-11-06] Sang Hyoun: Change comparator to TID.
//		return pos.compareTo(o.getPos());
		return (int)(this.attachedTaskID - o.getAttachedTaskID());
//		if (this.attachedTaskID > o.getAttachedTaskID()) {
//			return 1;
//		} else if (this.attachedTaskID == o.getAttachedTaskID()) {
//			return 0;
//		} else {
//			return -1;
//		}
	}

	public Integer getPos() {
		return pos;
	}

	public void setPos(Integer pos) {
		this.pos = pos;
	}

	public String getMessageFilePath() {
		return messageFilePath;
	}

	public void setMessageFilePath(String messageFilePath) {
		this.messageFilePath = messageFilePath;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getResultMessage() {
		return resultMessage;
	}

	public void setResultMessage(String resultMessage) {
		this.resultMessage = resultMessage;
	}

	public String getNewLineChar() {
		return newLineChar;
	}

	public void setNewLineChar(String newLineChar) {
		this.newLineChar = newLineChar;
	}
	
	public String makeLine() {
		return makeLine(true);
	}

	private String makeLine(boolean newLine) {
		String newLineChar = AwsEventGeneratorFileUtils.NEW_LINE;
		
		StringBuffer sb = new StringBuffer();
		
		sb.append(StringUtils.repeat("=", 80));
		sb.append(newLineChar);
		
		String messageFilePath = getMessageFilePath();
		String resultMessage = getResultMessage();
		
		sb.append(StringUtils.defaultIfBlank(messageFilePath, "(OVERALL)"));
		sb.append(delimiter);
		
		sb.append(StringUtils.defaultIfBlank(resultMessage, "SUCCESS"));
		sb.append(newLineChar);
		
		sb.append("- Syslog Server: " + this.syslogServer + newLineChar);
		sb.append("- Syslog Port: " + this.syslogPort + newLineChar);
		sb.append(
			"- Start Datetime: " +
			String.format(
				"%tF %8tT%n",
				this.startDate,
				this.startDate
			)
		);
		sb.append(
			"- End Datetime: " +
			String.format(
				"%tF %8tT%n",
				this.endDate,
				this.endDate
			)
		);
		sb.append(
			"- Duration: " +
			DurationFormatUtils.formatDuration(
				this.endDate.getTime() - this.startDate.getTime(),
				"H:mm:ss",
				true
			) + newLineChar
		);
		
		sb.append(StringUtils.repeat("-", 80));
		sb.append(newLineChar);

		sb.append("Total Sent Count: " + this.accumulatedSentCount + newLineChar);
		sb.append("Total Sent Bytes(RAW): " + this.accumulatedSentBytesRaw + newLineChar);
		sb.append("Total Sent Bytes(ENVELOPED): " + this.accumulatedSentBytesEnveloped + newLineChar);

		sb.append("Daily Sent Count: " + this.dailySentCount + newLineChar);
		sb.append("Daily Sent Bytes(RAW): " + this.dailySentBytesRaw + newLineChar);
		sb.append("Daily Sent Bytes(ENVELOPED): " + this.dailySentBytesEnveloped + newLineChar);
		
		sb.append(StringUtils.repeat("=", 80));
		sb.append(newLineChar);

		if (newLine) {
			sb.append(newLineChar);
		}
		
		return sb.toString();
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public void setAccumulatedSentCount(long accumulatedSentCount) {
		this.accumulatedSentCount = accumulatedSentCount;
	}

	public void setDailySentCount(long dailySentCount) {
		this.dailySentCount = dailySentCount;
	}

	public void setAccumulatedSentBytesRaw(long accumulatedSentBytesRaw) {
		this.accumulatedSentBytesRaw = accumulatedSentBytesRaw;
	}

	public void setDailySentBytesRaw(long dailySentBytesRaw) {
		this.dailySentBytesRaw = dailySentBytesRaw;
	}

	public void setAccumulatedSentBytesEnveloped(long accumulatedSentBytesEnveloped) {
		this.accumulatedSentBytesEnveloped = accumulatedSentBytesEnveloped;
	}

	public void setDailySentBytesEnveloped(long dailySentBytesEnveloped) {
		this.dailySentBytesEnveloped = dailySentBytesEnveloped;
	}

	public long getAttachedTaskID() {
		return attachedTaskID;
	}

	public void setAttachedTaskID(long attachedTaskID) {
		this.attachedTaskID = attachedTaskID;
	}

	public long getAccumulatedSentCount() {
		return accumulatedSentCount;
	}

	public long getDailySentCount() {
		return dailySentCount;
	}

	public long getAccumulatedSentBytesRaw() {
		return accumulatedSentBytesRaw;
	}

	public long getDailySentBytesRaw() {
		return dailySentBytesRaw;
	}

	public long getAccumulatedSentBytesEnveloped() {
		return accumulatedSentBytesEnveloped;
	}

	public long getDailySentBytesEnveloped() {
		return dailySentBytesEnveloped;
	}

	public void setSyslogServer(String syslogServer) {
		this.syslogServer = syslogServer;
	}

	public void setSyslogPort(int syslogPort) {
		this.syslogPort = syslogPort;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

}
