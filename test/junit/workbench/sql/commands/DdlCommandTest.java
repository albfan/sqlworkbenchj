/*
 * DdlCommandTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DdlCommandTest
	extends WbTestCase
{

	public DdlCommandTest()
	{
		super("DdlCommandTest");
	}

	@Test
	public void testIgnoreDropErrors()
		throws Exception
	{
		TestUtil util = getTestUtil();
		StatementRunner runner = util.createConnectedStatementRunner();
		String sql = "drop table does_not_exist";
		runner.setIgnoreDropErrors(true);
		runner.runStatement(sql);
		StatementRunnerResult result = runner.getResult();
		assertTrue(result.isSuccess());

		runner.setIgnoreDropErrors(false);
		runner.setUseSavepoint(true);
		runner.runStatement(sql);
		result = runner.getResult();
		assertFalse(result.isSuccess());
	}


}
