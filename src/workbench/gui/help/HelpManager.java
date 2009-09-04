/*
 * HelpManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.help;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.BrowserLauncher;
import workbench.util.ExceptionUtil;
import workbench.util.WbFile;

/**
 * A class to display the HTML and PDF manual from within the application.
 * 
 * @author support@sql-workbench.net
 */
public class HelpManager
{

	public static void showPdfHelp()
	{
		try
		{
			WbFile pdf = Settings.getInstance().getPDFManualPath();
			if (pdf == null)
			{
				String defaultPdf = Settings.getInstance().getDefaultPdf().getFullPath();
				String msg = ResourceMgr.getFormattedString("ErrManualNotFound", defaultPdf, WbManager.getInstance().getJarPath());
				WbSwingUtilities.showMessage(WbManager.getInstance().getCurrentWindow(), msg);
				return;
			}

			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN))
			{
				Desktop.getDesktop().open(pdf);
			}
			else
			{
				LogMgr.logError("HelpManager.showPdfHelp()", "Desktop not supported!", null);
				WbSwingUtilities.showErrorMessage("Desktop not supported by your Java version");
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("HelpManager.showPdf()", "Error when running PDF Viewer", ex);
			WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(ex));
		}
	}

	public static void showHistory()
	{
		File pdf = Settings.getInstance().getDefaultPdf();

		WbFile history = new WbFile(pdf.getParent(), "history.html");
		if (!history.exists())
		{
			String msg = ResourceMgr.getFormattedString("ErrHelpFileNotFound", "history.html", pdf.getParent());
			WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
			return;
		}
		try
		{
			BrowserLauncher.openURL(history.toURL().toString());
		}
		catch (Exception ex)
		{
			LogMgr.logError("ShowHelpAction.executeAction", "Error displaying manual", ex);
		}
	}

	public static void showHelpFile(String topic)
	{
		String basefile;

		if (Settings.getInstance().useSinglePageHelp())
		{
			basefile = "workbench-manual-single.html";
		}
		else if (topic != null)
		{
			basefile = topic + ".html";
		}
		else
		{
			basefile = "workbench-manual.html";
		}

		File dir = Settings.getInstance().getHtmlManualDir();
		if (dir == null)
		{
			File jardir = WbManager.getInstance().getJarFile().getParentFile();
			WbFile htmldir = new WbFile(jardir, "manual");
			String msg = ResourceMgr.getFormattedString("ErrHelpDirNotFound", htmldir.getFullPath());
			WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
			return;
		}

		File manual = null;
		if (dir != null)
		{
			manual = new File(dir, basefile);
		}

		if (manual == null || !manual.exists())
		{
			String msg = ResourceMgr.getFormattedString("ErrHelpFileNotFound", basefile, dir);
			WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
			return;
		}

		try
		{
			String url = manual.toURL().toString();
			if (Settings.getInstance().useSinglePageHelp() && topic != null)
			{
				url = url + "#" + topic;
			}
			LogMgr.logDebug("HelpManager.showHelpFile()", "Using URL: " + url);
			BrowserLauncher.openURL(url);
		}
		catch (Exception ex)
		{
			LogMgr.logError("ShowHelpAction.executeAction", "Error displaying manual", ex);
		}
	}

	public static void showHelpIndex()
	{
		showHelpFile("workbench-manual");
	}

	public static void showDataPumperHelp()
	{
		showHelpFile("data-pumper");
	}

	public static void showOptionsHelp()
	{
		showHelpFile("options");
	}

	public static void showProfileHelp()
	{
		showHelpFile("profiles");
	}
}
