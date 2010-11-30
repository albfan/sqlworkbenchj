/*
 * WbCopyPostgresTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import workbench.sql.wbcommands.*;
import java.sql.ResultSet;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class WbCopyPostgresTest
	extends WbTestCase
{

	private static final String TEST_ID = "wbcopy_test_pg";

	public WbCopyPostgresTest()
	{
		super(TEST_ID);
	}


	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"create table person (id integer, first_name varchar(50), last_name varchar(50));\n" +
			"insert into person (id, first_name, last_name) values (1, 'Arthur', 'Dent');\n" +
			"insert into person (id, first_name, last_name) values (2, 'Ford', 'Prefect');\n" +
			"commit;\n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		Settings.getInstance().setProperty("workbench.sql.ignoreschema.postgresql", "public");
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}
	
	@Test
	public void testTempTarget()
		throws Exception
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			WbConnection con = PostgresTestUtil.getPostgresConnection();
			if (con == null) return;
			
			con.getMetadata().resetSchemasToIgnores();
			
			Settings.getInstance().setProperty("workbench.sql.ignoreschema.postgresql", "public," + TEST_ID);

			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(con);

			TestUtil.executeScript(con,
				"create temporary table target_person (id integer, first_name varchar(50), last_name varchar(50));\n " +
				"commit;\n");

			String sql = 
				"wbcopy -sourceQuery='select id, first_name, last_name from person' " +
				"       -targetTable=target_person -skipTargetCheck=true";

			StatementRunnerResult result = copyCmd.execute(sql);
			String msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());

			stmt = con.createStatement();
			rs = stmt.executeQuery("select count(*) from target_person");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 2, count);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	public void testCopyFromH2()
		throws Exception
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			WbConnection pgCon = PostgresTestUtil.getPostgresConnection();
			if (pgCon == null) return;

			TestUtil util = getTestUtil();
			WbConnection source = util.getConnection("copyCreateTestSource"); // H2
			
			WbCopy copyCmd = new WbCopy();
			copyCmd.setConnection(pgCon);

			TestUtil.executeScript(source,
				"create table person (id integer, first_name varchar(50), last_name varchar(50));\n " +
				"insert into person (id, first_name, last_name) values (1, 'Arthur', 'Dent');\n" +
				"commit;\n");

			String sql =
				"wbcopy -sourceQuery='select id as person_id, first_name as vorname, last_name  as nachname from person' " +
				"       -targetTable=some_person -createTarget=true \n"+
				"       -sourceProfile=" + source.getProfile().getName() + " \n ";

			StatementRunnerResult result = copyCmd.execute(sql);
			String msg = result.getMessageBuffer().toString();
			assertEquals(msg, true, result.isSuccess());

			stmt = pgCon.createStatement();
			rs = stmt.executeQuery("select count(*) from some_person where nachname = 'Dent'");
			if (rs.next())
			{
				int count = rs.getInt(1);
				assertEquals("Incorrect number of rows copied", 1, count);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		
	}
}
