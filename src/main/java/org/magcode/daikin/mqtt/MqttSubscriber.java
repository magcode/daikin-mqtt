package org.magcode.daikin.mqtt;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.daikin.Constants;
import org.magcode.daikin.DaikinConfig;

import net.jonathangiles.daikin.enums.Mode;
import net.jonathangiles.daikin.util.DaikinUnreachableException;

public class MqttSubscriber implements MqttCallback {
	private Map<String, DaikinConfig> daikins;
	private String rootTopic;
	private static Logger logger = LogManager.getLogger(MqttSubscriber.class);

	public MqttSubscriber(Map<String, DaikinConfig> daikins, String topic) {
		this.daikins = daikins;
		this.rootTopic = topic;
	}

	@Override
	public void connectionLost(Throwable cause) {

	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		String node = StringUtils.substringAfter(topic, rootTopic + "/");
		node = StringUtils.substringBefore(node, "/");

		if (daikins.containsKey(node)) {
			DaikinConfig targetDaikin = daikins.get(node);

			try {
				targetDaikin.getDaikin().readDaikinState();

				String targetProperty = StringUtils.substringAfter(topic, rootTopic + "/" + node + "/aircon/");
				targetProperty = StringUtils.substringBefore(targetProperty, "/");

				switch (targetProperty) {
				case Constants.PR_TARGETTEMP:
					Float targetTemp = Float.parseFloat(message.toString());
					if (targetTemp > 9 && targetTemp < 30) {
						targetDaikin.getDaikin().setTargetTemperature(targetTemp);
						System.out.println("Set targettemp=" + targetTemp + " for " + node);
					}
					break;
				case Constants.PR_MODE:
					targetDaikin.getDaikin().setMode(Mode.Heat);
					break;
				case Constants.PR_POWER:
					String messageString = message.toString();
					if (StringUtils.isNotBlank(messageString)) {
						Boolean turnOn = Boolean.parseBoolean(message.toString());
						if (targetDaikin != null && targetDaikin.getDaikin() != null) {
							logger.info("Sending power={} to {}", turnOn, targetDaikin.getName());
							targetDaikin.getDaikin().setOn(turnOn);
						}
					}
					break;
				}
			} catch (DaikinUnreachableException e1) {
				logger.debug("Daikin {} is unreachable", targetDaikin.getName());
			}
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {

	}
}