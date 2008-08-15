/*
 * GuiTestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui;

import java.awt.Component;
import java.awt.Container;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import workbench.TestUtil;
import workbench.WbManager;
import workbench.gui.sql.SqlPanel;
import workbench.util.StringUtil;

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

//    JemmyProperties.getCurrentTimeouts().load(getClass().getClassLoader().getResourceAsStream("org/netbeans/jemmy/debug.timeouts"));
		TestOut out = JemmyProperties.getProperties().getOutput().createErrorOutput();
		JemmyProperties.getProperties().setOutput(out);
	}
	
	public void execute(Runnable r)
	{
		tool.invokeAndWait(r);
		tool.waitEmpty();
	}

	private static void printAllComponents(Container root, StringBuffer result, String indent)
	{
		Component[] all = root.getComponents();
		if (all == null) return;

		for (Component c : all)
		{
			result.append(indent);
			String name = c.getName();
			if (StringUtil.isEmptyString(name))
			{
				name = c.getClass().getName();
			}
			result.append(c.getName());
			result.append("\n");
			if (c instanceof Container)
			{
				printAllComponents((Container)c, result, indent + "  ");
			}
		}
	}

	public static void printAllComponents(Container c)
	{
		StringBuffer result = new StringBuffer(500);
		printAllComponents(c, result, "");
		System.out.println("********* \n" + result.toString() + "\n*****************");
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
