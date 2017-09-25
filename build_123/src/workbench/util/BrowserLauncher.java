/*
 * BrowserLauncher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.util;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;

import workbench.db.ConnectionInfoBuilder;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

/**
 * Some utility functions for the Desktop class
 *
 * @author Thomas Kellerer
 */
public class BrowserLauncher
{
  public static void openEmail(final String email, final WbConnection currentConnection)
  {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL))
    {
      WbThread t = new WbThread("OpenBrowser")
      {
        @Override
        public void run()
        {
          try
          {
            String subject = urlEncode("SQL Workbench/J (Build " + ResourceMgr.getBuildNumber()+ ") - feedback");
            String body = ResourceMgr.getFormattedString("TxtFeedbackMail", LogMgr.getLogfile().getFullPath());
            body += "\n\nSQL Workbench/J " + ResourceMgr.getBuildInfo();
            body += "\n" + ResourceMgr.getFullJavaInfo();
            long maxMem = MemoryWatcher.MAX_MEMORY / (1024*1024);
            body += "\n" + ResourceMgr.getOSInfo() + ", max. memory=" + maxMem + "MB";

            if (currentConnection != null)
            {
              ConnectionInfoBuilder builder = new ConnectionInfoBuilder();
              String info = builder.getPlainTextDisplay(currentConnection, 5);
              if (StringUtil.isNonEmpty(info))
              {
                String msg = ResourceMgr.getFormattedString("TxtFeedbackMailConInfo", info);
                body += "\n\n" + msg;
              }
            }
            body = urlEncode(body);
            URI uri = new URI("mailto:" + email + "?subject=" + subject + "&body=" + body);
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
    throws Exception
  {
    return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
  }

  public static void openURL(String url)
    throws Exception
  {
    openURL(new URI(url));
  }

  public static void openURL(final URI url)
    throws Exception
  {
    final URI realURI;

    String urlString = url.toString();

    if (urlString.indexOf('#') > -1 && GuiSettings.useHTMLRedirectForAnchor())
    {
      File tmpfile = File.createTempFile("sqlwb_show_help", ".html");
      tmpfile.deleteOnExit();

      String redirect =
        "<html><head>\n" +
        "<meta http-equiv=\"refresh\" content=\"0;url=" + urlString + "\"/>\n" +
        "</head></html>";

      FileUtil.writeString(tmpfile, redirect, "UTF-8", false);
      realURI = tmpfile.toURI();
      LogMgr.logDebug("BrowserLauncher.openURL", "Redirecting to an anchor using intermediate URL: " + realURI.toString());
    }
    else
    {
      realURI = url;
    }

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
            Desktop.getDesktop().browse(realURI);
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
      WbSwingUtilities.showErrorMessage("Starting the browser is not supported by your Java installation");
    }
  }
}
