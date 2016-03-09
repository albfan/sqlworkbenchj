/*
 * UpdateCheck.java
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
package workbench.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

/**
 * @author Thomas Kellerer
 */
public class UpdateCheck
  implements ActionListener
{
  private static final String TYPE_WB_VERSION = "version_check";
  private static final String TYPE_JAVA_VERSION = "java_check";
  public static final boolean DEBUG = Boolean.getBoolean("workbench.debug.versioncheck");

  private WbVersionReader versionReader;

  public void startUpdateCheck()
  {
    if (DEBUG)
    {
      startRead();
      return;
    }

    int interval = Settings.getInstance().getUpdateCheckInterval();
    Date lastCheck = Settings.getInstance().getLastUpdateCheck();

    if (needCheck(interval, new java.util.Date(), lastCheck))
    {
      startRead();
    }
    else
    {
      checkJavaVersion();
    }
  }

  private void checkJavaVersion()
  {
    if (!Settings.getInstance().checkJavaVersion()) return;
    if (ResourceMgr.getBuildNumber().getMajorVersion() == 999) return; // don't check if started from IDE

    VersionNumber minVersion = new VersionNumber(1,8);
    VersionNumber currentVersion = VersionNumber.getJavaVersion();
    if (!currentVersion.isNewerOrEqual(minVersion))
    {
      NotifierEvent event = new NotifierEvent("alert", ResourceMgr.getString("MsgOldJava"), this);
      event.setTooltip(ResourceMgr.getString("MsgOldJavaDetail"));
      event.setType(TYPE_JAVA_VERSION);
      EventNotifier.getInstance().displayNotification(event);
    }
  }

  /**
   * This is public so that the method is accessible for Unit-Testing
   */
  boolean needCheck(int interval, Date today, Date lastCheck)
  {
    if (interval < 1) return false;

    Calendar next = Calendar.getInstance();
    long nextCheck = Long.MIN_VALUE;
    if (lastCheck != null)
    {
      next.setLenient(true);
      next.setTime(lastCheck);
      next.set(Calendar.HOUR_OF_DAY, 0);
      next.clear(Calendar.MINUTE);
      next.clear(Calendar.SECOND);
      next.clear(Calendar.MILLISECOND);
      next.add(Calendar.DAY_OF_MONTH, interval);  // this rolls over correctly to the next month because of setLenient(true)
      nextCheck = next.getTimeInMillis();
    }

    Calendar now = Calendar.getInstance();
    now.setTimeInMillis(today.getTime());
    now.set(Calendar.HOUR_OF_DAY, 0);
    now.clear(Calendar.MINUTE);
    now.clear(Calendar.SECOND);
    now.clear(Calendar.MILLISECOND);

    long nowMillis = now.getTimeInMillis();

    return nextCheck <= nowMillis;
  }

  public void startRead()
  {
    LogMgr.logDebug("UpdateCheck.run()", "Checking versions...");
    versionReader = new WbVersionReader("automatic", this);
    versionReader.startCheckThread();
  }

  private void showNotification()
  {
    try
    {
      LogMgr.logDebug("UpdateCheck.run()", "Current stable version: " + versionReader.getStableBuildNumber());
      LogMgr.logDebug("UpdateCheck.run()", "Current development version: " + versionReader.getDevBuildNumber());

      UpdateVersion update = this.versionReader.getAvailableUpdate();
      NotifierEvent event = null;
      if (DEBUG || update == UpdateVersion.stable)
      {
        LogMgr.logInfo("UpdateCheck.run()", "New stable version available");
        event = new NotifierEvent("updates", ResourceMgr.getString("LblVersionNewStableAvailable"), this);
      }
      else if (update == UpdateVersion.devBuild)
      {
        LogMgr.logInfo("UpdateCheck.run()", "New dev build available");
        event = new NotifierEvent("updates", ResourceMgr.getString("LblVersionNewDevAvailable"), this);
      }
      else
      {
        LogMgr.logInfo("UpdateCheck.run()", "No updates found");
      }

      if (this.versionReader.success())
      {
        try
        {
          Settings.getInstance().setLastUpdateCheck();
        }
        catch (Exception e)
        {
          LogMgr.logError("UpdateCheck.run()", "Error when updating last update date", e);
        }
      }

      if (event == null)
      {
        // no new version so no event to display, we can start the check for the Java version
        checkJavaVersion();
      }
      else
      {
        event.setType(TYPE_WB_VERSION);
        EventNotifier.getInstance().displayNotification(event);
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("UpdateCheck.run()", "Could not check for updates", e);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == this.versionReader)
    {
      showNotification();
      return;
    }

    String command = e.getActionCommand();

    try
    {
      EventNotifier.getInstance().removeNotification();
      if (TYPE_WB_VERSION.equals(command))
      {
        BrowserLauncher.openURL("http://www.sql-workbench.net");
      }
      else
      {
        BrowserLauncher.openURL("http://www.java.com");
      }
    }
    catch (Exception ex)
    {
      WbSwingUtilities.showMessage(null, "Could not open browser (" + ExceptionUtil.getDisplay(ex) + ")");
    }
  }
}
