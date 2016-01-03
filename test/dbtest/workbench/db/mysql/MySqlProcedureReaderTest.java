/*
 * MySqlProcedureReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.mysql;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;

import workbench.sql.DelimiterDefinition;
import workbench.sql.parser.ScriptParser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import workbench.sql.parser.ParserType;

/**
 *
 * @author Thomas Kellerer
 */
public class MySqlProcedureReaderTest
	extends WbTestCase
{

	public MySqlProcedureReaderTest()
	{
		super("MySqlProcedureReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		MySQLTestUtil.initTestcase("MySqlProcedureReaderTest");
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;
		String sql =
			"CREATE PROCEDURE simpleproc (OUT param1 INT) \n"+
			"BEGIN \n" +
			"   SELECT COUNT(*) INTO param1 FROM t;\n" +
			"END\n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		if (con == null) return;
		String sql =
			"DROP PROCEDURE simpleproc;";
		TestUtil.executeScript(con, sql);
		MySQLTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetProcedureHeader()
		throws Exception
	{
		WbConnection con = MySQLTestUtil.getMySQLConnection();
		assertNotNull("No connection available", con);

		List<ProcedureDefinition> procs = con.getMetadata().getProcedureReader().getProcedureList(MySQLTestUtil.DB_NAME, null, null);
		assertNotNull(procs);
		assertEquals(1, procs.size());
		ProcedureDefinition proc = procs.get(0);
		assertEquals("simpleproc", proc.getProcedureName());
		con.getProfile().setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		String source = proc.getSource(con).toString();
//		System.out.println(source);
		ScriptParser p = new ScriptParser(source, ParserType.MySQL);
		p.setAlternateDelimiter(Settings.getInstance().getAlternateDelimiter(con, DelimiterDefinition.DEFAULT_ORA_DELIMITER));
		assertEquals(2, p.getSize());
		String create = p.getCommand(1);
		String expected =
			"CREATE PROCEDURE simpleproc (OUT param1 INT)\n"+
			"BEGIN \n" +
			"   SELECT COUNT(*) INTO param1 FROM t;\n" +
			"END";
		assertEquals(expected, create.trim());
	}
}
