package com.aws.proserve.korea.event.generator.performance;

public enum BoostState {

	/**
	 * Baselined mode.
	 */
	BASELINED(0, "BASELINED"),
	/**
	 * Daily peak boost mode.
	 */
	BOOSTED(1, "BOOSTED");

	private int code;
	private String label;

	private BoostState(int code, String label) {
		this.code = code;
		this.label = label;
	}
	
	public int code() {
		return code;
	}
	
	public String label() {
		return label;
	}
}
