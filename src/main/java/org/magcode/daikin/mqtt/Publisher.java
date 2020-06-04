package org.magcode.daikin.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public abstract class Publisher {
	protected MqttClient mqttClient;
	protected int qos = 0;
	protected boolean retained = false;
	private static Logger logger = LogManager.getLogger(Publisher.class);

	protected void Publish(String topic, String payload) {
		MqttMessage message = new MqttMessage(payload.getBytes());
		Publish(topic, message);
	}

	protected void Publish(String topic, boolean payload) {
		Publish(topic, new Boolean(payload).toString());
	}

	protected void Publish(String topic, float payload) {
		Publish(topic, Float.toString(payload));
	}

	protected void Publish(String topic, int payload) {
		Publish(topic, Integer.toString(payload));
	}

	private void Publish(String topic, MqttMessage message) {
		try {
			message.setRetained(this.retained);
			message.setQos(this.qos);
			this.mqttClient.publish(topic, message);
		} catch (MqttPersistenceException e) {
			logger.error("MqttPersistenceException", e);
		} catch (MqttException e) {
			logger.error("MqttException", e);
		}
	}
}
