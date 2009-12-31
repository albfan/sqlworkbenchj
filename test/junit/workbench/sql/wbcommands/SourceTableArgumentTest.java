/*
 * SourceTableArgumentTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SourceTableArgumentTest
  extends TestCase
{

  public SourceTableArgumentTest(String testName)
  {
    super(testName);
  }

  public void testExclude()
		throws Exception
  {
    WbConnection con = null;
    Statement stmt = null;
    try
    {
      TestUtil util = new TestUtil("argsExclude");
      con = util.getConnection();

			String script = "CREATE TABLE t1 (id integer);\n" +
				"create table t2 (id integer);\n" +
				"create table t3 (id integer);\n" +
				"create table ta (id integer);\n" +
				"create table taa (id integer);\n" +
				"create table taaa (id integer);\n" +
				"create table tb (id integer);\n" +
				"commit;\n";
			TestUtil.executeScript(con, script);
			

      SourceTableArgument parser = new SourceTableArgument("t%", "ta%", con);
      List<TableIdentifier> tables = parser.getTables();
      assertEquals("Wrong number of table", 4, tables.size());
    }
    finally
    {
      SqlUtil.closeStatement(stmt);
      ConnectionMgr.getInstance().disconnectAll();
    }
	}
	
  public void testGetTables()
		throws Exception
  {
    WbConnection con = null;
    Statement stmt = null;
    try
    {
      TestUtil util = new TestUtil("args");
      con = util.getConnection();

			String script =
				"create table arg_test (nr integer, data varchar(100));\n" +
				"create table first_table (id integer);\n" +
				"create table second_table (id integer);\n" +
				"create schema myschema;\n" +
				"set schema myschema;\n" +
				"create table third_table (id integer);\n" +
				"set schema public;\n" +
				"commit;\n";

			TestUtil.executeScript(con, script);

      SourceTableArgument parser = new SourceTableArgument(null, con);
      List<TableIdentifier> tables = parser.getTables();
      assertEquals("Wrong number of table", 0, tables.size());

      parser = new SourceTableArgument(" ", con);
      tables = parser.getTables();
      assertEquals("Wrong number of table", 0, tables.size());
			
      parser = new SourceTableArgument("first_table, second_table, myschema.third_table", con);
      tables = parser.getTables();
      assertEquals("Wrong number of table", 3, tables.size());
      assertEquals("Wrong table retrieved", true, tables.get(0).getTableName().equalsIgnoreCase("first_table"));
      assertEquals("Wrong table retrieved", true, tables.get(1).getTableName().equalsIgnoreCase("second_table"));
      assertEquals("Wrong table retrieved", true, tables.get(2).getTableName().equalsIgnoreCase("third_table"));
      assertEquals("Wrong table retrieved", true, tables.get(2).getSchema().equalsIgnoreCase("myschema"));

      parser = new SourceTableArgument("*", con);
      tables = parser.getTables();
      assertEquals("Wrong number of table", 4, tables.size());
			Collections.sort(tables); // make sure arg_test is at the beginning
      assertTrue("Wrong table retrieved", tables.get(0).getTableName().equalsIgnoreCase("arg_test"));
    }
    finally
    {
      SqlUtil.closeStatement(stmt);
      ConnectionMgr.getInstance().disconnectAll();
    }
  }
	
	public void testGetObjectNames()
	{
		try
		{
			String s = "\"MIND\",\"test\"";
      SourceTableArgument parser = new SourceTableArgument(null, null);			
			List<String> tables = parser.getObjectNames(s);
			assertEquals(2, tables.size());
			assertEquals("\"MIND\"", tables.get(0));
			assertEquals("\"test\"", tables.get(1));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}	
}
