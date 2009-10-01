/*
 * DbMetadataTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class DbMetadataTest
	extends WbTestCase
{

	public DbMetadataTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		super.tearDown();
	}

	public void testGetTableDefinition()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			WbConnection con = util.getConnection();
			TestUtil.executeScript(con, "create table \"MyTest\" (id integer);\n" +
				"create table person (id integer primary key, firstname varchar(20));\n" +
				"commit;");

			DbMetadata meta = con.getMetadata();
			TableIdentifier tbl = meta.findObject(new TableIdentifier("\"MyTest\""));
			assertNotNull(tbl);
			assertTrue(tbl.getNeverAdjustCase());
			assertEquals("MyTest", tbl.getTableName());

			TableDefinition def = meta.getTableDefinition(tbl);
			assertNotNull(def);
			assertNotNull(def.getTable());
			assertEquals("MyTest", def.getTable().getTableName());

			tbl = meta.findObject(new TableIdentifier("MyTest"), false);
			assertNotNull(tbl);
			assertEquals("MyTest", tbl.getTableName());

			TableIdentifier tbl2 = meta.findObject(new TableIdentifier("Person"));
			assertNotNull(tbl2);
			assertEquals("PERSON", tbl2.getTableName());

			List<TableIdentifier> tables = meta.getTableList();
			assertNotNull(tables);
			assertEquals(2, tables.size());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
