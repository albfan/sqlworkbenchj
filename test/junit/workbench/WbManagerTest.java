/*
 * WbManagerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
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
 * @author Thomas Kellerer
 */
public class WbManagerTest
	extends TestCase
{

	public WbManagerTest(String testName)
	{
		super(testName);
	}

	public void testBatchMode()
		throws Exception
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
		finally
		{
			ConnectionMgr.getInstance().clearProfiles();
			ConnectionMgr.getInstance().disconnectAll();
			LogMgr.shutdown();
			assertTrue(logfile.delete());
		}
	}


	public void testCommandParameter()
		throws Exception
	{
		System.setProperty("workbench.system.doexit", "false");

		TestUtil util = new TestUtil(getName());
		util.emptyBaseDirectory();

		WbFile logfile = new WbFile(util.getBaseDir(), "command.log");

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
												"-delimiter=\";\" -decimal=\",\"; select TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE from information_schema.tables;' "
												};

			WbManager.main(args);
			assertEquals(0, WbManager.getInstance().exitCode);
			assertTrue(export.exists());

			BufferedReader reader = EncodingUtil.createBufferedReader(export, "ISO-8859-1");
			List<String> lines = FileUtil.getLines(reader);
			assertEquals("Wrong header line", "TABLE_CATALOG;TABLE_SCHEMA;TABLE_NAME;TABLE_TYPE", lines.get(0));
		}
		finally
		{
			ConnectionMgr.getInstance().clearProfiles();
			ConnectionMgr.getInstance().disconnectAll();
			LogMgr.shutdown();
			assertTrue("Could not delete logfile", logfile.delete());
		}
	}

	public void testPropfile()
		throws Exception
	{
		TestUtil util = new TestUtil(getName());
		util.emptyBaseDirectory();

		WbFile logfile = new WbFile(util.getBaseDir(), "props.log");
		System.setProperty("workbench.system.doexit", "false");

		try
		{
			File db = new File(util.getBaseDir(), getName());
			WbFile export = new WbFile(util.getBaseDir(), "export.txt");

			WbFile propfile = new WbFile(util.getBaseDir(), "wbconnection.txt");
			PrintWriter writer = new PrintWriter(EncodingUtil.createWriter(propfile, "ISO-8859-1", false));

			WbFile script = new WbFile(util.getBaseDir(), "export.sql");
			TestUtil.writeFile(script, "WbExport -type=text -file='" + export.getFullPath() + "'\n "+
												          "         -delimiter=| -decimal=',';\n" +
																	"select TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE from information_schema.tables;\n");

			writer.println("driver=org.h2.Driver");
			writer.println("configdir=" + util.getBaseDir());
			writer.println("url=jdbc:h2:" + db.getAbsolutePath());
			writer.println(AppArguments.ARG_CONN_USER + "=sa");
			writer.println("logfile=" + logfile.getFullPath());
			writer.println("feedback=false");
			writer.println("abortOnError=true");
			writer.println("script=" + script.getFullPath());
			writer.close();

			String[] args = { "-" + AppArguments.ARG_PROPFILE + "=" + propfile.getFullPath() };

			WbManager.main(args);
			assertEquals(0, WbManager.getInstance().exitCode);
			assertTrue(export.exists());

			BufferedReader reader = EncodingUtil.createBufferedReader(export, "ISO-8859-1");
			List<String> lines = FileUtil.getLines(reader);
			assertEquals("Wrong header line", "TABLE_CATALOG|TABLE_SCHEMA|TABLE_NAME|TABLE_TYPE", lines.get(0));
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
