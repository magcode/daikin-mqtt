package org.magcode.daikin.mqtt;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.daikin.DaikinConfig;

import net.jonathangiles.daikin.enums.Mode;

public class Callback implements MqttCallback {
	private Map<String, DaikinConfig> daikins;
	private String rootTopic;

	public Callback(Map<String, DaikinConfig> daikins, String topic) {
		this.daikins = daikins;
		this.rootTopic = topic;
	}

	@Override
	public void connectionLost(Throwable cause) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		String node = StringUtils.substringAfter(topic, rootTopic + "/");
		node = StringUtils.substringBefore(node, "/");

		if (daikins.containsKey(node)) {
			DaikinConfig targetDaikin = daikins.get(node);
			String targetProperty = StringUtils.substringAfter(topic, rootTopic + "/" + node + "/aircon/");
			targetProperty = StringUtils.substringBefore(targetProperty, "/");
			switch (targetProperty) {
			case "targettemp":
				Float targetTemp = Float.parseFloat(message.toString());
				if (targetTemp > 9 && targetTemp < 30) {
					targetDaikin.getDaikin().setTargetTemperature(targetTemp);
					System.out.println("Set targettemp=" + targetTemp + " for " + node);
				}
				break;
			case "mode":
				targetDaikin.getDaikin().setMode(Mode.Heat);
				break;
			}
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub

	}
}