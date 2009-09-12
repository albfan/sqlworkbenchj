/*
 * DbObjectChangerTest.java
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

import workbench.TestUtil;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectChangerTest
	extends WbTestCase
{

	public DbObjectChangerTest(String testName)
	{
		super(testName);
	}

	public void testGetDropPK_H2()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			WbConnection con = util.getConnection();
			String sql = "create table person (nr integer primary key, firstname varchar(100));\n" +
				"commit;\n";
			TestUtil.executeScript(con, sql);
			TableIdentifier table = new TableIdentifier("PERSON");
			table.setType("TABLE");
			DbObjectChanger changer = new DbObjectChanger(con);
			String drop = changer.getDropPK(table);
//			System.out.println(drop);
			TestUtil.executeScript(con, drop);
			TableDefinition tbl = con.getMetadata().getTableDefinition(table);
			assertNull(tbl.getTable().getPrimaryKeyName());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
