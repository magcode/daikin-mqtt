package org.magcode.daikin.connector;

public class DaikinUnreachableException extends Exception {
	private static final long serialVersionUID = 2599495412071521238L;

	public DaikinUnreachableException() {
	}

	public DaikinUnreachableException(String message) {
		super(message);
	}
}