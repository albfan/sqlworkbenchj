/*
 * TableSourceBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class TableSourceBuilderTest
	extends WbTestCase
{

	public TableSourceBuilderTest()
	{
		super("TableSourceBuilderTest");
	}

	@Test
	public void testGetTableSource()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		try
		{
			TestUtil.executeScript(con,
				"CREATE TABLE person (id integer not null, firstname varchar(20), lastname varchar(20));\n" +
				"ALTER TABLE PERSON ADD constraint pk_person primary key (id);\n" +
				"COMMIT;\n");
			TableSourceBuilder builder = new TableSourceBuilder(con);
			TableIdentifier tbl = new TableIdentifier("PERSON");
			String sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.startsWith("CREATE TABLE PERSON"));
			assertTrue(sql.indexOf("PRIMARY KEY (ID)") > -1);

			String dbid = con.getMetadata().getDbId();
			Settings.getInstance().setProperty("workbench.db." + dbid + ".coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnChanger.PARAM_NULLABLE);

			builder = new TableSourceBuilder(con);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.indexOf("FIRSTNAME  VARCHAR(20)   NULL") > -1);

			TestUtil.executeScript(con,
				"ALTER TABLE person ALTER COLUMN firstname SET DEFAULT 'Arthur';" +
				"COMMIT;\n");

			builder = new TableSourceBuilder(con);
			Settings.getInstance().setProperty("workbench.db." + dbid + ".coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnChanger.PARAM_DEFAULT_VALUE + " " + ColumnChanger.PARAM_NULLABLE);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.indexOf("DEFAULT 'Arthur' NULL") > -1);

			builder = new TableSourceBuilder(con);
			Settings.getInstance().setProperty("workbench.db." + dbid + ".coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnChanger.PARAM_NULLABLE + " " + ColumnChanger.PARAM_DEFAULT_VALUE);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.indexOf("NULL DEFAULT 'Arthur'") > -1);

			builder = new TableSourceBuilder(con);
			Settings.getInstance().setProperty("workbench.db." + dbid + ".coldef", ColumnChanger.PARAM_DATATYPE + " " + ColumnDefinitionTemplate.PARAM_NOT_NULL + " " + ColumnChanger.PARAM_DEFAULT_VALUE);
			sql = builder.getTableSource(tbl, false, false);
//			System.out.println(sql);
			assertTrue(sql.indexOf("FIRSTNAME  VARCHAR(20)   DEFAULT 'Arthur'") > -1);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
			ConnectionMgr.getInstance().clearProfiles();
		}
	}

}
