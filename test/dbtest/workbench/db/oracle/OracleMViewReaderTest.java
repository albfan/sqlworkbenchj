/*
 * OracleMViewReaderTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleMViewReaderTest
	extends WbTestCase
{

	public OracleMViewReaderTest()
	{
		super("OracleMViewReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
	}

	@After
	public void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetMViewSource1()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		String sql =
			"CREATE table person (id integer primary key, name varchar(100));" +
			"create materialized view v_person  \n" +
			"  build immediate \n" +
			"  refresh complete on demand with rowid \n" +
			"  enable query rewrite \n" +
			"as\n" +
			"select * from person;";
		TestUtil.executeScript(con, sql);
		OracleMViewReader reader = new OracleMViewReader();
		TableIdentifier mview = con.getMetadata().findObject(new TableIdentifier("V_PERSON"));
		String source = reader.getMViewSource(con, mview, null, false).toString();
		assertNotNull(source);
		String expected =
			"CREATE OR REPLACE MATERIALIZED VIEW V_PERSON\n" +
			"  BUILD IMMEDIATE\n" +
			"  REFRESH COMPLETE ON DEMAND WITH ROWID\n" +
			"  ENABLE QUERY REWRITE\n" +
			"AS\n" +
			"SELECT \"PERSON\".\"ID\" \"ID\",\"PERSON\".\"NAME\" \"NAME\" FROM \"PERSON\" \"PERSON\";";
		assertEquals(expected, source.trim());
	}

	@Test
	public void testGetMViewSource2()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		String sql =
			"CREATE table person (id integer primary key, name varchar(100));" +
			"create materialized view v_person  \n" +
			"  build deferred \n" +
			"  refresh force on commit with primary key\n" +
			"  disable query rewrite \n" +
			"as\n" +
			"select * from person;";
		TestUtil.executeScript(con, sql);
		OracleMViewReader reader = new OracleMViewReader();
		TableIdentifier mview = con.getMetadata().findObject(new TableIdentifier("V_PERSON"));
		String source = reader.getMViewSource(con, mview, null, false).toString();
		assertNotNull(source);
		String expected =
			"CREATE OR REPLACE MATERIALIZED VIEW V_PERSON\n" +
			"  BUILD DEFERRED\n" +
			"  REFRESH FORCE ON COMMIT WITH PRIMARY KEY\n" +
			"  DISABLE QUERY REWRITE\n" +
			"AS\n" +
			"SELECT \"PERSON\".\"ID\" \"ID\",\"PERSON\".\"NAME\" \"NAME\" FROM \"PERSON\" \"PERSON\";";
		assertEquals(expected, source.trim());
	}

	@Test
	public void testRefreshSource()
		throws Exception
	{

		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		String sql =
			"CREATE TABLE person (id integer);\n " +
			"create materialized view mv_person \n" +
			"refresh complete  \n" +
			"start with sysdate next round(sysdate + 1) + 5/24  \n" +
			"as  \n" +
			"select count(*) \n" +
			"from person \n" +
			";";

		TestUtil.executeScript(con, sql);
		OracleMViewReader reader = new OracleMViewReader();
		TableIdentifier mview = con.getMetadata().findObject(new TableIdentifier("MV_PERSON"));
		String source = reader.getMViewSource(con, mview, null, false).toString();
		assertNotNull(source);
		String expected =
			"CREATE OR REPLACE MATERIALIZED VIEW MV_PERSON\n" +
			"  BUILD IMMEDIATE\n" +
			"  REFRESH COMPLETE ON DEMAND WITH ROWID\n" +
			"  NEXT round(sysdate + 1) + 5/24\n" +
			"  DISABLE QUERY REWRITE\n" +
			"AS\n" +
			"select count(*) \n" +
			"from person;";
//		System.out.println("***\n"+expected + "\n++++\n" + source);
		assertEquals(expected, source.trim());
	}
}