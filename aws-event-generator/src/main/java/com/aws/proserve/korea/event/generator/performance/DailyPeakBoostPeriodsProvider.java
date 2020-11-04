package com.aws.proserve.korea.event.generator.performance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class DailyPeakBoostPeriodsProvider implements Cloneable {
	
	private List<DailyPeakBoostPeriod> dailyPeakBoostPeriodsList;

	public DailyPeakBoostPeriodsProvider(
		List<DailyPeakBoostPeriod> dailyPeakBoostPeriodsList
	) {
		super();
		this.dailyPeakBoostPeriodsList = dailyPeakBoostPeriodsList;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		DailyPeakBoostPeriodsProvider aProvider = (DailyPeakBoostPeriodsProvider)
			super.clone();
		
		// Deep copy.
		aProvider.dailyPeakBoostPeriodsList = new ArrayList<DailyPeakBoostPeriod>();
		if (this.dailyPeakBoostPeriodsList != null &&
			!this.dailyPeakBoostPeriodsList.isEmpty()
		) {
			for (DailyPeakBoostPeriod period: this.dailyPeakBoostPeriodsList) {
				DailyPeakBoostPeriod aPeriod = (DailyPeakBoostPeriod)period.clone();
				aProvider.dailyPeakBoostPeriodsList.add(aPeriod);
			}
		}
		
		return aProvider;
	}

	
	public static DailyPeakBoostPeriodsProvider fromString(String dailyPeakBoostPeriodsString) {
		if (StringUtils.isBlank(dailyPeakBoostPeriodsString)) {
			return new DailyPeakBoostPeriodsProvider(new ArrayList<DailyPeakBoostPeriod>());
		}
		
		
		List<DailyPeakBoostPeriod> list = new ArrayList<DailyPeakBoostPeriod>();
		String[] periodStrs = StringUtils.splitByWholeSeparatorPreserveAllTokens(dailyPeakBoostPeriodsString, "|");
		for (String periodStr: periodStrs) {
			String[] periodAttrs = StringUtils.splitByWholeSeparatorPreserveAllTokens(periodStr, ",");
			if (periodAttrs == null || periodAttrs.length < 4) {
				continue;
			}
			
			String peakBoostStartTimeStr = periodAttrs[0];
			String peakBoostEndTimeStr = periodAttrs[1];
			Double peakBoostBaselinedEPS = NumberUtils.toDouble(periodAttrs[2], 100.0d);
			Double peakBoostFactor = NumberUtils.toDouble(periodAttrs[3], 2.d);
			
			DailyPeakBoostPeriod dailyPeakBoostPeriod = new DailyPeakBoostPeriod(
				peakBoostStartTimeStr,
				peakBoostEndTimeStr,
				peakBoostBaselinedEPS,
				peakBoostFactor
			);
			
			list.add(dailyPeakBoostPeriod);
		}
		
		return new DailyPeakBoostPeriodsProvider(list);
	}

	public List<DailyPeakBoostPeriod> getDailyPeakBoostPeriodsList() {
		return dailyPeakBoostPeriodsList;
	}

	public void setDailyPeakBoostPeriodsList(List<DailyPeakBoostPeriod> dailyPeakBoostPeriodsList) {
		this.dailyPeakBoostPeriodsList = dailyPeakBoostPeriodsList;
	}

	public boolean inPeakBoostPeriod() {
		DailyPeakBoostPeriod dailyPeakBoostPeriod = findCurrentDailyPeakBoostPeriod();
		
		if (dailyPeakBoostPeriod == null) {
			return false;
		}
		
		return dailyPeakBoostPeriod.inPeriod();
	}
	
	public double getCurrentPeakBoostFactor() {
		DailyPeakBoostPeriod dailyPeakBoostPeriod = findCurrentDailyPeakBoostPeriod();
		
		if (dailyPeakBoostPeriod == null) {
			// Return 0 to tell it's not valid.
			return 0;
		}
		
		return dailyPeakBoostPeriod.getPeakBoostFactor();
	}


	public double getCurrentPeakBoostBaselinedEPS() {
		DailyPeakBoostPeriod dailyPeakBoostPeriod = findCurrentDailyPeakBoostPeriod();
		
		if (dailyPeakBoostPeriod == null) {
			// Return 0 to tell it's not valid.
			return 0;
		}
		
		return dailyPeakBoostPeriod.getPeakBoostBaselinedEPS();
	}
	
	private DailyPeakBoostPeriod findCurrentDailyPeakBoostPeriod() {
		if (this.dailyPeakBoostPeriodsList == null ||
			this.dailyPeakBoostPeriodsList.isEmpty()
		) {
			// Just return 0 to designate peak boost mode should not be turned on.
			return null;
		}
		
		DailyPeakBoostPeriod ret = null;
		// Current datetime.
		Date nowDttm = Calendar.getInstance().getTime();
		for (DailyPeakBoostPeriod dailyPeakBoostPeriod:
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

}
