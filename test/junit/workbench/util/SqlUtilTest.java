/*
 * SqlUtilTest.java
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
package workbench.util;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.ResultInfo;

import workbench.sql.ErrorDescriptor;
import workbench.sql.lexer.SQLToken;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.*;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlUtilTest
	extends WbTestCase
{

	public SqlUtilTest()
	{
		super("SqlUtilTest");
	}

	@Test
	public void testGetErrorOffset()
	{
		String sql = "select x from foo";
		ErrorDescriptor error = new ErrorDescriptor();
		error.setErrorPosition(0, sql.indexOf('x'));
		int offset = SqlUtil.getErrorOffset(sql, error);
		assertEquals(sql.indexOf('x'), offset);

		sql =
			"select x\r\n"+
			"from foo";
		error.setErrorPosition(1, 5);
		offset = SqlUtil.getErrorOffset(sql, error);
		assertEquals(sql.indexOf("foo"), offset);

		sql =
			"CREATE OR REPLACE PROCEDURE do_refresh \n" +
			"AS\n" +
			"   l_last_run   timestamp \n" +
			"   l_job_run_id integer \n" +
			"   l_now        timestamp \n" +
			"BEGIN \n" +
			"  l_job_run_id := seq_job_run.nextval \n" +
			"\n" +
			"\r\n" +
			"  l_now := systimestamp \r\n" +
			"\n" +
			"  l_last_run = trunc(systimestamp) - 7 \n" +
			"   \n" +
			"end refresh_mv_dwh_product_inc; \n" +
			"/";
		error.setErrorPosition(11, 13);
		offset = SqlUtil.getErrorOffset(sql, error);
		assertEquals(sql.indexOf("= trunc"), offset);
	}

	@Test
	public void testTypeDisplay()
	{
		assertEquals("varchar(50)", SqlUtil.getSqlTypeDisplay("varchar", Types.VARCHAR, 50, 0));
		assertEquals("varchar", SqlUtil.getSqlTypeDisplay("varchar", Types.VARCHAR, 0, 0));
		assertEquals("varchar(10)", SqlUtil.getSqlTypeDisplay("varchar(10)", Types.VARCHAR, 999, 0));
		assertEquals("text", SqlUtil.getSqlTypeDisplay("text", Types.VARCHAR, Integer.MAX_VALUE, 0));
	}

	@Test
	public void testRemoveQuotes()
	{
		assertEquals("foo", SqlUtil.removeObjectQuotes("\"foo\""));
		assertEquals("\"foo", SqlUtil.removeObjectQuotes("\"foo"));
		assertEquals("foo", SqlUtil.removeObjectQuotes("`foo`"));
		assertEquals("", SqlUtil.removeObjectQuotes(""));
		assertEquals("\"", SqlUtil.removeObjectQuotes("\""));
		assertEquals("foo", SqlUtil.removeObjectQuotes("[foo]"));
		assertEquals("\"foo]", SqlUtil.removeObjectQuotes("\"foo]"));
	}

	@Test
	public void testFullyQualifiedName()
	{
		TableIdentifier tbl = new TableIdentifier("foobar");
		assertEquals("foobar", SqlUtil.fullyQualifiedName(null, tbl));

		tbl = new TableIdentifier("public", "foobar");
		assertEquals("public.foobar", SqlUtil.fullyQualifiedName(null, tbl));

		tbl = new TableIdentifier("dbo", "public", "foobar");
		assertEquals("dbo.public.foobar", SqlUtil.fullyQualifiedName(null, tbl));

		tbl = new TableIdentifier("my schema", "foobar");
		assertEquals("\"my schema\".foobar", SqlUtil.fullyQualifiedName(null, tbl));
	}

	@Test
	public void testEscapeWildcards()
	{
		assertNull(SqlUtil.escapeUnderscore(null, "\\"));
		assertEquals("sometable", SqlUtil.escapeUnderscore("sometable", "\\"));
		assertEquals("first#_name", SqlUtil.escapeUnderscore("first_name", "#"));
		assertEquals("first_name", SqlUtil.escapeUnderscore("first_name", (String)null));
		assertEquals("first\\_name", SqlUtil.escapeUnderscore("first_name", "\\"));
		assertEquals("test\\_table\\_", SqlUtil.escapeUnderscore("test_table_", "\\"));
		assertEquals("firstname", SqlUtil.escapeUnderscore("firstname", "#"));
	}

	@Test
	public void testAddSemicolon()
	{
		String input = "  from some_table;   ";
		String sql = SqlUtil.addSemicolon(input);
		assertEquals(input, sql);

		input = "  from some_table   ";
		sql = SqlUtil.addSemicolon(input);
		assertEquals(input + ";", sql);

		input = "  from; some_table   ";
		sql = SqlUtil.addSemicolon(input);
		assertEquals(input + ";", sql);

		input = "";
		sql = SqlUtil.addSemicolon(input);
		assertEquals(";", sql);

		input = "       ;        ";
		sql = SqlUtil.addSemicolon(input);
		assertEquals(input, sql);
	}

	@Test
	public void testTrimSemicolon()
	{
		String input = "  from some_table;   ";
		String trimmed = SqlUtil.trimSemicolon(input);
		assertEquals("  from some_table", trimmed);

		input = "DELETE FROM some_table; COMMIT; ";
		trimmed = SqlUtil.trimSemicolon(input);
		assertEquals("DELETE FROM some_table; COMMIT", trimmed);

		input = "DELETE FROM some_table; COMMIT";
		trimmed = SqlUtil.trimSemicolon(input);
		assertEquals("DELETE FROM some_table; COMMIT", trimmed);
	}

	@Test
	public void testQuoteObject()
	{
		String name = "test";
		String quoted = SqlUtil.quoteObjectname(name);

		// No quoting needed
		assertEquals(quoted, name);

		name = "\"test\"";
		quoted = SqlUtil.quoteObjectname(name);

		// No quoting needed because quotes are already there
		assertEquals(quoted, name);

		name = "stupid-name";
		quoted = SqlUtil.quoteObjectname(name);
		assertEquals("\"stupid-name\"", quoted);

		name = "foo.bar_pkey";
		quoted = SqlUtil.quoteObjectname(name, false, true, '"');
		assertEquals("\"foo.bar_pkey\"", quoted);

	}

	@Test
	public void testAppendAndCondition()
		throws Exception
	{
		StringBuilder sql = new StringBuilder("select * from sometable");
		SqlUtil.appendAndCondition(sql, "some_col", "some_condition", null);
		assertEquals("select * from sometable AND some_col = 'some_condition'", sql.toString());
		SqlUtil.appendAndCondition(sql, "some_col", null, null);
		assertEquals("select * from sometable AND some_col = 'some_condition'", sql.toString());
	}

	@Test
	public void testGetFunctionParams()
		throws Exception
	{
		List<String> params = SqlUtil.getFunctionParameters("execute myfunc(1,2,3)");
		assertEquals(3, params.size());
		params = SqlUtil.getFunctionParameters("exec some_func(  trunc(sysdate) - 1  )");
		assertEquals(1, params.size());
		assertEquals("trunc(sysdate) - 1", params.get(0));

		params = SqlUtil.getFunctionParameters("exec some_func(  trunc(sysdate) - 1  ,  ?)");
		assertEquals(2, params.size());
		assertEquals("trunc(sysdate) - 1", params.get(0));
		assertEquals("?", params.get(1));

		params = SqlUtil.getFunctionParameters("some_func()");
		assertEquals(0, params.size());

		params = SqlUtil.getFunctionParameters("some_func('1,2')");
		assertEquals(1, params.size());
		assertEquals("'1,2'", params.get(0));

		params = SqlUtil.getFunctionParameters("wbcall some_proc(\"1,2\")");
		assertEquals(1, params.size());
		assertEquals("\"1,2\"", params.get(0));
	}



	@Test
	public void testGetCreateType()
	{
		try
		{
			String sql = "create\n --comment\n table bla (nr integer);";
			String type = SqlUtil.getCreateType(sql);
			assertEquals("Wrong type returned", "TABLE", type);

			sql = "-- comment\ncreate view blub as select * from bla;";
			type = SqlUtil.getCreateType(sql);
			assertEquals("Wrong type returned", "VIEW", type);

			sql = "/* blubber */\ncreate \nor \nreplace -- comment\nview blub as select * from bla;";
			type = SqlUtil.getCreateType(sql);
			assertEquals("Wrong type returned", "VIEW", type);

			sql = "/* blubber */\nrecreate VIEW blub as select * from bla;";
			type = SqlUtil.getCreateType(sql);
			assertEquals("Wrong type returned", "VIEW", type);

			sql = "/* blubber */\ncreate package blub;";
			type = SqlUtil.getCreateType(sql);
			assertEquals("Wrong type returned", "PACKAGE", type);

			sql = "--- do something\ncreate\n or replace\n package body blub;";
			type = SqlUtil.getCreateType(sql);
			assertEquals("Wrong type returned", "PACKAGE BODY", type);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testGetDeleteTable()
		throws Exception
	{
		String sql = "delete \nfrom mytable";
		String table = SqlUtil.getDeleteTable(sql);
		assertEquals("Wrong table returned", "mytable", table);

		sql = "-- bla\ndelete mytable";
		table = SqlUtil.getDeleteTable(sql);
		assertEquals("Wrong table returned", "mytable", table);

		sql = "delete\n--bla\nmyschema.mytable";
		table = SqlUtil.getDeleteTable(sql);
		assertEquals("Wrong table returned", "myschema.mytable", table);

		sql = "delete from myschema.mytable";
		table = SqlUtil.getDeleteTable(sql);
		assertEquals("Wrong table returned", "myschema.mytable", table);

		sql = "delete \"FROM\"";
		table = SqlUtil.getDeleteTable(sql);
		assertEquals("Wrong table returned", "\"FROM\"", table);

		sql = "delete from \"FROM\"";
		table = SqlUtil.getDeleteTable(sql);
		assertEquals("Wrong table returned", "\"FROM\"", table);

		sql = "delete from mylib/sometable";
		table = SqlUtil.getDeleteTable(sql, '/', null);
		assertEquals("mylib/sometable", table);

		sql = "delete mylib/sometable where x = 1";
		table = SqlUtil.getDeleteTable(sql, '/', null);
		assertEquals("mylib/sometable", table);

	}

	@Test
	public void testGetInsertTable()
	{
		String sql = "insert into mytable";
		String table = SqlUtil.getInsertTable(sql, null);
		assertEquals("mytable", table);

		sql = "insert into theschema.mytable";
		table = SqlUtil.getInsertTable(sql, null);
		assertEquals("theschema.mytable", table);

		sql = "insert into \"into\"";
		table = SqlUtil.getInsertTable(sql, null);
		assertEquals("\"into\"", table);

		sql = "insert into mylib/sometable (";
		table = SqlUtil.getInsertTable(sql, '/', null);
		assertEquals("mylib/sometable", table);
	}

	@Test
	public void testGetUpdateTable()
	{
		String sql = "update mytable set foo=42";
		String table = SqlUtil.getUpdateTable(sql, null);
		assertEquals("mytable", table);

		sql = "update \"mytable\" set foo=42";
		table = SqlUtil.getUpdateTable(sql, null);
		assertEquals("\"mytable\"", table);

		sql = "update somelib/mytable set foo=42 where ";
		table = SqlUtil.getUpdateTable(sql, '/', null);
		assertEquals("somelib/mytable", table);
	}

	@Test
	public void testRemoveComments()
		throws Exception
	{
		String sql = "/* some comment 'something quoted' some comment */\n" +
								"SELECT 42 from dual";
		String clean = SqlUtil.makeCleanSql(sql, false, false);
		assertEquals("SELECT 42 from dual", clean);

		sql = "/* some comment 'something quoted' some comment */\n" +
								"SELECT 42\n from dual";
		clean = SqlUtil.makeCleanSql(sql, false, false);
		assertEquals("SELECT 42  from dual", clean);

		sql = "/* some comment 'something quoted' some comment */\n" +
								"SELECT 42\n from dual";
		clean = SqlUtil.makeCleanSql(sql, true, false);
		assertEquals("SELECT 42\n from dual", clean);
	}

	@Test
	public void testCleanSql()
	{
		String sql = "select \r\n from project";
		String clean = SqlUtil.makeCleanSql(sql, false, false);
		assertEquals("Not correctly cleaned", "select   from project", clean);

		sql = "select \r\n from project;";
		clean = SqlUtil.makeCleanSql(sql, false, false);
		assertEquals("Not correctly cleaned", "select   from project", clean);

		sql = "select *\r\n from project ; ";
		clean = SqlUtil.makeCleanSql(sql, false, false);
		assertEquals("Not correctly cleaned", "select *  from project", clean);

		sql = "select * from project\n;\n";
		clean = SqlUtil.makeCleanSql(sql, false, false);
		assertEquals("Not correctly cleaned", "select * from project", clean);

		sql = "select 'some\nvalue' from project";
		clean = SqlUtil.makeCleanSql(sql, false, false);
		// nothing should be changed!
		assertEquals("Not correctly cleaned", sql, clean);

		sql = "select 'some\nvalue' \nfrom project";
		clean = SqlUtil.makeCleanSql(sql, false, false);
		assertEquals("Not correctly cleaned", "select 'some\nvalue'  from project", clean);

		sql = "select\t'some\n\tvalue' from project";
		clean = SqlUtil.makeCleanSql(sql, false, false);
		assertEquals("Not correctly cleaned", "select 'some\n\tvalue' from project", clean);

		sql = "select from \"project\"";
		clean = SqlUtil.makeCleanSql(sql, false, false);
		// nothing should be changed!
		assertEquals("Not correctly cleaned", sql, clean);

		sql = "/* this is a comment */ select from \"project\"";
		clean = SqlUtil.makeCleanSql(sql, false, true);
		// nothing should be changed!
		assertEquals("Not correctly cleaned", sql, clean);

		sql = "/* this is a comment */\n select from \"project\"";
		clean = SqlUtil.makeCleanSql(sql, false, true);
		assertEquals("Not correctly cleaned", "/* this is a comment */  select from \"project\"", clean);

		sql = "    \"some /* comment */ in identifier\"\r\n";
		assertEquals("\"some /* comment */ in identifier\"", SqlUtil.makeCleanSql(sql, false));

    // check nested quotes
    sql =
        "wbcopy -sourceQuery=\"select firstname, nr, lastname, coalesce(some_data, '--- missing! ---') as some_data from source_data\" " +
				"       -targetTable=target_data " +
				"       -columns=tfirstname, tnr, tlastname, tsome_data";

    assertEquals("wbcopy -sourceQuery=\"select firstname, nr, lastname, coalesce(some_data, '--- missing! ---') as some_data from source_data\"        -targetTable=target_data        -columns=tfirstname, tnr, tlastname, tsome_data", SqlUtil.makeCleanSql(sql, false, false));
	}

	@Test
	public void testGetSelectColumns()
	{
		String sql = "select x,y,z from bla";
		List<String> l = SqlUtil.getSelectColumns(sql,true,null);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("x", l.get(0));
		assertEquals("z", l.get(2));

		sql = "select x,y,z";
		l = SqlUtil.getSelectColumns(sql,true,null);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("x", l.get(0));
		assertEquals("z", l.get(2));

		sql = "select x\n     ,y\n     ,z FROM bla";
		l = SqlUtil.getSelectColumns(sql,true,null);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("x", l.get(0));
		assertEquals("z", l.get(2));

		sql = "SELECT a.att1\n      ,a.att2\nFROM   adam   a";
		l = SqlUtil.getSelectColumns(sql,true,null);
		assertEquals("Not enough columns", 2, l.size());
		assertEquals("a.att1", l.get(0));
		assertEquals("a.att2", l.get(1));

		sql = "SELECT to_char(date_col, 'YYYY-MM-DD'), col2 as \"Comma, column\", func('bla,blub')\nFROM   adam   a";
		l = SqlUtil.getSelectColumns(sql,false,null);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("Wrong first column", "to_char(date_col, 'YYYY-MM-DD')", l.get(0));
		assertEquals("Wrong third column", "func('bla,blub')", l.get(2));

		sql = "SELECT extract(year from rec_date) FROM mytable";
		l = SqlUtil.getSelectColumns(sql,false,null);
		assertEquals("Not enough columns", 1, l.size());
		assertEquals("Wrong first column", "extract(year from rec_date)", l.get(0));

		sql = "SELECT extract(year from rec_date) FROM mytable";
		l = SqlUtil.getSelectColumns(sql,true,null);
		assertEquals("Not enough columns", 1, l.size());
		assertEquals("Wrong first column", "extract(year from rec_date)", l.get(0));

		sql = "SELECT extract(year from rec_date) as rec_year FROM mytable";
		l = SqlUtil.getSelectColumns(sql,true,null);
		assertEquals("Not enough columns", 1, l.size());
		assertEquals("Wrong first column", "extract(year from rec_date) as rec_year", l.get(0));

		sql = "SELECT distinct col1, col2 from mytable";
		l = SqlUtil.getSelectColumns(sql, true,null);
		assertEquals("Not enough columns", 2, l.size());
		assertEquals("Wrong first column", "col1", l.get(0));

		sql = "SELECT distinct on (col1, col2), col3 from mytable";
		l = SqlUtil.getSelectColumns(sql, true,null);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("Wrong first column", "col1", l.get(0));
		assertEquals("Wrong first column", "col2", l.get(1));
		assertEquals("Wrong first column", "col3", l.get(2));

		sql = "with cte1 (x,y,z) as (select a,b,c from foo) select x,y,z from cte1";
		List<String> cols = SqlUtil.getSelectColumns(sql, false, null);
		assertEquals(3, cols.size());
		assertEquals("x", cols.get(0));
		assertEquals("y", cols.get(1));
		assertEquals("z", cols.get(2));

		sql =
			"with cte1 (x,y,z) as (\n" +
			"  select a,b,c from foo\n" +
			"), cte2 as (\n" +
			" select c1, c2 from bar\n" +
			")\n " +
			"select t1.x as x1, t1.y as y1, t1.z, t2.col1 as tcol\n" +
			"from cte1 t1 \n" +
			"  join cte2 t2 on t1.z = t2.c2";
		cols = SqlUtil.getSelectColumns(sql, false, null);
		assertEquals(4, cols.size());
		assertEquals("t1.x", cols.get(0));
		assertEquals("t1.y", cols.get(1));
		assertEquals("t1.z", cols.get(2));
		assertEquals("t2.col1", cols.get(3));

		cols = SqlUtil.getSelectColumns(sql, true, null);
		assertEquals(4, cols.size());
		assertEquals("t1.x as x1", cols.get(0));
		assertEquals("t1.y as y1", cols.get(1));
		assertEquals("t1.z", cols.get(2));
		assertEquals("t2.col1 as tcol", cols.get(3));

	}


	@Test
	public void testStripColumnAlias()
	{
		String expression = "p.name as lastname";
		String col = SqlUtil.stripColumnAlias(expression);
		assertEquals("p.name", col);

		expression = "p.name";
		col = SqlUtil.stripColumnAlias(expression);
		assertEquals("p.name", col);

		expression = "p.name as";
		col = SqlUtil.stripColumnAlias(expression);
		assertEquals("p.name", col);

		expression = "to_char(dt, 'YYYY')";
		col = SqlUtil.stripColumnAlias(expression);
		assertEquals("to_char(dt, 'YYYY')", col);

	}

	@Test
	public void testGetSqlVerb()
	{
		String sql = "-- comment line1\nSELECT * from dummy";
		String verb = SqlUtil.getSqlVerb(sql);
		assertEquals("SELECT", verb);

		sql = "-- comment line1\n-- second line\n\n /* bla */\nSELECT";
		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("SELECT", verb);

		sql = "/* \n" +
					 "* $URL: some_script.sql $ \n" +
					 "* $Revision: 1.10 $ \n" +
					 "* $LastChangedDate: 2006-05-05 20:29:15 -0400 (Fri, 05 May 2006) $ \n" +
					 "*/ \n" +
					 "-- A quis Lorem consequat Aenean tellus risus convallis velit Maecenas arcu. \n" +
					 "-- Suspendisse Maecenas tempor Lorem congue laoreet vel congue sit malesuada nibh. \n" +
					 "-- Lorem ipsum dolor sit amet consectetuer vitae Suspendisse ante Nullam lacinia \n" +
					 " \n" +
					 "-- ############################################# \n" +
					 "-- ##                                         ## \n" +
					 "-- ##              Organizations              ## \n" +
					 "-- ##                                         ## \n" +
					 "alter table participants drop constraint fk_bla;   -- Laoreet laoreet condimentum iaculis commodo dui id quis tempus accumsan wisi. Justo quam Curabitur dictumst non facilisis arcu Morbi semper pretium volutpat. Vestibulum habitasse Donec sapien adipiscing Suspendisse tempus habitant sed consectetuer pellentesque! \n";

		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("ALTER", verb);

		sql = "-- comment\n   @bla.sql";
		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("@", verb);

		sql = "-- comment only";
		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("None-empty verb returned", true, StringUtil.isEmptyString(verb));

		sql = "\\i some_file.sql";
		verb = SqlUtil.getSqlVerb(sql);
		assertEquals("\\i", verb);
	}

	@Test
	public void testDataTypeNames()
		throws Exception
	{
		assumeThat(System.getProperty("java.version"), is("1.7"));

		Field[] fields = java.sql.Types.class.getDeclaredFields();
		boolean missing = false;
		for (Field field : fields)
		{
			int type = field.getInt(null);
			if (SqlUtil.getTypeName(type).equals("UNKNOWN"))
			{
				System.out.println("Type " + field.getName() + " not included in getTypeName()!");
				missing = true;
			}
		}
		assertFalse("Not all types mapped!", missing);
	}

	@Test
	public void testCleanup()
	{
		String cleaned = SqlUtil.cleanupIdentifier("SOME_THING");
		assertEquals("SOME_THING", cleaned);

		cleaned = SqlUtil.cleanupIdentifier("SOME THING");
		assertEquals("SOMETHING", cleaned);

		cleaned = SqlUtil.cleanupIdentifier("1&SOME-\\THING2");
		assertEquals("1SOMETHING2", cleaned);

		cleaned = SqlUtil.cleanupIdentifier("&\"SOM'E-\\THING");
		assertEquals("SOMETHING", cleaned);
	}

	@Test
	public void testQueryInfo()
		throws Exception
	{
		TestUtil util = getTestUtil();
		try
		{
			WbConnection con = util.getConnection();
			TestUtil.executeScript(con,
				"CREATE TABLE person (id integer, firstname varchar(100), lastname varchar(100));\n" +
				"COMMIT;\n");

			con.getDbSettings().setUsePreparedStatementForQueryInfo(false);
			ResultInfo info = SqlUtil.getResultInfoFromQuery("SELECT * from person", con);
			assertNotNull(info);
			assertEquals(3, info.getColumnCount());
			assertEquals("ID", info.getColumn(0).getColumnName());

			List<ColumnIdentifier> columns = SqlUtil.getResultSetColumns("SELECT * FROM PERSON", con);
			assertNotNull(columns);
			assertEquals(3, columns.size());

			con.getDbSettings().setUsePreparedStatementForQueryInfo(true);
			ResultInfo info2 = SqlUtil.getResultInfoFromQuery("SELECT * from person", con);
			assertNotNull(info2);
			assertEquals(3, info2.getColumnCount());
			assertEquals("ID", info2.getColumn(0).getColumnName());

			List<ColumnIdentifier> columns2 = SqlUtil.getResultSetColumns("SELECT * FROM PERSON", con);
			assertNotNull(columns2);
			assertEquals(3, columns2.size());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}

	}

	@Test
	public void testReplaceParameters()
		throws Exception
	{
		String sql = "select * from t where x = ? and y = ?";
		String newSql = SqlUtil.replaceParameters(sql, Integer.valueOf(42), "two");
		assertEquals("select * from t where x = 42 and y = 'two'", newSql);

		sql = "select * from t where x = ? and y = 42";
		newSql = SqlUtil.replaceParameters(sql, Integer.valueOf(42));
		assertEquals("select * from t where x = 42 and y = 42", newSql);

		sql = "select * from t where x = 1 and y = 42";
		newSql = SqlUtil.replaceParameters(sql, "foo", "bar");
		assertEquals("select * from t where x = 1 and y = 42", newSql);
	}

	@Test
	public void testCleanDataType()
	{
		String type = SqlUtil.getPlainTypeName("varchar");
		assertEquals("varchar", type);
		type = SqlUtil.getPlainTypeName("varchar(10)");
		assertEquals("varchar", type);
	}

	@Test
	public void testGetPreviousToken()
	{
		String sql = "select * from foo where x < 1";
		int pos = sql.indexOf('<')+ 1;
		SQLToken prev = SqlUtil.getOperatorBeforeCursor(sql, pos);
		assertNotNull(prev);
		assertTrue(prev.isOperator());
		assertEquals("<", prev.getContents());

		sql = "where a in ( )";
		pos = sql.indexOf('(') + 1;
		prev = SqlUtil.getOperatorBeforeCursor(sql, pos);
		assertNotNull(prev);
		assertEquals("IN", prev.getContents());
	}

	@Test
	public void testCompareIdentifiers()
	{
		assertTrue(SqlUtil.objectNamesAreEqual("foo", "FOO"));
		assertTrue(SqlUtil.objectNamesAreEqual("\"foo\"", "\"foo\""));
		assertFalse(SqlUtil.objectNamesAreEqual("\"foo\"", "\"Foo\""));
		assertFalse(SqlUtil.objectNamesAreEqual("\"foo\"", "\"Foo\""));
		assertTrue(SqlUtil.objectNamesAreEqual("\"FOO\"", "\"FOO\""));
		assertFalse(SqlUtil.objectNamesAreEqual("`Foo`", "`foo`"));
		assertFalse(SqlUtil.objectNamesAreEqual("[FOO]", "[foo]"));
	}

}
