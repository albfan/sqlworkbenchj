/*
 * WbDefineVarTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import org.junit.AfterClass;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.sql.StatementRunner;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;
import workbench.util.StringUtil;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

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
	public void testExecute()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			VariablePool.getInstance().clear();
			StatementRunner runner = util.createConnectedStatementRunner();

			String sql = "--define some vars\nwbvardef theanswer = 42";
			SqlCommand command = runner.getCommandToUse(sql);
			assertTrue(command instanceof WbDefineVar);
			runner.runStatement(sql);

			String varValue = VariablePool.getInstance().getParameterValue("theanswer");
			assertEquals("Wrong variable defined", "42", varValue);

			sql = "--remove the variable\nWbVarDelete theanswer";
			command = runner.getCommandToUse(sql);
			assertTrue(command instanceof WbRemoveVar);
			runner.runStatement(sql);

			varValue = VariablePool.getInstance().getParameterValue("theanswer");
			assertEquals("Variable not deleted", true, StringUtil.isEmptyString(varValue));

			StatementRunnerResult result = null;
			runner.runStatement("\n\n--do it\ncreate table vartest (nr integer)");
			result = runner.getResult();
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			runner.runStatement("-- new entry\ninsert into vartest (nr) values (7)");
			result = runner.getResult();
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			runner.runStatement("commit");
			result = runner.getResult();
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());

			sql = "--define some vars\nwbvardef theanswer = @\"select nr from vartest\"";
			runner.runStatement(sql);
			varValue = VariablePool.getInstance().getParameterValue("theanswer");
			assertEquals("SQL Variable not set", "7", varValue);

			sql = "--define some vars\nwbvardef theanswer = \"@select nr from vartest\"";
			runner.runStatement(sql);
			varValue = VariablePool.getInstance().getParameterValue("theanswer");
			assertEquals("SQL Variable not set", "7", varValue);

			File f = new File(util.getBaseDir(), "vardef.props");

			PrintWriter pw = new PrintWriter(new FileWriter(f));
			pw.println("lastname=Dent");
			pw.println("firstname=Arthur");
			pw.close();

			sql = "--define some vars\nwbvardef -file=this_will_not_exist.blafile";
			runner.runStatement(sql);
			result = runner.getResult();
			assertEquals("Invalid file not detected", false, result.isSuccess());

			sql = "--define some vars\nwbvardef -file='" + f.getAbsolutePath() + "'";
			runner.runStatement(sql);
			result = runner.getResult();
			assertEquals("File not processed", true, result.isSuccess());

			varValue = VariablePool.getInstance().getParameterValue("lastname");
			assertEquals("SQL Variable not set", "Dent", varValue);

			sql = "-- remove a variable \n   " + WbRemoveVar.VERB + " lastname";
			runner.runStatement(sql);
			result = runner.getResult();
			assertEquals("Error deleting variable", true, result.isSuccess());

			varValue = VariablePool.getInstance().getParameterValue("lastname");
			assertEquals("SQL Variable still available ", true, StringUtil.isEmptyString(varValue));

			sql = "WbVardef var5=' a '";
			runner.runStatement(sql);
			varValue = VariablePool.getInstance().getParameterValue("var5");
			result = runner.getResult();
			assertEquals(result.getMessageBuffer().toString(), true, result.isSuccess());
			assertEquals("Quoted spaces trimmed", " a ", varValue);

			sql = "WbVardef var5=";
			runner.runStatement(sql);
			varValue = VariablePool.getInstance().getParameterValue("var5");
			assertNull("Variable not deleted", varValue);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
