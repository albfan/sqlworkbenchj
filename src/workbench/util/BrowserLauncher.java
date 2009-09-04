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
	public static void openEmail(String email)
	{
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
		{
			try
			{
				URI uri = new URI("mailto:" + email);
				Desktop.getDesktop().mail(uri);
			}
			catch (Exception e)
			{
				LogMgr.logError("BrowserLauncher.openEmail()", "Could not open email program", null);
			}
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
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
		{
			URI target = new URI(url);
			Desktop.getDesktop().browse(target);
		}
		else
		{
			LogMgr.logError("BrowserLauncher.openURL()", "Desktop not supported!", null);
			WbSwingUtilities.showErrorMessage("Desktop not supported by your Java version");
		}
	}
}
