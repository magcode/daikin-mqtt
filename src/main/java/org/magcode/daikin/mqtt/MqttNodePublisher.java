package org.magcode.daikin.mqtt;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.daikin.Constants;
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.DaikinMqttClient;

import net.jonathangiles.daikin.IDaikin;
import net.jonathangiles.daikin.enums.Fan;
import net.jonathangiles.daikin.enums.FanDirection;
import net.jonathangiles.daikin.enums.Mode;
import net.jonathangiles.daikin.util.DaikinUnreachableException;

public class MqttNodePublisher implements Runnable {
	private MqttClient mqttClient;
	private String topic;
	private Map<String, DaikinConfig> daikinHosts;
	private static Logger logger = LogManager.getLogger(MqttNodePublisher.class);

	public MqttNodePublisher(Map<String, DaikinConfig> daikinHosts, String topic, MqttClient mqttClient) {
		this.daikinHosts = daikinHosts;
		this.topic = topic;
		this.mqttClient = mqttClient;
	}

	@Override
	public void run() {

		// nodes
		for (Entry<String, DaikinConfig> entry : this.daikinHosts.entrySet()) {
			DaikinConfig daikinConfig = entry.getValue();
			IDaikin daikinDevice = daikinConfig.getDaikin();
			String deviceTopic = topic + "/" + daikinConfig.getName();
			String nodeTopic = deviceTopic + "/" + DaikinMqttClient.nodeName + "/";
			MqttMessage message = new MqttMessage();
			message.setRetained(true);
			try {
				daikinDevice.readDaikinState();
				if (daikinConfig.getOnlineSince() == null) {
					daikinConfig.setOnlineSince(new Date());
				}

				Date now = new Date();
				long uptime = (now.getTime() - daikinConfig.getOnlineSince().getTime()) / 1000;
				message.setPayload(("" + uptime).getBytes());
				this.mqttClient.publish(deviceTopic + "/$stats/uptime", message);

				// current AC data
				message.setPayload(Boolean.toString(daikinDevice.isOn()).getBytes());
				this.mqttClient.publish(nodeTopic + Constants.PR_POWER, message);

				// we force "none" if the device is OFF
				if (daikinDevice.isOn()) {
					message.setPayload(daikinDevice.getMode().toString().getBytes());
					this.mqttClient.publish(nodeTopic + Constants.PR_MODE, message);
				} else {
					message.setPayload(Mode.None.toString().getBytes());
					this.mqttClient.publish(nodeTopic + Constants.PR_MODE, message);
				}

				message.setPayload(Float.toString(daikinDevice.getTargetTemperature()).getBytes());
				this.mqttClient.publish(nodeTopic + Constants.PR_TARGETTEMP, message);

				message.setPayload(Float.toString(daikinDevice.getInsideTemperature()).getBytes());
				this.mqttClient.publish(nodeTopic + Constants.PR_INTEMP, message);

				message.setPayload(Float.toString(daikinDevice.getOutsideTemperature()).getBytes());
				this.mqttClient.publish(nodeTopic + Constants.PR_OUTTEMP, message);

				message.setPayload(daikinDevice.getFan().toString().getBytes());
				this.mqttClient.publish(nodeTopic + Constants.PR_FAN, message);

				message.setPayload(daikinDevice.getFanDirection().toString().getBytes());
				this.mqttClient.publish(nodeTopic + Constants.PR_FANDIR, message);

				message.setPayload("ready".getBytes());
				this.mqttClient.publish(deviceTopic + "/$state", message);

			} catch (DaikinUnreachableException e1) {
				logger.debug("Daikin {} is unreachable", daikinConfig.getName());
				daikinConfig.setOnlineSince(null);
				try {
					message.setPayload(("0").getBytes());
					this.mqttClient.publish(deviceTopic + "/$stats/uptime", message);
					message.setPayload("lost".getBytes());
					this.mqttClient.publish(deviceTopic + "/$state", message);
					message.setPayload(Mode.None.toString().getBytes());
					this.mqttClient.publish(nodeTopic + Constants.PR_MODE, message);
					message.setPayload("false".getBytes());
					this.mqttClient.publish(nodeTopic + Constants.PR_POWER, message);
					message.setPayload("0".getBytes());
					this.mqttClient.publish(nodeTopic + Constants.PR_OUTTEMP, message);
					this.mqttClient.publish(nodeTopic + Constants.PR_INTEMP, message);
					this.mqttClient.publish(nodeTopic + Constants.PR_TARGETTEMP, message);
					message.setPayload(Fan.None.toString().getBytes());
					this.mqttClient.publish(nodeTopic + Constants.PR_FAN, message);
					message.setPayload(FanDirection.None.toString().getBytes());
					this.mqttClient.publish(nodeTopic + Constants.PR_FANDIR, message);
				} catch (MqttException e) {
					logger.error("MQTT error", e);
				}
			} catch (MqttException e) {
				logger.error("MQTT error", e);
			}
		}
	}
}