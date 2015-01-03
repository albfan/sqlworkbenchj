/*
 * DdlObjectInfoTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import org.junit.Test;

import static org.junit.Assert.*;

import workbench.sql.parser.ParserType;


/**
 *
 * @author Thomas Kellerer
 */
public class DdlObjectInfoTest
{
	public DdlObjectInfoTest()
	{
	}

	@Test
	public void testOracle()
		throws Exception
	{
		String sql = "-- test\ncreate or \t replace\n\nprocedure bla";
		DdlObjectInfo info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "bla");
		assertEquals(info.getDisplayType(), "Procedure");

		sql = "-- test\ncreate unique bitmap index idx_test on table (x,y);";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "idx_test");
		assertEquals(info.getDisplayType(), "Index");

		sql = "-- test\ncreate or replace package \n\n some_package \t\t\n as something";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals("some_package", info.getObjectName());
		assertEquals("PACKAGE", info.getObjectType());

		sql = "-- test\ncreate package body \n\n some_body \t\t\n as something";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals("some_body", info.getObjectName());
		assertEquals("PACKAGE BODY", info.getObjectType());

		sql = "CREATE FLASHBACK ARCHIVE main_archive";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals("main_archive", info.getObjectName());
		assertEquals("FLASHBACK ARCHIVE", info.getObjectType());

		sql = "analyze table foo validate structure";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals("foo", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		sql = "analyze index foo_idx validate structure";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals("foo_idx", info.getObjectName());
		assertEquals("INDEX", info.getObjectType());

		sql = "create type body my_type is begin\n null; end;";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals("my_type", info.getObjectName());
		assertEquals("TYPE BODY", info.getObjectType());

		sql = "alter function mystuff compile";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertNotNull(info);
		assertEquals("mystuff", info.getObjectName());
		assertEquals("FUNCTION", info.getObjectType());

		sql = "create force view v_test as select * from t;";
		info = new DdlObjectInfo(sql, ParserType.Oracle);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "v_test");
		assertEquals(info.getDisplayType(), "View");
	}

	@Test
	public void testPostgres()
	{
		DdlObjectInfo info;

		info = new DdlObjectInfo("create global temporary table stuff.temp if not exists (id integer);", ParserType.Postgres);
		assertTrue(info.isValid());
		assertEquals("stuff.temp", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		info = new DdlObjectInfo("create tablespace users location 'foobar';", ParserType.Postgres);
		assertTrue(info.isValid());
		assertEquals("users", info.getObjectName());
		assertEquals("TABLESPACE", info.getObjectType());

		info = new DdlObjectInfo("create unlogged table foobar if not exists (id integer);", ParserType.Postgres);
		assertTrue(info.isValid());
		assertEquals("foobar", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		info = new DdlObjectInfo("create extension hstore", ParserType.Postgres);
		assertTrue(info.isValid());
		assertEquals("hstore", info.getObjectName());
		assertEquals("EXTENSION", info.getObjectType());
	}

	@Test
	public void testSqlServer()
	{
		DdlObjectInfo info;
		String sql;

		sql = "create nonclustered index idx_test on table (x,y);";
		info = new DdlObjectInfo(sql, ParserType.SqlServer);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "idx_test");
		assertEquals(info.getDisplayType(), "Index");

		sql = "create table ##mytemp (id integer);";
		info = new DdlObjectInfo(sql, ParserType.SqlServer);
		assertTrue(info.isValid());
		assertEquals("##mytemp", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		sql = "create table [ignore the standard] (id integer);";
		info = new DdlObjectInfo(sql, ParserType.SqlServer);
		assertTrue(info.isValid());
		assertEquals("ignore the standard", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		sql = "create table #someTemp(some_col integer);";
		info = new DdlObjectInfo(sql, ParserType.SqlServer);
		assertTrue(info.isValid());
		assertEquals("#someTemp", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());
	}

	@Test
	public void testMySQL()
	{
		DdlObjectInfo info;
		String sql;

		sql = "create table `stupid` (id integer;";
		info = new DdlObjectInfo(sql, ParserType.MySQL);
		assertTrue(info.isValid());
		assertEquals("stupid", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());
	}

	@Test
	public void testGetObjectInfo()
		throws Exception
	{
		DdlObjectInfo info;
		String sql;

		sql = "recreate view v_test as select * from t;";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "v_test");
		assertEquals(info.getDisplayType(), "View");

		sql = "create table my_schema.my_table (nr integer);";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "my_schema.my_table");
		assertEquals(info.getDisplayType(), "Table");

		sql = "-- test\ncreate memory table my_table (nr integer);";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "my_table");
		assertEquals(info.getDisplayType(), "Table");

		sql = "drop memory table my_table;";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "my_table");
		assertEquals(info.getDisplayType(), "Table");

		sql = "drop index idx_test;";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "idx_test");
		assertEquals(info.getDisplayType(), "Index");

		sql = "drop function f_answer;";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals("f_answer", info.getObjectName());
		assertEquals("Function", info.getDisplayType());

		sql = "drop procedure f_answer;";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals("f_answer", info.getObjectName());
		assertEquals("Procedure", info.getDisplayType());

		sql = "drop sequence s;";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals(info.getObjectName(), "s");
		assertEquals(info.getDisplayType(), "Sequence");

		sql = "drop role s;";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals("s", info.getObjectName());
		assertEquals("ROLE", info.getObjectType());

		sql = "-- test\ncreate \n\ntrigger test_trg for mytable";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals("test_trg", info.getObjectName());
		assertEquals("TRIGGER", info.getObjectType());

		sql = "CREATE TABLE IF NOT EXISTS some_table (id integer)";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals("some_table", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		sql = "DROP TABLE old_table IF EXISTS";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals("old_table", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		sql = "analyze local table foobar";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals("foobar", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		sql = "create index \"FOO\".\"IDX\" on foo.bar (id);";
		info = new DdlObjectInfo(sql);
		assertTrue(info.isValid());
		assertEquals("IDX", info.getObjectName());
		assertEquals("INDEX", info.getObjectType());

		info = new DdlObjectInfo("create temporary table if not exists temp (id integer);");
		assertTrue(info.isValid());
		assertEquals("temp", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());

		info = new DdlObjectInfo("create table foo_backup as select * from foo;");
		assertTrue(info.isValid());
		assertEquals("foo_backup", info.getObjectName());
		assertEquals("TABLE", info.getObjectType());
	}

}