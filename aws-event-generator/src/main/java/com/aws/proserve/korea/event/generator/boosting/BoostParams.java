package com.aws.proserve.korea.event.generator.boosting;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.utils.LoggerUtils;


public class BoostParams implements Cloneable {

	private double baselinedEPS;
	private double boostFactor;
	private boolean boosted;
	
	public BoostParams(double baselinedEPS, double boostFactor) {
		super();
		this.baselinedEPS = baselinedEPS;
		this.boostFactor = boostFactor;
		
		this.boosted = (
			this.boostFactor >= .5d &&
			this.baselinedEPS >= AwsEventGenerator
				.getInstance()
				.getConfiguration()
				.getBoostBaselinedEPSThreshold()
		);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public double getBaselinedEPS() {
		return baselinedEPS;
	}

	public double getBoostFactor() {
		return boostFactor;
	}

	public boolean isBoosted() {
		return this.boosted;
	}

	public void setBoosted(boolean boosted) {
		this.boosted = boosted;
	}
	
	public static BoostParams fromString(String boostParamsStr) {
		try {
			if (StringUtils.isBlank(boostParamsStr)) {
				return null;
			}
			
			String[] boostParamsData = StringUtils
				.splitByWholeSeparatorPreserveAllTokens(
				boostParamsStr,
				"|"
			);
			
			if (ArrayUtils.isEmpty(boostParamsData)) {
				return null;
			}
			
			double baselinedEPS = Double.parseDouble(boostParamsData[0]);
			double boostFactor = Double.parseDouble(boostParamsData[1]);
			
			return new BoostParams(baselinedEPS, boostFactor);
		} catch (Exception e) {
			LoggerUtils.getLogger().warn(
				"ERROR: " + e.getLocalizedMessage()
			);
			
			return null;
		}
	}

}
