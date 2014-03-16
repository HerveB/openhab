/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.theenergydetective.internal;

import java.util.ArrayList;
import java.util.Arrays;

import org.openhab.binding.theenergydetective.TheEnergyDetectiveBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author rgordon
 * @since 1.5.0
 */
public class TheEnergyDetectiveGenericBindingProvider extends
		AbstractGenericBindingProvider implements
		TheEnergyDetectiveBindingProvider {

	private static final Logger logger = LoggerFactory
			.getLogger(TheEnergyDetectiveGenericBindingProvider.class);

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "theenergydetective";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
		if (!(item instanceof NumberItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only NumberItems are allowed - "
					+ "please check your *.items configuration");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item,
			String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);

		String[] configParts = bindingConfig.trim().split(":");
		if (configParts.length != 2) {
			throw new BindingConfigParseException(
					"TED binding must contain two parts separated by ':'");
		}
		String[] xmlPath = configParts[1].trim().split("\\.");
		if (xmlPath.length == 0) {
			throw new BindingConfigParseException(
					"TED binding for LiveData XML must contain at least one element"
							+ " with elements seperated by '.'. " + configParts[1]);
		}
		TheEnergyDetectiveBindingConfig config = new TheEnergyDetectiveBindingConfig(
				item, configParts[0], new ArrayList<String>(Arrays.asList(xmlPath)));

		logger.debug("item binding {} {}", item.getName(), bindingConfig);
		addBindingConfig(item, config);
	}

	@Override
	public String getGatewayForItem(String itemName) {
		TheEnergyDetectiveBindingConfig b = (TheEnergyDetectiveBindingConfig)bindingConfigs.get(itemName);
		if( b == null ){
			return null;
		}
		return b.getGateway();
	}
	
	@Override
	public ArrayList<String> getElementPathForItem(String itemName) {
		TheEnergyDetectiveBindingConfig b = (TheEnergyDetectiveBindingConfig)bindingConfigs.get(itemName);
		if( b == null ){
			return new ArrayList<String>();
		}
		return b.getElementsPath();
	}

	class TheEnergyDetectiveBindingConfig implements BindingConfig {
		// Item bound to XML data
		Item itemType;
		// XML Element path to LiveData value
		ArrayList<String> elementsPath;
		//Host or IP address of gateway
		String gateway;

		public TheEnergyDetectiveBindingConfig(Item itemType, String gw,
				ArrayList<String> elementsPath) {
			super();
			this.itemType = itemType;
			this.gateway = gw;
			this.elementsPath = elementsPath;
		}

		/**
		 * @return the itemType
		 */
		public Item getItemType() {
			return itemType;
		}

		/**
		 * @return the elementsPath
		 */
		public ArrayList<String> getElementsPath() {
			return elementsPath;
		}

		/**
		 * @return the gateway
		 */
		public String getGateway() {
			return gateway;
		}
	}
}
