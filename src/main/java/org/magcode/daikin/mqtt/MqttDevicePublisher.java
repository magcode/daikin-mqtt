package org.magcode.daikin.mqtt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.magcode.daikin.Constants;
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.DaikinMqttClient;

import net.jonathangiles.daikin.IDaikin;
import net.jonathangiles.daikin.util.DaikinUnreachableException;

public class MqttDevicePublisher implements Runnable {
	private MqttClient mqttClient;
	private String topic;
	private Map<String, DaikinConfig> daikinHosts;

	public MqttDevicePublisher(Map<String, DaikinConfig> daikinHosts, String topic, MqttClient mqttClient) {
		this.daikinHosts = daikinHosts;
		this.topic = topic;
		this.mqttClient = mqttClient;
	}

	@Override
	public void run() {
		for (Entry<String, DaikinConfig> entry : this.daikinHosts.entrySet()) {
			DaikinConfig value = entry.getValue();
			IDaikin daikinDevice = value.getDaikin();
			try {
				daikinDevice.readDaikinState();
			} catch (DaikinUnreachableException e1) {
				// we don't care
			}
			try {
				String deviceTopic = topic + "/" + value.getName();
				String nodePropTopic = deviceTopic + "/" + DaikinMqttClient.nodeName + "/";

				MqttMessage message = new MqttMessage();
				message.setRetained(true);

				// IP address
				try {
					String ipAddr = StringUtils.substringAfter(daikinDevice.getHost(), "http://");
					InetAddress address = InetAddress.getByName(ipAddr);

					message.setPayload(address.getHostAddress().getBytes());
					this.mqttClient.publish(deviceTopic + "/$localip", message);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}

				// state
				if (daikinDevice.isReachable()) {
					message.setPayload("ready".getBytes());
				} else {
					message.setPayload("lost".getBytes());
				}

				this.mqttClient.publish(deviceTopic + "/$state", message);

				// Homie
				message.setPayload("2.1.0".getBytes());
				this.mqttClient.publish(deviceTopic + "/$homie", message);

				// nodes
				message.setPayload(DaikinMqttClient.nodeName.getBytes());
				this.mqttClient.publish(deviceTopic + "/$nodes", message);
				// setable properties
				message.setPayload("true".getBytes());
				this.mqttClient.publish(nodePropTopic + Constants.PR_TARGETTEMP + Constants.PR_SETTABLE, message);
				this.mqttClient.publish(nodePropTopic + Constants.PR_FAN + Constants.PR_SETTABLE, message);
				this.mqttClient.publish(nodePropTopic + Constants.PR_FANDIR + Constants.PR_SETTABLE, message);
				this.mqttClient.publish(nodePropTopic + Constants.PR_MODE + Constants.PR_SETTABLE, message);

			} catch (MqttPersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}