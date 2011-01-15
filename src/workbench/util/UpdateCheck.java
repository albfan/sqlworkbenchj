/*
 * UpdateCheck.java
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * @author Thomas Kellerer
 */
public class UpdateCheck
	implements ActionListener
{
	private WbVersionReader versionReader;
	public static final boolean DEBUG = Boolean.getBoolean("workbench.debug.versioncheck");
	
	public void startUpdateCheck()
	{
		if (DEBUG)
		{
			startRead();
			return;
		}

		int interval = Settings.getInstance().getUpdateCheckInterval();
		if (interval < 1) return;

		Date lastCheck = Settings.getInstance().getLastUpdateCheck();

		if (needCheck(interval, new java.util.Date(), lastCheck))
		{
			startRead();
		}
	}

	/**
	 * This is public so that the method is accessible for Unit-Testing
	 */
	public boolean needCheck(int interval, Date today, Date lastCheck)
	{
		if (interval < 1) return false;
		Calendar next = Calendar.getInstance();
		long nextCheck = Long.MIN_VALUE;
		if (lastCheck != null)
		{
			next.setTime(lastCheck);
			next.set(Calendar.HOUR_OF_DAY, 0);
			next.clear(Calendar.MINUTE);
			next.clear(Calendar.SECOND);
			next.clear(Calendar.MILLISECOND);
			next.add(Calendar.DAY_OF_MONTH, interval);
			nextCheck = next.getTimeInMillis();
		}

		Calendar now = Calendar.getInstance();
		now.setTime(today);
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
		this.versionReader = new WbVersionReader("automatic", this);
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
				event = new NotifierEvent("updates.png", ResourceMgr.getString("LblVersionNewStableAvailable"), this);
			}
			else if (update == UpdateVersion.devBuild)
			{
				LogMgr.logInfo("UpdateCheck.run()", "New dev build available");
				event = new NotifierEvent("updates.png", ResourceMgr.getString("LblVersionNewDevAvailable"), this);
			}
			else
			{
				LogMgr.logInfo("UpdateCheck.run()", "No updates found");
			}

			if (event != null)
			{
				EventNotifier.getInstance().displayNotification(event);
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
		}
		catch (Exception e)
		{
			LogMgr.logError("UpdateCheck.run()", "Could not check for updates", e);
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.versionReader)
		{
			showNotification();
			return;
		}
		try
		{
			EventNotifier.getInstance().removeNotification();
			BrowserLauncher.openURL("http://www.sql-workbench.net");
		}
		catch (Exception ex)
		{
			WbSwingUtilities.showMessage(null, "Could not open browser (" + ExceptionUtil.getDisplay(ex) + ")");
		}
	}
}
