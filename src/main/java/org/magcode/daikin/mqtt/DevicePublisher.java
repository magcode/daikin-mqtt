package org.magcode.daikin.mqtt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.magcode.daikin.Constants;
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.DaikinMqttClient;

import net.jonathangiles.daikin.IDaikin;
import net.jonathangiles.daikin.util.DaikinUnreachableException;

public class DevicePublisher extends Publisher implements Runnable {
	private String topic;
	private Map<String, DaikinConfig> daikinHosts;
	private int deviceRefresh;
	private static Logger logger = LogManager.getLogger(DevicePublisher.class);

	public DevicePublisher(Map<String, DaikinConfig> daikinHosts, String topic, MqttClient mqttClient,
			int devicerefresh, Semaphore semaphore) {
		this.daikinHosts = daikinHosts;
		this.topic = topic;
		this.mqttClient = mqttClient;
		this.deviceRefresh = devicerefresh;
		this.semaphore = semaphore;
	}

	@Override
	public void run() {
		for (Entry<String, DaikinConfig> entry : this.daikinHosts.entrySet()) {
			DaikinConfig value = entry.getValue();
			IDaikin daikinDevice = value.getDaikin();
			try {
				daikinDevice.readDaikinState();
			} catch (DaikinUnreachableException e1) {
				logger.debug("Daikin {} is unreachable", value.getName());
			}

			String deviceTopic = topic + "/" + value.getName();
			String nodePropTopic = deviceTopic + "/" + DaikinMqttClient.nodeName + "/";

			// IP address
			try {
				String ipAddr = StringUtils.substringAfter(daikinDevice.getHost(), "http://");
				InetAddress address = InetAddress.getByName(ipAddr);
				Publish(deviceTopic + "/$localip", address.getHostAddress());
			} catch (UnknownHostException e) {
				logger.error("Could not get IP address", e);
			}

			// Homie version
			Publish(deviceTopic + "/$homie", "3.0.0");

			// name
			Publish(deviceTopic + "/$name", "Aircondition - " + value.getName());

			// firmware
			Publish(deviceTopic + "/$fw/name", "daikin-mqtt");
			Publish(deviceTopic + "/$fw/version", "1.0.1");

			// nodes
			Publish(deviceTopic + "/$nodes", DaikinMqttClient.nodeName);

			// implementation
			Publish(deviceTopic + "/$implementation", "daikin-mqtt");

			// stats
			Publish(deviceTopic + "/$stats/interval", deviceRefresh);

			// node details
			Publish(nodePropTopic + "$name", "Aircondition");
			Publish(nodePropTopic + "$type", "Aircondition");
			Publish(nodePropTopic + "$properties", String.join(",", Constants.PROPERTIES));

			// setable properties
			Publish(nodePropTopic + Constants.PR_TARGETTEMP + Constants.PR_SETTABLE, true);
			Publish(nodePropTopic + Constants.PR_FAN + Constants.PR_SETTABLE, true);
			Publish(nodePropTopic + Constants.PR_FANDIR + Constants.PR_SETTABLE, true);
			Publish(nodePropTopic + Constants.PR_MODE + Constants.PR_SETTABLE, true);
			Publish(nodePropTopic + Constants.PR_POWER + Constants.PR_SETTABLE, true);
		}
	}
}