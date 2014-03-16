/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.theenergydetective.internal;

import java.io.IOException;
import java.io.StringReader;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.theenergydetective.TheEnergyDetectiveBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.net.http.HttpUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implement this class if you are going create an actively polling service like
 * querying a Website/Device.
 * 
 * @author rgordon
 * @since 1.5.0
 */
public class TheEnergyDetectiveBinding extends
		AbstractActiveBinding<TheEnergyDetectiveBindingProvider> implements
		ManagedService {

	private static final Logger logger = LoggerFactory
			.getLogger(TheEnergyDetectiveBinding.class);

	/** TED default http port */
	private static final int DEFAULT_PORT = 80;

	/**
	 * the timeout to use for connecting to a given host (defaults to 5000
	 * milliseconds)
	 */
	private int timeout = 5000;

	/** RegEx to validate a config <code>'^(.*?)\\.(host|port)$'</code> */
	private static final Pattern EXTRACT_CONFIG_PATTERN = Pattern
			.compile("^(.*?)\\.(host|port)$");

	/**
	 * the refresh interval which is used to poll values from the
	 * TheEnergyDetective gateway (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 60000;

	/** Map table to store all available TED gateways configured by the user */
	protected Map<String, DeviceConfig> deviceConfigCache = null;

	public TheEnergyDetectiveBinding() {
	}

	public void activate() {
		super.activate();
		setProperlyConfigured(true);
	}

	public void deactivate() {
		// deallocate resources here that are no longer needed and
		// should be reset when activating this binding again
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected String getName() {
		return "The Energy Detective Refresh Service";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void execute() {
		// the frequently executed code (polling)
		logger.debug("execute() method is called!");

		if (deviceConfigCache != null) {
			for (String device : deviceConfigCache.keySet()) {
				DeviceConfig ted = deviceConfigCache.get(device);
				if (ted != null) {
					logger.debug("About to reload live data from {} {}:{}",
							ted.deviceId, ted.host, ted.port);
					String url = "http://" + ted.host + "/api/LiveData.xml";
					String response = HttpUtil.executeUrl("GET", url, null,
							null, null, timeout);
					processLiveData(device, response);
				}
			}
		}
	}

	protected void processLiveData(String device, String response) {
		logger.debug("processLiveData");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = factory.newDocumentBuilder();
			InputSource inStream = new InputSource();
			inStream.setCharacterStream(new StringReader(response));
			Document doc = db.parse(inStream);
			Element root = doc.getDocumentElement();
			if (root.getTagName().equals("LiveData")) {
				for (TheEnergyDetectiveBindingProvider provider : providers) {
					logger.debug("provider {}", provider.getItemNames()
							.toString());
					for (String itemName : provider.getItemNames()) {
						String gw = provider.getGatewayForItem(itemName);
						if (gw.equals(device)) {
							String val = getValueFromXml(root,
									provider.getElementPathForItem(itemName));
							State v = new DecimalType(Integer.parseInt(val));
							eventPublisher.postUpdate(itemName, v);
						}
					}
				}
			}
		} catch (ParserConfigurationException e) {
			logger.error("XML error {}", e.getMessage());
		} catch (SAXException e) {
			logger.error("XML error {}", e.getMessage());
		} catch (IOException e) {
			logger.error("XML error {}", e.getMessage());
		}
	}

	private String getValueFromXml(Element doc, List<String> elementPath) {
		if (elementPath.size() > 0) {
			NodeList nodes = doc.getElementsByTagName(elementPath.get(0));
			if (elementPath.size() == 1) {
				Element pow = (Element) nodes.item(0);
				return pow.getTextContent();
			} else {
				Element el = (Element) nodes.item(0);
				return getValueFromXml(el,
						elementPath.subList(1, elementPath.size()));
			}
		}
		return null;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.info("internalReceiveUpdate() is called!");
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		logger.debug("updated() is called!");
		if (config != null) {

			// to override the default refresh interval one has to add a
			// parameter to openhab.cfg like
			// <bindingName>:refresh=<intervalInMs>
			String refreshIntervalString = (String) config.get("refresh");
			if (StringUtils.isNotBlank(refreshIntervalString)) {
				refreshInterval = Long.parseLong(refreshIntervalString);
			}

			Enumeration<String> keys = config.keys();

			if (deviceConfigCache == null) {
				deviceConfigCache = new HashMap<String, DeviceConfig>();
			}

			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();

				// the config-key enumeration contains additional keys that we
				// don't want to process here ...
				if ("service.pid".equals(key)) {
					continue;
				}

				Matcher matcher = EXTRACT_CONFIG_PATTERN.matcher(key);

				if (!matcher.matches()) {
					logger.debug("given config key '"
							+ key
							+ "' does not follow the expected pattern '<id>.<host|port>'");
					continue;
				}

				matcher.reset();
				matcher.find();

				String deviceId = matcher.group(1);

				DeviceConfig deviceConfig = deviceConfigCache.get(deviceId);

				if (deviceConfig == null) {
					deviceConfig = new DeviceConfig(deviceId);
					deviceConfigCache.put(deviceId, deviceConfig);
				}

				String configKey = matcher.group(2);
				String value = (String) config.get(key);

				if ("host".equals(configKey)) {
					deviceConfig.host = value;
				} else if ("port".equals(configKey)) {
					deviceConfig.port = Integer.valueOf(value);
				} else {
					throw new ConfigurationException(configKey,
							"the given configKey '" + configKey
									+ "' is unknown");
				}
			}

			setProperlyConfigured(true);
		}
	}

	/**
	 * Internal data structure which carries the connection details of one
	 * device (there could be several)
	 */
	static class DeviceConfig {

		String host;
		int port = DEFAULT_PORT;

		String deviceId;

		public DeviceConfig(String deviceId) {
			this.deviceId = deviceId;
		}

		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		@Override
		public String toString() {
			return "Device [id=" + deviceId + ", host=" + host + ", port="
					+ port + "]";
		}
	}

}
