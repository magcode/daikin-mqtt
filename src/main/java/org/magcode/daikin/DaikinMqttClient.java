package org.magcode.daikin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.magcode.daikin.mqtt.Callback;
import org.magcode.daikin.mqtt.MqttNodePublisher;

import net.jonathangiles.daikin.DaikinFactory;
import net.jonathangiles.daikin.IDaikin;

public class DaikinMqttClient {
	private static Map<String, DaikinConfig> daikins;
	private static String mqttServer;
	private static int refresh = 60;
	private static String rootTopic;
	private static MqttClient mqttClient;
	public static final String nodeName = "aircon";

	public static void main(String[] args) throws Exception {

		daikins = new HashMap<String, DaikinConfig>();
		readProps();

		// connect to MQTT broker
		startMQTTClient();

		for (Entry<String, DaikinConfig> entry : daikins.entrySet()) {
			DaikinConfig value = entry.getValue();
			IDaikin daikin = null;
			if (value.getType().equals(DaikinType.wireless)) {
				daikin = DaikinFactory.createWirelessDaikin("http://" + value.getHost(), 0);
			} else if (value.getType().equals(DaikinType.wired)) {
				daikin = DaikinFactory.createWiredDaikin("http://" + value.getHost(), 0);
			}
			value.setDaikin(daikin);
		}

		// start mqtt device publisher
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		Runnable devicePublisher = new MqttNodePublisher(daikins, rootTopic, mqttClient);
		ScheduledFuture<?> devicePublisherFuture = executor.scheduleAtFixedRate(devicePublisher, 2, refresh,
				TimeUnit.SECONDS);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					MqttMessage message = new MqttMessage();
					message.setPayload("disconnected".getBytes());
					message.setRetained(true);
					mqttClient.publish(rootTopic + "/$state", message);

					mqttClient.disconnect();
					System.out.println("Disconnected from MQTT server");

					devicePublisherFuture.cancel(true);
					devicePublisherFuture.cancel(true);

				} catch (MqttException e) {
					System.out.println(e);
				}
			}
		});
	}

	private static void readProps() {
		Properties props = new Properties();
		InputStream input = null;

		try {
			File jarPath = new File(
					DaikinMqttClient.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			String propertiesPath = jarPath.getParentFile().getAbsolutePath();
			String filePath = propertiesPath + "/daikin.properties";
			System.out.println("Loading properties from " + filePath);
			input = new FileInputStream(filePath);
			props.load(input);

			rootTopic = props.getProperty("topic", "something");
			refresh = Integer.parseInt(props.getProperty("refresh", "60"));
			mqttServer = props.getProperty("mqttServer", "tcp://localhost");
			Enumeration<?> e = props.propertyNames();

			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				for (int i = 1; i < 11; i++) {
					if (key.equals("daikin" + i + ".host")) {
						DaikinConfig one = new DaikinConfig();
						one.setHost(props.getProperty("daikin" + i + ".host"));
						one.setName(props.getProperty("daikin" + i + ".name"));
						DaikinType type = DaikinType.valueOf(props.getProperty("daikin" + i + ".type"));
						one.setType(type);
						daikins.put(one.getName(), one);
					}
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void startMQTTClient() throws MqttException {
		System.out.println("Starting MQTT Client ...");
		mqttClient = new MqttClient(mqttServer, "client-for-daikin");
		mqttClient.setCallback(new Callback(daikins, rootTopic));
		mqttClient.connect();
		for (Entry<String, DaikinConfig> entry : daikins.entrySet()) {
			DaikinConfig value = entry.getValue();
			String subTopic = rootTopic + "/" + value.getName() + "/aircon/+/set";
			mqttClient.subscribe(subTopic);
			System.out.println("Subcribed to " + subTopic);
		}
		System.out.println("Connected to MQTT broker.");
	}
}