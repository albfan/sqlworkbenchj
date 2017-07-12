/*
 * PostgresSchemaDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.io.StringWriter;
import java.io.Writer;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
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
  public void testFilteredIndexDiff()
    throws Exception
  {
    WbConnection conn = PostgresTestUtil.getPostgresConnection();
    assertNotNull(conn);

    String sql =
      "create schema if not exists " + REFERENCE_SCHEMA + ";\n" +
      "create schema if not exists " + TARGET_SCHEMA + ";\n" +
      "CREATE TABLE " + REFERENCE_SCHEMA + ".foo1 \n" +
      "( \n" +
      "  id integer not null, \n" +
      "  is_active boolean not null \n" +
      ");\n" +
      "create index ix1 on " + REFERENCE_SCHEMA + ".foo1 (id) where (is_active = false);\n" +
      "\n" +
      "CREATE TABLE " + TARGET_SCHEMA + ".foo1 \n" +
      "( \n" +
      "  id integer not null, \n" +
      "  is_active boolean not null \n" +
      ");\n" +
      "create index ix1 on " + TARGET_SCHEMA + ".foo1 (id) where (is_active = true);\n" +
      "\n" +
      "commit;\n";

    TestUtil.executeScript(conn, sql);

    SchemaDiff diff = new SchemaDiff(conn, conn);
    diff.setSchemaNames(REFERENCE_SCHEMA, TARGET_SCHEMA);
    diff.setTableNames(CollectionUtil.arrayList(REFERENCE_SCHEMA + ".foo1"), CollectionUtil.arrayList(TARGET_SCHEMA + ".foo1"));
    diff.setIncludeIndex(true);
    StringWriter result = new StringWriter();
    diff.writeXml(result);
    String xml = result.toString();
//    System.out.println(xml);
    String value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table[@name='foo1'])");
    assertEquals("1", value);

    value = TestUtil.getXPathValue(xml, "count(/schema-diff/modify-table/modify-index[@name='ix1'])");
    assertEquals("1", value);

    value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table/modify-index/modified/filter-expression/target-expression");
    assertEquals("is_active = true", value);

    value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table/modify-index/modified/filter-expression/reference-expression");
    assertEquals("is_active = false", value);
  }

  @Test
  public void testCheckConstraints()
    throws Exception
  {
    WbConnection conn = PostgresTestUtil.getPostgresConnection();
    assertNotNull(conn);

    String schema =
      "CREATE SCHEMA " + TARGET_SCHEMA + ";\n" +
      "COMMIT;\n";

    String sql = "CREATE TABLE XXXX.foo \n" +
      "( \n" +
      "  id integer not null, \n" +
      "  weekday_start integer not null, \n" +
      "  weekday integer, \n" +
      "  is_active integer not null, \n" +
      "  CONSTRAINT CHK_WEEKDAY CHECK (WEEKDAY IS NULL OR (weekday IN (1,2,3,4,5,6,7))), \n " +
      "  CONSTRAINT CHK_WEEKDAY_START CHECK (weekday_start IN (1,2,3,4,5,6,7)), \n" +
      "  CONSTRAINT CKC_IS_ACTIVE CHECK (IS_ACTIVE IN (0,1)), \n" +
      "  CONSTRAINT pk_foo PRIMARY KEY (id) \n" +
      ");\n" +
      "commit;\n";

    String script = schema + sql.replace("XXXX", REFERENCE_SCHEMA) + sql.replace("XXXX", TARGET_SCHEMA);

    TestUtil.executeScript(conn, script);

    SchemaDiff diff = new SchemaDiff(conn, conn);
    diff.setIncludeViews(true);
    diff.setSchemas(REFERENCE_SCHEMA, TARGET_SCHEMA);

    diff.setIncludeViews(false);
    diff.setCompareConstraintsByName(true);
    diff.setIncludeTableConstraints(true);
    StringWriter out = new StringWriter();
    diff.writeXml(out);
    String xml = out.toString();
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
    assertNotNull(conn);

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
    diff.setAdditionalTypes(CollectionUtil.arrayList("enum", "type"));
    diff.setSchemas(REFERENCE_SCHEMA, TARGET_SCHEMA);

    TestUtil util = getTestUtil();

    File outfile = new File(util.getBaseDir(), "pg_addtypes_diff.xml");
    StringWriter out = new StringWriter();
    diff.writeXml(out);
    String xml = out.toString();
//    System.out.println(xml);
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
    assertNotNull(conn);

    TestUtil.executeScript(conn,
      "create schema " + TARGET_SCHEMA + "\n;\n" +
      "create function " + REFERENCE_SCHEMA + ".to_create() returns integer as $$ begin return 42; end; $$ language plpgsql;\n" +
      "\n" +
      "create function " + REFERENCE_SCHEMA + ".to_modify() returns integer as $$ begin return 42; end; $$ language plpgsql; \n" +
      "\n" +
      "create function " + REFERENCE_SCHEMA + ".to_modify(p1 integer) returns integer as $$ begin return 42; end; $$ language plpgsql; \n" +
      "\n" +
      "create function " + REFERENCE_SCHEMA + ".to_delete() returns integer as $$ begin return 1; end; $$ language plpgsql; \n" +
      "\n" +
      "create function " + TARGET_SCHEMA + ".to_modify() returns integer as $$ begin return 42; end; $$ language plpgsql; \n" +
      "\n" +
      "create function " + TARGET_SCHEMA + ".to_modify(p1 integer) returns integer as $$ begin return 1; end; $$ language plpgsql; \n" +
      "\n" +
      "create function " + TARGET_SCHEMA + ".to_delete() returns integer as $$ begin return 1; end; $$ language plpgsql; \n" +
      "\n" +
      "create function " + TARGET_SCHEMA + ".to_delete(p1 integer) returns integer as $$ begin return 1; end; $$ language plpgsql; \n" +
      "\n" +
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
    StringWriter out = new StringWriter();
    diff.writeXml(out);
    String xml = out.toString();
//		System.out.println(xml);

    String value = TestUtil.getXPathValue(xml, "/schema-diff/create-proc/proc-def/proc-name");
    assertEquals("to_create", value);

    value = TestUtil.getXPathValue(xml, "/schema-diff/update-proc/proc-def/proc-full-name");
    assertEquals("to_modify(integer)", value);

    value = TestUtil.getXPathValue(xml, "/schema-diff/drop-procedure/proc-def/proc-full-name");
    assertEquals("to_delete(integer)", value);

    assertTrue("Could not delete output", outfile.delete());
  }

  @Test
  public void testTriggerDiff()
    throws Exception
  {
    WbConnection conn = PostgresTestUtil.getPostgresConnection();
    assertNotNull(conn);

    String sql =
      "create schema if not exists " + TARGET_SCHEMA + ";\n" +
      "create table " + REFERENCE_SCHEMA + ".some_table(id integer, last_updated timestamp);\n" +
      "create table " + TARGET_SCHEMA + ".some_table(id integer, last_updated timestamp);\n" +
      "CREATE OR REPLACE FUNCTION " + REFERENCE_SCHEMA + ".my_trigger_func()   \n" +
      "RETURNS trigger AS   \n" +
      "$body$  \n" +
      "BEGIN  \n" +
      "    new.last_updated = current_timestamp; \n" +
      "    RETURN NEW; \n" +
      "END \n" +
      "$body$   \n" +
      "LANGUAGE plpgsql;\n" +
      "\n" +
      "CREATE OR REPLACE FUNCTION " + TARGET_SCHEMA + ".my_trigger_func()   \n" +
      "RETURNS trigger AS   \n" +
      "$body$  \n" +
      "BEGIN  \n" +
      "    new.last_updated = current_timestamp - interval '1' hour; \n" +
      "    RETURN NEW; \n" +
      "END \n" +
      "$body$   \n" +
      "LANGUAGE plpgsql;\n" +
      "CREATE TRIGGER person_trg BEFORE INSERT OR UPDATE ON " + REFERENCE_SCHEMA + ".some_table\n" +
      "    FOR EACH ROW EXECUTE PROCEDURE " + REFERENCE_SCHEMA + ".my_trigger_func();\n" +
      "\n" +
      "CREATE TRIGGER person_trg BEFORE INSERT ON " + TARGET_SCHEMA + ".some_table\n" +
      "    FOR EACH ROW EXECUTE PROCEDURE " + TARGET_SCHEMA + ".my_trigger_func();\n" +
      "\n" +
      "commit;\n";

//    System.out.println(sql);
    TestUtil.executeScript(conn, sql);

    SchemaDiff diff = new SchemaDiff(conn, conn);
    diff.setIncludeViews(false);
    diff.setIncludeIndex(false);
    diff.setIncludeSequences(false);
    diff.setIncludeProcedures(true);
    diff.setIncludeTriggers(true);
    diff.setSchemas(REFERENCE_SCHEMA, TARGET_SCHEMA);

    StringWriter out = new StringWriter();
    diff.writeXml(out);
    String xml = out.toString();
    assertNotNull(xml);

//		System.out.println(xml);
    String value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='some_table']/update-trigger/trigger-def/trigger-name");
    assertEquals("person_trg", value);

    value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='some_table']/update-trigger/trigger-def/trigger-event");
    assertEquals("INSERT, UPDATE", value);

    value = TestUtil.getXPathValue(xml, "/schema-diff/modify-table[@name='some_table']/update-trigger/trigger-def/trigger-type");
    assertEquals("BEFORE", value);
  }
}
