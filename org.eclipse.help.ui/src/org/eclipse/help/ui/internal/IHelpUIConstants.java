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
package org.eclipse.help.ui.internal;
/**
 * Interface for holding UI constants
 */
public interface IHelpUIConstants {
	// Help UI pluging id with a "." for convenience.
	public static final String HELP_UI_PLUGIN_ID =
		WorkbenchHelpPlugin.PLUGIN_ID + ".";
	// F1 ids
	public static final String F1_SHELL = HELP_UI_PLUGIN_ID + "f1Shell";
	public static final String PREF_PAGE_BROWSERS =
		HELP_UI_PLUGIN_ID + "prefPageBrowsers";
	public static final String PREF_PAGE_APPSERVER =
		HELP_UI_PLUGIN_ID + "prefPageAppServer";
	public static final String PREF_PAGE_CUSTOM_BROWSER_PATH =
		HELP_UI_PLUGIN_ID + "prefPageCustomBrowserPath";
	public static final String SEARCH_PAGE = HELP_UI_PLUGIN_ID + "searchPage";
	public static final String HIT_MARKER_ID =
		HELP_UI_PLUGIN_ID + "helpsearchhit";
	public static final String HIT_MARKER_ATTR_HREF = "href";
	public static final String HIT_MARKER_ATTR_LABEL = "label";
	public static final String HIT_MARKER_ATTR_ORDER = "order";
	public static final String HIT_MARKER_ATTR_RESULTOF = "resultfof";
	public static final String RESULTS_PAGE_ID =
		HELP_UI_PLUGIN_ID + "searchPage";
	public static final String IMAGE_KEY_SEARCH = "search_icon";
	public static final String IMAGE_KEY_TOPIC = "s_topic_icon";
	public static final String IMAGE_KEY_F1TOPIC = "f1_topic_icon";
	public static final String IMAGE_FILE_SEARCH = "search_menu.gif";
	public static final String IMAGE_FILE_TOPIC = "topic.gif";
	public static final String IMAGE_FILE_F1TOPIC = "topic.gif";
}
