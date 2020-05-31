package org.magcode.daikin.mqtt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.daikin.Constants;
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.DaikinMqttClient;

import net.jonathangiles.daikin.IDaikin;
import net.jonathangiles.daikin.util.DaikinUnreachableException;

public class MqttDevicePublisher implements Runnable {
	private MqttClient mqttClient;
	private String topic;
	private Map<String, DaikinConfig> daikinHosts;
	private int deviceRefresh;
	private static Logger logger = LogManager.getLogger(MqttDevicePublisher.class);

	public MqttDevicePublisher(Map<String, DaikinConfig> daikinHosts, String topic, MqttClient mqttClient,
			int devicerefresh) {
		this.daikinHosts = daikinHosts;
		this.topic = topic;
		this.mqttClient = mqttClient;
		this.deviceRefresh = devicerefresh;
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
			try {
				String deviceTopic = topic + "/" + value.getName();
				String nodePropTopic = deviceTopic + "/" + DaikinMqttClient.nodeName + "/";

				// IP address
				try {
					String ipAddr = StringUtils.substringAfter(daikinDevice.getHost(), "http://");
					InetAddress address = InetAddress.getByName(ipAddr);

					MqttMessage message = new MqttMessage(address.getHostAddress().getBytes());
					message.setRetained(true);
					this.mqttClient.publish(deviceTopic + "/$localip", message);
				} catch (UnknownHostException e) {
					logger.error("Could not get IP address", e);
				}

				// Homie version
				MqttMessage message = new MqttMessage("3.0.0".getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$homie", message);

				// name
				String name = "Aircondition - " + value.getName();
				message = new MqttMessage(name.getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$name", message);

				// firmware
				message = new MqttMessage("daikin-mqtt".getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$fw/name", message);

				message = new MqttMessage("1.0.1".getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$fw/version", message);

				// nodes
				message = new MqttMessage(DaikinMqttClient.nodeName.getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$nodes", message);

				// implementation
				message = new MqttMessage("daikin-mqtt".getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$implementation", message);

				// stats
				message = new MqttMessage(("" + deviceRefresh).getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$stats/interval", message);

				// node details

				message = new MqttMessage("Aircondition".getBytes());
				message.setRetained(true);
				this.mqttClient.publish(nodePropTopic + "$name", message);
				this.mqttClient.publish(nodePropTopic + "$type", message);

				message = new MqttMessage(String.join(",", Constants.PROPERTIES).getBytes());
				message.setRetained(true);
				this.mqttClient.publish(nodePropTopic + "$properties", message);

				// setable properties
				message = new MqttMessage("true".getBytes());
				message.setRetained(true);
				this.mqttClient.publish(nodePropTopic + Constants.PR_TARGETTEMP + Constants.PR_SETTABLE, message);
				this.mqttClient.publish(nodePropTopic + Constants.PR_FAN + Constants.PR_SETTABLE, message);
				this.mqttClient.publish(nodePropTopic + Constants.PR_FANDIR + Constants.PR_SETTABLE, message);
				this.mqttClient.publish(nodePropTopic + Constants.PR_MODE + Constants.PR_SETTABLE, message);
				this.mqttClient.publish(nodePropTopic + Constants.PR_POWER + Constants.PR_SETTABLE, message);

			} catch (MqttException e) {
				logger.error("MQTT error", e);
			}
		}
	}
}