/*
 * HelpManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.help;

import java.io.File;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.BrowserLauncher;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class HelpManager
{
	public static void showPdfHelp()
	{
		try
		{
			String pdf = Settings.getInstance().getPdfPath();
			if (pdf == null)
			{
				String defaultPdf = Settings.getInstance().getDefaultPdf().getFullPath();
				String msg = ResourceMgr.getFormattedString("ErrManualNotFound", defaultPdf, WbManager.getInstance().getJarPath());
				WbSwingUtilities.showMessage(WbManager.getInstance().getCurrentWindow(), msg);
				return;
			}
			
			String readerPath = Settings.getInstance().getPDFReaderPath();
			if (StringUtil.isEmptyString(readerPath))
			{
				String msg = ResourceMgr.getString("ErrNoReaderDefined");
				WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
				return;
			}
			
			File reader = new File(readerPath);
			if (!reader.exists() || !reader.canRead() || !reader.isFile())
			{
				String msg = ResourceMgr.getFormattedString("ErrExeNotAvail", readerPath);
				WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
				return;
			}
			
			String[] cmd = new String[] { readerPath, pdf };
			Runtime.getRuntime().exec(cmd);
		}
		catch (Exception ex)
		{
			LogMgr.logError("HelpManager.showPdf()", "Error when running PDF Viewer", ex);
		}
	}
	
	public static void showHelpFile(String filename)
	{
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
			manual = new File(dir, filename);
		}
		
		if (manual == null || !manual.exists())
		{
			String msg = ResourceMgr.getFormattedString("ErrHelpFileNotFound", filename, dir);
			WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
			return;
		}
		
		try
		{
			BrowserLauncher.openURL(manual.toURL().toString());
		}
		catch (Exception ex)
		{
			LogMgr.logError("ShowHelpAction.executeAction", "Error displaying manual", ex);
		}		
	}
	
	public static void showHelpIndex()
	{
		showHelpFile("workbench-manual.html");
	}
	
	public static void showDataPumperHelp()
	{
		showHelpFile("data-pumper.html");
	}
	
	public static void showOptionsHelp()
	{
		showHelpFile("options.html");
	}
	
	public static void showProfileHelp()
	{
		showHelpFile("profiles.html");
	}
	
}
