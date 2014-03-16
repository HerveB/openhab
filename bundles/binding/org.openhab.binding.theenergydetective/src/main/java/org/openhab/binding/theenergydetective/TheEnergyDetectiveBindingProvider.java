/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.theenergydetective;

import java.util.ArrayList;

import org.openhab.core.binding.BindingProvider;

/**
 * This interface is implemented by classes that can provide mapping information
 * between openHAB items and The Energy Detective items.
 * 
 * @author rgordon
 * @since 1.5.0
 */
public interface TheEnergyDetectiveBindingProvider extends BindingProvider {

	String getGatewayForItem(String itemName);

	ArrayList<String> getElementPathForItem(String itemName);
}
