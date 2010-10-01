/*
 * OracleSynonymReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import workbench.WbTestCase;
import java.util.List;
import workbench.db.TableIdentifier;
import java.util.Collection;
import workbench.db.SynonymReader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleSynonymReaderTest
	extends WbTestCase
{

	public OracleSynonymReaderTest()
	{
		super("OracleSynonymReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"CREATE TABLE person (id integer, firstname varchar(50), lastname varchar(50));\n" +
			"CREATE SYNONYM s_person FOR person;");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}


	@Test
	public void testGetSynonymList()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		SynonymReader reader = con.getMetadata().getSynonymReader();
		assertNotNull(reader);
		Collection<String> types = con.getMetadata().getObjectTypes();
		assertTrue(types.contains("SYNONYM"));
		List<TableIdentifier> objects = con.getMetadata().getObjectList(null, new String[] { "SYNONYM"});
		assertNotNull(objects);
		assertEquals(1, objects.size());
		TableIdentifier syn = objects.get(0);
		assertEquals("SYNONYM", syn.getObjectType());
		TableIdentifier table = con.getMetadata().getSynonymTable(syn);
		assertNotNull(table);
		assertEquals("PERSON", table.getTableName());
		String sql = reader.getSynonymSource(con, syn.getSchema(), syn.getTableName());
//		System.out.println(sql);
		String expected = "CREATE SYNONYM S_PERSON\n   FOR WBJUNIT.PERSON;";
		assertEquals(expected, sql.trim());
	}

}
