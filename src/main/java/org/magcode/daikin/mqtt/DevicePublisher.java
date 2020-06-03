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
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.DaikinMqttClient;

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
			DaikinConfig daikinConfig = entry.getValue();
			String deviceTopic = topic + "/" + daikinConfig.getName();
			String nodePropTopic = deviceTopic + "/" + DaikinMqttClient.nodeName + "/";

			// IP address
			try {
				String ipAddr = StringUtils.substringAfter(daikinConfig.getHost(), "http://");
				InetAddress address = InetAddress.getByName(ipAddr);
				Publish(deviceTopic + "/$localip", address.getHostAddress());
			} catch (UnknownHostException e) {
				logger.error("Could not get IP address", e);
			}

			// Homie version
			Publish(deviceTopic + "/$homie", "3.0.0");

			// name
			Publish(deviceTopic + "/$name", "Aircondition - " + daikinConfig.getName());

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
			Publish(nodePropTopic + "$properties", String.join(",", TopicConstants.PROPERTIES));

			// setable properties
			Publish(nodePropTopic + TopicConstants.PR_TARGETTEMP + TopicConstants.PR_SETTABLE, true);
			Publish(nodePropTopic + TopicConstants.PR_FAN + TopicConstants.PR_SETTABLE, true);
			Publish(nodePropTopic + TopicConstants.PR_FANDIR + TopicConstants.PR_SETTABLE, true);
			Publish(nodePropTopic + TopicConstants.PR_MODE + TopicConstants.PR_SETTABLE, true);
			Publish(nodePropTopic + TopicConstants.PR_POWER + TopicConstants.PR_SETTABLE, true);
		}
	}
}