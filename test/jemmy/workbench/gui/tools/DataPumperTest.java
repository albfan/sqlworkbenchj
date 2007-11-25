/*
 * DataPumperTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.tree.TreeModel;
import junit.framework.TestCase;
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
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class DataPumperTest
	extends TestCase
{
	private GuiTestUtil util;
	private WbFile targetDb;
	private WbFile sourceDb;
	public DataPumperTest(String testName)
	{
		super(testName);
		util = new GuiTestUtil(testName);
		targetDb  = new WbFile(util.getBaseDir(), "targetdb");
		sourceDb  = new WbFile(util.getBaseDir(), "sourcedb");
//		System.setProperty("h2.logAllErrors", "true");
//		System.setProperty("h2.logAllErrorsFile", "c:/temp/jemmy_h2.log");
		try
		{
			createProfiles();
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
		TreeModel model = tree.getModel();
		tree.selectRow(profileIndex);
		new JButtonOperator(dialog, "OK").push();

		DataPumper pumper = (DataPumper)mainWindow.getContentPane().getComponent(0);
		waitUntilConnected(pumper);
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
			public void run()
			{
				srctbl.setSelectedIndex(0);
			}
		});
		
		tool.waitEmpty();

		chooser.setName("targetTable");
		final JComboBoxOperator targettbl = new JComboBoxOperator(mainWindow, chooser);
		assertEquals(2, targettbl.getItemCount());
		
		tool.invokeAndWait(new Runnable()
		{
			public void run()
			{
				targettbl.setSelectedIndex(1);
			}
		});
		
		tool.waitEmpty();
		

		chooser.setName("modeSelector");
		JComboBoxOperator mode = new JComboBoxOperator(mainWindow, chooser);

		chooser.setName("deleteTargetCbx");
		JCheckBoxOperator deleteTarget = new JCheckBoxOperator(mainWindow, chooser);
		assertTrue(deleteTarget.isEnabled());

		mode.setSelectedIndex(1); // Update mode
		tool.waitEmpty();
		assertFalse(deleteTarget.isEnabled());

		mode.setSelectedItem("update,insert");
		tool.waitEmpty();
		assertFalse(deleteTarget.isEnabled());

		mode.setSelectedItem("insert,update");
		tool.waitEmpty();
		assertFalse(deleteTarget.isEnabled());

		mode.setSelectedItem("insert");
		tool.waitEmpty();
		assertTrue(deleteTarget.isEnabled());

		chooser.setName("startButton");
		JButtonOperator start = new JButtonOperator(mainWindow, chooser);
		start.pushNoBlock();

		DataPumper pumper = (DataPumper)mainWindow.getContentPane().getComponent(0);

		// Wait until copy has finished
		int count = 0;
		int sleepTime = 50;
		while (!pumper.isRunning())
		{
			//Thread.yield();
			try { Thread.sleep(sleepTime); } catch (Throwable th) {}
			count ++;
			if (count * sleepTime > 5000) break;
		}

		WbConnection target = ConnectionMgr.getInstance().findConnection("Dp-Target");
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
		while (!pumper.isConnecting)
		{
			//Thread.yield();
			try { Thread.sleep(sleepTime); } catch (Throwable th) {}
			count ++;
			if (count * sleepTime > 5000) break;
		}
	}

	private void prepareSource()
	{
		Connection con;
		Statement stmt;

		try
		{
			Class.forName("org.h2.Driver");
			con = DriverManager.getConnection("jdbc:h2:" + sourceDb.getFullPath(), "sa", "");
			stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE person (id integer primary key, firstname varchar(50), lastname varchar(50))");
			stmt.executeUpdate("insert into person (id, firstname, lastname) values (1, 'Arthur', 'Dent')");
			stmt.executeUpdate("insert into person (id, firstname, lastname) values (2, 'Mary', 'Moviestar')");
			stmt.executeUpdate("insert into person (id, firstname, lastname) values (3, 'Major', 'Bug')");
			stmt.executeUpdate("insert into person (id, firstname, lastname) values (4, 'General', 'Failure')");
			con.commit();
			stmt.close();
			con.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void prepareTarget()
	{
		Connection con;
		Statement stmt;

		try
		{
			con = DriverManager.getConnection("jdbc:h2:" + targetDb.getFullPath(), "sa", "");
			stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE person (id integer primary key, firstname varchar(50), lastname varchar(50))");
			con.commit();
			stmt.close();
			con.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void createProfiles()
		throws FileNotFoundException
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>  \n" +
             "<java version=\"1.5.0_08\" class=\"java.beans.XMLDecoder\">  \n" +
             "	 \n" +
             " <object class=\"java.util.ArrayList\">  \n" +
             "  <void method=\"add\">  \n" +
             "   <object class=\"workbench.db.ConnectionProfile\">  \n" +
             "    <void property=\"driverclass\">  \n" +
             "     <string>org.h2.Driver</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"name\">  \n" +
             "     <string>SourceConnection</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"url\">  \n" +
             "     <string>" + "jdbc:h2:" + StringUtil.replace(sourceDb.getFullPath(), "\\", "/") + "</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"username\">  \n" +
             "     <string>sa</string>  \n" +
             "    </void>  \n" +
             "   </object>  \n" +
             "  </void>  \n" +
             "	 \n" +
             "  <void method=\"add\">  \n" +
             "   <object class=\"workbench.db.ConnectionProfile\">  \n" +
             "    <void property=\"driverclass\">  \n" +
             "     <string>org.h2.Driver</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"name\">  \n" +
             "     <string>TargetConnection</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"url\">  \n" +
             "     <string>" + "jdbc:h2:" + StringUtil.replace(targetDb.getFullPath(), "\\", "/") + "</string>  \n" +
             "    </void>  \n" +
             "    <void property=\"username\">  \n" +
             "     <string>sa</string>  \n" +
             "    </void>  \n" +
             "   </object>  \n" +
             "  </void>  \n" +
             "	 \n" +
             " </object>  \n" +
             "</java> ";
		PrintWriter writer = new PrintWriter(new FileOutputStream(new File(util.getBaseDir(), "WbProfiles.xml")));
		writer.println(xml);
		writer.close();
		// Make sure the new profiles are read
		ConnectionMgr.getInstance().readProfiles();
	}

	public void closeWindow()
	{
		QueueTool tool = new QueueTool();
		JFrameOperator mainWindow = new JFrameOperator("Data Pumper");
		JButtonOperator close = new JButtonOperator(mainWindow);
		close.pushNoBlock();
	}
	
	public void testDataPumper()
	{
		try
		{
			prepareSource();
			prepareTarget();
			startDataPumper();
			connect("selectSource", 1);
			connect("selectTarget", 2);
			copyData();
			closeWindow();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
