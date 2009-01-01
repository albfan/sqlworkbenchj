/*
 * TableSourceBuilderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import junit.framework.TestCase;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableSourceBuilderTest
	extends TestCase
{

	public TableSourceBuilderTest(String testName)
	{
		super(testName);
	}

	public void testGetTableSource()
		throws Exception
	{
		TestUtil util = new TestUtil("TableSourceBuilder");
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
			System.out.println(sql);
			assertTrue(sql.startsWith("CREATE TABLE PERSON"));
			assertTrue(sql.indexOf("PRIMARY KEY (ID)") > -1);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
			ConnectionMgr.getInstance().clearProfiles();
		}
	}

}
