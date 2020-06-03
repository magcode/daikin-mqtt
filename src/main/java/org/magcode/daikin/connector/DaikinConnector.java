package org.magcode.daikin.connector;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.magcode.daikin.connector.enums.*;

public class DaikinConnector {
	private URI statusUrl;
	private URI sensorUrl;
	private DaikinState daikinState;
	private String host;
	private final long timeoutSeconds = 2;
	private CloseableHttpClient httpclient;
	final RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout(Timeout.ofSeconds(timeoutSeconds))
			.setConnectTimeout(Timeout.ofSeconds(timeoutSeconds)).build();
	private static Logger logger = LogManager.getLogger(DaikinConnector.class);

	public DaikinConnector(String host) {
		this.host = host;

		try {
			URIBuilder builder = new URIBuilder();
			builder.setScheme("http");
			builder.setHost(host);
			builder.setPath("/aircon/get_control_info");
			this.statusUrl = builder.build();
			builder.setPath("/aircon/get_sensor_info");
			this.sensorUrl = builder.build();
		} catch (URISyntaxException e) {
			logger.error("URI build failure", e);
		}
		httpclient = HttpClients.createDefault();
		this.daikinState = new DaikinState();
	}

	public void setTargetTemperature(float targetTemp) throws DaikinUnreachableException {
		this.daikinState.setTargetTemp(targetTemp);
		sendCommand();
	}

	public void setPower(Power power) throws DaikinUnreachableException {
		this.daikinState.setPower(power);
		sendCommand();
	}

	public void setFanDirection(FanDirection fanDirection) throws DaikinUnreachableException {
		this.daikinState.setFanDirection(fanDirection);
		sendCommand();
	}

	public void setFan(Fan fan) throws DaikinUnreachableException {
		this.daikinState.setFan(fan);
		sendCommand();
	}

	public void setMode(Mode mode) throws DaikinUnreachableException {
		this.daikinState.setMode(mode);
		sendCommand();
	}

	private void sendCommand() throws DaikinUnreachableException {
		URIBuilder builder = new URIBuilder();
		builder.setScheme("http");
		builder.setHost(host);
		builder.setPath("/aircon/set_control_info");
		builder.addParameter("pow", this.daikinState.getPower().getValue());
		builder.addParameter("mode", this.daikinState.getMode().getValue());
		builder.addParameter("stemp", String.valueOf(this.daikinState.getTargetTemp()));
		builder.addParameter("shum", "");
		builder.addParameter("f_rate", this.daikinState.getFan().getValue());
		builder.addParameter("f_dir", this.daikinState.getFanDirection().getValue());
		try {
			getOrSendStatus(builder.build());
		} catch (URISyntaxException e) {
			logger.error("URI build failure", e);
		}
	}

	public void updateStatus() throws DaikinUnreachableException {
		Map<String, String> properties = getOrSendStatus(this.statusUrl);
		Fan fan = Fan.getFromValue(properties.get("f_rate"));
		FanDirection fanDir = FanDirection.getFromValue(properties.get("f_dir"));
		Mode mode = Mode.getFromValue(properties.get("mode"));
		Power power = Power.get(properties.get("pow"));
		this.daikinState.setFan(fan);
		this.daikinState.setFanDirection(fanDir);
		this.daikinState.setMode(mode);
		this.daikinState.setPower(power);
		this.daikinState.setTargetTemp(parseFloat(properties.get("stemp")));

		properties = getOrSendStatus(this.sensorUrl);
		this.daikinState.setInsideTemp(parseFloat(properties.get("htemp")));
		this.daikinState.setOutsideTemp(parseFloat(properties.get("otemp")));
	}

	private Map<String, String> getOrSendStatus(URI uri) throws DaikinUnreachableException {
		final HttpGet httpget = new HttpGet(uri);
		httpget.setConfig(requestConfig);
		final HttpClientResponseHandler<String> responseHandler = new HttpClientResponseHandler<String>() {

			@Override
			public String handleResponse(final ClassicHttpResponse response) throws IOException {
				final int status = response.getCode();
				if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
					final HttpEntity entity = response.getEntity();
					try {
						return entity != null ? EntityUtils.toString(entity) : null;
					} catch (final ParseException ex) {
						throw new ClientProtocolException(ex);
					}
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}

		};
		String responseBody;
		try {
			responseBody = httpclient.execute(httpget, responseHandler);
			logger.trace("Got response for {}: {}", uri.toString(), responseBody);
			Map<String, String> properties = mapFromStateString(responseBody);
			String ret = properties.get("ret");
			if (!"OK".equals(ret)) {
				logger.warn("Got unexpected value for 'ret': {}", ret);
			}
			return properties;

		} catch (IOException e) {
			throw new DaikinUnreachableException();
		}

	}

	public DaikinState getState() {
		return this.daikinState;
	}

	private Map<String, String> mapFromStateString(String controlInfo) {
		Map<String, String> properties = new HashMap<>();
		String[] splitString = controlInfo.split(",");
		for (String property : splitString) {
			int equalsPos = property.indexOf("=");
			String key = property.substring(0, equalsPos);
			if ((equalsPos + 1) >= property.length()) {
				continue; // Dont add properties without a value
			}
			String value = property.substring(equalsPos + 1);
			properties.put(key, value);
		}
		return properties;
	}

	private float parseFloat(String value) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}
}
