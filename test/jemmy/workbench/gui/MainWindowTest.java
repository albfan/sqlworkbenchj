/*
 * MainWindowTest.java
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
package workbench.gui;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;

import workbench.db.ConnectionMgr;

import workbench.gui.actions.AppendResultsAction;
import workbench.gui.actions.SqlPanelReloadAction;
import workbench.gui.sql.SqlPanel;
import workbench.gui.tools.DbExplorerTester;

import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbThread;

import org.junit.Test;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JComponentOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JListOperator;
import org.netbeans.jemmy.operators.JMenuBarOperator;
import org.netbeans.jemmy.operators.JMenuItemOperator;
import org.netbeans.jemmy.operators.JMenuOperator;
import org.netbeans.jemmy.operators.JTableOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.netbeans.jemmy.operators.Operator.DefaultStringComparator;
import org.netbeans.jemmy.operators.Operator.StringComparator;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class MainWindowTest
{
	private GuiTestUtil testUtil;

	public MainWindowTest()
	{
		this.testUtil = new GuiTestUtil("MainWindowTest");
	}

	private void startApplication()
	{
		try
		{
			testUtil.startApplication(false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void aboutTest()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		new JMenuBarOperator(mainWindow).pushMenuNoBlock("Help|About", "|");

		JDialogOperator dialog = new JDialogOperator(mainWindow, "About SQL Workbench/J");

		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("close");
		JButtonOperator close = new JButtonOperator(dialog, chooser);
		close.push();
	}

	private void settingsTest()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		new JMenuBarOperator(mainWindow).pushMenuNoBlock("Tools|Options", "|");
		JDialogOperator dialog = new JDialogOperator(mainWindow, "Settings");
		final JListOperator pages = new JListOperator(dialog);

		int count = pages.getModel().getSize();
		assertEquals(17, count);

		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("pagetitle");

		QueueTool tool = new QueueTool();
		for (int i = 1; i < count; i++)
		{
			final int index = i;
			pages.selectItem(index);
			tool.waitEmpty();

			String pg = pages.getSelectedValue().toString();
			JLabelOperator title = new JLabelOperator(dialog, chooser);
			assertEquals(pg, title.getText());
		}

		new JButtonOperator(dialog, "Cancel").push();
	}

	private void dbExplorerTest()
		throws InterruptedException
	{
		DbExplorerSettings.setRetrieveDbExplorer(true);
		JFrameOperator mainWindowOp = new JFrameOperator("SQL Workbench");

		QueueTool tool = new QueueTool();

		new JMenuBarOperator(mainWindowOp).pushMenu("Tools|Show Database Explorer", "|");
		tool.waitEmpty();

		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("dbexplorer");
		JComponentOperator panel = new JComponentOperator(mainWindowOp, chooser);
		DbExplorerTester tester = new DbExplorerTester();
		tester.testPanel(panel);

		tool.waitEmpty();

		new JMenuBarOperator(mainWindowOp).pushMenu("View|Close Tab", "|");
		tool.waitEmpty();

		Thread.sleep(250);

		// Now test the DbExplorer in a stand-alone window

		new JMenuBarOperator(mainWindowOp).pushMenuNoBlock("Tools|New DbExplorer Window", "|");
		tool.waitEmpty();

		JFrameOperator dbExpOp = new JFrameOperator("Database Explorer");
		chooser.setName("dbexplorer");
		panel = new JComponentOperator(dbExpOp, chooser);
		tester = new DbExplorerTester();
		tester.testPanel(panel);
		dbExpOp.close();
	}

	private void definePKTest()
	{
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);

		QueueTool tool = new QueueTool();

		chooser.setName("sqlpanel-1");
		JComponentOperator panel = new JComponentOperator(mainWindow, chooser);
		final SqlPanel sqlPanel = (SqlPanel)panel.getSource();

		JMenuOperator dataMenu = new JMenuOperator(mainMenu.getMenu(3));
		JMenuItem saveItem = (JMenuItem)dataMenu.getMenuComponent(1);
		final JMenuItemOperator save = new JMenuItemOperator(saveItem);
//		assertFalse(save.isEnabled());

		runSql(sqlPanel, "create table nopk (id1 integer, data varchar(20));\n" +
			"commit;\n" +
			"insert into nopk (id1, data) values (1,'Ford');\n" +
			"insert into nopk (id1, data) values (2,'Zaphod');\n" +
			"commit;\n");
		runSql(sqlPanel, "select id1, data from nopk;");

		JTableOperator result = new JTableOperator(mainWindow);
		int rows = result.getRowCount();
		assertEquals(2, rows);
		assertEquals(2, result.getColumnCount());
		result.setValueAt("Arthur", 0, 1);

		tool.waitEmpty();
		assertEquals(3, result.getColumnCount());
		assertTrue(save.isEnabled());

		new JMenuBarOperator(mainWindow).pushMenuNoBlock("Data|Define key columns", "|");
		JDialogOperator definePK = new JDialogOperator("Select Key Columns");

		JTableOperator table = new JTableOperator(definePK);
		chooser.setName("ok");
		JButtonOperator ok = new JButtonOperator(definePK, chooser);

		table.clickOnCell(0, 1, 1);
		ok.push();

		saveChanges(sqlPanel);
		new JMenuBarOperator(mainWindow).pushMenuNoBlock("Data|Save changes to database", "|");
		testUtil.waitWhileBusy(sqlPanel);
		tool.waitEmpty();

		assertEquals(2, result.getColumnCount());
		runSql(sqlPanel, "select id1, data from nopk where data = 'Arthur';");

		result = new JTableOperator(mainWindow);
		rows = result.getRowCount();
		assertEquals(1, rows);
	}

	private void pkWarningsTest()
	{
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);

		QueueTool tool = new QueueTool();

		chooser.setName("sqlpanel-1");
		JComponentOperator panel = new JComponentOperator(mainWindow, chooser);
		final SqlPanel sqlPanel = (SqlPanel)panel.getSource();

		JMenuOperator dataMenu = new JMenuOperator(mainMenu.getMenu(3));
		JMenuItem saveItem = (JMenuItem)dataMenu.getMenuComponent(2);
		final JMenuItemOperator save = new JMenuItemOperator(saveItem);
		assertFalse(save.isEnabled());

		runSql(sqlPanel, "create table jtest (id1 integer, id2 integer, data varchar(20), primary key (id1, id2));\n" +
			"commit;\n" +
			"insert into jtest (id1, id2, data) values (1,1,'Ford');\n" +
			"commit;\n");
		runSql(sqlPanel, "select id1, data from jtest;");

		JTableOperator result = new JTableOperator(mainWindow);
		int rows = result.getRowCount();
		assertEquals(1, rows);
		assertEquals(2, result.getColumnCount());
		result.setValueAt("Arthur", 0, 1);

		tool.waitEmpty();
		assertEquals(3, result.getColumnCount());
		assertTrue(save.isEnabled());

		new JMenuBarOperator(mainWindow).pushMenuNoBlock("Data|Save changes to database", "|");
		JDialogOperator warning = new JDialogOperator("Missing key columns");
		JButtonOperator cancel = new JButtonOperator(warning, "Cancel");
		cancel.push();
		assertEquals(3, result.getColumnCount());
	}

	private void connect()
	{
		// Make sure not profiles exist
		ConnectionMgr.getInstance().clearProfiles();

		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		JMenuBar bar = mainWindow.getJMenuBar();
		//		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);
		JMenuBarOperator menu = new JMenuBarOperator(bar);
		menu.pushMenuNoBlock("File|Connect", "|");

		JDialogOperator dialog = new JDialogOperator(mainWindow, "Select Connection Profile");
		JTextFieldOperator profileName = new JTextFieldOperator(dialog, "New Profile");
		profileName.setText("Test Connection");

		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("driverclass");

		JComboBoxOperator driver = new JComboBoxOperator(dialog, chooser);
		driver.setToolTipText("test");
		StringComparator comp = new DefaultStringComparator(false, false);

		int index = driver.findItemIndex("H2 Database Engine", comp);
		if (index <= 0) fail("H2 Driver not found");
		driver.selectItem(index);

		chooser.setName("url");
		JTextFieldOperator url = new JTextFieldOperator(dialog, chooser);
		WbFile db = new WbFile(testUtil.getBaseDir(), "testdb");
		url.setText("jdbc:h2:" + db.getFullPath());

		chooser.setName("username");
		JTextFieldOperator username = new JTextFieldOperator(dialog, chooser);
		username.setText("sa");
		new JButtonOperator(dialog, "OK").push();

		// Connecting can take some time...
		QueueTool tool = new QueueTool();
		tool.waitEmpty();
		chooser.setName("sqlpanel-1");
		JComponentOperator panel = new JComponentOperator(mainWindow, chooser);
		SqlPanel sqlPanel = (SqlPanel)panel.getSource();
		testUtil.waitUntilConnected(sqlPanel);
	}

	private void testCopyActions(JMenuBarOperator mainMenu)
	{
		JMenuOperator dataMenu = new JMenuOperator(mainMenu.getMenu(3));

		JMenuItem copyAsText = (JMenuItem)dataMenu.getMenuComponent(12);
		assertTrue(copyAsText.isEnabled());

		JMenu copyAsSql = (JMenu)dataMenu.getMenuComponent(13);
		assertTrue(copyAsSql.isEnabled());

		assertEquals(5, copyAsSql.getMenuComponentCount());
		// Copy as text menu item
		for (int i=0; i < 5; i++)
		{
			JMenuItem item = copyAsSql.getItem(i);
			JMenuItemOperator op = new JMenuItemOperator(item);
			assertTrue(op.isEnabled());
		}
	}

	private void runSql()
	{
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqleditor1");
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);

		chooser.setName("sqlpanel-1");
		JComponentOperator panel = new JComponentOperator(mainWindow, chooser);
		final SqlPanel sqlPanel = (SqlPanel)panel.getSource();

		String msg = runSql(sqlPanel, "create table person (nr integer primary key, firstname varchar(20), lastname varchar(20));");
		System.out.println("Create message: " + msg);
		assertTrue(msg.indexOf("Table 'person' created") > -1);

		msg = runSql(sqlPanel, "insert into person (nr, firstname, lastname) values (42, 'Ford', 'Prefect');\ncommit;");
		assertNotNull(msg);
		assertTrue(msg.indexOf("1 row affected") > -1);

		runSql(sqlPanel, "select nr, firstname, lastname from person");

		JTableOperator result = new JTableOperator(mainWindow);
		int rows = result.getRowCount();
		assertEquals(1, rows);
		assertEquals(3, result.getColumnCount());

		Object nr = result.getValueAt(0, 0);
		assertEquals(nr, new Integer(42));

		testCopyActions(mainMenu);

		JMenuOperator dataMenu = new JMenuOperator(mainMenu.getMenu(3));
		JMenuItem reloadItem = (JMenuItem)dataMenu.getMenuComponent(5);
		Action a = reloadItem.getAction();
		assertTrue(a instanceof SqlPanelReloadAction);
		assertTrue(a.isEnabled());

		sqlPanel.showLogPanel();
		assertFalse(a.isEnabled());

		sqlPanel.showResultPanel(0);

		JMenuItem saveItem = (JMenuItem)dataMenu.getMenuComponent(2);
		JMenuItemOperator save = new JMenuItemOperator(saveItem);
		assertFalse(save.isEnabled());

		result.setValueAt("Arthur", 0, 1);
		QueueTool tool = new QueueTool();

		// The first call to setValueAt() will make the result table display
		// the status column
		assertEquals(4, result.getColumnCount());

		// because of the status column the lastname column
		// is the column with index 3 (not 2)
		result.setValueAt("Dent", 0, 3);

		assertTrue(save.isEnabled());

		saveChanges(sqlPanel);

		// Make sure the status column is turned off after saving
		assertEquals(3, result.getColumnCount());

		runSql(sqlPanel, "select nr, firstname, lastname from person where lastname = 'Dent';");
		tool.waitEmpty();

		// Obtain a new referenct to the result table as the
		// SQLPanel has created a new instance when running the select
		result = new JTableOperator(mainWindow);
		assertEquals(1, result.getRowCount());
		assertEquals(3, result.getColumnCount());

		String firstname = (String)result.getValueAt(0, 1);
		assertEquals("Arthur", firstname);

		msg = runSql(sqlPanel, "update person set firstname = null where nr = 42;\ncommit;");
		System.out.println("update message: " + msg);
		assertTrue(msg.indexOf("1 row affected") > -1);

		msg = runSql(sqlPanel, "select nr, firstname, lastname from person where lastname = 'Dent';");
		System.out.println("Message: " + msg);

		result = new JTableOperator(mainWindow);
		firstname = (String)result.getValueAt(0, 1);
		assertTrue(StringUtil.isBlank(firstname));

		result.setValueAt("Arthur", 0, 1);
		tool.waitEmpty();
		msg = saveChanges(sqlPanel);
		tool.waitEmpty();
		System.out.println("*** runSql() finished");
	}

	private void showLogFile()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		new JMenuBarOperator(mainWindow).pushMenuNoBlock("Help|View Logfile", "|");
		NamedComponentChooser chooser = new NamedComponentChooser();
		WbFile f = LogMgr.getLogfile();
		String fname = f.getFullPath();
		JFrameOperator logWindow = new JFrameOperator(fname);
		assertNotNull(logWindow);
		logWindow.close();
	}

	private void appendTest()
	{
		JFrameOperator mainWindow = new JFrameOperator("SQL Workbench");
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("sqlpanel-1");
		JComponentOperator panel = new JComponentOperator(mainWindow, chooser);
		final SqlPanel sqlPanel = (SqlPanel)panel.getSource();

		JMenuBarOperator mainMenu = new JMenuBarOperator(mainWindow);
		JMenuOperator sqlMenu = new JMenuOperator(mainMenu.getMenu(4));

		JMenu settings = (JMenu)sqlMenu.getItem(26);
		JMenuItem appendItem = settings.getItem(1);

		AppendResultsAction action = (AppendResultsAction)appendItem.getAction();
		assertFalse(appendItem.isSelected());

		runSql(sqlPanel, "select * from person");

		chooser.setName("resultspane");
		JComponentOperator comp = new JComponentOperator(mainWindow, chooser);
		JTabbedPane resultTab = (JTabbedPane)comp.getSource();

		assertEquals(2, resultTab.getTabCount());

		QueueTool tool = new QueueTool();
		mainMenu.pushMenu("SQL|Settings|Append new results", "|");
		tool.waitEmpty();
		assertTrue(sqlPanel.getAppendResults());
		assertTrue(appendItem.isSelected());
		assertTrue(action.getButton().isSelected());

		runSql(sqlPanel, "select * from person");
		assertEquals(3, resultTab.getTabCount());

		mainMenu.pushMenu("SQL|Settings|Append new results", "|");
		tool.waitEmpty();
		assertFalse(sqlPanel.getAppendResults());
		assertFalse(appendItem.isSelected());
		assertFalse(action.getButton().isSelected());

		runSql(sqlPanel, "select * from person");
		WbThread.sleepSilently(500);
		assertEquals(2, resultTab.getTabCount());
	}

	private String saveChanges(final SqlPanel panel)
	{
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				panel.updateDb();
			}
		};
		testUtil.execute(r);
		QueueTool tool = new QueueTool();
		tool.waitEmpty();
		testUtil.waitWhileBusy(panel);
		tool.waitEmpty();
		return panel.getLogMessage();
	}

	private String runSql(final SqlPanel panel, final String sql)
	{
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				panel.getEditor().setText(sql);
				panel.runAll();
			}
		};

		testUtil.execute(r);
		QueueTool tool = new QueueTool();
		tool.waitEmpty();
		testUtil.waitWhileBusy(panel);
		tool.waitEmpty();
		return panel.getLogMessage();
	}

	@Test
	public void testWindow()
		throws Exception
	{
		try
		{
			System.setProperty("workbench.db.hsql_database_engine.table_type.show_always", "false");
			startApplication();
			aboutTest();
			settingsTest();
			showLogFile();
			connect();
			runSql();
			appendTest();
			pkWarningsTest();
			definePKTest();
			dbExplorerTest();
		}
		finally
		{
			testUtil.stopApplication();
		}
	}
}
