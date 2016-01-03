/*
 * GuiTestUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
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
		startApplication(false);
	}

	public void startApplication(boolean useDebugTimeouts)
		throws Exception
	{
		System.setProperty("workbench.system.doexit", "false");
		System.setProperty("workbench.warn.java5", "100");

		new ClassReference("workbench.WbManager").startApplication(getArgs(false));

		if (useDebugTimeouts)
		{
	    JemmyProperties.getCurrentTimeouts().load(getClass().getClassLoader().getResourceAsStream("org/netbeans/jemmy/debug.timeouts"));
		}
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
			result.append(name);
			result.append('\n');
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
			WbThread.sleepSilently(sleepTime);
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
			WbThread.sleepSilently(sleepTime);
			count ++;
			if (count * sleepTime > 5000) break;
		}
	}

	public void stopApplication()
	{
		WbManager.getInstance().exitWorkbench(null, false);
	}
}
