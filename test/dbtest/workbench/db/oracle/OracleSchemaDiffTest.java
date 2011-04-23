/*
 * OracleSchemaDiffTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;
import workbench.util.FileUtil;
import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class OracleSchemaDiffTest
	extends WbTestCase
{

	public OracleSchemaDiffTest()
	{
		super("OracleSchemaDiffTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con1 = OracleTestUtil.getOracleConnection();
		if (con1 == null) return;
		String sql1 =
			"create table person (\n" +
			"   id integer not null primary key, " +
			"   firstname varchar(100), " +
			"   lastname varchar(100) not null\n" +
			");\n " +
			"create materialized view v_person as \n" +
			"select id, firstname, lastname from person;\n";

		TestUtil.executeScript(con1, sql1);

		WbConnection con2 = OracleTestUtil.getOracleConnection2();
		OracleTestUtil.initTestCase(OracleTestUtil.SCHEMA2_NAME);
		if (con2 == null) return;
		String sql2 =
			"create table person (\n" +
			"   id integer not null primary key, " +
			"   firstname varchar(50), " +
			"   lastname varchar(100) not null\n" +
			");\n " +
			"create materialized view v_person as \n" +
			"select id, lastname from person;\n";
		TestUtil.executeScript(con2, sql2);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
		OracleTestUtil.cleanUpTestCase(OracleTestUtil.SCHEMA2_NAME);
	}

	@Test
	public void testDiff()
		throws Exception
	{
		WbConnection reference = OracleTestUtil.getOracleConnection();
		WbConnection target = OracleTestUtil.getOracleConnection2();

		SchemaDiff diff = new SchemaDiff(reference, target);
		diff.setIncludeViews(true);
		diff.setSchemas(OracleTestUtil.SCHEMA_NAME, OracleTestUtil.SCHEMA2_NAME);
		TestUtil util = getTestUtil();
		File outfile = new File(util.getBaseDir(), "ora_diff.xml");
		Writer out = new FileWriter(outfile);
		diff.writeXml(out);
		FileUtil.closeQuietely(out);
		assertTrue(outfile.exists());
		assertTrue(outfile.length() > 0);
		Reader in = new FileReader(outfile);
		String xml = FileUtil.readCharacters(in);
		assertNotNull(xml);

		String value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='PERSON']/modify-column[@name='FIRSTNAME'])");
		assertEquals("1", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/update-view[@type='MATERIALIZED VIEW']/view-def[@name='V_PERSON'])");
		assertEquals("1", value);
	}

}
