package org.magcode.daikin;

import net.jonathangiles.daikin.IDaikin;

public class DaikinConfig {
	private String host;
	private String name;
	private DaikinType type;
	private IDaikin daikin;

	public DaikinType getType() {
		return type;
	}

	public void setType(DaikinType type) {
		this.type = type;
	}

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

	public IDaikin getDaikin() {
		return daikin;
	}

	public void setDaikin(IDaikin daikin) {
		this.daikin = daikin;
	}
}
