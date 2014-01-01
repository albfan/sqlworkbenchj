/*
 * DdlCommandTest.java
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
