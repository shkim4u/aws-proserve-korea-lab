package com.aws.proserve.korea.event.generator.performance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class DailyBoostPeriodsProvider implements Cloneable {
	// Daily peak boost periods list.
	private List<DailyBoostPeriod> dailyPeakBoostPeriodsList;
	// Daily over-boost periods list.
	private List<DailyBoostPeriod> dailyOverBoostPeriodsList;

	public DailyBoostPeriodsProvider(
		List<DailyBoostPeriod> dailyPeakBoostPeriodsList,
		List<DailyBoostPeriod> dailyOverBoostPeriodsList
	) {
		super();
		this.dailyPeakBoostPeriodsList = dailyPeakBoostPeriodsList;
		this.dailyOverBoostPeriodsList = dailyOverBoostPeriodsList;
	}

	public DailyBoostPeriodsProvider() {
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		DailyBoostPeriodsProvider aProvider = (DailyBoostPeriodsProvider)
			super.clone();
		
		// Deep copy.
		aProvider.dailyPeakBoostPeriodsList = null;
		if (this.dailyPeakBoostPeriodsList != null) {
			aProvider.dailyPeakBoostPeriodsList = 
				new ArrayList<DailyBoostPeriod>();
		}
		if (this.dailyPeakBoostPeriodsList != null &&
			!this.dailyPeakBoostPeriodsList.isEmpty()
		) {
			for (DailyBoostPeriod period: this.dailyPeakBoostPeriodsList) {
				DailyBoostPeriod aPeriod = (DailyBoostPeriod)period.clone();
				aProvider.dailyPeakBoostPeriodsList.add(aPeriod);
			}
		}
		
		aProvider.dailyOverBoostPeriodsList = null;
		if (this.dailyOverBoostPeriodsList != null) {
			aProvider.dailyOverBoostPeriodsList = 
				new ArrayList<DailyBoostPeriod>();
		}
		if (this.dailyOverBoostPeriodsList != null &&
			!this.dailyOverBoostPeriodsList.isEmpty()
		) {
			for (DailyBoostPeriod period: this.dailyOverBoostPeriodsList) {
				DailyBoostPeriod aPeriod = (DailyBoostPeriod)period.clone();
				aProvider.dailyOverBoostPeriodsList.add(aPeriod);
			}
		}
		
		return aProvider;
	}

	
	public static DailyBoostPeriodsProvider fromString(
		String dailyPeakBoostPeriodsString,
		String dailyOverBoostPeriodsString
	) {
		DailyBoostPeriodsProvider provider = 
			new DailyBoostPeriodsProvider();
		
		if (StringUtils.isNotBlank(dailyPeakBoostPeriodsString)) {
			provider.setDailyPeakBoostPeriodsList(
				periodsList(dailyPeakBoostPeriodsString)
			);
		}
		
		if (StringUtils.isNotBlank(dailyOverBoostPeriodsString)) {
			provider.setDailyOverBoostPeriodsList(
				periodsList(dailyOverBoostPeriodsString)
			);
		}
		
		return provider;
	}

	private static List<DailyBoostPeriod> periodsList(
		String periodsString
	) {
		List<DailyBoostPeriod> list = new ArrayList<DailyBoostPeriod>();
		String[] periodStrs = StringUtils
			.splitByWholeSeparatorPreserveAllTokens(
				periodsString,
				"|"
			);
		for (String periodStr: periodStrs) {
			String[] periodAttrs = StringUtils.splitByWholeSeparatorPreserveAllTokens(periodStr, ",");
			if (periodAttrs == null || periodAttrs.length < 4) {
				continue;
			}
			
			String boostStartTimeStr = periodAttrs[0];
			String boostEndTimeStr = periodAttrs[1];
			Double boostBaselinedEPS = NumberUtils.toDouble(periodAttrs[2], 100.0d);
			Double boostFactor = NumberUtils.toDouble(periodAttrs[3], 2.d);
			
			DailyBoostPeriod boostPeriod = new DailyBoostPeriod(
				boostStartTimeStr,
				boostEndTimeStr,
				boostBaselinedEPS,
				boostFactor
			);
			
			list.add(boostPeriod);
		}
		
		return list;
	}

	public List<DailyBoostPeriod> getDailyPeakBoostPeriodsList() {
		return dailyPeakBoostPeriodsList;
	}

	public void setDailyPeakBoostPeriodsList(List<DailyBoostPeriod> dailyPeakBoostPeriodsList) {
		this.dailyPeakBoostPeriodsList = dailyPeakBoostPeriodsList;
	}

	public boolean inPeakBoostPeriod() {
		DailyBoostPeriod dailyPeakBoostPeriod = findCurrentDailyPeakBoostPeriod();
		
		if (dailyPeakBoostPeriod == null) {
			return false;
		}
		
		return dailyPeakBoostPeriod.inPeriod();
	}
	
	public double getCurrentPeakBoostFactor() {
		DailyBoostPeriod dailyPeakBoostPeriod = findCurrentDailyPeakBoostPeriod();
		
		if (dailyPeakBoostPeriod == null) {
			// Return 0 to tell it's not valid.
			return 0;
		}
		
		return dailyPeakBoostPeriod.getBoostFactor();
	}


	public double getCurrentPeakBoostBaselinedEPS() {
		DailyBoostPeriod dailyPeakBoostPeriod = findCurrentDailyPeakBoostPeriod();
		
		if (dailyPeakBoostPeriod == null) {
			// Return 0 to tell it's not valid.
			return 0;
		}
		
		return dailyPeakBoostPeriod.getPeakBoostBaselinedEPS();
	}
	
	private DailyBoostPeriod findCurrentDailyPeakBoostPeriod() {
		if (this.dailyPeakBoostPeriodsList == null ||
			this.dailyPeakBoostPeriodsList.isEmpty()
		) {
			// Just return 0 to designate peak boost mode should not be turned on.
			return null;
		}
		
		DailyBoostPeriod ret = null;
		// Current datetime.
		Date nowDttm = Calendar.getInstance().getTime();
		for (DailyBoostPeriod dailyPeakBoostPeriod:
			this.dailyPeakBoostPeriodsList)
		{
			boolean inPeriod = dailyPeakBoostPeriod.inPeriod(nowDttm);
			if (inPeriod) {
				ret = dailyPeakBoostPeriod;
				break;
			}
		}
		
		return ret;
	}

	public BoostState getDailyPeakBoostPeriodState() {
		return (
			this.inPeakBoostPeriod() ?
			BoostState.BOOSTED :
			BoostState.BASELINED
		);
	}

	public List<DailyBoostPeriod> getDailyOverBoostPeriodsList() {
		return dailyOverBoostPeriodsList;
	}

	public void setDailyOverBoostPeriodsList(List<DailyBoostPeriod> dailyOverBoostPeriodsList) {
		this.dailyOverBoostPeriodsList = dailyOverBoostPeriodsList;
	}

	public boolean inOverBoostPeriod() {
		DailyBoostPeriod dailyOverBoostPeriod = findCurrentDailyOverBoostPeriod();
		
		if (dailyOverBoostPeriod == null) {
			return false;
		}
		
		return dailyOverBoostPeriod.inPeriod();
	}

	private DailyBoostPeriod findCurrentDailyOverBoostPeriod() {
		if (this.dailyOverBoostPeriodsList == null ||
			this.dailyOverBoostPeriodsList.isEmpty()
		) {
			// Just return 0 to designate peak boost mode should not be turned on.
			return null;
		}
		
		DailyBoostPeriod ret = null;
		// Current datetime.
		Date nowDttm = Calendar.getInstance().getTime();
		for (DailyBoostPeriod dailyOverBoostPeriod:
			this.dailyOverBoostPeriodsList)
		{
			boolean inPeriod = dailyOverBoostPeriod.inPeriod(nowDttm);
			if (inPeriod) {
				ret = dailyOverBoostPeriod;
				break;
			}
		}
		
		return ret;
	}

	public double getCurrentOverBoostBaselinedEPS() {
		DailyBoostPeriod dailyOverBoostPeriod = findCurrentDailyOverBoostPeriod();
		
		if (dailyOverBoostPeriod == null) {
			// Return 0 to tell it's not valid.
			return 0;
		}
		
		return dailyOverBoostPeriod.getPeakBoostBaselinedEPS();
	}

	public double getCurrentOverBoostFactor() {
		DailyBoostPeriod dailyOverBoostPeriod = findCurrentDailyOverBoostPeriod();
		
		if (dailyOverBoostPeriod == null) {
			// Return 0 to tell it's not valid.
			return 0;
		}
		
		return dailyOverBoostPeriod.getBoostFactor();	}

}
