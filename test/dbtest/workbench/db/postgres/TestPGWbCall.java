/*
 * TestPGWbCall.java
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
import org.junit.Test;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.WbCall;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TestPGWbCall
	extends WbTestCase
{
	private static final String TEST_ID = "wbcalltest";

	public TestPGWbCall()
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

//		String sql = "commit;\n";
//		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testGetAggregateSource()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		WbCall call = new WbCall();
		String cmd = "WbCall get_proc(?);";
		StatementRunnerResult result = call.execute(cmd);
		assertTrue(result.isSuccess());
	}

}
