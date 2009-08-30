package workbench.util;

import java.awt.Desktop;
import java.net.URI;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;

public class BrowserLauncher
{

	public static void openURL(String url)
		throws Exception
	{
		if (Desktop.isDesktopSupported())
		{
			Desktop desktop = Desktop.getDesktop();

			if (desktop.isSupported(Desktop.Action.BROWSE))
			{
				URI target = new URI(url);
				desktop.browse(target);
			}
			else
			{
				LogMgr.logError("BrowserLauncher.openURL()", "Action.BROWSE not supported!", null);
				WbSwingUtilities.showErrorMessage("Desktop not supported by your Java version");
			}
		}
		else
		{
			LogMgr.logError("BrowserLauncher.openURL()", "Desktop not supported!", null);
			WbSwingUtilities.showErrorMessage("Desktop not supported by your Java version");
		}
	}
}
