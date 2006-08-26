/*
 * WbIncludeTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import junit.framework.*;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.DefaultStatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.util.EncodingUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbIncludeTest extends TestCase
{
	
	public WbIncludeTest(String testName)
	{
		super(testName);
	}

	public void testExecute() throws Exception
	{
		try
		{
			TestUtil util = new TestUtil();
			util.prepareEnvironment();
			WbConnection con = util.getConnection();
			DefaultStatementRunner runner = util.createConnectedStatementRunner(con);
			
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
			
			runner.runStatement("wbinclude -file='" + main.getAbsolutePath() + "';\n",-1,-1);
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
	
}
