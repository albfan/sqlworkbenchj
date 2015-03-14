/*
 * PostgresSchemaDiffTest.java
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class PostgresSchemaDiffTest
	extends WbTestCase
{

	private static final String REFERENCE_SCHEMA = "refschema";
	private static final String TARGET_SCHEMA = "targetschema";

	public PostgresSchemaDiffTest()
	{
		super("PostgresSchemaDiffTest");
	}

	@Before
	public void beforeTest()
		throws Exception
	{
		PostgresTestUtil.initTestCase(REFERENCE_SCHEMA);

	}

	@After
	public void afterTest()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testCheckConstraints()
		throws Exception
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null)
		{
			return;
		}

		String schema =
			"CREATE SCHEMA " + TARGET_SCHEMA + ";\n" +
			"COMMIT;\n";

		String sql = "CREATE TABLE XXXX.foo \n" +
			"( \n" +
			"  id integer not null, \n" +
			"  weekday_start integer not null, \n" +
			"  weekday integer, \n" +
			"  is_active integer not null, \n" +
			"  CONSTRAINT CHK_WEEKDAY CHECK (WEEKDAY IS NULL OR (weekday IN (1,2,3,4,5,6,7))), \n "+
			"  CONSTRAINT CHK_WEEKDAY_START CHECK (weekday_start IN (1,2,3,4,5,6,7)), \n" +
      "  CONSTRAINT CKC_IS_ACTIVE CHECK (IS_ACTIVE IN (0,1)), \n" +
			"  CONSTRAINT pk_foo PRIMARY KEY (id) \n" +
			");\n" +
			"commit;\n";

		String script = schema + sql.replace("XXXX", REFERENCE_SCHEMA) +
			sql.replace("XXXX", TARGET_SCHEMA);

		TestUtil.executeScript(conn, script);

		SchemaDiff diff = new SchemaDiff(conn, conn);
		diff.setIncludeViews(true);
		diff.setSchemas(REFERENCE_SCHEMA, TARGET_SCHEMA);

		diff.setIncludeViews(false);
		diff.setCompareConstraintsByName(true);
		diff.setIncludeTableConstraints(true);
		TestUtil util = getTestUtil();
		File outfile = new File(util.getBaseDir(), "pg_check_diff.xml");
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

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='foo']/table-constraints)");
		assertEquals("0", value);
	}

	@Test
	public void testExtendedDiff()
		throws Exception
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null)
		{
			return;
		}

		String sql =
			"CREATE SCHEMA " + TARGET_SCHEMA + ";\n" +
			"COMMIT;\n" +
			"create type " + REFERENCE_SCHEMA + ".mood as enum ('sad','ok','happy');\n" +
			"create type " + REFERENCE_SCHEMA + ".address_type AS (city varchar(100), street varchar(50), zipcode varchar(10));\n" +
			"create type " + TARGET_SCHEMA + ".mood as enum ('notok','ok','happy');\n" +
			"create type " + TARGET_SCHEMA + ".address_type AS (city varchar(50), street varchar(50), zipcode varchar(10));\n" +
			"commit\n";

		TestUtil.executeScript(conn, sql);

		SchemaDiff diff = new SchemaDiff(conn, conn);
		diff.setIncludeViews(false);
		diff.setIncludeIndex(false);
		diff.setIncludeSequences(false);
		diff.setIncludeProcedures(false);
		diff.setIncludeTriggers(false);
		diff.setAdditionalTypes(CollectionUtil.arrayList("enum","type"));
		diff.setSchemas(REFERENCE_SCHEMA, TARGET_SCHEMA);

		TestUtil util = getTestUtil();

		File outfile = new File(util.getBaseDir(), "pg_addtypes_diff.xml");
		Writer out = new FileWriter(outfile);
		diff.writeXml(out);
		FileUtil.closeQuietely(out);

		assertTrue(outfile.exists());
		assertTrue(outfile.length() > 0);
		Reader in = new FileReader(outfile);
		String xml = FileUtil.readCharacters(in);
		assertNotNull(xml);
		System.out.println(xml);
		String value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-object)");
		assertEquals("1", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-object/reference-object/object-def[@name='mood'])");
		assertEquals("1", value);

		value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-type/modify-column[@name='city'])");
		assertEquals("1", value);

		assertTrue("Could not delete output", outfile.delete());
	}

	@Test
	public void testProcDiff()
		throws Exception
	{
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null)
		{
			return;
		}

		TestUtil.executeScript(conn,
			"create schema " + TARGET_SCHEMA  + "\n;\n" +
			"create function " + REFERENCE_SCHEMA + ".to_create() returns integer as $$ begin return 42; end; $$ language plpgsql;\n" +
			"\n" +
			"create function " + REFERENCE_SCHEMA + ".to_modify() returns integer as $$ begin return 42; end; $$ language plpgsql; \n" +
			"\n" +
			"create function " + REFERENCE_SCHEMA + ".to_modify(p1 integer) returns integer as $$ begin return 42; end; $$ language plpgsql; \n" +
			"\n" +
			"create function " + REFERENCE_SCHEMA + ".to_delete() returns integer as $$ begin return 1; end; $$ language plpgsql; \n" +
			"\n"  +
			"create function " + TARGET_SCHEMA + ".to_modify() returns integer as $$ begin return 42; end; $$ language plpgsql; \n" +
			"\n" +
			"create function " + TARGET_SCHEMA + ".to_modify(p1 integer) returns integer as $$ begin return 1; end; $$ language plpgsql; \n" +
			"\n" +
			"create function " + TARGET_SCHEMA + ".to_delete() returns integer as $$ begin return 1; end; $$ language plpgsql; \n" +
			"\n"  +
			"create function " + TARGET_SCHEMA + ".to_delete(p1 integer) returns integer as $$ begin return 1; end; $$ language plpgsql; \n" +
			"\n"  +
			"commit;\n");

		SchemaDiff diff = new SchemaDiff(conn, conn);
		diff.setIncludeViews(false);
		diff.setIncludeIndex(false);
		diff.setIncludeSequences(false);
		diff.setIncludeProcedures(true);
		diff.setIncludeTriggers(false);
		diff.setSchemas(REFERENCE_SCHEMA, TARGET_SCHEMA);

		TestUtil util = getTestUtil();

		File outfile = new File(util.getBaseDir(), "pg_proc_diff.xml");
		Writer out = new FileWriter(outfile);
		diff.writeXml(out);
		FileUtil.closeQuietely(out);

		assertTrue(outfile.exists());
		assertTrue(outfile.length() > 0);
		Reader in = new FileReader(outfile);
		String xml = FileUtil.readCharacters(in);
		assertNotNull(xml);
//		System.out.println(xml);

		String value = TestUtil.getXPathValue(xml, "/schema-diff/create-proc/proc-def/proc-name");
		assertEquals("to_create", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/update-proc/proc-def/proc-full-name");
		assertEquals("to_modify(integer)", value);

		value = TestUtil.getXPathValue(xml, "/schema-diff/drop-procedure/proc-def/proc-full-name");
		assertEquals("to_delete(integer)", value);

		assertTrue("Could not delete output", outfile.delete());
	}
}
