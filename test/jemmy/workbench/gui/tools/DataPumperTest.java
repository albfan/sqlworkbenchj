/*
 * DataPumperTest.java
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
package workbench.gui.tools;

import java.sql.ResultSet;
import java.sql.Statement;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JTreeOperator;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.gui.GuiTestUtil;
import workbench.gui.NamedComponentChooser;
import workbench.util.SqlUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DataPumperTest
{
	private GuiTestUtil util;
	private WbFile targetDb;
	private WbFile sourceDb;

	public DataPumperTest()
	{
		util = new GuiTestUtil("DataPumperTest");
		targetDb  = new WbFile(util.getBaseDir(), "targetdb");
		sourceDb  = new WbFile(util.getBaseDir(), "sourcedb");
		try
		{
			util.createProfiles(sourceDb, targetDb);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void startDataPumper()
		throws Exception
	{
		String[] args = { "-datapumper -nosettings -configdir=" + util.getBaseDir() };
		new ClassReference("workbench.WbManager").startApplication(args);
		System.setProperty("workbench.system.doexit", "false");

//		JemmyProperties.getCurrentTimeouts().loadDebugTimeouts();
		TestOut out = JemmyProperties.getProperties().getOutput().createErrorOutput();
		JemmyProperties.getProperties().setOutput(out);
	}

	public void connect(String buttonName, int profileIndex)
	{
		JFrameOperator mainWindow = new JFrameOperator("Data Pumper");

		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName(buttonName);
		JButtonOperator select = new JButtonOperator(mainWindow, chooser);
		select.push();

		JDialogOperator dialog = new JDialogOperator(mainWindow, "Select Connection Profile");

		chooser.setName("profileTree");

		JTreeOperator tree = new JTreeOperator(dialog, chooser);
		tree.selectRow(profileIndex);
		new JButtonOperator(dialog, "OK").push();

		DataPumper pumper = (DataPumper)mainWindow.getContentPane().getComponent(0);
		waitUntilConnected(pumper);
		QueueTool tool = new QueueTool();
		tool.waitEmpty();
	}

	public void copyData()
	{
		QueueTool tool = new QueueTool();
		JFrameOperator mainWindow = new JFrameOperator("Data Pumper");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sourceTable");

		final JComboBoxOperator srctbl = new JComboBoxOperator(mainWindow, chooser);

		tool.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				srctbl.setSelectedIndex(0);
			}
		});

		tool.waitEmpty();
		try
		{
			// wait a bit to make sure the target dropdown is populated
			Thread.sleep(250);
		}
		catch (Exception e)
		{
			// ignore
		}

		chooser.setName("targetTable");
		final JComboBoxOperator targettbl = new JComboBoxOperator(mainWindow, chooser);
		assertEquals(2, targettbl.getItemCount());

		tool.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				targettbl.setSelectedIndex(1);
			}
		});

		tool.waitEmpty();

		chooser.setName("modeSelector");
		final JComboBoxOperator mode = new JComboBoxOperator(mainWindow, chooser);

		chooser.setName("deleteTargetCbx");
		final JCheckBoxOperator deleteTarget = new JCheckBoxOperator(mainWindow, chooser);
		assertTrue(deleteTarget.isEnabled());

		tool.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				mode.setSelectedIndex(1); // Update mode
				assertFalse(deleteTarget.isEnabled());

				mode.setSelectedItem("update,insert");
				assertFalse(deleteTarget.isEnabled());

				mode.setSelectedItem("insert,update");
				assertFalse(deleteTarget.isEnabled());

				mode.setSelectedItem("insert");
				assertTrue(deleteTarget.isEnabled());
			}
		});

		DataPumper pumper = (DataPumper)mainWindow.getContentPane().getComponent(0);

		chooser.setName("startButton");
		JButtonOperator start = new JButtonOperator(mainWindow, chooser);
		start.push();
		tool.waitEmpty();

		// Wait until copy has finished
		int count = 0;
		int sleepTime = 50;
		while (pumper.isRunning())
		{
			Thread.yield();
			WbThread.sleepSilently(sleepTime);
			count ++;
			if (count * sleepTime > 5000)
			{
				System.out.println("*** Cancelling wait !!!!!");
				break;
			}
		}
		tool.waitEmpty();
	}

	public void checkData()
	{
		WbConnection target = ConnectionMgr.getInstance().findConnection("Dp-Target1");
		assertNotNull(target);

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = target.createStatement();
			rs = stmt.executeQuery("select count(*) from person");
			if (rs.next())
			{
				int numrows = rs.getInt(1);
				assertEquals(4, numrows);
			}
			else
			{
				fail("No data copied");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	public void waitUntilConnected(DataPumper pumper)
	{
		int count = 0;
		int sleepTime = 50;
		while (pumper.isConnecting)
		{
			Thread.yield();
			WbThread.sleepSilently(sleepTime);
			count ++;
			if (count * sleepTime > 5000) break;
		}
	}

	public void closeWindow()
	{
		JFrameOperator mainWindow = new JFrameOperator("Data Pumper");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("closeButton");
		JButtonOperator close = new JButtonOperator(mainWindow, chooser);
		close.push();
	}

	@Test
	public void testDataPumper()
		throws Exception
	{
		try
		{
			util.prepareSource(sourceDb);
			util.prepareTarget(targetDb);
			startDataPumper();
			connect("selectSource", 1);
			connect("selectTarget", 2);
			copyData();
			checkData();
			closeWindow();
			WbThread.sleep(500);
		}
		finally
		{
			util.stopApplication();
			util.emptyBaseDirectory();
		}
	}
}
