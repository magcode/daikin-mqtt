package org.magcode.daikin;

import java.util.Date;

import org.magcode.daikin.connector.DaikinConnector;

public class DaikinConfig {
	private String host;
	private String name;
	private DaikinConnector daikin;
	private Date onlineSince;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Date getOnlineSince() {
		return onlineSince;
	}

	public void setOnlineSince(Date onlineSince) {
		this.onlineSince = onlineSince;
	}

	public DaikinConnector getDaikin() {
		return daikin;
	}

	public void setDaikin(DaikinConnector daikin) {
		this.daikin = daikin;
	}
}
