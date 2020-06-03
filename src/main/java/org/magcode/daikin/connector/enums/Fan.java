package org.magcode.daikin.connector.enums;

import java.util.HashMap;
import java.util.Map;

public enum Fan {
	Auto("A"), Silence("B"), F1("3"), F2("4"), F3("5"), F4("6"), F5("7"), None("");

	private static final Map<String, Fan> lookup = new HashMap<>();

	static {
		for (Fan fan : Fan.values()) {
			lookup.put(fan.getValue(), fan);
		}
	}

	private String value;

	Fan(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static Fan getFromValue(String value) {
		return lookup.get(value);
	}

	public static Fan getFromString(String value) {
		try {
			return Fan.valueOf(value);
		} catch (Exception e) {
			// ignore
		}
		return Fan.None;
	}
}