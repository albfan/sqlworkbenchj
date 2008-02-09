/*
 * SourceTableArgumentTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.Statement;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class SourceTableArgumentTest
  extends TestCase
{

  public SourceTableArgumentTest(String testName)
  {
    super(testName);
  }

  public void testGetTables()
  {
    WbConnection con = null;
    Statement stmt = null;
    try
    {
      TestUtil util = new TestUtil("args");
      con = util.getConnection();
      stmt = con.createStatement();
      stmt.executeUpdate("create table arg_test (nr integer, data varchar(100))");
      con.commit();

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
      assertEquals("Wrong number of table", 1, tables.size());
      assertEquals("Wrong table retrieved", true, tables.get(0).getTableName().equalsIgnoreCase("arg_test"));
    }
    catch (Exception e)
    {
      e.printStackTrace();
      fail(e.getMessage());
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
