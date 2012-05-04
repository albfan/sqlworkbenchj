/*
 * PostgresIndexReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.SQLException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.ScriptParser;
import workbench.util.SqlUtil;

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

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TESTID);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetIndexSource()
		throws SQLException
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null)
		{
			System.out.println("No local postgres connection. Skipping test...");
			return;
		}

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
		if (indexes.isEmpty())
		{
			System.err.println("No indexes returned. If you are running PostgreSQL 9.0.0 please upgrade to 9.0.1");
		}
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
	public void testGetIndexSource2()
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
		if (conn == null)
		{
			System.out.println("No local postgres connection. Skipping test...");
			return;
		}

		DbMetadata meta = conn.getMetadata();
		IndexReader reader = meta.getIndexReader();

		assertNotNull(reader);
		assertTrue(reader instanceof PostgresIndexReader);

		TestUtil.executeScript(conn, sql);

		TableIdentifier table = meta.findTable(new TableIdentifier("films"));
		List<IndexDefinition> indexes = reader.getTableIndexList(table);
		assertEquals(3, indexes.size());

		String source = table.getSource(conn).toString();
		ScriptParser p = new ScriptParser(source);
		assertEquals(4, p.getSize());
		String alter = p.getCommand(1);
		assertTrue(alter.startsWith("ALTER TABLE films"));
		String idx1 = p.getCommand(2);
		assertEquals("CREATE INDEX title_idx_nulls_low ON films USING btree (title NULLS FIRST)", idx1);
		String idx2 = p.getCommand(3);
		assertEquals("CREATE INDEX lower_title_idx ON films USING btree (lower((title)::text))", idx2);
	}

}
