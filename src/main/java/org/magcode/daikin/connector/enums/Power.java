package org.magcode.daikin.connector.enums;

import java.util.HashMap;
import java.util.Map;

public enum Power {

	On("1"), Off("0"), None("");

	private static final Map<String, Power> lookup = new HashMap<>();

	static {
		for (Power power : Power.values()) {
			lookup.put(power.getValue(), power);
		}
	}

	private String value;

	Power(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static Power get(String value) {
		return lookup.get(value);
	}
}