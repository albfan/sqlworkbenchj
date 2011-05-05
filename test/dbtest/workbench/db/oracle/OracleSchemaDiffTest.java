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
		if (reference == null || target == null)
		{
			return;
		}

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

	@Test
	public void testCheckConstraints()
		throws Exception
	{
		String sql = "CREATE TABLE foo \n" +
			"( \n" +
			"  id integer not null, \n" +
			"  weekday_start integer not null, \n" +
			"  weekday integer, \n" +
			"  is_active integer not null, \n" +
			"  CONSTRAINT CHK_WEEKDAY CHECK (WEEKDAY IS NULL OR (weekday IN (1,2,3,4,5,6,7))), \n "+
			"  CONSTRAINT CHK_WEEKDAY_START CHECK (weekday_start IN (1,2,3,4,5,6,7)), \n" +
      "  CONSTRAINT CKC_IS_ACTIVE CHECK (IS_ACTIVE IN (0,1)), \n" +
			"  CONSTRAINT pk_foo PRIMARY KEY (id) \n" +
			")";

		WbConnection reference = OracleTestUtil.getOracleConnection();
		WbConnection target = OracleTestUtil.getOracleConnection2();
		if (reference == null || target == null)
		{
			return;
		}
		// Remove other tables
		TestUtil.executeScript(reference, " DROP MATERIALIZED VIEW V_PERSON; DROP TABLE person CASCADE CONSTRAINTS;");
		TestUtil.executeScript(target, "DROP MATERIALIZED VIEW V_PERSON; DROP TABLE person CASCADE CONSTRAINTS;");

		TestUtil.executeScript(reference, sql);
		TestUtil.executeScript(target, sql);

		SchemaDiff diff = new SchemaDiff(reference, target);
		diff.setIncludeViews(true);
		diff.setSchemas(OracleTestUtil.SCHEMA_NAME, OracleTestUtil.SCHEMA2_NAME);
		diff.setCompareConstraintsByName(true);
		diff.setIncludeTableConstraints(true);
		TestUtil util = getTestUtil();
		File outfile = new File(util.getBaseDir(), "ora_diff2.xml");
		Writer out = new FileWriter(outfile);
		diff.writeXml(out);
		FileUtil.closeQuietely(out);
		assertTrue(outfile.exists());
		assertTrue(outfile.length() > 0);
		Reader in = new FileReader(outfile);
		String xml = FileUtil.readCharacters(in);
		assertNotNull(xml);

		String value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table)");
		assertEquals("0", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='FOO']/table-constraints)");
		assertEquals("0", value);
	}

}
