/*
 * WbGrepDataTest.java
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

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class WbGrepDataTest
	extends WbTestCase
{
	private WbConnection con;

	public WbGrepDataTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		TestUtil util = getTestUtil();
		con = util.getConnection();
		TestUtil.executeScript(con, "create table person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
			"insert into person values (1, 'Arthur', 'Dent');\n" +
			"insert into person values (2, 'Ford', 'Prefect');\n" +
			"commit;" +
			"create table address (nr integer, person_id integer, address_info varchar(100));" +
			"insert into address values (1, 1, 'Arthur''s Address');\n" +
			"insert into address values (2, 1, 'His old address');\n" +
			"insert into address values (3, 2, 'Ford''s Address');\n" +
			"commit;\n" +
			"create view v_person as select nr * 10, firstname, lastname from person;" +
			"commit;");
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		super.tearDown();
		ConnectionMgr.getInstance().disconnectAll();
	}

	public void testExecute()
		throws Exception
	{
		String sql = "WbGrepData -tables=person -searchValue=arthur";
		WbGrepData instance = new WbGrepData();
		instance.setConnection(con);
		StatementRunnerResult result = instance.execute(sql);
		assertTrue(result.isSuccess());
		List<DataStore> data = result.getDataStores();
		assertNotNull(data);
		assertEquals(1, data.size());
		assertEquals(1, data.get(0).getRowCount());
		assertEquals(1, data.get(0).getValueAsInt(0, 0, -1));

		sql = "WbGrepData -tables=person, address -searchValue=arthur";
		result = instance.execute(sql);
		assertTrue(result.isSuccess());
		data = result.getDataStores();
		assertNotNull(data);
		assertEquals(2, data.size());
		assertEquals(1, data.get(0).getRowCount());
		assertEquals(1, data.get(1).getRowCount());

		sql = "WbGrepData -tables=%person% -searchValue=arthur -types=table,view";
		result = instance.execute(sql);
		assertTrue(result.isSuccess());
		data = result.getDataStores();
		assertNotNull(data);
		assertEquals(2, data.size());
		assertEquals(1, data.get(0).getRowCount());
		assertEquals(1, data.get(1).getRowCount());

		sql = "WbGrepData -tables=%person% -searchValue=arthur -types=view";
		result = instance.execute(sql);
		assertTrue(result.isSuccess());
		data = result.getDataStores();
		assertNotNull(data);
		assertEquals(1, data.size());
		assertEquals(1, data.get(0).getRowCount());
		assertEquals(10, data.get(0).getValueAsInt(0, 0, -1));

		sql = "WbGrepData -searchValue=arthur -types=view";
		result = instance.execute(sql);
		assertTrue(result.isSuccess());
		data = result.getDataStores();
		assertNotNull(data);
		assertEquals(1, data.size());
		assertEquals(1, data.get(0).getRowCount());
		assertEquals(10, data.get(0).getValueAsInt(0, 0, -1));

	}

}
