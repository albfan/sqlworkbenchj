/*
 * WbManagerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.io.File;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import junit.framework.TestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.util.EncodingUtil;

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

	private static final String UMLAUTS = "\u00f6\u00e4\u00fc";

	private String createScript(String basedir)
	{
		File f = new File(basedir, "batch_script.sql");
		PrintWriter w = null;
		try
		{
			w = new PrintWriter(EncodingUtil.createWriter(f, "UTF8", false));
			w.println("create table batch_test (nr integer, name varchar(100));\n");
			w.println("insert into batch_test (nr, name) values (1, '" + UMLAUTS + "');\n");
			w.println("commit;\n");
			w.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return f.getAbsolutePath();
	}
	
	public void testBatchMode()
	{
		try
		{
			TestUtil util = new TestUtil(getName());
			System.setProperty("workbench.system.doexit", "false");
			File db = new File(util.getBaseDir(), getName());
			String script = createScript(util.getBaseDir());
			String args[] = { "-embedded", 
												"-nosettings",
												"-configdir=" + util.getBaseDir(),
												"-url='jdbc:h2:" + db.getAbsolutePath() + "'",
												"-user=sa",
												"-driver=org.h2.Driver",
												"-script='" + script + "'",
												"-encoding=UTF8"
												};
			WbManager.main(args);
			WbConnection con = util.getConnection(db);
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select nr, name from batch_test");
			if (rs.next())
			{
				int nr = rs.getInt(1);
				String name = rs.getString(2);
				assertEquals("Wrong id retrieved", 1, nr);
				assertEquals("Wrong name retrieved", UMLAUTS, name);
			}
			rs.close();
			stmt.close();
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
