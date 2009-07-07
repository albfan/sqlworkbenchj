/*
 * SqlUtilTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Pattern;
import workbench.WbTestCase;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlUtilTest
	extends WbTestCase
{

	public SqlUtilTest(String testName)
	{
		super(testName);
	}

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

	}
	public void testIsSelectIntoNewTable()
		throws Exception
	{
		String p = Settings.getInstance().getProperty("workbench.db.microsoft_sql_server.selectinto.pattern", null);

		Pattern selectIntoPattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);

		String sql = "select * into new_table from old_table;";
		assertTrue("Pattern for SQL Server not working", SqlUtil.isSelectIntoNewTable(selectIntoPattern, sql));

		sql = "-- Test\n" +
					"select * into #temp2 from #temp1;\n";
		assertTrue("Pattern for SQL Server not working", SqlUtil.isSelectIntoNewTable(selectIntoPattern, sql));


		p = Settings.getInstance().getProperty("workbench.db.postgresql.selectinto.pattern", null);
		selectIntoPattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
		sql = "select * into new_table from old_table;";
		assertTrue("Pattern for Postgres not working", SqlUtil.isSelectIntoNewTable(selectIntoPattern, sql));

		sql = "-- Test\n" +
					"select * into new_table from old_table;\n";
		assertTrue("Pattern for Postgres not working", SqlUtil.isSelectIntoNewTable(selectIntoPattern, sql));

		p = Settings.getInstance().getProperty("workbench.db.informix-online.selectinto.pattern", null);
		selectIntoPattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
	}

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
	public void testGetDeleteTable()
	{
		try
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public void testGetObjectInfo()
		throws Exception
	{
		String sql = "-- test\ncreate or \t replace\n\nprocedure bla";
		SqlUtil.DdlObjectInfo info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "bla");
		assertEquals(info.getDisplayType(), "Procedure");

		sql = "-- test\ncreate unique bitmap index idx_test on table (x,y);";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "idx_test");
		assertEquals(info.getDisplayType(), "Index");

		sql = "recreate view v_test as select * from t;";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "v_test");
		assertEquals(info.getDisplayType(), "View");

		sql = "create nonclustered index idx_test on table (x,y);";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "idx_test");
		assertEquals(info.getDisplayType(), "Index");

		sql = "-- test\ncreate memory table my_table (nr integer);";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "my_table");
		assertEquals(info.getDisplayType(), "Table");

		sql = "create table dbo.my_table (nr integer);";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "dbo.my_table");
		assertEquals(info.getDisplayType(), "Table");

		sql = "create force view v_test as select * from t;";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "v_test");
		assertEquals(info.getDisplayType(), "View");

		sql = "drop memory table my_table;";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "my_table");
		assertEquals(info.getDisplayType(), "Table");

		sql = "drop index idx_test;";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "idx_test");
		assertEquals(info.getDisplayType(), "Index");

		sql = "drop function f_answer;";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "f_answer");
		assertEquals(info.getDisplayType(), "Function");

		sql = "drop procedure f_answer;";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "f_answer");
		assertEquals(info.getDisplayType(), "Procedure");

		sql = "drop sequence s;";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals(info.objectName, "s");
		assertEquals(info.getDisplayType(), "Sequence");

		sql = "drop role s;";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertNull(info);

		sql = "-- test\ncreate \n\ntrigger test_trg for mytable";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals("test_trg", info.objectName);
		assertEquals("TRIGGER", info.objectType);

		sql = "-- test\ncreate or replace package \n\n some_package \t\t\n as something";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals("some_package", info.objectName);
		assertEquals("PACKAGE", info.objectType);

		sql = "-- test\ncreate package body \n\n some_body \t\t\n as something";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals("some_body", info.objectName);
		assertEquals("PACKAGE BODY", info.objectType);

		sql = "CREATE FLASHBACK ARCHIVE main_archive";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals("main_archive", info.objectName);
		assertEquals("FLASHBACK ARCHIVE", info.objectType);

		sql = "CREATE TABLE IF NOT EXISTS some_table (id integer)";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals("some_table", info.objectName);
		assertEquals("TABLE", info.objectType);

		sql = "DROP TABLE old_table IF EXISTS";
		info = SqlUtil.getDDLObjectInfo(sql);
		assertEquals("old_table", info.objectName);
		assertEquals("TABLE", info.objectType);

	}



	public void testGetInsertTable()
	{
		try
		{
			String sql = "insert into mytable";
			String table = SqlUtil.getInsertTable(sql);
			assertEquals("Wrong table returned", "mytable", table);

			sql = "insert into theschema.mytable";
			table = SqlUtil.getInsertTable(sql);
			assertEquals("Wrong table returned", "theschema.mytable", table);

			sql = "insert into \"into\"";
			table = SqlUtil.getInsertTable(sql);
			assertEquals("Wrong table returned", "\"into\"", table);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

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

	}


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
	}

	public static void testDataTypeNames()
	{
		try
		{
//			System.out.println("Checking if all types defined by java.sql.Types are covered by getTypeName()...");
//			System.out.println(System.getProperty("java.version"));
			Field[] fields = java.sql.Types.class.getDeclaredFields();
			boolean missing = false;
			for (int i=0; i < fields.length; i++)
			{
				int type = fields[i].getInt(null);
				if (SqlUtil.getTypeName(type).equals("UNKNOWN"))
				{
					System.out.println("Type " + fields[i].getName() + " not included in getTypeName()!");
					missing = true;
				}
			}
			assertFalse("Not all types mapped!", missing);
		}
		catch (Throwable th)
		{
			th.printStackTrace();
			fail(th.getMessage());
		}
	}


}
