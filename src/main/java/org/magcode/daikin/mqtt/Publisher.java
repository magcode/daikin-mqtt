package org.magcode.daikin.mqtt;

import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public abstract class Publisher {
	protected MqttClient mqttClient;
	protected Semaphore semaphore;
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
			message.setRetained(true);
			if (semaphore.availablePermits() < 10) {
				logger.warn("Semaphore available permits: {}", semaphore.availablePermits());
			}
			semaphore.acquire();
			this.mqttClient.publish(topic, message);
		} catch (InterruptedException e) {
			logger.error("Interrupt", e);
		} catch (MqttPersistenceException e) {
			logger.error("MqttPersistenceException", e);
		} catch (MqttException e) {
			logger.error("MqttException", e);
		}
	}
}
