/*
 * WbIncludeTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.util.EncodingUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

/**
 *
 * @author Thomas Kellerer
 */
public class WbIncludeTest
	extends WbTestCase
{
	private TestUtil util;
	private StatementRunner runner;

	public WbIncludeTest()
	{
		super("WbIncludeTest");
		util = getTestUtil();
	}

	@Before
	public void setUp()
		throws Exception
	{
		util.emptyBaseDirectory();
		runner = util.createConnectedStatementRunner();
	}

	@Test
	public void testAlternateInclude()
		throws Exception
	{
		try
		{
			WbConnection con = runner.getConnection();

			Statement stmt = con.createStatement();
			stmt.execute("create table include_test (file_name varchar(100))");
			con.commit();

			String encoding = "ISO-8859-1";
			File scriptFile = new File(util.getBaseDir(), "test.sql");

			Writer w = EncodingUtil.createWriter(scriptFile, encoding, false);
			w.write("insert into include_test (file_name) values ('" + scriptFile.getAbsolutePath() + "');\n");
			w.write("commit;\n");
			w.close();

			String sql = "-- comment\n\n@test.sql\n";
			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			assertEquals("Statement not executed", true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select count(*) from include_test");

			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Rows not inserted", 1, count);
			}
			else
			{
				fail("Select failed");
			}
			rs.close();
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testFileNotFound()
		throws Exception
	{
		try
		{
			runner.runStatement("WbInclude -file=/this/will/not/be/there/i_hope.sql");
			StatementRunnerResult result = runner.getResult();
			assertFalse("Runner was successful", result.isSuccess());
			String msg = result.getMessageBuffer().toString();
			assertTrue("Wrong error", msg.indexOf("not found") > -1);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testNestedInclude()
		throws Exception
	{
		try
		{
			WbConnection con = runner.getConnection();

			File subdir1 = new File(util.getBaseDir(), "subdir1");
			subdir1.mkdir();

			File include1 = new File(subdir1, "include1.sql");

			Statement stmt = con.createStatement();
			stmt.execute("create table include_test (file_name varchar(100))");
			con.commit();

			String encoding = "ISO-8859-1";
			Writer w = EncodingUtil.createWriter(include1, encoding, false);
			w.write("insert into include_test (file_name) values ('" + include1.getAbsolutePath() + "');\n");
			w.write("commit;\n");
			w.close();

			File main = new File(util.getBaseDir(), "main.sql");
			w = EncodingUtil.createWriter(main, encoding, false);
			w.write("insert into include_test (file_name) values ('" + main.getAbsolutePath() + "');\n");
			w.write("commit;\n");
			w.write("@./" + subdir1.getName() + "/" + include1.getName() + "\n");
			w.close();

			runner.runStatement("wbinclude -file='" + main.getAbsolutePath() + "';\n");
			StatementRunnerResult result = runner.getResult();
			assertEquals("Runner not successful", true, result.isSuccess());

			ResultSet rs = stmt.executeQuery("select * from include_test");
			List files = new ArrayList();
			while (rs.next())
			{
				files.add(rs.getString(1));
			}
			rs.close();
			assertEquals("Not enough values retrieved", 2, files.size());
			assertEquals("Main file not run", true, files.contains(main.getAbsolutePath()));
			assertEquals("Second file not run", true, files.contains(include1.getAbsolutePath()));
			stmt.close();
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
