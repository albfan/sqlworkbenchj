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

import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.JWindow;

/**
 * @author thomas
 */
public class RunningJobIndicator
{
	private JFrame clientWindow;
	private int runningJobs = 0;
	private String lastTitle = null;
	public RunningJobIndicator(JFrame client)
	{
		this.clientWindow = client;
	}
	
	public synchronized void jobStarted()
	{
		if (runningJobs == 0)
		{
			this.lastTitle = this.clientWindow.getTitle();
			this.clientWindow.setTitle("» " + lastTitle);
		}
		runningJobs ++;
	}
	
	public synchronized void jobEnded()
	{
		runningJobs --;
		if (runningJobs == 0)
		{
			this.clientWindow.setTitle(lastTitle);
		}
	}
	
}
