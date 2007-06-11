/*
 * ConstantColumnValuesTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;
import workbench.util.ValueConverter;

/**
 *
 * @author tkellerer
 */
public class ConstantColumnValuesTest extends TestCase
{
	
	public ConstantColumnValuesTest(String testName)
	{
		super(testName);
	}

  public void testGetStaticValues()
  {
		List<workbench.db.ColumnIdentifier> columns = new ArrayList();
		columns.add(new workbench.db.ColumnIdentifier("test_run_id", java.sql.Types.INTEGER));
		columns.add(new workbench.db.ColumnIdentifier("title", java.sql.Types.VARCHAR));
    try
    {
      ConstantColumnValues values = new ConstantColumnValues("test_run_id=42,title=\"hello, world\"", columns);
			assertEquals(2, values.getColumnCount());
			assertEquals(new Integer(42), values.getValue(0));
			assertEquals("hello, world", values.getValue(1));
    }
    catch (Exception ex)
    {
			ex.printStackTrace();
      fail(ex.getMessage());
    }
  }

	public void testInitFromDb()
	{
		TestUtil util = new TestUtil("testConstants");
		WbConnection con = null;
		String tablename = "constant_test";
		Statement stmt = null;
    try
    {
			con = util.getConnection("cons_test");
			stmt = con.createStatement();
			stmt.executeUpdate("create table constant_test (test_run_id integer, title varchar(20))");
			ValueConverter converter = new ValueConverter("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss");
      ConstantColumnValues values = new ConstantColumnValues("test_run_id=42,title=\"hello, world\"", con, tablename, converter);
			assertEquals(2, values.getColumnCount());
			assertEquals(new Integer(42), values.getValue(0));
			assertEquals("hello, world", values.getValue(1));
    }
    catch (Exception ex)
    {
			ex.printStackTrace();
      fail(ex.getMessage());
    }
		finally
		{
			SqlUtil.closeStatement(stmt);
			try { con.disconnect(); } catch (Throwable th) {}
		}
	}
}
