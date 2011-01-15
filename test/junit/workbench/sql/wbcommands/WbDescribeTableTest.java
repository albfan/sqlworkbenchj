/*
 * WbDescribeTableTest.java
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

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.console.DataStorePrinter;
import workbench.db.ConnectionMgr;
import workbench.db.IndexDefinition;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbDescribeTableTest;
import workbench.storage.DataStore;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDescribeTableTest
	extends WbTestCase
{

	public WbDescribeTableTest()
	{
		super("WbDescribeTableTest");
	}

	@After
	public void tearDown()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.prepareEnvironment();
		StatementRunner runner = util.createConnectedStatementRunner();
		String sql = "create table describe_test (nr integer, info_text varchar(100));";
		runner.runStatement(sql);
		StatementRunnerResult result = runner.getResult();
		assertEquals("Could not create table", true, result.isSuccess());

		sql = "-- show table definition\ndesc describe_test;";
		runner.runStatement(sql);
		result = runner.getResult();
		assertEquals("Describe failed", true, result.isSuccess());

		List<DataStore> data = result.getDataStores();
		assertNotNull("No description returned", data);
		assertEquals("No data returned", 1, data.size());
		assertEquals("Wrong number of rows returned", 2, data.get(0).getRowCount());

		sql = "-- show table definition\ndescribe \"DESCRIBE_TEST\"\n-- for table;";
		runner.runStatement(sql);
		result = runner.getResult();
		assertEquals("Describe failed", true, result.isSuccess());

		data = result.getDataStores();
		assertNotNull("No description returned", data);
		assertEquals("No data returned", 1, data.size());
		assertEquals("Wrong number of rows returned", 2, data.get(0).getRowCount());

		runner.runStatement("create index idx_nr on describe_test (nr);");

		sql = "describe describe_test;";
		runner.runStatement(sql);
		result = runner.getResult();
		assertEquals("Describe failed", true, result.isSuccess());

		data = result.getDataStores();
		assertNotNull(data);
		assertEquals("Not enough returned", 2, data.size());
		DataStore indexDs = data.get(1);
		DataStorePrinter p = new DataStorePrinter(indexDs);

		assertEquals(1, indexDs.getRowCount());
		assertEquals("IDX_NR", indexDs.getValue(0, "INDEX_NAME"));
		assertEquals("NO", indexDs.getValue(0, "UNIQUE"));
		Object o = indexDs.getValue(0, "DEFINITION");
		assertTrue(o instanceof IndexDefinition);
		String def = ((IndexDefinition) o).getExpression();
		assertEquals("NR ASC", def);

	}
}
