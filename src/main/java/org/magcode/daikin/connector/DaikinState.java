package org.magcode.daikin.connector;

import org.magcode.daikin.connector.enums.Fan;
import org.magcode.daikin.connector.enums.FanDirection;
import org.magcode.daikin.connector.enums.Mode;
import org.magcode.daikin.connector.enums.Power;

public class DaikinState {
	private Power power;
	private Fan fan;
	private Mode mode;
	private FanDirection fanDirection;
	private float insideTemp;
	private float outsideTemp;
	private float targetTemp;

	public DaikinState() {
		this.fan = Fan.None;
		this.fanDirection = FanDirection.None;
		this.mode = Mode.None;
	}

	public Fan getFan() {
		return fan;
	}

	public void setFan(Fan fan) {
		this.fan = fan;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public FanDirection getFanDirection() {
		return fanDirection;
	}

	public void setFanDirection(FanDirection fanDirection) {
		this.fanDirection = fanDirection;
	}

	public Power getPower() {
		return power;
	}

	public void setPower(Power power) {
		this.power = power;
	}

	public float getInsideTemp() {
		return insideTemp;
	}

	public void setInsideTemp(float insideTemp) {
		this.insideTemp = insideTemp;
	}

	public float getOutsideTemp() {
		return outsideTemp;
	}

	public void setOutsideTemp(float outsideTemp) {
		this.outsideTemp = outsideTemp;
	}

	public float getTargetTemp() {
		return targetTemp;
	}

	public void setTargetTemp(float targetTemp) {
		this.targetTemp = targetTemp;
	}

}
