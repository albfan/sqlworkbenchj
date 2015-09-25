/*
 * VariablePoolTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.sql;

import java.io.File;
import java.util.Set;

import workbench.AppArguments;
import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.storage.DataStore;

import workbench.util.ArgumentParser;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class VariablePoolTest
	extends WbTestCase
{

	public VariablePoolTest()
	{
		super("VariablePoolTest");
	}

	@AfterClass
	public static void tearDown()
	{
		VariablePool.getInstance().reset();
	}

	@Before
	public void beforeTest()
	{
		VariablePool.getInstance().reset();
	}

	@Test
	public void testNoSuffix()
	{
		VariablePool pool = VariablePool.getInstance();
		pool.setPrefixSuffix(":", "");
		pool.setParameterValue("some_id", "1");
		String replaced = pool.replaceAllParameters("select * from foo where id = :some_id");
		assertEquals("select * from foo where id = 1", replaced);

		replaced = pool.replaceAllParameters("select * from foo where id =:some_id and col = 42");
		assertEquals("select * from foo where id =1 and col = 42", replaced);

		replaced = pool.replaceAllParameters("select * from foo where id >:some_id");
		assertEquals("select * from foo where id >1", replaced);

		pool.setParameterValue("other_id", "2");
		replaced = pool.replaceAllParameters("select * from foo where id in (:some_id, :other_id)");
		assertEquals("select * from foo where id in (1, 2)", replaced);

		pool.setParameterValue("some_name", "Dent");
		replaced = pool.replaceAllParameters("select * from person where lastname = ':some_name';");
		assertEquals("select * from person where lastname = 'Dent';", replaced);
	}

	@Test
	public void testRemoveVars()
	{
		VariablePool pool = VariablePool.getInstance();
		String result = pool.removeVariables("42$[foo]");
		assertEquals("42", result);

		result = pool.removeVariables("$[foo]42");
		assertEquals("42", result);

		result = pool.removeVariables("$[?foo]42");
		assertEquals("42", result);

		result = pool.removeVariables("4$[&foo]2");
		assertEquals("42", result);
	}

	@Test
	public void testInitFromCommandLine()
		throws Exception
	{
		TestUtil util = getTestUtil();
		VariablePool pool = VariablePool.getInstance();

		ArgumentParser p = new ArgumentParser();
		p.addArgument(AppArguments.ARG_VARDEF);
		p.parse("-" + AppArguments.ARG_VARDEF + "='#exportfile=/user/home/test.txt'");
		pool.readDefinition(p.getValue(AppArguments.ARG_VARDEF));
		assertEquals("Wrong parameter retrieved from commandline", "/user/home/test.txt", pool.getParameterValue("exportfile"));

		File f = new File(util.getBaseDir(), "vars.properties");

    TestUtil.writeFile(f,
      "exportfile=/user/home/export.txt\n" +
      "exporttable=person\n");
		pool.clear();
		p.parse("-" + AppArguments.ARG_VARDEF + "='" + f.getAbsolutePath() + "'");
		pool.readDefinition(p.getValue(AppArguments.ARG_VARDEF));
		assertEquals("Wrong parameter retrieved from file", "/user/home/export.txt", pool.getParameterValue("exportfile"));
		assertEquals("Wrong parameter retrieved from file", "person", pool.getParameterValue("exporttable"));
	}

	@Test
	public void testInitFromProperties()
		throws Exception
	{
		VariablePool pool = VariablePool.getInstance();
		System.setProperty(VariablePool.PROP_PREFIX + "testvalue", "value1");
		System.setProperty(VariablePool.PROP_PREFIX + "myprop", "value2");
		System.setProperty("someprop.testvalue", "value2");

		pool.initFromProperties(System.getProperties());
		assertEquals("Wrong firstvalue", "value1", pool.getParameterValue("testvalue"));
		assertEquals("Wrong firstvalue", "value2", pool.getParameterValue("myprop"));
	}

	@Test
	public void testPool()
		throws Exception
	{
		TestUtil util = getTestUtil();
		util.prepareEnvironment();

		VariablePool pool = VariablePool.getInstance();

		pool.setParameterValue("id", "42");

		String value = pool.getParameterValue("id");
		assertEquals("Wrong value stored", "42", value);

		String sql = "select * from test where id=$[id]";
		String realSql = pool.replaceAllParameters(sql);
		assertEquals("Parameter not replaced", "select * from test where id=42", realSql);

		sql = "select * from test where id=$[?id]";
		boolean hasPrompt = pool.hasPrompt(sql);
		assertEquals("Prompt not detected", true, hasPrompt);

		sql = "select * from test where id=$[&id]";
		Set vars = pool.getVariablesNeedingPrompt(sql);
		assertEquals("Prompt not detected", 0, vars.size());

		pool.removeValue("id");
		vars = pool.getVariablesNeedingPrompt(sql);
		assertEquals("Prompt not detected", 1, vars.size());
		assertEquals("Variable not in prompt pool", true, vars.contains("id"));

		File f = new File(util.getBaseDir(), "vardef.props");

    TestUtil.writeFile(f,
      "lastname=Dent\n" +
       "firstname=Arthur");
		pool.readFromFile(f.getAbsolutePath(), null);

		value = pool.getParameterValue("lastname");
		assertEquals("Lastname not defined", "Dent", value);
		value = pool.getParameterValue("firstname");
		assertEquals("Firstname not defined", "Arthur", value);
		assertTrue(f.delete());
	}

	@Test
	public void testDataStore()
		throws Exception
	{
		VariablePool pool = VariablePool.getInstance();
		pool.clear();
		DataStore ds = pool.getVariablesDataStore();
		assertEquals(0, ds.getRowCount());
		int row = ds.addRow();
		ds.setValue(row, 0, "varname");
		ds.setValue(row, 1, "value");
		ds.updateDb(null, null);
		assertEquals(1, pool.getParameterCount());
		assertEquals("value", pool.getParameterValue("varname"));
	}

	@Test
	public void testAlternatePrefix()
	{
		VariablePool pool = VariablePool.getInstance();
		pool.setPrefixSuffix("${", "}");
		pool.setParameterValue("foo.bar.value", "1");
		String sql = "select * from foo where bar = ${foo.bar.value}";
		String replaced = pool.replaceAllParameters(sql);
		assertEquals("select * from foo where bar = 1", replaced);
	}
}
