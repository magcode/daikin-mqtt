package org.magcode.daikin.mqtt;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.magcode.daikin.Constants;
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.DaikinMqttClient;

import net.jonathangiles.daikin.IDaikin;
import net.jonathangiles.daikin.enums.Fan;
import net.jonathangiles.daikin.enums.FanDirection;
import net.jonathangiles.daikin.enums.Mode;
import net.jonathangiles.daikin.util.DaikinUnreachableException;

public class NodePublisher extends Publisher implements Runnable {
	private String topic;
	private Map<String, DaikinConfig> daikinHosts;
	private static Logger logger = LogManager.getLogger(NodePublisher.class);

	public NodePublisher(Map<String, DaikinConfig> daikinHosts, String topic, MqttClient mqttClient,
			Semaphore semaphore) {
		this.daikinHosts = daikinHosts;
		this.topic = topic;
		this.mqttClient = mqttClient;
		this.semaphore = semaphore;
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
				Publish(deviceTopic + "/$stats/uptime", "" + uptime);

				// current AC data
				Publish(nodeTopic + Constants.PR_POWER, daikinDevice.isOn());

				// we force "none" if the device is OFF
				if (daikinDevice.isOn()) {
					Publish(nodeTopic + Constants.PR_MODE, daikinDevice.getMode().toString());
				} else {
					Publish(nodeTopic + Constants.PR_MODE, Mode.None.toString());
				}

				Publish(nodeTopic + Constants.PR_TARGETTEMP, daikinDevice.getTargetTemperature());
				Publish(nodeTopic + Constants.PR_INTEMP, daikinDevice.getInsideTemperature());
				Publish(nodeTopic + Constants.PR_OUTTEMP, daikinDevice.getOutsideTemperature());
				Publish(nodeTopic + Constants.PR_FAN, daikinDevice.getFan().toString());
				Publish(nodeTopic + Constants.PR_FANDIR, daikinDevice.getFanDirection().toString());
				Publish(deviceTopic + "/$state", "ready");

			} catch (DaikinUnreachableException e1) {
				logger.debug("Daikin {} is unreachable", daikinConfig.getName());
				daikinConfig.setOnlineSince(null);
				Publish(deviceTopic + "/$stats/uptime", 0);
				Publish(deviceTopic + "/$state", "lost");
				Publish(nodeTopic + Constants.PR_MODE, Mode.None.toString());
				Publish(nodeTopic + Constants.PR_POWER, false);
				Publish(nodeTopic + Constants.PR_OUTTEMP, 0.0f);
				Publish(nodeTopic + Constants.PR_INTEMP, 0.0f);
				Publish(nodeTopic + Constants.PR_TARGETTEMP, 0.0f);
				Publish(nodeTopic + Constants.PR_FAN, Fan.None.toString());
				Publish(nodeTopic + Constants.PR_FANDIR, FanDirection.None.toString());
			}
		}
	}
}