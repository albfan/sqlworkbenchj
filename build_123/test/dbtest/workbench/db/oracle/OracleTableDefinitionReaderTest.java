/*
 * OracleTableDefinitionReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.oracle;

import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbObjectComparator;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTableDefinitionReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "orametadata";

	public OracleTableDefinitionReaderTest()
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
			"/\n" +
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
		assertNotNull("Oracle not available", con);

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
		assertEquals("Got wrong tables: " + tables.toString(), 3, tables.size());
		TableIdentifier tbl = tables.get(0);
		assertEquals("MV_PERSON", tbl.getTableName());
		assertEquals("MATERIALIZED VIEW", tbl.getType());

		TableIdentifier person = tables.get(1);
		assertEquals("PERSON", person.getTableName());
		assertEquals("person comment", person.getComment());

		List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(person);
		assertEquals("Got wrong columns: " + columns, 3, columns.size());
		ColumnIdentifier id = columns.get(0);
		assertEquals("ID", id.getColumnName());
		assertEquals("person id", id.getComment());

		String sql = tbl.getSource(con).toString().trim();
		assertTrue(sql.startsWith("CREATE MATERIALIZED VIEW MV_PERSON"));
		IndexReader r = con.getMetadata().getIndexReader();
		assertNotNull(r);
		assertTrue(r instanceof OracleIndexReader);
		OracleIndexReader reader = (OracleIndexReader)r;

		List<IndexDefinition> indexes = reader.getTableIndexList(person, false);
		assertEquals("Got wrong indexes: " + indexes, 3, indexes.size());

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
	public void testRowIDConverter()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);

		Settings.getInstance().setConvertOracleTypes(true);
		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			stmt.executeUpdate("INSERT INTO person (id, first_name, last_name) values (1, 'Arthur', 'Dent')");
			con.commit();

			DataStore ds = TestUtil.getQueryResult(con, "SELECT rowid, id FROM person");
			Object id = ds.getValue(0, 0);
			assertTrue(id instanceof String);
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	@Test
	public void testNumberColumns()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);
    try
    {
      TestUtil.executeScript(con,
        "create table number_test \n" +
        "(\n" +
        "  n_plain number, \n" +
        "  n_max number(38,2), \n" +
        "  n_star number(*,2), \n" +
        "  n_two number(10,2), \n" +
        "  n_minus number(10,-2), \n" +
        "  n_int integer\n" +
        ");");
      List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(new TableIdentifier("NUMBER_TEST"));
      assertNotNull(columns);
      assertEquals(6, columns.size());
      ColumnIdentifier plain = ColumnIdentifier.findColumnInList(columns, "N_PLAIN");
      assertEquals("NUMBER", plain.getDbmsType());

      ColumnIdentifier max = ColumnIdentifier.findColumnInList(columns, "N_MAX");
      assertEquals("NUMBER(38,2)", max.getDbmsType());

      ColumnIdentifier star = ColumnIdentifier.findColumnInList(columns, "N_STAR");
      assertEquals("NUMBER(*,2)", star.getDbmsType());

      ColumnIdentifier two = ColumnIdentifier.findColumnInList(columns, "N_TWO");
      assertEquals("NUMBER(10,2)", two.getDbmsType());

      ColumnIdentifier minus = ColumnIdentifier.findColumnInList(columns, "N_MINUS");
      assertEquals("NUMBER(10,-2)", minus.getDbmsType());

      ColumnIdentifier intcol = ColumnIdentifier.findColumnInList(columns, "N_INT");
      assertEquals("NUMBER", intcol.getDbmsType());
    }
    finally
    {
      TestUtil.executeScript(con, "drop table number_test;");
    }
	}

	@Test
	public void testStupidTimestamp()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);

		List<ColumnIdentifier> columns = con.getMetadata().getTableColumns(new TableIdentifier("TS_TEST"));
		assertNotNull(columns);
		assertEquals(1, columns.size());
		assertEquals(Types.TIMESTAMP, columns.get(0).getDataType());
	}

  @Test
  public void testIdentityColumns()
    throws Exception
  {
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		if (!JdbcUtils.hasMinimumServerVersion(con, "12.1"))
		{
			System.out.println("No Oracle 12 detected. Skipping test for identity columns");
			return;
		}

		try
		{
      String sql =
        "create table id_test\n" +
        "(\n" +
        "  id integer generated always as identity \n" +
        ");";

			TestUtil.executeScript(con, sql);
			TableDefinition def = con.getMetadata().getTableDefinition(new TableIdentifier("ID_TEST"));
			assertNotNull(def);
			List<ColumnIdentifier> columns = def.getColumns();
			assertNotNull(columns);
			assertEquals(1, columns.size());
			ColumnIdentifier id = columns.get(0);
			assertEquals("ID", id.getColumnName());
			assertEquals("GENERATED ALWAYS AS IDENTITY", id.getComputedColumnExpression());
		}
		finally
		{
			TestUtil.executeScript(con, "drop table id_test");
		}
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

		try
		{
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
		finally
		{
			TestUtil.executeScript(con, "drop table virtual_col_test");
		}
	}
}
