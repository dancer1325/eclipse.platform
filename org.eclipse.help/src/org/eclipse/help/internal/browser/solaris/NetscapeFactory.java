/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.browser.solaris;

import org.eclipse.help.browser.*;

public class NetscapeFactory implements IBrowserFactory {
	private NetscapeBrowserAdapter browserInstance = null;
	/**
	 * Constructor.
	 */
	public NetscapeFactory() {
		super();
	}
	/*
	 * @see IBrowserFactory#isAvailable()
	 */
	public boolean isAvailable() {
		return System.getProperty("os.name").toLowerCase().startsWith(
			"SunOS".toLowerCase());
	}
	/*
	 * @see IBrowserFactory#createBrowser()
	 */
	public IBrowser createBrowser() {
		// Create single browser for all clients
		if (browserInstance == null) {
			browserInstance = new NetscapeBrowserAdapter();
		}
		return browserInstance;
	}
}
