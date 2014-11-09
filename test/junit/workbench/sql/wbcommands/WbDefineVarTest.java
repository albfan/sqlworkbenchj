/*
 * WbDefineVarTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;

import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;

import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDefineVarTest
	extends WbTestCase
{

	public WbDefineVarTest()
	{
		super("WbDefineVarTest");
	}

	@AfterClass
	public static void tearDown()
	{
		VariablePool.getInstance().clear();
	}

	@Test
	public void testEmpty()
		throws Exception
	{
		VariablePool.getInstance().clear();
		WbDefineVar cmd = new WbDefineVar();
		cmd.execute("WbVardef foo=$[foo]2");
		String value = VariablePool.getInstance().getParameterValue("foo");
		assertEquals("$[foo]2", value);

		VariablePool.getInstance().clear();
		cmd.execute("WbVardef -removeUndefined foo=$[foo]42");
		value = VariablePool.getInstance().getParameterValue("foo");
		assertEquals("42", value);
	}

	@Test
	public void testSelect()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			VariablePool.getInstance().clear();
			StatementRunner runner = util.createConnectedStatementRunner();

			WbConnection conn = runner.getConnection();

			TestUtil.executeScript(conn,
				"create table vartest (nr integer, some_value integer);\n" +
				"insert into vartest (nr, some_value) values (7, 42);\n" +
				"commit;\n");

			String sql = "--define some vars\nwbvardef theanswer = @\"select nr from vartest\"";
			runner.runStatement(sql);
			String varValue = VariablePool.getInstance().getParameterValue("theanswer");
			assertEquals("SQL Variable not set", "7", varValue);

			sql = "--define some vars\nwbvardef theanswer = \"@select nr from vartest\"";
			runner.runStatement(sql);
			varValue = VariablePool.getInstance().getParameterValue("theanswer");
			assertEquals("SQL Variable not set", "7", varValue);

			sql = "wbvardef id, value=@\"select nr, some_value from vartest\"";
			runner.runStatement(sql);
			StatementRunnerResult result = runner.getResult();
			String idValue = VariablePool.getInstance().getParameterValue("id");
			assertEquals("SQL Variable not set", "7", idValue);
			String someValue = VariablePool.getInstance().getParameterValue("value");
			assertEquals("SQL Variable not set", "42", someValue);

			String msg = result.getMessageBuffer().toString();
			assertTrue(msg.contains("Variable id defined with value '7'"));
			assertTrue(msg.contains("Variable value defined with value '42'"));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();
		VariablePool.getInstance().clear();
		WbDefineVar cmd = new WbDefineVar();
		WbRemoveVar remove = new WbRemoveVar();

		cmd.execute("wbvardef theanswer = 42");

		String varValue = VariablePool.getInstance().getParameterValue("theanswer");
		assertEquals("Wrong variable defined", "42", varValue);


		remove.execute("nWbVarDelete theanswer");

		varValue = VariablePool.getInstance().getParameterValue("theanswer");
		assertEquals("Variable not deleted", true, StringUtil.isEmptyString(varValue));

		File f = new File(util.getBaseDir(), "vardef.props");

		TestUtil.writeFile(f,
			"lastname=Dent\n" +
			"firstname=Arthur");

		StatementRunnerResult result = cmd.execute("wbvardef -file=this_will_not_exist.blafile");
		assertFalse("Invalid file not detected", result.isSuccess());

		result = cmd.execute("wbvardef -file='" + f.getAbsolutePath() + "'");
		assertTrue("File not processed", result.isSuccess());

		varValue = VariablePool.getInstance().getParameterValue("lastname");
		assertEquals("SQL Variable not set", "Dent", varValue);

		result = remove.execute(WbRemoveVar.VERB + " lastname");
		assertTrue("Error deleting variable", result.isSuccess());

		varValue = VariablePool.getInstance().getParameterValue("lastname");
		assertEquals("SQL Variable still available ", true, StringUtil.isEmptyString(varValue));

		result = cmd.execute("WbVardef var5=' a '");
		varValue = VariablePool.getInstance().getParameterValue("var5");
		assertTrue(result.getMessageBuffer().toString(), result.isSuccess());
		assertEquals("Quoted spaces trimmed", " a ", varValue);

		cmd.execute("WbVardef var5=");
		varValue = VariablePool.getInstance().getParameterValue("var5");
		assertNull("Variable not deleted", varValue);
	}

	@Test
	public void testLookup()
		throws SQLException
	{
		VariablePool.getInstance().clear();
		WbDefineVar cmd = new WbDefineVar();
		StatementRunnerResult result = cmd.execute("wbvardef -variable=xxx -values=one,two,three");
		assertNotNull(result);
		assertTrue(result.isSuccess());
		List<String> values = VariablePool.getInstance().getLookupValues("xxx");
		assertNotNull(values);
		assertEquals(3, values.size());
		assertEquals("one", values.get(0));
		assertEquals("two", values.get(1));
		assertEquals("three", values.get(2));

		VariablePool.getInstance().clear();
		result = cmd.execute("wbvardef -variable=xxx -value=foo -values=one,two,three,four");
		assertNotNull(result);
		assertTrue(result.isSuccess());
		values = VariablePool.getInstance().getLookupValues("xxx");
		assertNotNull(values);
		assertEquals("foo", VariablePool.getInstance().getParameterValue("xxx"));
		assertEquals(4, values.size());
		assertEquals("one", values.get(0));
		assertEquals("two", values.get(1));
		assertEquals("three", values.get(2));
		assertEquals("four", values.get(3));
	}

	@Test
	public void testReadFileContent()
		throws Exception
	{
		VariablePool.getInstance().clear();
		WbDefineVar cmd = new WbDefineVar();
		TestUtil util = getTestUtil();
		WbFile data = new WbFile(util.getBaseDir(), "data.txt");
		String content = "this is the variable value";
		TestUtil.writeFile(data, content, "UTF-8");
		cmd.execute("WbDefineVar -contentFile='" + data.getFullPath() + "' -encoding='UTF-8' -variable=filevar");
		assertEquals(content, VariablePool.getInstance().getParameterValue("filevar"));
	}

	@Test
	public void testReadFileContentWithVars()
		throws Exception
	{
		VariablePool.getInstance().clear();
		VariablePool.getInstance().setParameterValue("somevalue", "42");

		WbDefineVar cmd = new WbDefineVar();
		TestUtil util = getTestUtil();
		WbFile data = new WbFile(util.getBaseDir(), "data.txt");
		String content = "the answer: $[somevalue]";
		TestUtil.writeFile(data, content, "UTF-8");

		cmd.execute("WbDefineVar -contentFile='" + data.getFullPath() + "' -encoding='UTF-8' -variable=filevar");
		String expected = "the answer: 42";
		String value = VariablePool.getInstance().getParameterValue("filevar");
		assertEquals(expected, value);
	}

}
