/*
 * WbDefinePkTest.java
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
package workbench.sql.wbcommands;

import java.util.Map;
import org.junit.AfterClass;
import workbench.TestUtil;
import workbench.sql.StatementRunner;
import workbench.sql.SqlCommand;
import workbench.storage.PkMapping;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDefinePkTest
	extends WbTestCase
{

	public WbDefinePkTest()
	{
		super("WbDefinePkTest");
	}

	@AfterClass
	public static void tearDown()
	{
		PkMapping.getInstance().clear();
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = new TestUtil(getClass().getName() + "_testExecute");
		util.prepareEnvironment();
		StatementRunner runner = util.createConnectedStatementRunner();

		String sql = "--define a new PK for a view\nwbdefinepk junitpk=id,name";
		SqlCommand command = runner.getCommandToUse(sql);
		assertTrue(command instanceof WbDefinePk);
		runner.runStatement(sql);

		Map mapping = PkMapping.getInstance().getMapping();
		String cols = (String) mapping.get("junitpk");
		assertEquals("Wrong pk mapping stored", "id,name", cols);

	}
}
