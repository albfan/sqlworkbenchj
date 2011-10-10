/*
 * PostgresTransactionCheckerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import static junit.framework.Assert.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.DefaultTransactionChecker;
import workbench.db.TransactionChecker;
import workbench.db.WbConnection;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresTransactionCheckerTest
	extends WbTestCase
{

	private static final String TEST_ID = "transactionchecker";

	public PostgresTransactionCheckerTest()
	{
		super("PostgresTransactionCheckerTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;
		TestUtil.executeScript(con, "create table t (id integer);\n" +
			"commit;\n");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testChecker()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		try
		{
			con.getProfile().setDetectOpenTransaction(true);
			TransactionChecker checker = con.getTransactionChecker();
			assertFalse(checker == TransactionChecker.NO_CHECK);
			assertTrue(checker instanceof DefaultTransactionChecker);

			assertFalse(checker.hasUncommittedChanges(con));

			TestUtil.executeScript(con, "insert into t values (42);");
			assertTrue(checker.hasUncommittedChanges(con));

			TestUtil.executeScript(con, "commit;");
			assertFalse(checker.hasUncommittedChanges(con));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}


}
