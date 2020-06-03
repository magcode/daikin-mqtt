package org.magcode.daikin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.magcode.daikin.mqtt.Subscriber;
import org.magcode.daikin.mqtt.DevicePublisher;
import org.magcode.daikin.mqtt.NodePublisher;

import net.jonathangiles.daikin.DaikinFactory;
import net.jonathangiles.daikin.IDaikin;
import java.util.concurrent.Semaphore;

/**
 * @author magcode MQTT Gateway for Daikin Wifi Adapter
 *
 */
public class DaikinMqttClient {
	private static Map<String, DaikinConfig> daikins;
	private static String mqttServer;
	private static int refresh = 60;
	private static String rootTopic;
	private static MqttClient mqttClient;
	public static final String nodeName = "aircon";
	private static final int deviceRefresh = 600;
	private static final int MAX_INFLIGHT = 100;
	private static Semaphore semaphore;
	private static Logger logger = LogManager.getLogger(DaikinMqttClient.class);

	public static void main(String[] args) throws Exception {
		semaphore = new Semaphore(MAX_INFLIGHT);
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

		// start mqtt node publisher
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		Runnable nodePublisher = new NodePublisher(daikins, rootTopic, mqttClient, semaphore);
		ScheduledFuture<?> nodePublisherFuture = executor.scheduleAtFixedRate(nodePublisher, 10, refresh,
				TimeUnit.SECONDS);

		// start mqtt device publisher
		Runnable devicePublisher = new DevicePublisher(daikins, rootTopic, mqttClient, deviceRefresh, semaphore);
		ScheduledFuture<?> devicePublisherFuture = executor.scheduleAtFixedRate(devicePublisher, 15, deviceRefresh,
				TimeUnit.SECONDS);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Logger logger2 = LogManager.getLogger("shutdown");
				try {
					MqttMessage message = new MqttMessage();
					message.setPayload("disconnected".getBytes());
					message.setRetained(true);
					mqttClient.publish(rootTopic + "/$state", message);
					mqttClient.disconnect();
					logger2.info("Disconnected from MQTT server");
					nodePublisherFuture.cancel(true);
					devicePublisherFuture.cancel(true);
					((LifeCycle) LogManager.getContext()).stop();
				} catch (MqttException e) {
					logger2.error("Error during shutdown", e);
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
			logger.info("Loading properties from " + filePath);
			input = new FileInputStream(filePath);
			props.load(input);

			rootTopic = props.getProperty("rootTopic", "home");
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
			logger.error("Could not read properties", ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("Failed to close file", e);
				}
			}
		}
	}

	private static void startMQTTClient() throws MqttException {
		String hostName = "";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.error("Failed to get hostname", e);
		}
		mqttClient = new MqttClient(mqttServer, "client-for-daikin-on-" + hostName, new MemoryPersistence());
		MqttConnectOptions connOpt = new MqttConnectOptions();
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		connOpt.setMaxInflight(MAX_INFLIGHT);
		connOpt.setAutomaticReconnect(true);
		mqttClient.setCallback(new Subscriber(daikins, rootTopic, semaphore));
		mqttClient.connect();
		logger.info("Connected to MQTT broker.");
		for (Entry<String, DaikinConfig> entry : daikins.entrySet()) {
			DaikinConfig value = entry.getValue();
			String subTopic = rootTopic + "/" + value.getName() + "/aircon/+/set";
			mqttClient.subscribe(subTopic);
			logger.info("Subscribed to {}", subTopic);
		}
	}
}