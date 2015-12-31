/*
 * OracleSchemaDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.oracle;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;

import workbench.sql.DelimiterDefinition;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;

import org.junit.BeforeClass;
import org.junit.Test;

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
		OracleTestUtil.initTestCase(OracleTestUtil.SCHEMA2_NAME);
	}

	private void createTypes(WbConnection con1, WbConnection con2)
		throws Exception
	{
		String sql1 =
			"create type person_type is object (\n" +
			"   id integer, " +
			"   firstname varchar(100), " +
			"   lastname varchar(100)\n" +
			");\n" +
			"/\n" +
			"create type foo_type is object (\n" +
			"   id integer \n " +
			");\n" +
			"/";

		TestUtil.executeScript(con1, sql1);

		if (con2 == null) return;
		String sql2 =
			"create type person_type is object (\n" +
			"   id integer, " +
			"   firstname varchar(50), " +
			"   lastname varchar(50)\n" +
			");\n" +
			"/";
		TestUtil.executeScript(con2, sql2);
	}

	private void createTables(WbConnection con1, WbConnection con2)
		throws Exception
	{
		String sql1 =
			"create table person (\n" +
			"   id integer not null primary key, " +
			"   firstname varchar(100), " +
			"   lastname varchar(100) not null\n" +
			");\n " +
			"create materialized view v_person as \n" +
			"select id, firstname, lastname from person;\n";

		TestUtil.executeScript(con1, sql1);

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

	private void createPackages(WbConnection con1, WbConnection con2)
		throws Exception
	{
		String pck1 =
			"CREATE PACKAGE proc_pckg  \n" +
			"AS  \n" +
			"  PROCEDURE process_pkg_data(some_value out number, some_id in number); \n" +
			"  FUNCTION get_answer RETURN INTEGER; \n" +
			"END proc_pckg;  \n" +
			"/ \n" +
			" \n" +
			"CREATE PACKAGE BODY proc_pckg \n" +
			"AS \n" +
			"  PROCEDURE process_pkg_data(some_value out number, some_id in number) \n" +
			"  IS  \n" +
			"  BEGIN  \n" +
			"    some_value := some_id * 2;   \n" +
			"  END process_pkg_data;   \n" +
			" \n" +
			"  FUNCTION get_answer \n" +
			"    RETURN INTEGER \n" +
			"  IS \n" +
			"  BEGIN \n" +
			"    return 42; \n" +
			"  END get_answer;\n" +
			"END proc_pckg; \n" +
			"/";

		String pck2 =
			"CREATE PACKAGE pckg_to_create  \n" +
			"AS  \n" +
			"  PROCEDURE process_pkg_data2(some_value out number, some_id in number); \n" +
			"  FUNCTION get_answer2 RETURN INTEGER; \n" +
			"END pckg_to_create;  \n" +
			"/ \n" +
			" \n" +
			"CREATE PACKAGE BODY pckg_to_create \n" +
			"AS \n" +
			"  PROCEDURE process_pkg_data2(some_value out number, some_id in number) \n" +
			"  IS  \n" +
			"  BEGIN  \n" +
			"    some_value := some_id * 2;   \n" +
			"  END process_pkg_data2;   \n" +
			" \n" +
			"  FUNCTION get_answer2 \n" +
			"    RETURN INTEGER \n" +
			"  IS \n" +
			"  BEGIN \n" +
			"    return 42; \n" +
			"  END get_answer2;\n" +
			"END pckg_to_create; \n" +
			"/";

		String pck3_a =
			"CREATE PACKAGE pckg_to_update  \n" +
			"AS  \n" +
			"  PROCEDURE process_pkg_data4(some_value out number, some_id in number); \n" +
			"  FUNCTION get_answer4 RETURN INTEGER; \n" +
			"END pckg_to_update;  \n" +
			"/ \n" +
			" \n" +
			"CREATE PACKAGE BODY pckg_to_update \n" +
			"AS \n" +
			"  PROCEDURE process_pkg_data4(some_value out number, some_id in number) \n" +
			"  IS  \n" +
			"  BEGIN  \n" +
			"    some_value := some_id * 2;   \n" +
			"  END process_pkg_data4;   \n" +
			" \n" +
			"  FUNCTION get_answer4 \n" +
			"    RETURN INTEGER \n" +
			"  IS \n" +
			"  BEGIN \n" +
			"    return 42; \n" +
			"  END get_answer4;\n" +
			"END pckg_to_update; \n" +
			"/";

		String pck3_b =
			"CREATE PACKAGE pckg_to_update  \n" +
			"AS  \n" +
			"  PROCEDURE process_pkg_data4(some_value out number, some_id in number); \n" +
			"END pckg_to_update;  \n" +
			"/ \n" +
			" \n" +
			"CREATE PACKAGE BODY pckg_to_update \n" +
			"AS \n" +
			"  PROCEDURE process_pkg_data4(some_value out number, some_id in number) \n" +
			"  IS  \n" +
			"  BEGIN  \n" +
			"    some_value := some_id * 2;   \n" +
			"  END process_pkg_data4;   \n" +
			" \n" +
			"END pckg_to_update; \n" +
			"/";

		String pck4 =
			"CREATE PACKAGE pckg_to_delete  \n" +
			"AS  \n" +
			"  PROCEDURE process_pkg_data3(some_value out number, some_id in number); \n" +
			"  FUNCTION get_answer3 RETURN INTEGER; \n" +
			"END pckg_to_delete;  \n" +
			"/ \n" +
			" \n" +
			"CREATE PACKAGE BODY pckg_to_delete \n" +
			"AS \n" +
			"  PROCEDURE process_pkg_data3(some_value out number, some_id in number) \n" +
			"  IS  \n" +
			"  BEGIN  \n" +
			"    some_value := some_id * 2;   \n" +
			"  END process_pkg_data3;   \n" +
			" \n" +
			"  FUNCTION get_answer3 \n" +
			"    RETURN INTEGER \n" +
			"  IS \n" +
			"  BEGIN \n" +
			"    return 42; \n" +
			"  END get_answer3;\n" +
			"END pckg_to_delete; \n" +
			"/";

		TestUtil.executeScript(con1, pck1, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		TestUtil.executeScript(con1, pck2, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		TestUtil.executeScript(con1, pck3_a, DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		TestUtil.executeScript(con2, pck1, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		TestUtil.executeScript(con2, pck3_b, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		TestUtil.executeScript(con2, pck4, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@Test
	public void testTypeDiff()
		throws Exception
	{
		WbConnection reference = OracleTestUtil.getOracleConnection();
		WbConnection target = OracleTestUtil.getOracleConnection2();
		assertNotNull("Oracle not available", reference);
		assertNotNull("Oracle not available", target);

		try
		{
			createTypes(reference, target);

			SchemaDiff diff = new SchemaDiff(reference, target);
			diff.setIncludeViews(false);
			diff.setIncludeIndex(false);
			diff.setIncludeSequences(false);
			diff.setIncludeProcedures(false);
			diff.setIncludeTriggers(false);
			diff.setAdditionalTypes(CollectionUtil.arrayList("TYPE"));
			diff.setSchemas(OracleTestUtil.SCHEMA_NAME, OracleTestUtil.SCHEMA2_NAME);

			TestUtil util = getTestUtil();

			File outfile = new File(util.getBaseDir(), "ora_types_diff.xml");
			Writer out = new FileWriter(outfile);
			diff.writeXml(out);
			FileUtil.closeQuietely(out);

			assertTrue(outfile.exists());
			assertTrue(outfile.length() > 0);
			Reader in = new FileReader(outfile);
			String xml = FileUtil.readCharacters(in);
			assertNotNull(xml);

			String value = TestUtil.getXPathValue(xml, "count(/schema-diff/add-type[@name='FOO_TYPE'])");
			assertEquals("1", value);

			value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-type[@name='PERSON_TYPE'])");
			assertEquals("1", value);

			assertTrue("Could not delete output", outfile.delete());
		}
		finally
		{
			OracleTestUtil.dropAllObjects(reference);
			OracleTestUtil.dropAllObjects(target);
		}
	}

	@Test
	public void testPackageDiff()
		throws Exception
	{
		WbConnection reference = OracleTestUtil.getOracleConnection();
		WbConnection target = OracleTestUtil.getOracleConnection2();

		if (reference == null || target == null)
		{
			return;
		}
		createPackages(reference, target);

		try
		{
			SchemaDiff diff = new SchemaDiff(reference, target);
			diff.setIncludeViews(false);
			diff.setIncludeProcedures(true);
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
			// System.out.println("**************\n" + xml + "\n*********************");

			String value = TestUtil.getXPathValue(xml, "count(/schema-diff/create-package/package-def[@packageName='PCKG_TO_CREATE'])");
			assertEquals("1", value);

			value = TestUtil.getXPathValue(xml, "count(/schema-diff/update-package/package-def[@packageName='PCKG_TO_UPDATE'])");
			assertEquals("1", value);

			value = TestUtil.getXPathValue(xml, "count(/schema-diff/drop-package/package-def[@packageName='PCKG_TO_DELETE'])");
			assertEquals("1", value);

			value = TestUtil.getXPathValue(xml, "count(//package-def[@packageName='PROC_PCKG'])");
			assertEquals("0", value);

			assertTrue("Could not delete output", outfile.delete());
		}
		finally
		{
			OracleTestUtil.dropAllObjects(reference);
			OracleTestUtil.dropAllObjects(target);
		}
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

		try
		{
			createTables(reference, target);

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
			assertTrue("Could not delete output", outfile.delete());
		}
		finally
		{
			OracleTestUtil.dropAllObjects(reference);
			OracleTestUtil.dropAllObjects(target);
		}
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

		File outfile = null;
		try
		{
			// Make sure no tables are there
			OracleTestUtil.dropAllObjects(reference);
			OracleTestUtil.dropAllObjects(target);

			TestUtil.executeScript(reference, sql);
			TestUtil.executeScript(target, sql);

			SchemaDiff diff = new SchemaDiff(reference, target);
			diff.setIncludeViews(true);
			diff.setSchemas(OracleTestUtil.SCHEMA_NAME, OracleTestUtil.SCHEMA2_NAME);
			diff.setCompareConstraintsByName(true);
			diff.setIncludeTableConstraints(true);
			TestUtil util = getTestUtil();
			outfile = new File(util.getBaseDir(), "ora_diff2.xml");
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
			assertTrue("Could not delete output", outfile.delete());
		}
		finally
		{
			OracleTestUtil.dropAllObjects(reference);
			OracleTestUtil.dropAllObjects(target);
		}
	}

}
