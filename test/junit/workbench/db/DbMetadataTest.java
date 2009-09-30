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
			TestUtil.executeScript(con, "create table \"MyTest\" (id integer);\ncommit;");

			DbMetadata meta = con.getMetadata();
			TableIdentifier tbl = meta.findObject(new TableIdentifier("\"MyTest\""));
			assertNotNull(tbl);
			assertTrue(tbl.getNeverAdjustCase());
			TableDefinition def = meta.getTableDefinition(tbl);
			assertNotNull(tbl);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
