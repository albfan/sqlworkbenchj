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
//		System.out.println("********** jobStarted() ************** \n" + ExceptionUtil.getStackTrace(new Exception()));
		runningJobs ++;
		updateTitle();
	}
	
	public synchronized void jobEnded()
	{
//		System.out.println("********** jobEnded() ************** \n" + ExceptionUtil.getStackTrace(new Exception()));
		if (runningJobs > 0) runningJobs --;
		updateTitle();
	}

}
