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
package org.eclipse.help.internal.browser;
import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.help.browser.*;
import org.eclipse.help.internal.*;
import org.eclipse.help.internal.util.*;

/**
 * Browser adapter for browsers supporting
 * -remote openURL command line option
 * i.e. Mozilla and Netscape.
 */
public class MozillaBrowserAdapter implements IBrowser {
	// delay that it takes mozilla to start responding
	// to remote command after mozilla has been called
	private static final int DELAY = 5000;
	private long browserFullyOpenedAt = 0;
	private BrowserThread lastBrowserThread = null;
	private int x, y;
	private int width, height;
	private boolean setLocationPending = false;
	private boolean setSizePending = false;
	private String executable;
	private String executableName;
	private Thread uiThread;
	/**
	 * Constructor
	 * @executable executable filename to launch
	 * @executableName name of the program to display when error occurs
	 */
	MozillaBrowserAdapter(String executable, String executableName) {
		this.uiThread = Thread.currentThread();
		this.executable = executable;
		this.executableName = executableName;
	}
	/*
	 * @see IBrowser#close()
	 */
	public void close() {
	}
	/*
	 * @see IBrowser#displayURL(String)
	 */
	public void displayURL(String url) {
		if (lastBrowserThread != null)
			lastBrowserThread.exitRequested = true;
		if (setLocationPending || setSizePending) {
			url = createPositioningURL(url);
		}
		lastBrowserThread = new BrowserThread(url);
		lastBrowserThread.start();
		setLocationPending = false;
		setSizePending = false;
	}
	/*
	 * @see IBrowser#isCloseSupported()
	 */
	public boolean isCloseSupported() {
		return false;
	}
	/*
	 * @see IBrowser#isSetLocationSupported()
	 */
	public boolean isSetLocationSupported() {
		return true;
	}
	/*
	 * @see IBrowser#isSetSizeSupported()
	 */
	public boolean isSetSizeSupported() {
		return true;
	}
	/*
	 * @see IBrowser#setLocation(int, int)
	 */
	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
		setLocationPending = true;
	}
	/*
	 * @see IBrowser#setSize(int, int)
	 */
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		setSizePending = true;
	}
	private synchronized String createPositioningURL(String url) {
		IPath pluginPath = HelpPlugin.getDefault().getStateLocation();
		File outFile =
			pluginPath
				.append("mozillaPositon")
				.append("position.html")
				.toFile();
		try {
			outFile.getParentFile().mkdirs();
			PrintWriter writer =
				new PrintWriter(
					new BufferedWriter(
						new OutputStreamWriter(
							new FileOutputStream(outFile),
							"UTF8")),
					false);
			writer.println(
				"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
			writer.println("<html><head>");
			writer.println(
				"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
			writer.print("<title></title><script language=\"JavaScript\">");
			if (setSizePending)
				writer.print("window.resizeTo(" + width + "," + height + ");");
			if (setLocationPending)
				writer.print("window.moveTo(" + x + "," + y + ");");
			writer.print("location.replace(\"" + url + "\");");
			writer.print("</script></head><body>");
			writer.print("<a href=\"" + url + "\">--&gt;</a>");
			writer.print("</body></html>");
			writer.close();
			return "file://" + outFile.getAbsolutePath();
		} catch (IOException ioe) {
			// return the original url
			return url;
		}
	}
	private class BrowserThread extends Thread {
		public boolean exitRequested = false;
		private String url;
		public BrowserThread(String urlName) {
			this.url = urlName;
		}
		private int openBrowser(String browserCmd) {
			try {
				Process pr = Runtime.getRuntime().exec(browserCmd);
				(new StreamConsumer(pr.getInputStream())).start();
				(new StreamConsumer(pr.getErrorStream())).start();
				pr.waitFor();
				return pr.exitValue();
			} catch (InterruptedException e) {
			} catch (IOException e) {
				String msg =
					Resources.getString(
						"MozillaBrowserAdapter.executeFailed",
						executableName);
				HelpPlugin.logError(msg, e);
				HelpSystem.getDefaultErrorUtil().displayError(msg, uiThread);
				// return success so second command does not execute
				return 0;
			}
			return -1;
		}
		public void run() {
			// If browser is opening, wait until it fully opens,
			waitForBrowser();
			if (exitRequested)
				return;
			//workaround for bug 23750, mozilla bug 159092
			// instead of simply -remote openURL(" + url + ")", call a javascript
			if (openBrowser(executable
				+ " -remote openURL(javascript:window.focus();window.location=\'"
				+ url
				+ "\')")
				== 0) {
				return;
			}
			if (exitRequested)
				return;
			browserFullyOpenedAt = System.currentTimeMillis() + DELAY;
			openBrowser(executable + " " + url);
		}
		private void waitForBrowser() {
			while (System.currentTimeMillis() < browserFullyOpenedAt)
				try {
					if (exitRequested)
						return;
					Thread.sleep(100);
				} catch (InterruptedException ie) {
				}
		}
	}
}
