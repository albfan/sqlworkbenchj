/*
 * SourceTableArgumentTest.java
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

import java.sql.Statement;
import java.util.List;
import java.util.Set;
import workbench.TestUtil;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SourceTableArgumentTest
  extends WbTestCase
{

  public SourceTableArgumentTest()
  {
    super("SourceTableArgumentTest");
  }

	@Test
  public void testExcludeWithWildcard()
		throws Exception
  {
    WbConnection con = null;
    Statement stmt = null;
    try
    {
      TestUtil util = new TestUtil("argsExclude");
      con = util.getConnection();

			Settings.getInstance().setProperty("workbench.sql.ignorecatalog.h2", "argsexclude");
			con.getMetadata().clearIgnoredCatalogs();

			String script = "CREATE TABLE t1 (id integer);\n" +
				"create table t2 (id integer);\n" +
				"create table t3 (id integer);\n" +
				"create table ta (id integer);\n" +
				"create table taa (id integer);\n" +
				"create table taaa (id integer);\n" +
				"create table tb (id integer);\n" +
				"commit;\n";
			TestUtil.executeScript(con, script);

      SourceTableArgument parser = new SourceTableArgument("t%", "ta%", "TABLE", con);
      List<TableIdentifier> tables = parser.getTables();
      assertEquals(4, tables.size());
    }
    finally
    {
      SqlUtil.closeStatement(stmt);
      ConnectionMgr.getInstance().disconnectAll();
    }
	}

	@Test
  public void testExcludeSingleTable()
		throws Exception
  {
    WbConnection con = null;
    Statement stmt = null;
		String oldCatIgnore = Settings.getInstance().getProperty("workbench.sql.ignorecatalog.h2", null);
    try
    {
      TestUtil util = new TestUtil("argsExclude");
      con = util.getConnection();
			con.getMetadata().clearIgnoredCatalogs();

			Settings.getInstance().setProperty("workbench.sql.ignorecatalog.h2", "argsExclude");

			String script =
				"CREATE SCHEMA one;\n" +
				"SET SCHEMA one;\n" +
				"CREATE TABLE t1 (id integer);\n" +
				"create table t2 (id integer);\n" +
				"create table t3 (id integer);\n" +
				"create table ta (id integer);\n" +
				"create table taa (id integer);\n" +
				"create table taaa (id integer);\n" +
				"create table tb (id integer);\n" +
				"commit;\n";
			TestUtil.executeScript(con, script);

      SourceTableArgument parser = new SourceTableArgument("t%", "T2,T3", "TABLE", con);
      List<TableIdentifier> tables = parser.getTables();
      assertEquals("Wrong number of table", 5, tables.size());
			for (TableIdentifier tbl : tables)
			{
				if (tbl.getTableName().equals("T2"))
				{
					fail("T2 not excluded!");
				}
				if (tbl.getTableName().equals("T3"))
				{
					fail("T2 not excluded!");
				}
			}
    }
    finally
    {
      SqlUtil.closeStatement(stmt);
      ConnectionMgr.getInstance().disconnectAll();
			Settings.getInstance().setProperty("workbench.sql.ignorecatalog.h2", oldCatIgnore);
    }
	}

	@Test
  public void testGetTables()
		throws Exception
  {
    WbConnection con = null;
    Statement stmt = null;
		String oldCatIgnore = Settings.getInstance().getProperty("workbench.sql.ignorecatalog.h2", null);
		String schemaIgnore = Settings.getInstance().getProperty("workbench.sql.ignoreschema.h2", null);
    try
    {
			Settings.getInstance().setProperty("workbench.sql.ignorecatalog.h2", "args");
			Settings.getInstance().setProperty("workbench.sql.ignoreschema.h2", null);

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

			con.getMetadata().clearIgnoredCatalogs();
			con.getMetadata().clearIgnoredSchemas();

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
			Set<String> names = CollectionUtil.caseInsensitiveSet("THIRD_TABLE", "ARG_TEST", "FIRST_TABLE", "SECOND_TABLE");
			for (TableIdentifier tbl : tables)
			{
				assertTrue(names.remove(tbl.getTableName()));
			}
			assertEquals(0, names.size());
    }
    finally
    {
			Settings.getInstance().setProperty("workbench.sql.ignorecatalog.h2", oldCatIgnore);
			Settings.getInstance().setProperty("workbench.sql.ignoreschema.h2", schemaIgnore);
      SqlUtil.closeStatement(stmt);
      ConnectionMgr.getInstance().disconnectAll();
    }
  }

	@Test
	public void testGetObjectNames()
		throws Exception
	{
		String s = "\"MIND\",\"test\"";
		SourceTableArgument parser = new SourceTableArgument(null, null);
		List<String> tables = parser.getObjectNames(s);
		assertEquals(2, tables.size());
		assertEquals("\"MIND\"", tables.get(0));
		assertEquals("\"test\"", tables.get(1));
	}
}
