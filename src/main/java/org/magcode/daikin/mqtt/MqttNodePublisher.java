package org.magcode.daikin.mqtt;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.magcode.daikin.Constants;
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.DaikinMqttClient;

import net.jonathangiles.daikin.IDaikin;
import net.jonathangiles.daikin.util.DaikinUnreachableException;

public class MqttNodePublisher implements Runnable {
	private MqttClient mqttClient;
	private String topic;
	private Map<String, DaikinConfig> daikinHosts;

	public MqttNodePublisher(Map<String, DaikinConfig> daikinHosts, String topic, MqttClient mqttClient) {
		this.daikinHosts = daikinHosts;
		this.topic = topic;
		this.mqttClient = mqttClient;
	}

	@Override
	public void run() {

		// nodes
		for (Entry<String, DaikinConfig> entry : this.daikinHosts.entrySet()) {
			DaikinConfig value = entry.getValue();
			IDaikin daikinDevice = value.getDaikin();
			try {
				daikinDevice.readDaikinState();
			} catch (DaikinUnreachableException e1) {
				// TODO Auto-generated catch block
			}
			try {

				MqttMessage message = new MqttMessage();
				message.setRetained(true);

				String nodePropTopic = topic + "/" + value.getName() + "/" + DaikinMqttClient.nodeName + "/";

				// stats
				RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
				long uptime = rb.getUptime();
				message.setPayload(("" + uptime).getBytes());
				this.mqttClient.publish(topic + value.getName() + "/$stats/uptime", message);

				message.setPayload(Boolean.toString(daikinDevice.isOn()).getBytes());
				this.mqttClient.publish(nodePropTopic + Constants.PR_POWER, message);

				message.setPayload(daikinDevice.getMode().toString().getBytes());
				this.mqttClient.publish(nodePropTopic + Constants.PR_MODE, message);

				message.setPayload(Float.toString(daikinDevice.getTargetTemperature()).getBytes());
				this.mqttClient.publish(nodePropTopic + Constants.PR_TARGETTEMP, message);

				message.setPayload(Float.toString(daikinDevice.getInsideTemperature()).getBytes());
				this.mqttClient.publish(nodePropTopic + Constants.PR_INTEMP, message);

				message.setPayload(Float.toString(daikinDevice.getOutsideTemperature()).getBytes());
				this.mqttClient.publish(nodePropTopic + Constants.PR_OUTTEMP, message);

				message.setPayload(Boolean.toString(daikinDevice.isReachable()).getBytes());
				this.mqttClient.publish(nodePropTopic + "reachable", message);

				message.setPayload(daikinDevice.getFan().toString().getBytes());
				this.mqttClient.publish(nodePropTopic + Constants.PR_FAN, message);

				message.setPayload(daikinDevice.getFanDirection().toString().getBytes());
				this.mqttClient.publish(nodePropTopic + Constants.PR_FANDIR, message);

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