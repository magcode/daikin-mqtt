package org.magcode.daikin.mqtt;

import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.connector.DaikinConnector;
import org.magcode.daikin.connector.DaikinState;
import org.magcode.daikin.connector.DaikinUnreachableException;
import org.magcode.daikin.connector.enums.Fan;
import org.magcode.daikin.connector.enums.FanDirection;
import org.magcode.daikin.connector.enums.Mode;
import org.magcode.daikin.connector.enums.Power;

public class Subscriber implements MqttCallback {
	private Map<String, DaikinConfig> daikins;
	private String rootTopic;
	private static Logger logger = LogManager.getLogger(Subscriber.class);
	private Semaphore semaphore;

	public Subscriber(Map<String, DaikinConfig> daikins, String topic, Semaphore semaphore) {
		this.daikins = daikins;
		this.rootTopic = topic;
		this.semaphore = semaphore;
	}

	@Override
	public void connectionLost(Throwable cause) {
		logger.warn("MQTT connection lost", cause);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		String node = StringUtils.substringAfter(topic, rootTopic + "/");
		node = StringUtils.substringBefore(node, "/");

		if (daikins.containsKey(node)) {
			DaikinConfig targetDaikin = daikins.get(node);

			try {
				DaikinConnector daikin = targetDaikin.getDaikin();
				if (daikin == null) {
					logger.warn("dakin {} not initialized?", node);
					return;
				}

				DaikinState state = targetDaikin.getDaikin().getState();

				String targetProperty = StringUtils.substringAfter(topic, rootTopic + "/" + node + "/aircon/");
				targetProperty = StringUtils.substringBefore(targetProperty, "/");

				switch (targetProperty) {
				case TopicConstants.PR_TARGETTEMP:
					Float targetTemp = Float.parseFloat(message.toString());
					if (targetTemp > 9 && targetTemp < 30) {
						logger.info("Sending targettemp={} to {}", targetTemp, targetDaikin.getName());
						targetDaikin.getDaikin().updateStatus();
						targetDaikin.getDaikin().setTargetTemperature(targetTemp);
					}
					break;
				case TopicConstants.PR_MODE:
					String givenMode = message.toString();
					Mode mode = Mode.getFromString(givenMode);
					// Turn off
					if (mode == Mode.None) {
						logger.info("Sending power=off to {}", targetDaikin.getName());
						targetDaikin.getDaikin().updateStatus();
						targetDaikin.getDaikin().setPower(Power.Off);
						break;
					}
					// for any other mode we check if AC is already on. If not we turn it on and set
					// mode afterwards.
					if (state.getPower() != Power.On) {
						logger.info("Sending power=on to {}", targetDaikin.getName());
						targetDaikin.getDaikin().updateStatus();
						targetDaikin.getDaikin().setPower(Power.On);
						Thread.sleep(200);
					}
					logger.info("Sending mode={} to {}", mode, targetDaikin.getName());
					targetDaikin.getDaikin().updateStatus();
					targetDaikin.getDaikin().setMode(mode);
					break;
				case TopicConstants.PR_POWER:
					String powerString = message.toString();
					Power power = Power.valueOf(powerString);
					logger.info("Sending power={} to {}", power, targetDaikin.getName());
					targetDaikin.getDaikin().updateStatus();
					targetDaikin.getDaikin().setPower(power);
					break;
				case TopicConstants.PR_FAN:
					String fanString = message.toString();
					Fan fan = Fan.getFromString(fanString);
					logger.info("Sending fan={} to {}", fan, targetDaikin.getName());
					targetDaikin.getDaikin().updateStatus();
					targetDaikin.getDaikin().setFan(fan);
					break;
				case TopicConstants.PR_FANDIR:
					String fanDirString = message.toString();
					FanDirection fanDir = FanDirection.getFromString(fanDirString);
					logger.info("Sending fanDirection={} to {}", fanDir, targetDaikin.getName());
					targetDaikin.getDaikin().updateStatus();
					targetDaikin.getDaikin().setFanDirection(fanDir);
					break;
				}
			} catch (DaikinUnreachableException e1) {
				logger.debug("Daikin {} is unreachable", targetDaikin.getName());
			}
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		semaphore.release();
	}
}