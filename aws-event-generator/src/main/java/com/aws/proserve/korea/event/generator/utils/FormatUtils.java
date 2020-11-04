package com.aws.proserve.korea.event.generator.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class FormatUtils {

	public static final NumberFormat dfNoDecimal = new DecimalFormat("#,##0");
	public static final NumberFormat dfOneDecimal = new DecimalFormat("#,##0.0#");

}
