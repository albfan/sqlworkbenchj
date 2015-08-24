/*
 * PostgresIndexReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import java.sql.SQLException;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.parser.ScriptParser;

import workbench.util.SqlUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import workbench.db.ReaderFactory;
import workbench.db.UniqueConstraintReader;
import workbench.sql.parser.ParserType;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresIndexReaderTest
	extends WbTestCase
{
	private static final String TESTID = "indexreader";

	public PostgresIndexReaderTest()
	{
		super("PostgresIndexReaderTest");
	}

	@Before
	public void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TESTID);
	}

	@After
	public void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetIndexList()
		throws Exception
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
    assertNotNull(conn);

		String sql =
			"CREATE TABLE foo \n" +
			"( \n" +
			"   id integer not null primary key, \n" +
			"   code varchar(40) not null, \n" +
			"   name text not null, \n" +
			"   some_date date \n" +
			"); \n" +
			" \n" +
			"CREATE unique INDEX foo_one ON foo (code); \n" +
			"CREATE INDEX name_lower ON foo (lower((name)::text)); \n" +
			" \n" +
			"COMMIT;";

		TestUtil.executeScript(conn, sql);

		DbMetadata meta = conn.getMetadata();
		IndexReader reader = meta.getIndexReader();

		assertTrue(reader.supportsIndexList());
		List<IndexDefinition> indexList = reader.getIndexes(null, TESTID, null, null);
		assertNotNull(indexList);
		assertEquals(3, indexList.size());
	}

	@Test
	public void testGetIndexSource()
		throws SQLException
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		assertNotNull(conn);

		DbMetadata meta = conn.getMetadata();
		IndexReader reader = meta.getIndexReader();

		assertNotNull(reader);
		assertTrue(reader instanceof PostgresIndexReader);

		TestUtil.executeScript(conn,
			"create table person (id integer, firstname varchar(50), lastname varchar(50));\n" +
			"create index idx_person_id on person (id);\n" +
			"alter table person add constraint uq_firstname unique (firstname);\n" +
			"commit;\n");

		TableIdentifier table = meta.findTable(new TableIdentifier("person"));
		List<IndexDefinition> indexes = reader.getTableIndexList(table);
    UniqueConstraintReader uniqueReader = ReaderFactory.getUniqueConstraintReader(conn);
    uniqueReader.readUniqueConstraints(indexes, conn);

		assertFalse(indexes.isEmpty());

		IndexDefinition index = indexes.get(0);
		assertEquals("idx_person_id", index.getObjectName());
		String sql = index.getSource(conn).toString();
		String type = SqlUtil.getCreateType(sql);

//		System.out.println(sql);
		assertEquals("INDEX", type);
		assertTrue(sql.contains("idx_person_id"));
		assertTrue(sql.contains("(id"));

		index = indexes.get(1);
		assertEquals("uq_firstname", index.getObjectName());
		sql = index.getSource(conn).toString();
		assertTrue(sql.startsWith("ALTER TABLE"));
	}

	@Test
	public void testTableSourceWithIndexes()
		throws Exception
	{
		String sql =
			"CREATE TABLE films \n" +
			"( \n" +
			"   code       char(5), \n" +
			"   title      varchar(40), \n" +
			"   did        integer, \n" +
			"   date_prod  date, \n" +
			"   kind       varchar(10), \n" +
			"   len        interval \n" +
			"); \n" +
			" \n" +
			"ALTER TABLE films \n" +
			"   ADD CONSTRAINT production UNIQUE (date_prod); \n" +
			"CREATE INDEX title_idx_nulls_low ON films USING btree (title NULLS FIRST); \n" +
			"CREATE INDEX lower_title_idx ON films USING btree (lower((title)::text)); \n" +
			" \n" +
			"COMMIT;";

		WbConnection conn = PostgresTestUtil.getPostgresConnection();
    assertNotNull(conn);

		DbMetadata meta = conn.getMetadata();
		IndexReader reader = meta.getIndexReader();

		assertNotNull(reader);
		assertTrue(reader instanceof PostgresIndexReader);

		TestUtil.executeScript(conn, sql);

		TableIdentifier table = meta.findTable(new TableIdentifier("films"));
		List<IndexDefinition> indexes = reader.getTableIndexList(table);
		assertEquals(3, indexes.size());

		String source = table.getSource(conn).toString();
//		System.out.println("***** \n" + source);
		ScriptParser p = new ScriptParser(source, ParserType.Postgres);
		assertEquals(4, p.getSize());
		String alter = p.getCommand(1);
		assertTrue(alter.startsWith("ALTER TABLE films"));
		for (int i=2; i <=3; i++)
		{
			String idx = p.getCommand(2);
			if (idx.contains("title_idx_nulls_low"))
			{
				assertEquals("CREATE INDEX title_idx_nulls_low ON films USING btree (title NULLS FIRST)", idx);
			}
			else if (idx.contains("lower_title_idx"))
			{
				assertEquals("CREATE INDEX lower_title_idx ON films USING btree (lower((title)::text))", idx);
			}
		}
	}

}
