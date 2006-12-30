/*
 * VariablePoolTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import junit.framework.*;
import java.util.Set;
import workbench.TestUtil;

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
		pool.readFromFile(f.getAbsolutePath());
		
		value = pool.getParameterValue("lastname");
		assertEquals("Lastname not defined", "Dent", value);
		value = pool.getParameterValue("firstname");
		assertEquals("Firstname not defined", "Arthur", value);
		
		f.delete();
	}
}
