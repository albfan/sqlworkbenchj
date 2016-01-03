/*
 * PostgresTypeReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.BaseObjectType;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTypeReaderTest
	extends WbTestCase
{

	private static final String TEST_ID = "typereader";

	public PostgresTypeReaderTest()
	{
		super(TEST_ID);
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		String sql =
			"CREATE TYPE address_type AS (city varchar(100), street varchar(50), zipcode varchar(10));\n" +
			"comment on type address_type is 'a single address';\n" +
			"comment on column address_type.city is 'the city';\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetTypes()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

		List<TableIdentifier> types = con.getMetadata().getObjectList(TEST_ID, new String[] { "TYPE" });
		assertNotNull(types);
		assertEquals(1, types.size());
		TableIdentifier tbl = types.get(0);
		assertEquals("address_type", tbl.getObjectName());
		assertEquals("TYPE", tbl.getType());
//		assertEquals("a single address", tbl.getComment());

		PostgresTypeReader reader = new PostgresTypeReader();
		BaseObjectType type = reader.getObjectDefinition(con, tbl);
		assertNotNull(type);
		List<ColumnIdentifier> cols = type.getAttributes();
		assertNotNull(cols);
		assertEquals(3, cols.size());
		assertEquals("TYPE", type.getObjectType());
		assertEquals("a single address", type.getComment());

		ColumnIdentifier city = cols.get(0);
		assertEquals("city", city.getColumnName());
		assertEquals("varchar(100)", city.getDbmsType());
		assertEquals("the city", city.getComment());

		ColumnIdentifier street = cols.get(1);
		assertEquals("street", street.getColumnName());
		assertEquals("varchar(50)", street.getDbmsType());

		String sql = reader.getObjectSource(con, type);
		assertTrue(sql.startsWith("CREATE TYPE address_type AS"));
		assertTrue(sql.contains("city "));
		assertTrue(sql.contains("street "));
		assertTrue(sql.contains("zipcode "));
		assertTrue(sql.contains("COMMENT ON TYPE address_type IS 'a single address'"));
		assertTrue(sql.contains("COMMENT ON COLUMN address_type.city IS 'the city'"));
	}

}
