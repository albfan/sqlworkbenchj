/*
 * OracleIndexReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import workbench.WbTestCase;
import workbench.util.CollectionUtil;
import java.util.Collections;
import java.util.List;
import workbench.db.IndexDefinition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.db.DbObjectComparator;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;
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
	public void testGetIndexSource()
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		try
		{
			TableIdentifier tbl = new TableIdentifier("SOME_TABLE");
			List<IndexDefinition> indexes = con.getMetadata().getIndexReader().getTableIndexList(tbl);

			// Make sure aaa_upper is the first index
			Collections.sort(indexes, new DbObjectComparator());

			// Make sure the built-in templates are used
			con.getMetadata().getDbSettings().setUseOracleDBMSMeta("index", false);

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
			con.getMetadata().getDbSettings().setUseOracleDBMSMeta("index", true);

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
			con.getMetadata().getDbSettings().setUseOracleDBMSMeta("index", false);
		}
	}

	@Test
	public void testAlternatePKIndex()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		String sql =
			"CREATE TABLE index_test (id integer not null);\n" +
			"ALTER TABLE index_test ADD CONSTRAINT pk_t PRIMARY KEY (id) USING INDEX (CREATE UNIQUE INDEX UNIQUE_ID ON index_test (id));";
		TestUtil.executeScript(con, sql);

		TableDefinition tbl = con.getMetadata().getTableDefinition(new TableIdentifier("INDEX_TEST"));
		List<IndexDefinition> idx = con.getMetadata().getIndexReader().getTableIndexList(tbl.getTable());
		assertEquals(1, idx.size());
		IndexDefinition def = idx.get(0);
		assertTrue(def.isPrimaryKeyIndex());
		assertEquals("UNIQUE_ID", def.getName());
		assertEquals("PK_T", tbl.getTable().getPrimaryKeyName());

		TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(con);
		String pkSource = builder.getPkSource(tbl.getTable(), CollectionUtil.arrayList("ID"), "PK_T").toString();
		assertTrue(pkSource.indexOf("USING INDEX") > -1);
		assertTrue(pkSource.indexOf("CREATE UNIQUE INDEX " + OracleTestUtil.SCHEMA_NAME + ".UNIQUE_ID") > -1);
	}

}
