/*
 * OracleIndexReaderTest.java
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

import java.util.Collections;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.DbObjectComparator;
import workbench.db.IndexDefinition;
import workbench.db.PkDefinition;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleIndexReaderTest
	extends WbTestCase
{

	public OracleIndexReaderTest()
	{
		super("OracleIndexReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		String sql =
			"create table some_table (id integer, some_data varchar(100));\n" +
			"create index aaa_upper on some_table (upper(some_data));\n" +
			"create index bbb_id on some_table(id) reverse;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetIndexList()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);
		TestUtil.executeScript(con,
			"create table foo (id integer);\n" +
			"create index zzz_foo on foo (id);");
		try
		{
			List<IndexDefinition> indexes = con.getMetadata().getIndexReader().getIndexes(null, OracleTestUtil.SCHEMA_NAME, null, null);
			assertEquals(3, indexes.size());
			assertEquals("AAA_UPPER", indexes.get(0).getName());
			assertEquals("BBB_ID", indexes.get(1).getName());
			assertEquals("ZZZ_FOO", indexes.get(2).getName());
		}
		finally
		{
			TestUtil.executeScript(con,"drop table foo;");
		}
	}

	@Test
	public void testCompress()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		try
		{
			TestUtil.executeScript(con,
				"create table foo (id1 integer, id2 integer, id3 integer);\n" +
				"create index c1 on foo (id1, id2) compress 1;\n" +
				"create index c2 on foo (id1, id2, id3) compress 2;\n"
			);
			TableIdentifier tbl = new TableIdentifier("FOO");
			List<IndexDefinition> indexList = con.getMetadata().getIndexReader().getTableIndexList(tbl, false);
			assertNotNull(indexList);
			assertEquals(2, indexList.size());
			IndexDefinition c1 = indexList.get(0);
			String source1 = c1.getSource(con).toString();
			assertTrue(source1.contains("COMPRESS 1"));

			IndexDefinition c2 = indexList.get(1);
			String source2 = c2.getSource(con).toString();
			assertTrue(source2.contains("COMPRESS 2"));
		}
		finally
		{
			TestUtil.executeScript(con,"drop table foo;");
		}
	}

	@Test
	public void testGetIndexSource()
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		try
		{
			TableIdentifier tbl = new TableIdentifier("SOME_TABLE");
			List<IndexDefinition> indexes = con.getMetadata().getIndexReader().getTableIndexList(tbl, false);

			// Make sure aaa_upper is the first index
			Collections.sort(indexes, new DbObjectComparator());

			// Make sure the built-in templates are used
			OracleUtils.setUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.index, false);

			assertEquals(2, indexes.size());
			IndexDefinition upper = indexes.get(0);
			String sql = upper.getSource(con).toString();
			assertTrue(sql.startsWith("CREATE INDEX AAA_UPPER"));
			assertTrue(sql.contains("UPPER(\"SOME_DATA"));

			IndexDefinition reverse = indexes.get(1);
			sql = reverse.getSource(con).toString();
			assertTrue(sql.startsWith("CREATE INDEX BBB_ID"));
			assertTrue(sql.contains("SOME_TABLE (ID"));
			assertTrue(sql.contains("REVERSE"));

			// Now use dbms_meta
			OracleUtils.setUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.index, true);

			sql = upper.getSource(con).toString();
			assertNotNull(sql);
			assertTrue(sql.contains("AAA_UPPER"));
			assertTrue(sql.contains("PCTFREE"));

			sql = reverse.getSource(con).toString();
			assertNotNull(sql);
			assertTrue(sql.contains("BBB_ID"));
			assertTrue(sql.contains("PCTFREE"));
			assertTrue(sql.contains("REVERSE"));
		}
		finally
		{
			// Reset the index retrieval
			OracleUtils.setUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.index, false);
		}
	}

	@Test
	public void testAlternatePKIndex()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		try
		{
			String sql =
				"CREATE TABLE index_test (id integer not null);\n" +
				"ALTER TABLE index_test ADD CONSTRAINT pk_t PRIMARY KEY (id) USING INDEX (CREATE UNIQUE INDEX UNIQUE_ID ON index_test (id));";
			TestUtil.executeScript(con, sql);

			TableDefinition tbl = con.getMetadata().getTableDefinition(new TableIdentifier("INDEX_TEST"));
			List<IndexDefinition> idx = con.getMetadata().getIndexReader().getTableIndexList(tbl.getTable(), false);
			assertEquals(1, idx.size());
			IndexDefinition def = idx.get(0);
			assertTrue(def.isPrimaryKeyIndex());
			assertEquals("UNIQUE_ID", def.getName());
			assertEquals("PK_T", tbl.getTable().getPrimaryKeyName());

			TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(con);
			PkDefinition pk = new PkDefinition(CollectionUtil.arrayList("ID"));
			pk.setPkName("PK_T");
			pk.setPkIndexName("UNIQUE_ID");
			String pkSource = builder.getPkSource(tbl.getTable(), pk, false, false).toString();
//			System.out.println(pkSource);
			assertTrue(pkSource.indexOf("USING INDEX") > -1);
			assertTrue(pkSource.indexOf("CREATE UNIQUE INDEX UNIQUE_ID") > -1);
		}
		finally
		{
			TestUtil.executeScript(con, "drop table index_test;");
		}
	}

}
