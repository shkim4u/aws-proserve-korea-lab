package com.aws.proserve.korea.event.generator.metric;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import com.aws.proserve.korea.event.generator.AwsEventGenerator;
import com.aws.proserve.korea.event.generator.utils.FormatUtils;
import com.aws.proserve.korea.event.generator.utils.LoggerUtils;


public class AwsEventGeneratorMetric {

	private long dailySentCountLimit = 0;
	private long dailySentBytesRawLimit = 0;
	private long dailySentBytesEnvelopedLimit = 0;
	
	private AtomicLong accumulatedSentCount = new AtomicLong(0);
	private AtomicLong dailySentCount = new AtomicLong(0);
	
	private AtomicLong accumulatedSentBytesRaw = new AtomicLong(0);
	private AtomicLong dailySentBytesRaw = new AtomicLong(0);
	
	private AtomicLong accumulatedSentBytesEnveloped = new AtomicLong(0);
	private AtomicLong dailySentBytesEnveloped = new AtomicLong(0);
	private boolean touchedDailyLimit;
	private String dailySentCountLimitStr;
	private String dailySentBytesRawLimitStr;
	private String dailySentBytesEnvelopedLimitStr;
	private boolean modified;
	private Date resetDate;


	public long getDailySentCountLimit() {
		return dailySentCountLimit;
	}

	public long getDailySentBytesRawLimit() {
		return dailySentBytesRawLimit;
	}
	
	public long getDailySentBytesEnvelopedLimit() {
		return dailySentBytesEnvelopedLimit;
	}
	
	public void setDailySentCountLimit(long dailySentCountLimit) {
		this.dailySentCountLimit = dailySentCountLimit;
	}

	public void setDailySentBytesRawLimit(long dailySentBytesRawLimit) {
		this.dailySentBytesRawLimit = dailySentBytesRawLimit;
	}

	public void setDailySentBytesEnvelopedLimit(long dailySentBytesEnvelopedLimit) {
		this.dailySentBytesEnvelopedLimit = dailySentBytesEnvelopedLimit;
	}


	public AtomicLong getAccumulatedSentCount() {
		return accumulatedSentCount;
	}

	public AtomicLong getDailySentCount() {
		return dailySentCount;
	}

	public AtomicLong getAccumulatedSentBytesRaw() {
		return accumulatedSentBytesRaw;
	}

	public AtomicLong getDailySentBytesRaw() {
		return dailySentBytesRaw;
	}

	public AtomicLong getAccumulatedSentBytesEnveloped() {
		return accumulatedSentBytesEnveloped;
	}

	public AtomicLong getDailySentBytesEnveloped() {
		return dailySentBytesEnveloped;
	}

	public void addDeltas(long sentCount, long sentBytesRaw, long sentBytesEnvoloped) {
		this.accumulatedSentCount.addAndGet(sentCount);
		this.dailySentCount.addAndGet(sentCount);

		this.accumulatedSentBytesRaw.addAndGet(sentBytesRaw);
		this.dailySentBytesRaw.addAndGet(sentBytesRaw);

		this.accumulatedSentBytesEnveloped.addAndGet(sentBytesEnvoloped);
		this.dailySentBytesEnveloped.addAndGet(sentBytesEnvoloped);
	}

