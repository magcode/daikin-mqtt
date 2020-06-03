package org.magcode.daikin.connector.enums;

import java.util.HashMap;
import java.util.Map;

public enum FanDirection {
	Off("0"), None(""), Vertical("1"), Horizontal("2"), VerticalAndHorizontal("3");

	private static final Map<String, FanDirection> lookup = new HashMap<>();

	static {
		for (FanDirection fanDir : FanDirection.values()) {
			lookup.put(fanDir.getValue(), fanDir);
		}
	}

	private String value;

	FanDirection(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static FanDirection getFromValue(String value) {
		return lookup.get(value);
	}

	public static FanDirection getFromString(String value) {
		try {
			return FanDirection.valueOf(value);
		} catch (Exception e) {
			// ignore
		}
		return FanDirection.None;
	}
}