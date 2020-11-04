package com.aws.proserve.korea.event.generator.performance;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

public class DailyPeakBoostPeriod implements Cloneable {

	private String peakBoostStartTimeStr;
	private String peakBoostEndTimeStr;
	private Double peakBoostBaselinedEPS;
	private Double peakBoostFactor;
	private int peakBoostStartTimeSecondsOfDay = 0;
	private int peakBoostEndTimeSecondsOfDay = 0;
	private boolean isInvalid = false;
	private boolean isTransitioning = false;

	public DailyPeakBoostPeriod(
		String peakBoostStartTimeStr,
		String peakBoostEndTimeStr,
		Double peakBoostBaselinedEPS,
		Double peakBoostFactor
	) {
		this.peakBoostStartTimeStr = peakBoostStartTimeStr;
		this.peakBoostEndTimeStr = peakBoostEndTimeStr;
		this.peakBoostBaselinedEPS = peakBoostBaselinedEPS;
		this.peakBoostFactor = peakBoostFactor;
		
		// Convert time string to seconds of day.
		try {
			this.peakBoostStartTimeSecondsOfDay = DailyPeakBoostPeriod.secondsOfDay(this.peakBoostStartTimeStr);
			this.peakBoostEndTimeSecondsOfDay = DailyPeakBoostPeriod.secondsOfDay(this.peakBoostEndTimeStr);
			if (this.peakBoostStartTimeSecondsOfDay > this.peakBoostEndTimeSecondsOfDay) {
				this.isTransitioning = true;
			}
		} catch (ParseException e) {
			this.isInvalid  = true;
		}
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	private static int secondsOfDay(String timeString) throws ParseException {
		Date time = DateUtils.parseDate(timeString,	new String[] {"HH:mm:ss"});
		
		return secondsOfDay(time);
	}
	

	private static int secondsOfDay(Date checkDttm) {
		Calendar calStartTime = DateUtils.toCalendar(checkDttm);
		int hour = calStartTime.get(Calendar.HOUR_OF_DAY);
		int minute = calStartTime.get(Calendar.MINUTE);
		int second = calStartTime.get(Calendar.SECOND);
		
		return (3600 * hour + 60 * minute + second);
	}

	public String getPeakBoostStartTimeStr() {
		return peakBoostStartTimeStr;
	}

	public void setPeakBoostStartTimeStr(String peakBoostStartTimeStr) {
		this.peakBoostStartTimeStr = peakBoostStartTimeStr;
	}

	public String getPeakBoostEndTimeStr() {
		return peakBoostEndTimeStr;
	}

	public void setPeakBoostEndTimeStr(String peakBoostEndTimeStr) {
		this.peakBoostEndTimeStr = peakBoostEndTimeStr;
	}
	
	public Double getPeakBoostBaselinedEPS() {
		return this.peakBoostBaselinedEPS;
	}
	
	public void setPeakBoostBaselinedEPS(Double peakBoostBaselinedEPS) {
		this.peakBoostBaselinedEPS = peakBoostBaselinedEPS;
	}

	public Double getPeakBoostFactor() {
		return peakBoostFactor;
	}

	public void setPeakBoostFactor(Double peakBoostFactor) {
		this.peakBoostFactor = peakBoostFactor;
	}

	public boolean inPeriod() {
		return inPeriod(new Date());
	}
	
	public boolean inPeriod(Date checkDttm) {
		int checkDttmSecondsOfDay = DailyPeakBoostPeriod.secondsOfDay(checkDttm);
		if (this.isTransitioning) {
			return ((checkDttmSecondsOfDay >= this.peakBoostStartTimeSecondsOfDay) ||
				(checkDttmSecondsOfDay <= this.peakBoostEndTimeSecondsOfDay)
			);
		} else {
			return ((checkDttmSecondsOfDay >= this.peakBoostStartTimeSecondsOfDay) &&
				(checkDttmSecondsOfDay <= this.peakBoostEndTimeSecondsOfDay)
			);
		}
	}

	public boolean isInvalid() {
		return isInvalid;
	}

	public boolean isTransitioning() {
		return isTransitioning;
	}


}
