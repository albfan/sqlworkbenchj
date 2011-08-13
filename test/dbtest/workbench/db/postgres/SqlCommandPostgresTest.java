/*
 * PostgresSqlCommandTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.util.List;

import org.junit.*;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlCommandPostgresTest
	extends WbTestCase
{

	private static final String TEST_ID = "pgSqlCommand";

	public SqlCommandPostgresTest()
	{
		super("PostgresSqlCommandTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"create table person (id integer, firstname varchar(100), lastname varchar(100));\n" +
			"commit;\n"
		);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Before
	public void setUpTest()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"insert into person (id, firstname, lastname) values (1, 'Arthur', 'Dent');\n" +
			"insert into person (id, firstname, lastname) values (2, 'Zaphod', 'Beeblebrox');\n" +
			"commit;\n"
		);
	}

	@After
	public void cleanupTest()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"truncate table person;\n" +
			"commit;\n"
		);
	}

	@Test
	public void testSelect()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		String sql = "select * from person order by id;";
		StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);

		runner.runStatement(sql);
		StatementRunnerResult result = runner.getResult();

		assertTrue(result.isSuccess());
		assertTrue(result.hasDataStores());
		List<DataStore> data = result.getDataStores();
		assertEquals(1, data.size());
		DataStore person = data.get(0);
		assertEquals(2, person.getRowCount());
		assertEquals(3, person.getColumnCount());
		assertEquals(1, person.getValueAsInt(0, 0, -1));
		assertEquals("Arthur", person.getValueAsString(0, "firstname"));
		assertEquals("Dent", person.getValueAsString(0, "lastname"));

		assertEquals(2, person.getValueAsInt(1, 0, -1));
		assertEquals("Zaphod", person.getValueAsString(1, 1));
		assertEquals("Beeblebrox", person.getValueAsString(1, "lastname"));
	}

	@Test
	public void testDeleteReturning()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		String sql = "delete from person where id = 1 returning *;";

		StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);

		runner.runStatement(sql);
		StatementRunnerResult result = runner.getResult();

		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());

		assertTrue(result.hasDataStores());
		List<DataStore> data = result.getDataStores();
		assertEquals(1, data.size());
		DataStore person = data.get(0);
		assertEquals(1, person.getRowCount());
		assertEquals(3, person.getColumnCount());
		assertEquals(1, person.getValueAsInt(0, 0, -1));
		assertEquals("Arthur", person.getValueAsString(0, "firstname"));
		assertEquals("Dent", person.getValueAsString(0, "lastname"));
	}

}
