package org.magcode.daikin.mqtt;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.magcode.daikin.DaikinConfig;
import org.magcode.daikin.DaikinMqttClient;
import org.magcode.daikin.connector.DaikinConnector;
import org.magcode.daikin.connector.DaikinState;
import org.magcode.daikin.connector.DaikinUnreachableException;
import org.magcode.daikin.connector.enums.Fan;
import org.magcode.daikin.connector.enums.FanDirection;
import org.magcode.daikin.connector.enums.Mode;
import org.magcode.daikin.connector.enums.Power;

public class NodePublisher extends Publisher implements Runnable {
	private String topic;
	private Map<String, DaikinConfig> daikinHosts;
	private static Logger logger = LogManager.getLogger(NodePublisher.class);

	public NodePublisher(Map<String, DaikinConfig> daikinHosts, String topic, MqttClient mqttClient, boolean retained,
			int qos) {
		this.daikinHosts = daikinHosts;
		this.topic = topic;
		this.mqttClient = mqttClient;
		this.qos = qos;
		this.retained = retained;
	}

	@Override
	public void run() {

		// nodes
		for (Entry<String, DaikinConfig> entry : this.daikinHosts.entrySet()) {
			DaikinConfig daikinConfig = entry.getValue();
			DaikinConnector daikinDevice = daikinConfig.getDaikin();
			String deviceTopic = topic + "/" + daikinConfig.getName();
			String nodeTopic = deviceTopic + "/" + DaikinMqttClient.nodeName + "/";
			try {
				daikinDevice.updateStatus();
				DaikinState status = daikinDevice.getState();

				if (daikinConfig.getOnlineSince() == null) {
					daikinConfig.setOnlineSince(new Date());
				}

				Date now = new Date();
				long uptime = (now.getTime() - daikinConfig.getOnlineSince().getTime()) / 1000;
				Publish(deviceTopic + "/$stats/uptime", "" + uptime);

				Publish(nodeTopic + TopicConstants.PR_POWER, status.getPower().toString());
				Publish(nodeTopic + TopicConstants.PR_MODE, status.getMode().toString());
				Publish(nodeTopic + TopicConstants.PR_TARGETTEMP, status.getTargetTemp());
				Publish(nodeTopic + TopicConstants.PR_INTEMP, status.getInsideTemp());
				Publish(nodeTopic + TopicConstants.PR_OUTTEMP, status.getOutsideTemp());
				Publish(nodeTopic + TopicConstants.PR_FAN, status.getFan().toString());
				Publish(nodeTopic + TopicConstants.PR_FANDIR, status.getFanDirection().toString());
				Publish(deviceTopic + "/$state", "ready");

			} catch (DaikinUnreachableException e1) {
				logger.debug("Daikin {} is unreachable", daikinConfig.getName());
				daikinConfig.setOnlineSince(null);
				Publish(deviceTopic + "/$stats/uptime", 0);
				Publish(deviceTopic + "/$state", "lost");
				Publish(nodeTopic + TopicConstants.PR_MODE, Mode.None.toString());
				Publish(nodeTopic + TopicConstants.PR_POWER, Power.Off.toString());
				Publish(nodeTopic + TopicConstants.PR_OUTTEMP, 0.0f);
				Publish(nodeTopic + TopicConstants.PR_INTEMP, 0.0f);
				Publish(nodeTopic + TopicConstants.PR_TARGETTEMP, 0.0f);
				Publish(nodeTopic + TopicConstants.PR_FAN, Fan.None.toString());
				Publish(nodeTopic + TopicConstants.PR_FANDIR, FanDirection.None.toString());
			}
		}
	}
}