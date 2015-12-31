/*
 * WbManagerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench;

import java.io.BufferedReader;
import java.io.File;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.WbFile;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbManagerTest
	extends WbTestCase
{

	public WbManagerTest()
	{
		super("WbManagerTest");
	}

	@Test
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
				"create table batch_test (nr integer, name varchar(100));\n" +
				"insert into batch_test (nr, name) values (1, '" + umlauts + "');\n" +
				"commit;\n";

			TestUtil.writeFile(scriptFile, script, "UTF-8");

			File db = new File(util.getBaseDir(), getName());
			String[] args =
			{
				"-" + AppArguments.ARG_NOSETTNGS,
				"-configdir=" + util.getBaseDir(),
				"-url='jdbc:h2:" + db.getAbsolutePath() + "'",
				"-" + AppArguments.ARG_CONN_USER + "=sa",
				"-logfile='" + logfile.getFullPath() + "'",
				"-password= ",
				"-feedback=false",
				"-driver=org.h2.Driver ",
				"-script='" + scriptFile.getFullPath() + "'",
				"-encoding=UTF8"
			};

			WbManager.main(args);
			assertEquals(0, WbManager.getInstance().exitCode);
			WbConnection con = util.getConnection(db);
			try (Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("select nr, name from batch_test"))
			{
				if (rs.next())
				{
					int nr = rs.getInt(1);
					String name = rs.getString(2);
					assertEquals("Wrong id retrieved", 1, nr);
					assertEquals("Wrong name retrieved", umlauts, name);
				}
			}
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

	@Test
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

			String[] args =
			{
				"-" + AppArguments.ARG_NOSETTNGS + " ",
				"-driver='org.h2.Driver' -configdir='" + util.getBaseDir() + "' ",
				"-url='jdbc:h2:" + db.getAbsolutePath() + "' ",
				"-" + AppArguments.ARG_CONN_USER + "='sa' ",
				"-logfile='" + logfile.getFullPath() + "'",
				"-password= ",
				"-feedback=false -abortOnError=true ",
				"-command='WbExport -type=\"text\" -file=\"" + export.getFullPath() + "\" " +
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

	@Test
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

			WbFile script = new WbFile(util.getBaseDir(), "export.sql");
			TestUtil.writeFile(script,
				"WbExport -type=text -file='" + export.getFullPath() + "'\n " +
				"         -delimiter=| -decimal=',';\n" +
				"select TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE from information_schema.tables;\n");

			WbFile propfile = new WbFile(util.getBaseDir(), "wbconnection.txt");
			String props =
				"driver=org.h2.Driver \n" +
				"configdir=" + util.getBaseDir() + "\n" +
				"url=jdbc:h2:" + db.getAbsolutePath() + "\n" +
				AppArguments.ARG_CONN_USER + "=sa\n"+
				"logfile=" + logfile.getFullPath() + "\n" +
				"password= \n"+
				"feedback=false\n"+
				"abortOnError=true\n"+
				"script=" + script.getFullPath() + "\n";
			FileUtil.writeString(propfile, props, "ISO-8859-1", false);

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
