/*
 * GuiTestUtil.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import workbench.TestUtil;
import workbench.WbManager;
import workbench.gui.sql.SqlPanel;

/**
 *
 * @author support@sql-workbench.net
 */
public class GuiTestUtil
	extends TestUtil
{
	private QueueTool tool = new QueueTool();

	public GuiTestUtil(String name)
	{
		super(name, false);
	}

	public void startApplication()
		throws Exception
	{
		new ClassReference("workbench.WbManager").startApplication(getArgs(false));
		System.setProperty("workbench.system.doexit", "false");

//		JemmyProperties.getCurrentTimeouts().loadDebugTimeouts();
		TestOut out = JemmyProperties.getProperties().getOutput().createErrorOutput();
		JemmyProperties.getProperties().setOutput(out);
	}

	public void execute(Runnable r)
	{
		tool.invokeAndWait(r);
		tool.waitEmpty();
	}


	public void waitWhileBusy(SqlPanel panel)
	{
		int count = 0;
		int sleepTime = 10;
		while (panel.isBusy())
		{
			try { Thread.sleep(sleepTime); } catch (Throwable th) {}
			count ++;
			if (count * sleepTime > 30000) break;
		}
	}
	
	public void waitUntilConnected(SqlPanel panel)
	{
		int count = 0;
		int sleepTime = 10;
		while (!panel.isConnected())
		{
			//Thread.yield();
			try { Thread.sleep(sleepTime); } catch (Throwable th) {}
			count ++;
			if (count * sleepTime > 5000) break;
		}
	}
	
	public void stopApplication()
	{
		WbManager.getInstance().exitWorkbench(null);
	}
}
