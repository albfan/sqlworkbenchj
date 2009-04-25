/*
 * VariablePoolTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Set;
import junit.framework.TestCase;
import workbench.AppArguments;
import workbench.TestUtil;
import workbench.storage.DataStore;
import workbench.util.ArgumentParser;

/**
 *
 * @author support@sql-workbench.net
 */
public class VariablePoolTest extends TestCase
{
	
	public VariablePoolTest(String testName)
	{
		super(testName);
	}


	public void testInitFromCommandLine()
	{
		try
		{
			TestUtil util = new TestUtil(this.getName());
			
			ArgumentParser p = new ArgumentParser();
			p.addArgument(AppArguments.ARG_VARDEF);
			p.parse("-" + AppArguments.ARG_VARDEF + "='#exportfile=/user/home/test.txt'");
			VariablePool pool = VariablePool.getInstance();
			pool.readDefinition(p.getValue(AppArguments.ARG_VARDEF));
			assertEquals("Wrong parameter retrieved from commandline", "/user/home/test.txt", pool.getParameterValue("exportfile"));
			
			File f = new File(util.getBaseDir(), "vars.properties");
			
			FileWriter out = new FileWriter(f);
			out.write("exportfile=/user/home/export.txt\n");
			out.write("exporttable=person\n");
			out.close();

			pool.clear();
			p.parse("-" + AppArguments.ARG_VARDEF + "='" + f.getAbsolutePath() + "'");
			pool.readDefinition(p.getValue(AppArguments.ARG_VARDEF));
			assertEquals("Wrong parameter retrieved from file", "/user/home/export.txt", pool.getParameterValue("exportfile"));
			assertEquals("Wrong parameter retrieved from file", "person", pool.getParameterValue("exporttable"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testInitFromProperties()
	{
		try
		{
			System.setProperty(VariablePool.PROP_PREFIX + "testvalue", "value1");
			System.setProperty(VariablePool.PROP_PREFIX + "myprop", "value2");
			System.setProperty("someprop.testvalue", "value2");
			VariablePool pool = VariablePool.getInstance();
		
			pool.initFromProperties(System.getProperties());
			assertEquals("Wrong firstvalue", "value1", pool.getParameterValue("testvalue"));
			assertEquals("Wrong firstvalue", "value2", pool.getParameterValue("myprop"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testPool()
		throws Exception
	{
		TestUtil util = new TestUtil("testPool");
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
		
		PrintWriter pw = new PrintWriter(new FileWriter(f));
		pw.println("lastname=Dent");
		pw.println("firstname=Arthur");
		pw.close();
		pool.readFromFile(f.getAbsolutePath(), null);
		
		value = pool.getParameterValue("lastname");
		assertEquals("Lastname not defined", "Dent", value);
		value = pool.getParameterValue("firstname");
		assertEquals("Firstname not defined", "Arthur", value);
		
		f.delete();
	}

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
}
