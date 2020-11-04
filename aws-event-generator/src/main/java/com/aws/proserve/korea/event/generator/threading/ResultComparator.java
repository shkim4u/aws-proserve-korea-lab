package com.aws.proserve.korea.event.generator.threading;

import java.util.Comparator;


public class ResultComparator implements Comparator<Result> {

	@Override
	public int compare(Result o1, Result o2) {
		if (o1 == null && o2 != null) {
			return -1;
		}
		
		if (o1 != null && o2 == null) {
			return 1;
		}
		
		if (o1 == null && o2 == null) {
			return 0;
		}
		
		return o1.compareTo(o2);
	}
	
}
