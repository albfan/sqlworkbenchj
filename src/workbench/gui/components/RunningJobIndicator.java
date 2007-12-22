/*
 * RunningJobIndicator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JFrame;

/**
 * @author support@sql-workbench.net
 */
public class RunningJobIndicator
{
	private JFrame clientWindow;
	private int runningJobs = 0;
	private String lastTitle = null;
	public static final String TITLE_PREFIX = "\u00bb ";
	
	public RunningJobIndicator(JFrame client)
	{
		this.clientWindow = client;
	}
	
	public synchronized void baseTitleChanged()
	{
		this.lastTitle = this.clientWindow.getTitle();
		if (lastTitle.startsWith(TITLE_PREFIX))
		{
			lastTitle = lastTitle.substring(TITLE_PREFIX.length());
		}
		updateTitle();
	}
	
	private synchronized void updateTitle()
	{
		if (runningJobs > 0)
		{
			
			String title = this.clientWindow.getTitle();
			if (!title.startsWith(TITLE_PREFIX))
			{
				clientWindow.setTitle(TITLE_PREFIX + lastTitle);
			}
		}
	}
	
	public synchronized void jobStarted()
	{
		runningJobs ++;
		if (runningJobs > 0)
		{
			this.lastTitle = this.clientWindow.getTitle();
		}
		updateTitle();
	}
	
	public synchronized void jobEnded()
	{
		runningJobs --;
		if (runningJobs == 0)
		{
			clientWindow.setTitle(lastTitle);
		}
	}

}
