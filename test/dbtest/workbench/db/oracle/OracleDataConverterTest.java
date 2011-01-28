/*
 * OracleDataConverterTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.Types;
import java.sql.ResultSet;
import java.sql.Statement;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDataConverterTest
	extends WbTestCase
{

	public OracleDataConverterTest()
	{
		super("OracleDataConverterTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql = 
			"create table some_table (id integer, some_data raw(200));\n" +
			"insert into some_table (id, some_data) values (42, utl_raw.cast_to_raw('0123'));\n" +
		  "commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testConvertValue()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		String select = "SELECT rowid, some_data FROM some_table";
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(select);
		OracleDataConverter converter = OracleDataConverter.getInstance();
		assertNotNull(converter);
		if (rs.next())
		{
			Object rowid = rs.getObject(1);
			Object convertedId = converter.convertValue(Types.ROWID, "ROWID", rowid);
			assertNotNull(convertedId);
			assertTrue(convertedId instanceof String);

			Object raw = rs.getObject(2);
			Object convertedRaw = converter.convertValue(Types.VARBINARY, "RAW", raw);
			assertNotNull(convertedId);
			assertTrue(convertedId instanceof String);
			assertEquals("30313233", convertedRaw);
		}
	}
}
