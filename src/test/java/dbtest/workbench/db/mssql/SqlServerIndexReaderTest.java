/*
 * SqlServerIndexReaderTest.java
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
package workbench.db.mssql;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerIndexReaderTest
	extends WbTestCase
{
	public SqlServerIndexReaderTest()
	{
		super("SqlServerIndexReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerProcedureReaderTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", conn);
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		Assume.assumeNotNull("No connection available", conn);
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@Test
	public void testReader()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		assertNotNull("No connection available", con);
		String sql =
				"create table foo \n" +
				"( \n" +
				"   id1 integer, \n" +
				"   id2 integer \n" +
				")\n"  +
			"create index ix_one on foo (id1) include (id2); " +
			"commit;\n";
		TestUtil.executeScript(con, sql);
		IndexReader reader = con.getMetadata().getIndexReader();
		TableIdentifier tbl = new TableIdentifier("dbo.foo");
		List<IndexDefinition> indexes = reader.getTableIndexList(tbl, false);
		assertEquals(1, indexes.size());
		IndexDefinition index = indexes.get(0);
		assertEquals("ix_one", index.getName());
		String source = reader.getIndexSource(tbl, index).toString();
		assertTrue(source.contains("INCLUDE (id2)"));

		List<IndexDefinition> indexList = reader.getIndexes(null, "dbo", null, null);
		assertNotNull(indexList);
		assertEquals(1, indexList.size());
		assertEquals("ix_one", indexList.get(0).getName());

		TestUtil.executeScript(con,
			"drop index ix_one on foo;\n" +
			"create unique index ix_two on foo (id1, id2) with (ignore_dup_key = on);\n " +
			"commit;\n");

		indexes = reader.getTableIndexList(tbl, false);
		assertEquals(1, indexes.size());
		IndexDefinition idx2 = indexes.get(0);
		assertEquals("ix_two", idx2.getName());
		source = TestUtil.cleanupSql(reader.getIndexSource(tbl, idx2));
		assertEquals("CREATE UNIQUE NONCLUSTERED INDEX ix_two ON dbo.foo (id1 ASC, id2 ASC) WITH (IGNORE_DUP_KEY = ON);", source);

		TestUtil.executeScript(con,
			"drop index ix_two on foo;\n" +
			"create unique index ix_three on foo (id1) where id2 > 0 with (fillfactor = 80, PAD_INDEX = on);\n " +
			"commit;\n");

		indexes = reader.getTableIndexList(tbl, false);
		assertEquals(1, indexes.size());
		IndexDefinition idx3 = indexes.get(0);
		assertEquals("ix_three", idx3.getName());
		source = TestUtil.cleanupSql(reader.getIndexSource(tbl, idx3));
		assertEquals("CREATE UNIQUE NONCLUSTERED INDEX ix_three ON dbo.foo (id1 ASC) WHERE ([id2]>(0)) WITH (FILLFACTOR = 80, PAD_INDEX = ON);", source);
	}
}