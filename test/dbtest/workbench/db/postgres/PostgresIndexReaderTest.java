/*
 * PostgresIndexReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.SQLException;
import java.util.Collection;
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
		WbConnection conn = TestUtil.getPostgresConnection();
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
		Collection<IndexDefinition> indexes = reader.getTableIndexList(table);
		IndexDefinition index = indexes.iterator().next();
		assertEquals("idx_person_id", index.getObjectName());
		String sql = index.getSource(conn).toString();
		String type = SqlUtil.getCreateType(sql);
		assertEquals("INDEX", type);
		assertTrue(sql.contains("idx_person_id"));
		assertTrue(sql.contains("(id)"));
	}

}
