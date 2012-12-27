/*
 * WbImportPostgresTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.postgres;

import java.io.File;

import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbImportPostgresTest
	extends WbTestCase
{

	private static final String TEST_ID = "wb_import_pg";

	public WbImportPostgresTest()
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
			"create table foo (id integer, firstname text, lastname text);\n" +
			"commit;\n");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void testImportCopy()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		StatementRunner runner = getTestUtil().createConnectedStatementRunner(con);

		TestUtil util = getTestUtil();
		File data = new File(util.getBaseDir(), "foo.txt");
		String content = "id|firstname|lastname\n1|Arthur|Dent\n2|Ford|Prefect\n";
		TestUtil.writeFile(data, content, "UTF-8");

		runner.runStatement("WbImport -file='" + data.getAbsolutePath() + "' -table=foo -type=text -header=true -delimiter='|' -usePgCopy");
		StatementRunnerResult result = runner.getResult();

//		String msg = result.getMessageBuffer().toString();
//		System.out.println(msg);
		assertTrue(result.isSuccess());

		Statement stmt = null;
		ResultSet rs = null;
		int rows = 0;
		try
		{
			stmt = con.createStatement();
			rs = stmt.executeQuery("select count(*) from foo");
			if (rs.next())
			{
				rows = rs.getInt(1);
				assertEquals(2, rows);
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}

		content = "id\tfirstname\tlastname\n1\tArthur\tDent\n2\tFord\tPrefect\n";
		TestUtil.writeFile(data, content, "UTF-8");

		runner.runStatement("WbImport -truncateTable=true -file='" + data.getAbsolutePath() + "' -table=foo -type=text -header=true -delimiter='\\t' -usePgCopy");
		result = runner.getResult();

		try
		{
			rs = stmt.executeQuery("select id, firstname, lastname from foo");
			rows = 0;
			while (rs.next())
			{
				rows++;
				int id = rs.getInt(1);
				String fname = rs.getString(2);
				String lname = rs.getString(3);
				if (id == 1)
				{
					assertEquals("Arthur", fname);
					assertEquals("Dent", lname);
				}
				else if (id == 2)
				{
					assertEquals("Ford", fname);
					assertEquals("Prefect", lname);
				}
				else
				{
					fail("Incorrect id imported");
				}
			}
			assertEquals(2, rows);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

	}

}
