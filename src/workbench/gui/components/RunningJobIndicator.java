/*
 * RunningJobIndicator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JFrame;

/**
 * @author thomas
 */
public class RunningJobIndicator
{
	private JFrame clientWindow;
	private int runningJobs = 0;
	private String lastTitle = null;
	private final String prefix = "» ";
	
	public RunningJobIndicator(JFrame client)
	{
		this.clientWindow = client;
	}
	
	public synchronized void baseTitleChanged()
	{
		this.lastTitle = this.clientWindow.getTitle();
		if (lastTitle.startsWith(prefix))
		{
			lastTitle = lastTitle.substring(this.prefix.length());
		}
		updateTitle();
	}
	
	private synchronized void updateTitle()
	{
		if (runningJobs > 0)
		{
			
			String title = this.clientWindow.getTitle();
			if (!title.startsWith(prefix))
			{
				clientWindow.setTitle(prefix + lastTitle);
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
