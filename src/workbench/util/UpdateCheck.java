/*
 * UpdateCheck.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class UpdateCheck
	implements Runnable, ActionListener
{
	
	public UpdateCheck()
	{
	}
	
	public void startUpdateCheck()
	{
		int interval = Settings.getInstance().getUpdateCheckInterval();
		Date lastCheck = Settings.getInstance().getLastUpdateCheck();
		
		if (needCheck(interval, new java.util.Date(), lastCheck))
		{
			WbThread upd = new WbThread(this, "UpdateCheck");
			upd.start();
		}
		else
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			LogMgr.logInfo("UpdateCheck.startUpdateCheck()", "Check not necessary. Last check on: " + sdf.format(lastCheck) + ", interval="+ interval + " days");
		}
		
	}
	
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
	
	public void run()
	{
		try
		{
			LogMgr.logDebug("UpdateCheck.run()", "Checking versions...");
			WbVersionReader reader = new WbVersionReader("automatic ");
			if (reader.success())
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
			
			LogMgr.logDebug("UpdateCheck.run()", "Current stable version: " + reader.getStableBuildNumber());
			LogMgr.logDebug("UpdateCheck.run()", "Current dev version: " + reader.getDevBuildNumber());
			
			UpdateVersion update = reader.getAvailableUpdate();
			NotifierEvent event = null;
			if (update == UpdateVersion.stable)
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
			
			if (event != null)
			{
				EventNotifier.getInstance().displayNotification(event);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("UpdateCheck.run()", "Could not check for updates", e);
		}
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		try
		{
			EventNotifier.getInstance().removeNotification();
			BrowserLauncher.openURL("http://www.sql-workbench.net");
		}
		catch (Exception ex)
		{
			WbSwingUtilities.showMessage(null, "Could not open browser (" + ExceptionUtil.getDisplay(ex)+ ")");
		}
	}
	
}
