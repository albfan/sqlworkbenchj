/*
 * RunningJobIndicator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.components;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;

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

	private final Object iconMonitor = new Object();
	private final Object counterMonitor = new Object();

	public RunningJobIndicator(JFrame client)
	{
		this.clientWindow = client;
	}

	public void baseTitleChanged()
	{
		updateTitle();
	}

	private void updateTitle()
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

	public void jobStarted()
	{
		synchronized (counterMonitor)
		{
			if (runningJobs == 0)
			{
				startTime = System.currentTimeMillis();
				removeTrayIcon();
			}
			runningJobs ++;
			updateTitle();
		}
	}

	public void allJobsEnded()
	{
		synchronized (counterMonitor)
		{
			runningJobs = 0;
			updateTitle();
		}
	}
	public void jobEnded()
	{
		long duration = 0;
		boolean showAlert = false;

		synchronized (counterMonitor)
		{
			boolean wasDecreased = (runningJobs > 0);
			if (runningJobs > 0)
			{
				runningJobs--;
			}
			updateTitle();
			duration = System.currentTimeMillis() - startTime;
			if (runningJobs > 0)
			{
				showAlert = false;
			}
			else if (runningJobs == 0 && wasDecreased)
			{
				showAlert = GuiSettings.showScriptFinishedAlert();
			}
		}

		if (!showAlert) return;

		long minDuration = GuiSettings.getScriptFinishedAlertDuration();
		if (minDuration == 0 || duration > minDuration)
		{
			String msg = ResourceMgr.getString("TxtScriptFinished");
			boolean useTray = GuiSettings.useSystemTrayForAlert() && SystemTray.isSupported();

			if (useTray)
			{
				showTrayIcon(msg);
			}
			else if (clientWindow != null)
			{
				WbSwingUtilities.invoke(new Runnable()
				{
					@Override
					public void run()
					{
						showClient();
					}
				});
				WbSwingUtilities.showMessage(clientWindow, msg);
			}
		}
	}

	private void showTrayIcon(String msg)
	{
		synchronized (iconMonitor)
		{
			SystemTray tray = SystemTray.getSystemTray();
			try
			{
				if (trayIcon == null)
				{
					trayIcon = new TrayIcon(ResourceMgr.getPngIcon("workbench").getImage());
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

	private void removeTrayIcon()
	{
		synchronized (iconMonitor)
		{
			if (SystemTray.isSupported())
			{
				SystemTray tray = SystemTray.getSystemTray();
				tray.remove(trayIcon);
				trayIcon = null;
			}
		}
	}

	private void showClient()
	{
		if (clientWindow == null) return;

		if (clientWindow.getState() == Frame.ICONIFIED)
		{
			clientWindow.setState(Frame.NORMAL);
		}
		clientWindow.setVisible(true);
		clientWindow.requestFocus();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		removeTrayIcon();
		if (WbAction.isCtrlPressed(e) && clientWindow != null)
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					showClient();
				}
			});
		}
	}


}