	public boolean checkDailyLimitTouched() {
		long dailySentCount = this.dailySentCount.get();
		boolean touchedDailySentCountLimit = (
			this.dailySentCountLimit > 0 ?
			dailySentCount >= this.dailySentCountLimit :
			false
		);
		
		long dailySentBytesRaw = this.dailySentBytesRaw.get();
		boolean touchedDailySentBytesRawLimit = (
			this.dailySentBytesRawLimit > 0 ?
			dailySentBytesRaw >= this.dailySentBytesRawLimit :
			false
		);
		
		long dailySentBytesEnveloped = this.dailySentBytesEnveloped.get();
		boolean touchedDailySentBytesEnvelopedLimit = (
			this.dailySentBytesEnvelopedLimit > 0 ?
			dailySentBytesEnveloped >= this.dailySentBytesEnvelopedLimit :
			false
		);
		
		if (touchedDailySentCountLimit) {
			LoggerUtils.getLogger().info(
				String.format(
					"Daily sent count limit exceeded: [%d >= %d]",
					dailySentCount,
					this.dailySentCountLimit
				)
			);
		}
		
		if (touchedDailySentBytesRawLimit) {
			LoggerUtils.getLogger().info(
				String.format(
					"Daily sent bytes(RAW) limit exceeded: [%d >= %d]",
					dailySentBytesRaw,
					this.dailySentBytesRawLimit
				)
			);
		}
		
		if (touchedDailySentBytesEnvelopedLimit) {
			LoggerUtils.getLogger().info(
				String.format(
					"Daily sent bytes(ENVELOPED) limit exceeded: [%d >= %d]",
					dailySentBytesEnveloped,
					this.dailySentBytesEnvelopedLimit
				)
			);
		}
		
		boolean ret = (touchedDailySentCountLimit ||
			touchedDailySentBytesRawLimit ||
			touchedDailySentBytesEnvelopedLimit
		);
		
		AwsEventGenerator.getInstance().setDailyLimitTouched(ret);
		
		return ret;
	}

	public void setDailyLimitTouched(boolean touchedDailyLimit) {
		this.touchedDailyLimit = touchedDailyLimit;
	}

	public boolean isDailyLimitTouched() {
		return this.touchedDailyLimit;
	}

	public void setDailySentCountLimitStr(String dailySentCountLimitStr) {
		this.dailySentCountLimitStr = dailySentCountLimitStr;
	}

	public void setDailySentBytesRawLimitStr(String dailySentBytesRawLimitStr) {
		this.dailySentBytesRawLimitStr = dailySentBytesRawLimitStr;
	}

	public void setDailySentBytesEnvelopedLimitStr(String dailySentBytesEnvelopedLimitStr) {
		this.dailySentBytesEnvelopedLimitStr = dailySentBytesEnvelopedLimitStr;
	}

	public String getDailySentCountLimitStr() {
		return dailySentCountLimitStr;
	}

	public String getDailySentBytesRawLimitStr() {
		return dailySentBytesRawLimitStr;
	}

	public String getDailySentBytesEnvelopedLimitStr() {
		return dailySentBytesEnvelopedLimitStr;
	}
	
	public void setModified(boolean modified) {
		this.modified = modified;
	}
	
	public boolean isModified() {
		return this.modified;
	}

	public String getDailySentCountLimitFormatted() {
		String ret = FormatUtils.dfNoDecimal.format(
			this.getDailySentCountLimit()
		);
		return ret;
	}
	
	public String getDailySentBytesRawLimitFormatted() {
//		if (this.modified) {
		String ret = FormatUtils.dfNoDecimal.format(
			this.getDailySentBytesRawLimit()
		);
		return ret;
		
//		} else {
//			
//		}
		
//		boolean isDailyLimitsModified = AwsEventGenerator.getInstance()
//			.getAwsEventGeneratorMetric().isModified();
//		// Values.
//		sb.append(
//			String.format(
//				"%-45.45s %-15.15s%n",
//				Configs.CONFIG_KEY_DAILY_SENT_COUNT_LIMIT,
//				(isDailyLimitsModified ?
//					AwsEventGenerator.getInstance()
//						.getAwsEventGeneratorMetric().getDailySentBytesRawLimitStr() :
//					AwsEventGenerator.getInstance()
//						.getConfiguration()
//						.getDailySentCountLimitDisplayString()
//				)
//			)
//		);
	}

	public String getDailySentBytesEnvelopedLimitFormatted() {
		String ret = FormatUtils.dfNoDecimal.format(
			this.getDailySentBytesEnvelopedLimit()
		);
		return ret;
	}

	public void resetDailyMetrics() {
		this.dailySentCount.set(0);
		this.dailySentBytesRaw.set(0);
		this.dailySentBytesEnveloped.set(0);
		this.touchedDailyLimit = false;
		
		this.resetDate = new Date();
	}

	public Date getResetDate() {
		return resetDate;
	}

	public String getResetDateFromatted() {
		if (this.resetDate == null) {
			return "";
		} else {
			String ret = String.format(
				"reset at %tF %8tT",
				this.resetDate,
				this.resetDate
			);
			
			return ret;
		}
	}

	
}
