package com.aws.proserve.korea.event.generator.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversionUtils {

	public static String units = "BKMGTPEZY";

	public static long parse(String numberString) {
		int spaceNdx = numberString.indexOf(" ");
		double ret = Double.parseDouble(numberString.substring(0, spaceNdx));
		String unitString = numberString.substring(spaceNdx + 1);
		int unitChar = unitString.charAt(0);
		int power = units.indexOf(unitChar);
		boolean isSi = unitString.indexOf('i') != -1;
		int factor = 1024;
		if (isSi) {
			factor = 1000;
		}

		return new Double(ret * Math.pow(factor, power)).longValue();
	}

	/** @return index of pattern in s or -1, if not found */
	public static int indexOf(Pattern pattern, String search) {
		Matcher matcher = pattern.matcher(search);
		return matcher.find() ? matcher.start() : -1;
	}

	public static long parseAny(String numberString) {
		int index = indexOf(Pattern.compile("[A-Za-z]"), numberString);
		double ret = Double.parseDouble(numberString.substring(0, index));
		String unitString = numberString.substring(index);
		int unitChar = unitString.charAt(0);
		int power = units.indexOf(unitChar);
		boolean isSi = unitString.indexOf('i') != -1;
		int factor = 1024;
		if (isSi) {
			factor = 1000;
		}

		return new Double(ret * Math.pow(factor, power)).longValue();

	}

	public static void main(String[] args) {
		System.out.println(parse("300.00 GiB")); // requires a space
		System.out.println(parse("300.00 GB"));
		System.out.println(parse("300.00 B"));
		System.out.println(parse("300 EB"));
		System.out.println(parseAny("300.00 GiB"));
		System.out.println(parseAny("300M"));
		System.out.println(parseAny("3000000"));
	}
}