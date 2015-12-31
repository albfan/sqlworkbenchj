/*
 * PostgresResolveDataTypesTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Statement;
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
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.util.SqlUtil;
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

		TestUtil.executeScript(con,
			"create table some_table (simple_bit bit, three_bits bit(3), bit_var bit varying(8), some_flag boolean);\n" +
			"insert into some_table (simple_bit, three_bits, bit_var, some_flag) " +
			" values " +
			"('1', '101', '101010', false);\n" +
			"commit;\n");

	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testBitRetrieval()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

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

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT simple_bit, three_bits, bit_var FROM some_table");
			DataStore ds = new DataStore(rs, con, true);
			ResultInfo info = ds.getResultInfo();
			assertEquals(3, info.getColumnCount());

			simple = info.getColumn(0);
			assertEquals("bit(1)", simple.getDbmsType());
			assertEquals(Types.BIT, simple.getDataType());

			three = info.getColumn(1);
			assertEquals("bit(3)", three.getDbmsType());
			assertEquals(Types.BIT, simple.getDataType());
			assertEquals("java.lang.String", simple.getColumnClassName());
			Object o = ds.getValue(0, 1);
			assertEquals("101", o);

			varBits = info.getColumn(2);
			assertEquals("bit varying(8)", varBits.getDbmsType());
			Object o2 = ds.getValue(0, 2);
			assertEquals("101010", o2.toString());
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}
}
