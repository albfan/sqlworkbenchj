/*
 * PostgresDataTypeResolverTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.Types;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresResolveDataTypesTest
	extends WbTestCase
{
		private static final String TEST_SCHEMA = "typeresolver";

	public PostgresResolveDataTypesTest()
	{
		super("PostgresDataTypeResolverTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_SCHEMA);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con, "create table some_table (simple_bit bit, three_bits bit(3), bit_var bit varying(8), some_flag boolean);\n" +
			"commit;\n");

	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_SCHEMA);
	}

	@Test
	public void testSomeMethod()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TableDefinition tbl = con.getMetadata().getTableDefinition(new TableIdentifier(TEST_SCHEMA, "some_table"));
		assertNotNull(tbl);
		List<ColumnIdentifier> cols = tbl.getColumns();
		assertEquals(4, cols.size());
		ColumnIdentifier simple = cols.get(0);
		assertEquals("bit(1)", simple.getDbmsType());
		assertEquals(Types.BIT, simple.getDataType());

		ColumnIdentifier three = cols.get(1);
		assertEquals("bit(3)", three.getDbmsType());
		assertEquals(Types.BIT, simple.getDataType());

		ColumnIdentifier varBits = cols.get(2);
		assertEquals("bit varying(8)", varBits.getDbmsType());

		ColumnIdentifier flag = cols.get(3);
		assertEquals("boolean", flag.getDbmsType());
		assertEquals(Types.BOOLEAN, flag.getDataType());
	}
}
