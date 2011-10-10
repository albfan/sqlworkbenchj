/*
 * OracleMetadataTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import workbench.db.ColumnIdentifier;
import java.sql.Types;
import java.util.Collections;
import workbench.db.IndexColumn;
import workbench.db.IndexReader;
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
import workbench.db.DbObjectComparator;
import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;
import workbench.db.*;

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

		String sql = "create table person (id integer primary key, first_name varchar(100), last_name varchar(100), check (id > 0));\n" +
			"create view v_person (id, full_name) as select id, first_name || ' ' || last_name from person;\n" +
			"create synonym syn_person for person;\n" +
			"create materialized view mv_person as select * from person;\n" +
			"create type address_type as object (street varchar(100), city varchar(50), zipcode varchar(10));\n" +
			"create index idx_person_a on person (upper(first_name));\n" +
		  "create index idx_person_b on person (last_name) reverse;\n" +
			"create table ts_test (modified_at timestamp(3));\n" +
			"comment on table person is 'person comment';\n" +
			"comment on column person.id is 'person id';\n";
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
		assertEquals(3, tables.size());
		TableIdentifier tbl = tables.get(0);
		assertEquals("MV_PERSON", tbl.getTableName());
		assertEquals("MATERIALIZED VIEW", tbl.getType());

		TableIdentifier person = tables.get(1);
		assertEquals("PERSON", person.getTableName());
		assertEquals("person comment", person.getComment());

		List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(person);
		assertEquals(3, columns.size());
		ColumnIdentifier id = columns.get(0);
		assertEquals("ID", id.getColumnName());
		assertEquals("person id", id.getComment());

		String sql = tbl.getSource(con).toString().trim();
		assertTrue(sql.startsWith("CREATE OR REPLACE MATERIALIZED VIEW MV_PERSON"));
		IndexReader r = con.getMetadata().getIndexReader();
		assertNotNull(r);
		assertTrue(r instanceof OracleIndexReader);
		OracleIndexReader reader = (OracleIndexReader)r;

		List<IndexDefinition> indexes = reader.getTableIndexList(person);
		assertEquals(3, indexes.size());

		Collections.sort(indexes, new DbObjectComparator());

		IndexDefinition functionIndex = indexes.get(0);
		IndexDefinition reverse = indexes.get(1);
		IndexDefinition pk = indexes.get(2);

		List<IndexColumn> cols = functionIndex.getColumns();
		assertEquals(1, cols.size());
		String expr = cols.get(0).getExpression();
		assertEquals("UPPER(\"FIRST_NAME\") ASC", expr);

		assertEquals("NORMAL/REV", reverse.getIndexType());
		String source = reverse.getSource(con).toString();
		assertTrue(source.trim().endsWith("REVERSE;"));

		assertTrue(pk.isUnique());
		assertTrue(pk.isPrimaryKeyIndex());
	}

	@Test
	public void testObjectCompiler()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		OracleObjectCompiler compiler = new OracleObjectCompiler(con);
		TableIdentifier tbl = con.getMetadata().findObject(new TableIdentifier("V_PERSON"));
		assertTrue(OracleObjectCompiler.canCompile(tbl));
		String error = compiler.compileObject(tbl);
		assertNull(error);

		TableIdentifier mv = con.getMetadata().findObject(new TableIdentifier("MV_PERSON"));
		assertTrue(OracleObjectCompiler.canCompile(mv));
		error = compiler.compileObject(mv);
		assertNull(error);
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

	@Test
	public void testStupidTimestamp()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(new TableIdentifier("TS_TEST"));
		assertNotNull(columns);
		assertEquals(1, columns.size());
		assertEquals(Types.TIMESTAMP, columns.get(0).getDataType());
	}

	@Test
	public void testVirtualColumns()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		if (!JdbcUtils.hasMinimumServerVersion(con, "11.1"))
		{
			System.out.println("No Oracle 11 detected. Skipping test for virtual columns");
			return;
		}

		String script =
			"create table virtual_col_test (some_name varchar(100), lower_name generated always as (lower(some_name)));";
		TestUtil.executeScript(con, script);
		TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("VIRTUAL_COL_TEST"));
		assertNotNull(def);
		List<ColumnIdentifier> columns = def.getColumns();
		assertNotNull(columns);
		assertEquals(2, columns.size());
		ColumnIdentifier lower = columns.get(1);
		assertEquals("LOWER_NAME", lower.getColumnName());
		assertEquals("GENERATED ALWAYS AS (LOWER(\"SOME_NAME\"))", lower.getComputedColumnExpression());
	}
}
