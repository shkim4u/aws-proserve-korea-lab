package com.aws.proserve.korea.event.generator.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexHelper {
//	public static String DATETIME_PATTERN_CHECKPOINT_FIREWALL = "^(?:(((Jan(uary)?|Ma(r(ch)?|y)|Jul(y)?|Aug(ust)?|Oct(ober)?|Dec(ember)?)\\ 31)|((Jan(uary)?|Ma(r(ch)?|y)|Apr(il)?|Ju((ly?)|(ne?))|Aug(ust)?|Oct(ober)?|(Sept|Nov|Dec)(ember)?)\\ (0?[1-9]|([12]\\d)|30))|(Feb(ruary)?\\ (0?[1-9]|1\\d|2[0-8]|(29(?=,\\ ((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00)))))))\\ )(?:[0-1]?[0-9]|[2][1-4]):[0-5]?[0-9]:[0-5]?[0-9]\\s?";
	public static String DATETIME_PATTERN_CHECKPOINT_FIREWALL_LEADING = "^(?:(((Jan(uary)?|Ma(r(ch)?|y)|Jul(y)?|Aug(ust)?|Oct(ober)?|Dec(ember)?)\\ 31)|((Jan(uary)?|Ma(r(ch)?|y)|Apr(il)?|Ju((ly?)|(ne?))|Aug(ust)?|Oct(ober)?|(Sept|Nov|Dec)(ember)?|(Sep))\\ (0?[1-9]|([12]\\d)|30))|(Feb(ruary)?\\ (0?[1-9]|1\\d|2[0-8]|(29(?=,\\ ((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00)))))))\\ )(?:[0-1]?[0-9]|[2][1-4]):[0-5]?[0-9]:[0-5]?[0-9]";

	private Map<String, Pattern> cachedPatterns = new HashMap<String, Pattern>();
	
	public Pattern getCachedPattern(String regex) {
		Pattern pattern = cachedPatterns.get(regex);
		if (pattern == null) {
			pattern = Pattern.compile(regex);
			cachedPatterns.put(regex, pattern);
		}
		
		return pattern;
	}

}
