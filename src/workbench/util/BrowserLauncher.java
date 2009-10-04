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

	public static void openURL(final String url)
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
						URI target = new URI(url);
						Desktop.getDesktop().browse(target);
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
