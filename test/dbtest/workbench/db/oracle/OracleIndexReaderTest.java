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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.IndexDefinition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.db.DbObjectComparator;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleIndexReaderTest
{

	public OracleIndexReaderTest()
	{
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
		TableIdentifier tbl = new TableIdentifier("SOME_TABLE");
		List<IndexDefinition> indexes = new ArrayList<IndexDefinition>(con.getMetadata().getIndexReader().getTableIndexList(tbl));
		
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

}
