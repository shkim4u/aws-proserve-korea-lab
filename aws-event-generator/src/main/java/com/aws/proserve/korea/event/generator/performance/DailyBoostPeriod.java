package com.aws.proserve.korea.event.generator.performance;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

public class DailyBoostPeriod implements Cloneable {

	private String boostStartTimeStr;
	private String boostEndTimeStr;
	private Double boostBaselinedEPS;
	private Double boostFactor;
	private int boostStartTimeSecondsOfDay = 0;
	private int boostEndTimeSecondsOfDay = 0;
	private boolean isInvalid = false;
	private boolean isTransitioning = false;

	public DailyBoostPeriod(
		String boostStartTimeStr,
		String boostEndTimeStr,
		Double boostBaselinedEPS,
		Double boostFactor
	) {
		this.boostStartTimeStr = boostStartTimeStr;
		this.boostEndTimeStr = boostEndTimeStr;
		this.boostBaselinedEPS = boostBaselinedEPS;
		this.boostFactor = boostFactor;
		
		// Convert time string to seconds of day.
		try {
			this.boostStartTimeSecondsOfDay = DailyBoostPeriod.secondsOfDay(this.boostStartTimeStr);
			this.boostEndTimeSecondsOfDay = DailyBoostPeriod.secondsOfDay(this.boostEndTimeStr);
			if (this.boostStartTimeSecondsOfDay > this.boostEndTimeSecondsOfDay) {
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

	public String getBoostStartTimeStr() {
		return boostStartTimeStr;
	}

	public void setBoostStartTimeStr(String boostStartTimeStr) {
		this.boostStartTimeStr = boostStartTimeStr;
	}

	public String getBoostEndTimeStr() {
		return boostEndTimeStr;
	}

	public void setPeakBoostEndTimeStr(String peakBoostEndTimeStr) {
		this.boostEndTimeStr = peakBoostEndTimeStr;
	}
	
	public Double getPeakBoostBaselinedEPS() {
		return this.boostBaselinedEPS;
	}
	
	public void setBoostBaselinedEPS(Double boostBaselinedEPS) {
		this.boostBaselinedEPS = boostBaselinedEPS;
	}

	public Double getBoostFactor() {
		return boostFactor;
	}

	public void setPeakBoostFactor(Double peakBoostFactor) {
		this.boostFactor = peakBoostFactor;
	}

	public boolean inPeriod() {
		return inPeriod(new Date());
	}
	
	public boolean inPeriod(Date checkDttm) {
		int checkDttmSecondsOfDay = DailyBoostPeriod.secondsOfDay(checkDttm);
		if (this.isTransitioning) {
			return ((checkDttmSecondsOfDay >= this.boostStartTimeSecondsOfDay) ||
				(checkDttmSecondsOfDay <= this.boostEndTimeSecondsOfDay)
			);
		} else {
			return ((checkDttmSecondsOfDay >= this.boostStartTimeSecondsOfDay) &&
				(checkDttmSecondsOfDay <= this.boostEndTimeSecondsOfDay)
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
