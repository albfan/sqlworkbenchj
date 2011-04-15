/*
 * PostgresTypeReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import workbench.db.ColumnIdentifier;
import workbench.db.BaseObjectType;
import workbench.TestUtil;
import java.util.List;
import workbench.db.TableIdentifier;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.db.WbConnection;
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
			"CREATE TYPE address AS (city varchar(100), street varchar(50), zipcode varchar(10));\n" +
			"comment on type address is 'a single address';\n" +
			"comment on column address.city is 'the city';\n" +
			"commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testGetTypes()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		List<TableIdentifier> types = con.getMetadata().getObjectList(TEST_ID, new String[] { "TYPE" });
		assertNotNull(types);
		assertEquals(1, types.size());
		TableIdentifier tbl = types.get(0);
		assertEquals("address", tbl.getObjectName());
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
		assertTrue(sql.startsWith("CREATE TYPE address AS"));
		assertTrue(sql.contains("city "));
		assertTrue(sql.contains("street "));
		assertTrue(sql.contains("zipcode "));
		assertTrue(sql.contains("COMMENT ON TYPE address IS 'a single address'"));
		assertTrue(sql.contains("COMMENT ON COLUMN address.city IS 'the city'"));
	}


}
