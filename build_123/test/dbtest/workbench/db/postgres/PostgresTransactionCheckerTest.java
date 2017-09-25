/*
 * PostgresTransactionCheckerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.postgres;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.DefaultTransactionChecker;
import workbench.db.TransactionChecker;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;

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
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testChecker()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
    assertNotNull(con);

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
