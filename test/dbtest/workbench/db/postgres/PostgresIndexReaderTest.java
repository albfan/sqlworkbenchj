/*
 * PostgresIndexReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
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
import workbench.storage.DataStore;
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
		PostgresTestUtil.cleanUpTestCase(TESTID);
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

		System.out.println(sql);
		assertEquals("INDEX", type);
		assertTrue(sql.contains("idx_person_id"));
		assertTrue(sql.contains("(id"));

		DataStore indexDS = conn.getMetadata().getIndexReader().getTableIndexInformation(table);
		String sql2 = reader.getIndexSource(table, indexDS, null).toString();

		System.out.println(sql2);

	}



}
