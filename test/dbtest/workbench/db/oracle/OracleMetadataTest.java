/*
 * OracleMetadataTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.Types;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.storage.DataStore;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleMetadataTest
	extends WbTestCase
{
	private static final String TEST_ID = "orametadata";

	public OracleMetadataTest()
	{
		super(TEST_ID);
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql = "create table person (id integer, first_name varchar(100), last_name varchar(100), check (id > 0));\n" +
			"create view v_person (id, full_name) as select id, first_name || ' ' || last_name from person;\n" +
			"create synonym syn_person for person;\n" +
			"create materialized view mv_person as select * from person;\n" +
			"create type address_type as object (street varchar(100), city varchar(50), zipcode varchar(10));\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testRetrieveObjects()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		List<TableIdentifier> views = con.getMetadata().getObjectList("WBJUNIT", new String[] { "VIEW" });
		assertEquals(1, views.size());
		TableIdentifier v = views.get(0);
		assertEquals("VIEW", v.getType());
		assertEquals("V_PERSON", v.getTableName());

		List<TableIdentifier> syns = con.getMetadata().getObjectList("WBJUNIT", new String[] { "SYNONYM" });
		assertEquals(1, syns.size());
		TableIdentifier syn = syns.get(0);
		assertEquals("SYNONYM", syn.getType());
		assertEquals("SYN_PERSON", syn.getTableName());

		List<TableIdentifier> types = con.getMetadata().getObjectList("WBJUNIT", new String[] { "TYPE" });
		assertEquals(1, types.size());
		TableIdentifier type = types.get(0);
		assertEquals("TYPE", type.getType());

		List<TableIdentifier> tables = con.getMetadata().getObjectList("WBJUNIT", new String[] { "TABLE" });
		assertEquals(2, tables.size());
		TableIdentifier tbl = tables.get(0);
		assertEquals("MV_PERSON", tbl.getTableName());
		assertEquals("MATERIALIZED VIEW", tbl.getType());

		String sql = tbl.getSource(con).toString().trim();
		assertTrue(sql.startsWith("CREATE MATERIALIZED VIEW MV_PERSON"));
	}

	@Test
	public void testRowIDConverter()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		Settings.getInstance().setConvertOracleTypes(true);
		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			stmt.executeUpdate("INSERT INTO person (id, first_name, last_name) values (1, 'Arthur', 'Dent')");
			con.commit();
			ResultSet rs = stmt.executeQuery("SELECT rowid, id FROM person");
			DataStore ds = new DataStore(rs.getMetaData(), con);
			ds.initData(rs);
			rs.close();
			Object id = ds.getValue(0, 0);
			assertTrue(id instanceof String);
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	@Test
	public void testGetSqlTypeDisplay()
	{
		// Test with BYTE as default semantics
		OracleMetadata meta = new OracleMetadata(OracleMetadata.BYTE_SEMANTICS, false);

		// Test non-Varchar types
		assertEquals("CLOB", meta.getSqlTypeDisplay("CLOB", Types.CLOB, -1, -1, 0));
		assertEquals("NVARCHAR(300)", meta.getSqlTypeDisplay("NVARCHAR", Types.VARCHAR, 300, -1, 0));
		assertEquals("CHAR(5)", meta.getSqlTypeDisplay("CHAR", Types.CHAR, 5, -1, 0));
		assertEquals("NUMBER(10,2)", meta.getSqlTypeDisplay("NUMBER", Types.NUMERIC, 10, 2, 0));

		String display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200)", display);

		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200 Char)", display);

		meta = new OracleMetadata(OracleMetadata.CHAR_SEMANTICS, false);

		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200 Byte)", display);

		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200)", display);

		meta = new OracleMetadata(OracleMetadata.CHAR_SEMANTICS, true);

		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200 Byte)", display);

		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200 Char)", display);

	}
}
