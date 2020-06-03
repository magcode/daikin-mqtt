package org.magcode.daikin.connector.enums;

import java.util.HashMap;
import java.util.Map;

public enum Mode {

	Auto("0"), Dry("2"), Cool("3"), Heat("4"), Fan("6"), None("");

	private static final Map<String, Mode> lookup = new HashMap<>();

	static {
		for (Mode mode : Mode.values()) {
			lookup.put(mode.getValue(), mode);
		}
	}

	private String value;

	Mode(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static Mode getFromValue(String value) {
		if (lookup.get(value) != null) {
			return lookup.get(value);
		} else {
			return Mode.None;
		}
	}

	public static Mode getFromString(String value) {
		try {
			return Mode.valueOf(value);
		} catch (Exception e) {
			// ignore
		}
		return Mode.None;
	}
}