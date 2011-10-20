/*
 * RunningJobIndicator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class RunningJobIndicator
	implements ActionListener
{
	private JFrame clientWindow;
	private int runningJobs = 0;
	public static final String TITLE_PREFIX = "\u00bb ";
	private long startTime;
	private TrayIcon trayIcon;

	public RunningJobIndicator(JFrame client)
	{
		this.clientWindow = client;
	}

	public synchronized void baseTitleChanged()
	{
		updateTitle();
	}

	private synchronized void updateTitle()
	{
		String title = this.clientWindow.getTitle();
		if (runningJobs > 0)
		{
			if (!title.startsWith(TITLE_PREFIX))
			{
				clientWindow.setTitle(TITLE_PREFIX + title);
			}
		}
		else
		{
			if (title.startsWith(TITLE_PREFIX))
			{
				clientWindow.setTitle(title.substring(TITLE_PREFIX.length()));
			}
		}
	}

	public synchronized void jobStarted()
	{
		if (runningJobs == 0)
		{
			startTime = System.currentTimeMillis();
		}
		runningJobs ++;
		updateTitle();
	}

	public synchronized void jobEnded()
	{
		if (runningJobs > 0) runningJobs --;
		updateTitle();
		if (runningJobs > 0 || !GuiSettings.showScriptFinishedAlert()) return;

		long minDuration = GuiSettings.getScriptFinishedAlertDuration();
		long duration = System.currentTimeMillis() - startTime;
		if (minDuration == 0 || duration > minDuration)
		{
			String msg = ResourceMgr.getString("TxtScriptFinished");

			boolean useTray = GuiSettings.useSystemTrayForAlert() && SystemTray.isSupported();

			if (!useTray && clientWindow != null)
			{
				WbSwingUtilities.invoke(new Runnable()
				{
					@Override
					public void run()
					{
						if (clientWindow.getState() == Frame.ICONIFIED)
						{
							clientWindow.setState(Frame.NORMAL);
						}
						clientWindow.setVisible(true);
					}
				});
				WbSwingUtilities.showMessage(clientWindow, msg);
			}

			if (useTray)
			{
				SystemTray tray = SystemTray.getSystemTray();
				try
				{
					if (trayIcon == null)
					{
						trayIcon = new TrayIcon(ResourceMgr.getPng("workbench16").getImage());
						trayIcon.addActionListener(this);
						trayIcon.setToolTip(msg + " (" + StringUtil.getCurrentTimestamp() + ")");
						tray.add(trayIcon);
					}
					trayIcon.displayMessage(ResourceMgr.TXT_PRODUCT_NAME, msg, TrayIcon.MessageType.INFO);
				}
				catch (AWTException ex)
				{
					LogMgr.logWarning("RunningJobIndicator.jobEnded()", "Could not install tray icon", ex);
				}
			}
		}
	}

	public synchronized void allJobsEnded()
	{
		runningJobs = 0;
		updateTitle();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() != this.trayIcon) return;
		if (SystemTray.isSupported())
		{
			LogMgr.logWarning("RunningJobIndicator.actionPerformed()", "Removing system tray icon");
			SystemTray tray = SystemTray.getSystemTray();
			tray.remove(trayIcon);
			trayIcon = null;
		}
	}

}
