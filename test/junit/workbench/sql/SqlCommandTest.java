/*
 * SqlCommandTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;
import java.util.List;
import org.junit.AfterClass;

import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;import org.junit.BeforeClass;
import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.storage.DataStore;


/**
 *
 * @author Thomas Kellerer
 */
public class SqlCommandTest
	extends WbTestCase
{

	public SqlCommandTest()
	{
		super("SqlCommandTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();

		StatementRunner runner = util.createConnectedStatementRunner();
		WbConnection con = runner.getConnection();

		TestUtil.executeScript(con,
			"create table person (id integer, firstname varchar(100), lastname varchar(100));\n" +
			"insert into person (id, firstname, lastname) values (1, 'Arthur', 'Dent');\n" +
			"insert into person (id, firstname, lastname) values (2, 'Zaphod', 'Beeblebrox');\n" +
			"commit;\n"
		);

		String sql = "select * from person order by id;";
		SqlCommand command = new SqlCommand();
		command.setConnection(con);
		command.setStatementRunner(runner);
		
		StatementRunnerResult result = command.execute(sql);

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
}
