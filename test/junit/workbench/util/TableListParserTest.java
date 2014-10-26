/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
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

import java.util.List;

import workbench.db.TableIdentifier;

import workbench.sql.parser.ParserType;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableListParserTest
{

	public TableListParserTest()
	{
	}

	@Test
	public void testSqlServer()
	{
		TableListParser parser = new TableListParser( '.', '.', ParserType.SqlServer);
		List<Alias> tables = parser.getTables("select * from [some table];", false);
		assertEquals(tables.size(), 1);
		assertEquals("[some table]", tables.get(0).getName());

		tables = parser.getTables("select * from [dbo].[some table];", false);
		assertEquals(tables.size(), 1);
		assertEquals("[dbo].[some table]", tables.get(0).getName());
	}

	@Test
	public void testDb2Parsing()
	{
		TableListParser parser = new TableListParser('/', '.', ParserType.Standard);
		String select = "select * from mylib/sometable where belegid=20100234";
		List<Alias> tables = parser.getTables(select, true);
		assertEquals(1, tables.size());
		assertEquals("mylib/sometable", tables.get(0).getName());

		parser = new TableListParser('/', '.', ParserType.Standard);
		tables = parser.getTables("select * from ordermgmt.\"FOO.BAR\";", false);
		assertEquals(tables.size(), 1);
		assertEquals("ordermgmt.\"FOO.BAR\"", tables.get(0).getName());

		parser = new TableListParser('/', '/', ParserType.Standard);
		tables = parser.getTables("select * from RICH/\"TT.PBILL\";", false);
		assertEquals(tables.size(), 1);

		assertEquals("RICH/\"TT.PBILL\"", tables.get(0).getName());
		TableIdentifier tbl = new TableIdentifier(tables.get(0).getObjectName(), '/', '/');
		assertEquals("RICH", tbl.getSchema());
		assertEquals("TT.PBILL", tbl.getTableName());
	}

	@Test
	public void testGetTables()
	{
		TableListParser parser = new TableListParser();
		String sql = "select *\nfrom\n-- list the tables here\ntable1 t1, table2 t2, table3 t3";
		List<Alias> l = parser.getTables(sql, false);
		assertEquals(3, l.size());

		assertEquals("table1",l.get(0).getName());
		assertEquals("table2",l.get(1).getName());
		assertEquals("table3",l.get(2).getName());

		l = parser.getTables(sql, true);
		assertEquals(3, l.size());

		assertEquals("table1 t1", l.get(0).getName());
		assertEquals("table2 t2", l.get(1).getName());
		assertEquals("table3 t3", l.get(2).getName());

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

		l = parser.getTables(sql, true);
		assertEquals(5, l.size());
		assertEquals("dbo.tblcreditrow cr", l.get(0).getName());
		assertEquals("bdb_ie.dbo.tblbdbproduct p", l.get(1).getName());
		assertEquals("tblbidquantity bq", l.get(2).getName());
		assertEquals("tblcredit c", l.get(3).getName());
		assertEquals("tblcreditstate cs", l.get(4).getName());

		l = parser.getTables(sql, false);
		assertEquals(5, l.size());
		assertEquals("dbo.tblcreditrow", l.get(0).getName());
		assertEquals("bdb_ie.dbo.tblbdbproduct", l.get(1).getName());
		assertEquals("tblbidquantity", l.get(2).getName());
		assertEquals("tblcredit", l.get(3).getName());
		assertEquals("tblcreditstate", l.get(4).getName());

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
		l = parser.getTables(sql, false);
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

		l = parser.getTables(sql, true);
		assertEquals(13, l.size());

		sql = "select avg(km_pro_jahr) from ( \n" +
             "select min(f.km), max(f.km), max(f.km) - min(f.km) as km_pro_jahr, extract(year from e.exp_date) \n" +
             "from fuel f, expense e \n" +
             "where f.exp_id = e.exp_id \n" +
             "group by extract(year from e.exp_date) \n" +
             ")";

		l = parser.getTables(sql, true);
		assertEquals(0, l.size());

		sql = "select avg(km_pro_jahr) from ( \n" +
             "select min(f.km), max(f.km), max(f.km) - min(f.km) as km_pro_jahr, extract(year from e.exp_date) \n" +
             "from fuel f, expense e \n" +
             "where f.exp_id = e.exp_id \n" +
             "group by extract(year from e.exp_date) \n" +
             ") t, table2";

		l = parser.getTables(sql, true);
		assertEquals(2, l.size());
		assertEquals("table2", l.get(1).getName());

		// Make sure the getTables() is case preserving
		sql = "select * from MyTable";
		l = parser.getTables(sql, true);
		assertEquals(1, l.size());
		assertEquals("MyTable", l.get(0).getName());

		// Make sure the getTables() is case preserving
		sql = "select * from table1 as t1, table2";
		l = parser.getTables(sql, true);
		assertEquals(2, l.size());
		assertEquals("table1 as t1", l.get(0).getName());
		assertEquals("table2", l.get(1).getName());

		sql = "select r.id, r.name + ', ' + r.first_name, ara.* \n" +
             "from project_resource pr left join assigned_resource_activity ara ON (pr.resource_id = ara.id) \n" +
             "                         join resource r on (pr.resource_id = r.id) \n" +
             "where pr.project_id = 42 \n" +
             "and ara.id is null";
		l = parser.getTables(sql, true);
		assertEquals(3, l.size());
		assertEquals("project_resource pr", l.get(0).getName());
		assertEquals("assigned_resource_activity ara", l.get(1).getName());
		assertEquals("resource r", l.get(2).getName());

		sql = "SELECT x. FROM \"Dumb Named Schema\".\"Problematically Named Table\" x";
		l = parser.getTables(sql, false);
		assertEquals(l.size(), 1);
		assertEquals("\"Dumb Named Schema\".\"Problematically Named Table\"", l.get(0).getName());

		l = parser.getTables(sql, true);
		assertEquals(l.size(), 1);
		assertEquals("\"Dumb Named Schema\".\"Problematically Named Table\" x", l.get(0).getName());

		l = parser.getTables("select * from some_table limit 100;", false);
		assertEquals(l.size(), 1);
		assertEquals("some_table", l.get(0).getName());

		l = parser.getTables("select * from some_table as something;", false);
		assertEquals(l.size(), 1);
		assertEquals("some_table", l.get(0).getName());

		l = parser.getTables("select * from \"foo.bar\";", false);
		assertEquals(l.size(), 1);
		assertEquals("\"foo.bar\"", l.get(0).getName());

		l = parser.getTables("select * from public.\"foo.bar\";", false);
		assertEquals(l.size(), 1);
		assertEquals("public.\"foo.bar\"", l.get(0).getName());

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
		l = parser.getTables(sql, false);
		assertEquals(l.size(), 1);
		assertEquals("some_data", l.get(0).getName());

		l = parser.getTables(sql, true);
		assertEquals(1, l.size());
		assertEquals("some_data d", l.get(0).getName());

		sql = "select b.* from public.t1 a join public.t2 as b using (id)";
		l = parser.getTables(sql, true);
		assertEquals(2, l.size());
		assertEquals("public.t1 a", l.get(0).getName());
		assertEquals("public.t2 as b", l.get(1).getName());

		l = parser.getTables(sql, false);
		assertEquals(2, l.size());
		assertEquals("public.t1", l.get(0).getName());
		assertEquals("public.t2", l.get(1).getName());

		sql = "select * from \"table one\" as t1 join \"table two\" t2";
		l = parser.getTables(sql, false);
		assertEquals(2, l.size());
		assertEquals("\"table one\"", l.get(0).getName());
		assertEquals("\"table two\"", l.get(1).getName());

		sql = "select * from \"table one\" AS t1 join \"table two\" t2";
		l = parser.getTables(sql, true);
		assertEquals(2, l.size());
		assertEquals("\"table one\" AS t1", l.get(0).getName());
		assertEquals("\"table two\" t2", l.get(1).getName());

		sql =
			"select * " +
			"from (select * from foo) as t1 \n" +
			"  join (select * from foo2 ) AS t2 \n" +
			"    on (t1.id = t2.id)";
		l = parser.getTables(sql, false);
		assertEquals(2, l.size());
		assertEquals("t1", l.get(0).getName());
		assertEquals("t2", l.get(1).getName());
	}

}
