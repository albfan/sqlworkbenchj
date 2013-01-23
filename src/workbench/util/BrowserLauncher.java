/*
 * BrowserLauncher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.awt.Desktop;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 * A Wrapper around Desktop.browse()
 *
 * @author Thomas Kellerer
 */
public class BrowserLauncher
{
	public static void openEmail(final String email)
	{
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
		{
			WbThread t = new WbThread("OpenBrowser")
			{
				@Override
				public void run()
				{
					try
					{
						URI uri = new URI("mailto:" + email + "?subject=" + urlEncode("SQL Workbench/J " + ResourceMgr.getBuildInfo()+ " - feedback"));
						Desktop.getDesktop().mail(uri);
					}
					catch (Exception e)
					{
						LogMgr.logError("BrowserLauncher.openEmail()", "Could not open email program", e);
						WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
					}
				}
			};
			t.start();
		}
		else
		{
			LogMgr.logError("BrowserLauncher.openEmail()", "Desktop not supported!", null);
			WbSwingUtilities.showErrorMessage("Desktop not supported by your Java version");
		}
	}

	private static String urlEncode(String str)
	{
		try
		{
			return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void openURL(String url)
		throws Exception
	{
		openURL(new URI(url));
	}

	public static void openURL(final URI url)
		throws Exception
	{
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
		{
			WbThread t = new WbThread("OpenBrowser")
			{
				@Override
				public void run()
				{
					try
					{
						LogMgr.logDebug("BrowserLauncher.openURL", "Opening URL: " + url.toString());
						Desktop.getDesktop().browse(url);
					}
					catch (Exception e)
					{
						LogMgr.logError("BrowserLauncher.openURL()", "Error starting browser", e);
						WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
					}
				}
			};
			t.start();
		}
		else
		{
			LogMgr.logError("BrowserLauncher.openURL()", "Desktop not supported!", null);
			WbSwingUtilities.showErrorMessage("Desktop not supported by your Java version");
		}
	}
}
