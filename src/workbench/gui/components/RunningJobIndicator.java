/*
 * RunningJobIndicator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JFrame;
import workbench.log.LogMgr;

/**
 * @author support@sql-workbench.net
 */
public class RunningJobIndicator
{
	private JFrame clientWindow;
	private int runningJobs = 0;
	public static final String TITLE_PREFIX = "\u00bb ";
	
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
		runningJobs ++;
		LogMgr.logDebug("RunningJobIndicator.jobStarted()", "New runcount = " + runningJobs);
		updateTitle();
	}
	
	public synchronized void jobEnded()
	{
		if (runningJobs > 0) runningJobs --;
		LogMgr.logDebug("RunningJobIndicator.jobEnded()", "New runcount = " + runningJobs);
		updateTitle();
	}

}
