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

			try {
				daikinDevice.readDaikinState();
				if (daikinConfig.getOnlineSince() == null) {
					daikinConfig.setOnlineSince(new Date());
				}

				Date now = new Date();
				long uptime = (now.getTime() - daikinConfig.getOnlineSince().getTime()) / 1000;
				MqttMessage message = new MqttMessage(("" + uptime).getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$stats/uptime", message);

				// current AC data
				message = new MqttMessage(Boolean.toString(daikinDevice.isOn()).getBytes());
				this.mqttClient.publish(nodeTopic + Constants.PR_POWER, message);

				// we force "none" if the device is OFF
				if (daikinDevice.isOn()) {
					message = new MqttMessage(daikinDevice.getMode().toString().getBytes());
					message.setRetained(true);
					this.mqttClient.publish(nodeTopic + Constants.PR_MODE, message);
				} else {
					message = new MqttMessage(Mode.None.toString().getBytes());
					message.setRetained(true);
					this.mqttClient.publish(nodeTopic + Constants.PR_MODE, message);
				}

				message = new MqttMessage(Float.toString(daikinDevice.getTargetTemperature()).getBytes());
				message.setRetained(true);
				this.mqttClient.publish(nodeTopic + Constants.PR_TARGETTEMP, message);

				message = new MqttMessage(Float.toString(daikinDevice.getInsideTemperature()).getBytes());
				message.setRetained(true);
				this.mqttClient.publish(nodeTopic + Constants.PR_INTEMP, message);

				message = new MqttMessage(Float.toString(daikinDevice.getOutsideTemperature()).getBytes());
				message.setRetained(true);
				this.mqttClient.publish(nodeTopic + Constants.PR_OUTTEMP, message);

				message = new MqttMessage(daikinDevice.getFan().toString().getBytes());
				message.setRetained(true);
				this.mqttClient.publish(nodeTopic + Constants.PR_FAN, message);

				message = new MqttMessage(daikinDevice.getFanDirection().toString().getBytes());
				message.setRetained(true);
				this.mqttClient.publish(nodeTopic + Constants.PR_FANDIR, message);

				message = new MqttMessage("ready".getBytes());
				message.setRetained(true);
				this.mqttClient.publish(deviceTopic + "/$state", message);

			} catch (DaikinUnreachableException e1) {
				logger.debug("Daikin {} is unreachable", daikinConfig.getName());
				daikinConfig.setOnlineSince(null);
				try {
					MqttMessage message = new MqttMessage(("0").getBytes());
					message.setRetained(true);
					this.mqttClient.publish(deviceTopic + "/$stats/uptime", message);

					message = new MqttMessage("lost".getBytes());
					message.setRetained(true);
					this.mqttClient.publish(deviceTopic + "/$state", message);

					message = new MqttMessage(Mode.None.toString().getBytes());
					message.setRetained(true);
					this.mqttClient.publish(nodeTopic + Constants.PR_MODE, message);

					message = new MqttMessage("false".getBytes());
					message.setRetained(true);
					this.mqttClient.publish(nodeTopic + Constants.PR_POWER, message);

					message = new MqttMessage("0".getBytes());
					message.setRetained(true);
					this.mqttClient.publish(nodeTopic + Constants.PR_OUTTEMP, message);
					this.mqttClient.publish(nodeTopic + Constants.PR_INTEMP, message);
					this.mqttClient.publish(nodeTopic + Constants.PR_TARGETTEMP, message);

					message = new MqttMessage(Fan.None.toString().getBytes());
					message.setRetained(true);
					this.mqttClient.publish(nodeTopic + Constants.PR_FAN, message);

					message = new MqttMessage(FanDirection.None.toString().getBytes());
					message.setRetained(true);
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