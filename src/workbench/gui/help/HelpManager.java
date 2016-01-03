/*
 * HelpManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.help;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.net.URI;

import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

import workbench.util.BrowserLauncher;
import workbench.util.ExceptionUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;

/**
 * A class to display the HTML and PDF manual from within the application.
 *
 * @author Thomas Kellerer
 */
public class HelpManager
{

  public static final String TOPIC_ALTERNATE_DELIMITER = "understand-alternate-delim";

	public static WbFile getDefaultPdf()
	{
		String pdfManual = Settings.getInstance().getProperty("workbench.manual.pdf.file", "SQLWorkbench-Manual.pdf");

		WbFile f = new WbFile(pdfManual);
		if (f.isDirectory())
		{
			f = new WbFile(f, "SQLWorkbench-Manual.pdf");
		}

		if (f.exists())
		{
			return f;
		}

		String jarDir = WbManager.getInstance().getJarPath();
		WbFile pdf = new WbFile(jarDir, pdfManual);

		return pdf;
	}

	public static WbFile getPDFManualPath()
	{
		WbFile pdf = getDefaultPdf();

		if (pdf.exists() && pdf.canRead())
		{
			return pdf;
		}

		if (!pdf.exists())
		{
			pdf = new WbFile(Settings.getInstance().getConfigDir(), pdf.getFileName());
		}

		if (pdf.exists() && pdf.canRead())
		{
			return pdf;
		}
		else
		{
			return null;
		}
	}

	/**
	 * Returns the directory where the HTML manual is located.
	 *
	 * @return the directory where the HTML manual is located or null if it cannot be found
	 */
	public static File getHtmlManualDir()
	{
		// Allow overriding the default location of the HTML manual
		String dir = Settings.getInstance().getProperty("workbench.manual.html.dir", null);
		File htmldir = null;

		if (dir == null)
		{
			// First look in the directory of the jar file.
			File jardir = WbManager.getInstance().getJarFile().getParentFile();
			htmldir = new File(jardir, "manual");
		}
		else
		{
			htmldir = new File(dir);
		}

		if (!htmldir.exists())
		{
			htmldir = new File(Settings.getInstance().getConfigDir(), "manual");
		}

		if (htmldir.exists())
		{
			return htmldir;
		}
		else
		{
			LogMgr.logWarning("HelpManager.getHtmlManualDir()", "Help directory '" + htmldir.getAbsolutePath() + "' not found!");
		}

		return null;
	}

	public static void showPdfHelp()
	{
		final WbFile pdf = getPDFManualPath();
		if (pdf == null)
		{
			String defaultPdf = getDefaultPdf().getFullPath();
			String msg = ResourceMgr.getFormattedString("ErrManualNotFound", defaultPdf, WbManager.getInstance().getJarPath());
			WbSwingUtilities.showMessage(WbManager.getInstance().getCurrentWindow(), msg);
			return;
		}

		LogMgr.logDebug("HelpManager.showPdfHelp()", "Using PDF: " + pdf.getFullPath());

		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN))
		{
			WbThread t = new WbThread("OpenPDF")
			{
				@Override
				public void run()
				{
					try
					{
						Desktop.getDesktop().open(pdf);
					}
					catch (Exception ex)
					{
						LogMgr.logError("HelpManager.showPdf()", "Error when running PDF Viewer", ex);
						WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(ex));
					}
				}
			};
			t.start();
		}
		else
		{
			LogMgr.logError("HelpManager.showPdfHelp()", "Desktop not supported!", null);
			WbSwingUtilities.showErrorMessage("Desktop not supported by your Java version");
		}
	}

	public static void showHistory()
	{
		File pdf = getDefaultPdf();

		WbFile history = new WbFile(pdf.getParent(), "history.html");
		if (!history.exists())
		{
			String msg = ResourceMgr.getFormattedString("ErrHelpFileNotFound", "history.html", pdf.getParent());
			WbSwingUtilities.showErrorMessage(WbManager.getInstance().getCurrentWindow(), msg);
			return;
		}
		try
		{
			BrowserLauncher.openURL(history.toURI());
		}
		catch (Exception ex)
		{
			LogMgr.logError("ShowHelpAction.executeAction", "Error displaying manual", ex);
		}
	}

	public static void showHelpFile(String topic)
	{
		showHelpFile(topic, null);
	}

	public static void showHelpFile(String topic, String anchor)
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

		File dir = getHtmlManualDir();
		if (dir == null)
		{
			showOnlineHelp(basefile, anchor);
			return;
		}

		File manual = null;
		if (dir != null)
		{
			manual = new File(dir, basefile);
		}

		if (manual == null || !manual.exists())
		{
			LogMgr.logInfo("HelpManager.showHelpFile()", "Help file: '" + manual + "' not found. Showing online help");
			showOnlineHelp(basefile, anchor);
			return;
		}

		try
		{
			String url = manual.toURI().toURL().toString();
			if (Settings.getInstance().useSinglePageHelp() && topic != null)
			{
				url = url + "#" + topic;
			}
			else if (anchor != null)
			{
				url += "#" + anchor;
			}
			BrowserLauncher.openURL(new URI(url));
		}
		catch (Exception ex)
		{
			LogMgr.logError("ShowHelpAction.executeAction", "Error displaying manual", ex);
		}
	}

	private static void showOnlineHelp(String basefile, String anchor)
	{
		try
		{
			String page = (basefile == null ? "" : basefile);
			if (anchor != null)
			{
				page += "#" + anchor;
			}
			String baseUrl = "http://www.sql-workbench.net/";
			if (WbManager.getInstance().isDevBuild())
			{
				baseUrl += "dev";
			}
			baseUrl += "manual/";
			BrowserLauncher.openURL(baseUrl + page);
		}
		catch (Exception ex)
		{
			LogMgr.logError("HelpManager.showHelpFile", "Could not open online help", ex);
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

	public static void showDriverHelp()
	{
		showHelpFile("jdbc-setup");
	}

	public static void showDateFormatHelp()
	{
		showHelpFile("options", "options-data-formatting");
	}

}
