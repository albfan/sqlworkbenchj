/*
 * RunningJobIndicator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Frame;
import javax.swing.JFrame;
import workbench.gui.WbSwingUtilities;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;

/**
 * @author Thomas Kellerer
 */
public class RunningJobIndicator
{
	private JFrame clientWindow;
	private int runningJobs = 0;
	public static final String TITLE_PREFIX = "\u00bb ";
	private long startTime;

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
		if (runningJobs == 0 && GuiSettings.showScriptFinishedAlert())
		{
			long minDuration = GuiSettings.getScriptFinishedAlertDuration();
			long duration = System.currentTimeMillis() - startTime;
			if (minDuration == 0 || duration > minDuration)
			{
				if (clientWindow != null)
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
				}
				WbSwingUtilities.showMessage(clientWindow, ResourceMgr.getString("TxtScriptFinished"));
			}
		}
	}

}
