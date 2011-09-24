/*
 * PostgresTransactionCheckerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

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
public class OracleTransactionCheckerTest
	extends WbTestCase
{
	public OracleTransactionCheckerTest()
	{
		super("OracleTransactionCheckerTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;
		TestUtil.executeScript(con, "create table t (id integer);");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testChecker()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		try
		{
			Settings.getInstance().setProperty("workbench.db.oracle.opentransaction.check", true);
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
			Settings.getInstance().setProperty("workbench.db.oracle.opentransaction.check", false);
			ConnectionMgr.getInstance().disconnectAll();
		}
	}


}
