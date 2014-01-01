/*
 * SqlUtilTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
import workbench.sql.formatter.SQLToken;

import org.junit.Test;

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
	public void testGetFromPart()
	{
		String sql =
			"with some_data as (\n" +
			"  select foo,\n" +
			"         bar \n" +
			"  from foobar f \n" +
			"  where f.id = 42\n" +
			")\n" +
			"select foo, \n" +
			"       count(*) as hit_count \n" +
			"from some_data d\n" +
			"group by d.foo\n" +
			"order by 2 desc";
		int pos = SqlUtil.getFromPosition(sql);
		int fromPos = sql.indexOf("from some_data d");
		assertEquals(fromPos, pos);

		String from = SqlUtil.getFromPart(sql);
		assertEquals("some_data d", from.trim());

		sql = "select a.id, b.pid from foo a join bar b where a.id > 42;";

		from = SqlUtil.getFromPart(sql);
		assertEquals("foo a join bar b", from.trim());
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
	public void testDb2Parsing()
	{
		String select = "select * from mylib/sometable where belegid=20100234";
		List<String> tables = SqlUtil.getTables(select, true, '/', '.');
		assertEquals(1, tables.size());
		assertEquals("mylib/sometable", tables.get(0));

		tables = SqlUtil.getTables("select * from ordermgmt.\"FOO.BAR\";", false, '/', '.');
		assertEquals(tables.size(), 1);
		assertEquals("ordermgmt.\"FOO.BAR\"", tables.get(0));

		tables = SqlUtil.getTables("select * from RICH/\"TT.PBILL\";", false, '/', '/');
		assertEquals(tables.size(), 1);

		assertEquals("RICH/\"TT.PBILL\"", tables.get(0));
		TableIdentifier tbl = new TableIdentifier(tables.get(0), '/', '/');
		assertEquals("RICH", tbl.getSchema());
		assertEquals("TT.PBILL", tbl.getTableName());
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
		table = SqlUtil.getDeleteTable(sql, '/');
		assertEquals("mylib/sometable", table);

		sql = "delete mylib/sometable where x = 1";
		table = SqlUtil.getDeleteTable(sql, '/');
		assertEquals("mylib/sometable", table);

	}

	@Test
	public void testGetInsertTable()
	{
		String sql = "insert into mytable";
		String table = SqlUtil.getInsertTable(sql);
		assertEquals("mytable", table);

		sql = "insert into theschema.mytable";
		table = SqlUtil.getInsertTable(sql);
		assertEquals("theschema.mytable", table);

		sql = "insert into \"into\"";
		table = SqlUtil.getInsertTable(sql);
		assertEquals("\"into\"", table);

		sql = "insert into mylib/sometable (";
		table = SqlUtil.getInsertTable(sql, '/');
		assertEquals("mylib/sometable", table);
	}

	@Test
	public void testGetUpdateTable()
	{
		String sql = "update mytable set foo=42";
		String table = SqlUtil.getUpdateTable(sql);
		assertEquals("mytable", table);

		sql = "update \"mytable\" set foo=42";
		table = SqlUtil.getUpdateTable(sql);
		assertEquals("\"mytable\"", table);

		sql = "update somelib/mytable set foo=42 where ";
		table = SqlUtil.getUpdateTable(sql, '/');
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
	}

	@Test
	public void testGetSelectColumns()
	{
		String sql = "select x,y,z from bla";
		List<String> l = SqlUtil.getSelectColumns(sql,true);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("x", l.get(0));
		assertEquals("z", l.get(2));

		sql = "select x,y,z";
		l = SqlUtil.getSelectColumns(sql,true);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("x", l.get(0));
		assertEquals("z", l.get(2));

		sql = "select x\n     ,y\n     ,z FROM bla";
		l = SqlUtil.getSelectColumns(sql,true);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("x", l.get(0));
		assertEquals("z", l.get(2));

		sql = "SELECT a.att1\n      ,a.att2\nFROM   adam   a";
		l = SqlUtil.getSelectColumns(sql,true);
		assertEquals("Not enough columns", 2, l.size());

		sql = "SELECT to_char(date_col, 'YYYY-MM-DD'), col2 as \"Comma, column\", func('bla,blub')\nFROM   adam   a";
		l = SqlUtil.getSelectColumns(sql,false);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("Wrong first column", "to_char(date_col, 'YYYY-MM-DD')", l.get(0));
		assertEquals("Wrong third column", "func('bla,blub')", l.get(2));

		sql = "SELECT extract(year from rec_date) FROM mytable";
		l = SqlUtil.getSelectColumns(sql,false);
		assertEquals("Not enough columns", 1, l.size());
		assertEquals("Wrong first column", "extract(year from rec_date)", l.get(0));

		sql = "SELECT extract(year from rec_date) FROM mytable";
		l = SqlUtil.getSelectColumns(sql,true);
		assertEquals("Not enough columns", 1, l.size());
		assertEquals("Wrong first column", "extract(year from rec_date)", l.get(0));

		sql = "SELECT extract(year from rec_date) as rec_year FROM mytable";
		l = SqlUtil.getSelectColumns(sql,true);
		assertEquals("Not enough columns", 1, l.size());
		assertEquals("Wrong first column", "extract(year from rec_date) as rec_year", l.get(0));

		sql = "SELECT distinct col1, col2 from mytable";
		l = SqlUtil.getSelectColumns(sql, true);
		assertEquals("Not enough columns", 2, l.size());
		assertEquals("Wrong first column", "col1", l.get(0));

		sql = "SELECT distinct on (col1, col2), col3 from mytable";
		l = SqlUtil.getSelectColumns(sql, true);
		assertEquals("Not enough columns", 3, l.size());
		assertEquals("Wrong first column", "col1", l.get(0));
		assertEquals("Wrong first column", "col2", l.get(1));
		assertEquals("Wrong first column", "col3", l.get(2));

		sql = "with cte1 (x,y,z) as (select a,b,c from foo) select x,y,z from cte1";
		List<String> cols = SqlUtil.getSelectColumns(sql, false);
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
		cols = SqlUtil.getSelectColumns(sql, false);
		assertEquals(4, cols.size());
		assertEquals("t1.x", cols.get(0));
		assertEquals("t1.y", cols.get(1));
		assertEquals("t1.z", cols.get(2));
		assertEquals("t2.col1", cols.get(3));

		cols = SqlUtil.getSelectColumns(sql, true);
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
		String col = SqlUtil.striptColumnAlias(expression);
		assertEquals("p.name", col);

		expression = "p.name";
		col = SqlUtil.striptColumnAlias(expression);
		assertEquals("p.name", col);

		expression = "p.name as";
		col = SqlUtil.striptColumnAlias(expression);
		assertEquals("p.name", col);

		expression = "to_char(dt, 'YYYY')";
		col = SqlUtil.striptColumnAlias(expression);
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
	}

	@Test
	public void testGetTables()
	{
		String sql = "select *\nfrom\n-- list the tables here\ntable1 t1, table2 t2, table3 t3";
		List<String> l = SqlUtil.getTables(sql, false);
		assertEquals(3, l.size());

		assertEquals("table1",l.get(0));
		assertEquals("table2",l.get(1));
		assertEquals("table3",l.get(2));

		l = SqlUtil.getTables(sql, true);
		assertEquals(3, l.size());

		assertEquals("table1 t1", l.get(0));
		assertEquals("table2 t2", l.get(1));
		assertEquals("table3 t3", l.get(2));

		sql = "SELECT cr.dealid, \n" +
					 "       cs.state, \n" +
					 "       bq.* \n" +
					 "FROM dbo.tblcreditrow cr  \n" +
					 "-- bla blubber \n" +
					 "INNER  JOIN bdb_ie.dbo.tblbdbproduct p ON cr.partnumber = p.partnumber  \n" +
					 "RIGHT  OUTER  JOIN tblbidquantity bq ON bq.partnumber LIKE p.mainpartnumber + '%'AND bq.bidid = cr.bidid  \n" +
					 "INNER  JOIN tblcredit c ON c.creditid = cr.creditid  \n" +
					 "INNER  JOIN tblcreditstate cs ON cs.creditstateid = c.creditstateid \n" +
					 "WHERE c.arrivaldate >= '2006-04-01'";

		l = SqlUtil.getTables(sql, true);
		assertEquals(5, l.size());
		assertEquals("dbo.tblcreditrow cr", l.get(0));
		assertEquals("bdb_ie.dbo.tblbdbproduct p", l.get(1));
		assertEquals("tblbidquantity bq", l.get(2));
		assertEquals("tblcredit c", l.get(3));
		assertEquals("tblcreditstate cs", l.get(4));

		l = SqlUtil.getTables(sql, false);
		assertEquals(5, l.size());
		assertEquals("dbo.tblcreditrow", l.get(0));
		assertEquals("bdb_ie.dbo.tblbdbproduct", l.get(1));
		assertEquals("tblbidquantity", l.get(2));
		assertEquals("tblcredit", l.get(3));
		assertEquals("tblcreditstate", l.get(4));

		sql = "SELECT c.cntry_name as country,  \n" +
             "case  \n" +
             "   when r.ref_name is null then p.plr_name  \n" +
             "   else r.ref_name \n" +
             "end as name, \n" +
             "case  \n" +
             "   when r.ref_name is null then 'PLAYER' \n" +
             "   else 'REF' \n" +
             "end as type \n" +
             "from country c right outer join referee r on (c.)  \n" +
             "               right outer join  \n" +
             "where c.cntry_id = p.cntry_id (+) \n" +
             "and c.cntry_id = r.cntry_id (+)";
		l = SqlUtil.getTables(sql, false);
		assertEquals(2, l.size());

		sql = "SELECT DISTINCT CONVERT(VARCHAR(50),an.oid) AS an_oid, \n" +
					 "       an.cid AS an_cid, \n" +
					 "       CONVERT(VARCHAR(50),an.anrede) AS an_anrede, \n" +
					 "       an.titel AS an_titel, \n" +
					 "       an.akadgrad AS an_grad, \n" +
					 "       an.vorname AS an_vorname, \n" +
					 "       an.nachname AS an_nachname, \n" +
					 "       an.nummer AS an_nummer, \n" +
					 "       an.gebdatum AS an_gdat, \n" +
					 "       an_adr.ort AS an_adr_ort, \n" +
					 "       an_adr.plz AS an_adr_plz, \n" +
					 "       an_adr.strasse AS an_adr_str, \n" +
					 "       CONVERT(VARCHAR(50),an_adr.staatoid) AS an_adr_staat, \n" +
					 "       CONVERT(VARCHAR(50),an_adr.bland) AS an_adr_land, \n" +
					 "       ang.bezeichnung AS ang_bezeichnung, \n" +
					 "       CONVERT(VARCHAR(50),ag.oid) AS ag_oid, \n" +
					 "       CONVERT(VARCHAR(50),ag.art) AS ag_art, \n" +
					 "       ag.name AS ag_name, \n" +
					 "       ag.nummer AS ag_nummer, \n" +
					 "       ag.gdatum AS ag_gdat, \n" +
					 "       CONVERT(VARCHAR(50),ag.rform) AS ag_rechtsform, \n" +
					 "       ag_adr.ort AS ag_adr_ort, \n" +
					 "       ag_adr.plz AS ag_adr_plz, \n" +
					 "       ag_adr.strasse AS ag_adr_str, \n" +
					 "       CONVERT(VARCHAR(50),ag_adr.staatoid) AS ag_adr_staat, \n" +
					 "       CONVERT(VARCHAR(50),ag_adr.bland) AS ag_adr_land, \n" +
					 "       CONVERT(VARCHAR(50),ber.anrede) AS ber_anrede, \n" +
					 "       ber.titel AS ber_titel, \n" +
					 "       ber.akadgrad AS ber_grad, \n" +
					 "       ber.vorname AS ber_vorname, \n" +
					 "       ber.nachname AS ber_nachname, \n" +
					 "       ber.nummer AS ber_nummer \n" +
					 "FROM (((((((((((accright acc LEFT JOIN \n" +
					 "      stuser u_ber ON u_ber.userid = acc.userid AND u_ber.dc = acc.dc) LEFT JOIN \n" +
					 "      nperson ber ON u_ber.person_oid = ber.oid AND u_ber.dc = ber.dc) LEFT JOIN \n" +
					 "      nperson an ON acc.subject_oid = an.oid AND acc.dc = an.dc) LEFT JOIN \n" +
					 "      bavdaten bav ON bav.modeloid = an.oid AND bav.dc = an.dc) LEFT JOIN \n" +
					 "      bavangroup ang ON bav.angruppe_oid = ang.oid AND bav.dc = ang.dc) LEFT JOIN \n" +
					 "      adresse an_adr ON an_adr.quelleoid = an.oid AND an_adr.dc = an.dc) LEFT OUTER JOIN \n" +
					 "      beziehung bez ON bez.zieloid = an.oid AND bez.zielcid = an.cid AND bez.dc = an.dc) LEFT OUTER JOIN \n" +
					 "      jperson ag ON ag.oid = bez.quelleoid AND ag.cid = bez.quellecid AND ag.dc = bez.dc) LEFT OUTER JOIN \n" +
					 "      bavagdaten bavag ON bavag.modeloid = ag.oid AND bavag.dc = ag.dc) LEFT OUTER JOIN \n" +
					 "      adresse ag_adr ON ag_adr.quelleoid = ag.oid AND ag_adr.dc = ag.dc) LEFT JOIN \n" +
					 "      accright acc_ag ON acc_ag.subject_oid = ag.oid AND acc_ag.dc = ag.dc) LEFT JOIN \n" +
					 "      stuser u_ag ON u_ag.userid = acc_ag.userid AND u_ag.dc = acc_ag.dc \n" +
					 "WHERE ((u_ag.userid = '17564'OR u_ag.bossid IN (SELECT userid \n" +
					 "                                                FROM stuser \n" +
					 "                                                WHERE (userid = '17564'OR bossid = '17564') \n" +
					 "                                                AND   deaktiv = '0' \n" +
					 "                                                AND   dc = ' ')) \n" +
					 "AND   (acc_ag.rolename = 'berater')) AND ('berater'= '' \n" +
					 "OR    acc.rolename LIKE 'berater') AND ('CVM02000'= '' \n" +
					 "OR    acc.subject_cid = 'CVM02000') AND (bez.bezeichnung = 'B2E5AE00-9050-4401-B8E1-8A3B55B22CA9' \n" +
					 "OR    bez.bezeichnung IS NULL) AND ((bavag.anoptok = '1'AND '1'= '1') \n" +
					 "OR    ((bavag.anoptok = '0'OR bavag.anoptok IS NULL) AND '1'= '1')) AND an.dc = ' 'AND ber.nummer = '65346' \n" +
					 "ORDER BY an.nachname,an.vorname,an.nummer";

		l = SqlUtil.getTables(sql, true);
		assertEquals(13, l.size());

		sql = "select avg(km_pro_jahr) from ( \n" +
             "select min(f.km), max(f.km), max(f.km) - min(f.km) as km_pro_jahr, extract(year from e.exp_date) \n" +
             "from fuel f, expense e \n" +
             "where f.exp_id = e.exp_id \n" +
             "group by extract(year from e.exp_date) \n" +
             ")";

		l = SqlUtil.getTables(sql, true);
		assertEquals(0, l.size());

		sql = "select avg(km_pro_jahr) from ( \n" +
             "select min(f.km), max(f.km), max(f.km) - min(f.km) as km_pro_jahr, extract(year from e.exp_date) \n" +
             "from fuel f, expense e \n" +
             "where f.exp_id = e.exp_id \n" +
             "group by extract(year from e.exp_date) \n" +
             ") t, table2";

		l = SqlUtil.getTables(sql, true);
		assertEquals(2, l.size());
		assertEquals("table2", l.get(1));

		// Make sure the getTables() is case preserving
		sql = "select * from MyTable";
		l = SqlUtil.getTables(sql, true);
		assertEquals(1, l.size());
		assertEquals("MyTable", l.get(0));

		// Make sure the getTables() is case preserving
		sql = "select * from table1 as t1, table2";
		l = SqlUtil.getTables(sql, true);
		assertEquals(2, l.size());
		assertEquals("table1 AS t1", l.get(0));
		assertEquals("table2", l.get(1));

		sql = "select r.id, r.name + ', ' + r.first_name, ara.* \n" +
             "from project_resource pr left join assigned_resource_activity ara ON (pr.resource_id = ara.id) \n" +
             "                         join resource r on (pr.resource_id = r.id) \n" +
             "where pr.project_id = 42 \n" +
             "and ara.id is null";
		l = SqlUtil.getTables(sql, true);
		assertEquals(3, l.size());
		assertEquals("project_resource pr", l.get(0));
		assertEquals("assigned_resource_activity ara", l.get(1));
		assertEquals("resource r", l.get(2));

		sql = "SELECT x. FROM \"Dumb Named Schema\".\"Problematically Named Table\" x";
		l = SqlUtil.getTables(sql, false);
		assertEquals(l.size(), 1);
		assertEquals( "\"Dumb Named Schema\".\"Problematically Named Table\"", l.get(0));

		l = SqlUtil.getTables(sql, true);
		assertEquals(l.size(), 1);
		assertEquals( "\"Dumb Named Schema\".\"Problematically Named Table\" x", l.get(0));

		l = SqlUtil.getTables("select * from some_table limit 100;");
		assertEquals(l.size(), 1);
		assertEquals("some_table", l.get(0));

		l = SqlUtil.getTables("select * from some_table as something;");
		assertEquals(l.size(), 1);
		assertEquals("some_table", l.get(0));

		l = SqlUtil.getTables("select * from \"foo.bar\";");
		assertEquals(l.size(), 1);
		assertEquals("\"foo.bar\"", l.get(0));

		l = SqlUtil.getTables("select * from public.\"foo.bar\";");
		assertEquals(l.size(), 1);
		assertEquals("public.\"foo.bar\"", l.get(0));

		sql = "with some_data as (\n" +
			"  select foo,\n" +
			"         bar \n" +
			"  from foobar f \n" +
			"  where f.id = 42\n" +
			")\n" +
			"select foo, \n" +
			"       count(*) as hit_count \n" +
			"from some_data d\n" +
			"group by d.foo\n" +
			"order by 2 desc";
		l = SqlUtil.getTables(sql, false);
		assertEquals(l.size(), 1);
		assertEquals("some_data", l.get(0));

		l = SqlUtil.getTables(sql, true);
		assertEquals(l.size(), 1);
		assertEquals("some_data d", l.get(0));
	}

	@Test
	public void testDataTypeNames()
		throws Exception
	{
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
		SQLToken prev = SqlUtil.getTokenBeforeCursor(sql, pos);
		assertNotNull(prev);
		assertTrue(prev.isOperator());
		assertEquals("<", prev.getContents());
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
