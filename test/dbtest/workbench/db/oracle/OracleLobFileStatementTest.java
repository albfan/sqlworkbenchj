/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.oracle;

import java.io.File;
import java.sql.PreparedStatement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.util.FileUtil;
import workbench.util.LobFileStatement;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleLobFileStatementTest
	extends WbTestCase
{

	public OracleLobFileStatementTest()
	{
		super("OraLobFileTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"CREATE TABLE lob_test (id integer, some_data clob);\n");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testInsert()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull(con);
		TestUtil util = getTestUtil();
		String dir = util.getBaseDir();
		File data = new File(dir, "data.txt");

		FileUtil.writeString(data, "this is a test", false);

		String sql = "insert into lob_test (id, some_data) values (1, {$clobfile='" + data.getAbsolutePath() + "'})";
		LobFileStatement stmt = new LobFileStatement(sql);
		PreparedStatement pstmt = stmt.prepareStatement(con);
		int rows = pstmt.executeUpdate();
		assertEquals(1, rows);
		String content = (String)TestUtil.getSingleQueryValue(con, "select to_char(some_data) from lob_test where id = 1");
		assertEquals("this is a test", content);
	}

}
