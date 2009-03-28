/*
 * WbManagerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.io.BufferedReader;
import java.io.File;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import junit.framework.TestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbManagerTest extends TestCase
{
	
	public WbManagerTest(String testName)
	{
		super(testName);
	}

	public void testBatchMode()
	{
		String umlauts = "\u00f6\u00e4\u00fc";
		TestUtil util = new TestUtil(getName());
		util.emptyBaseDirectory();
		
		WbFile logfile = new WbFile(util.getBaseDir(), "junit_wb_test.log");
		System.setProperty("workbench.system.doexit", "false");
		
		try
		{

			WbFile scriptFile = new WbFile(util.getBaseDir(), "batch_script.sql");
			String script =
				"create table batch_test (nr integer, name varchar(100));\n"  +
				"insert into batch_test (nr, name) values (1, '" + umlauts + "');\n" +
				"commit;\n";
			
			TestUtil.writeFile(scriptFile, script, "UTF-8");
			
			File db = new File(util.getBaseDir(), getName());
			String[] args = { "-" + AppArguments.ARG_NOSETTNGS,
												"-configdir=" + util.getBaseDir(),
												"-url='jdbc:h2:" + db.getAbsolutePath() + "'",
												"-" + AppArguments.ARG_CONN_USER + "=sa",
												"-logfile='" + logfile.getFullPath() + "'",
												"-feedback=false",
												"-driver=org.h2.Driver ",
												"-script='" + scriptFile.getFullPath() + "'",
												"-encoding=UTF8"
												};

			WbManager.main(args);
			assertEquals(0, WbManager.getInstance().exitCode);
			WbConnection con = util.getConnection(db);
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, name from batch_test");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				String name = rs.getString(2);
				assertEquals("Wrong id retrieved", 1, nr);
				assertEquals("Wrong name retrieved", umlauts, name);
			}
			rs.close();
			stmt.close();
			assertTrue(logfile.exists());
			assertTrue(scriptFile.delete());
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().clearProfiles();
			ConnectionMgr.getInstance().disconnectAll();
			LogMgr.shutdown();
			assertTrue(logfile.delete());
		}
	}


	public void testCommandParameter()
	{
		TestUtil util = new TestUtil(getName());
		util.emptyBaseDirectory();

		WbFile logfile = new WbFile(util.getBaseDir(), "command.log");
		System.setProperty("workbench.system.doexit", "false");

		try
		{
			File db = new File(util.getBaseDir(), getName());
			WbFile export = new WbFile(util.getBaseDir(), "export.txt");
			
			String[] args = { "-" + AppArguments.ARG_NOSETTNGS + " ",
												"-driver='org.h2.Driver' -configdir='" + util.getBaseDir() + "' ",
												"-url='jdbc:h2:" + db.getAbsolutePath() + "' ",
												"-" + AppArguments.ARG_CONN_USER + "='sa' ",
												"-logfile='" + logfile.getFullPath() + "'",
												"-feedback=false -abortOnError=true ",
												"-command='WbExport -type=\"text\" -file=\"" + export.getFullPath() + "\" "+
												"-delimiter=\";\" -decimal=\",\"; select * from information_schema.tables;' "
												};

			WbManager.main(args);
			assertEquals(0, WbManager.getInstance().exitCode);
			assertTrue(export.exists());

			BufferedReader reader = EncodingUtil.createBufferedReader(export, "ISO-8859-1");
			List<String> lines = FileUtil.getLines(reader);
			assertEquals("Wrong header line", "TABLE_CATALOG;TABLE_SCHEMA;TABLE_NAME;TABLE_TYPE;STORAGE_TYPE;SQL;REMARKS;ID", lines.get(0));
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().clearProfiles();
			ConnectionMgr.getInstance().disconnectAll();
			LogMgr.shutdown();
			assertTrue("Could not delete logfile", logfile.delete());
		}
	}


}
