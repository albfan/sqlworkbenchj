/*
 * BrowserLauncher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.awt.Desktop;
import java.net.URI;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;

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
				public void run()
				{
					try
					{
						URI uri = new URI("mailto:" + email);
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
