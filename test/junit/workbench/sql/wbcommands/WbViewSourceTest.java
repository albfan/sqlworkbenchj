/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.sql.wbcommands;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import workbench.sql.StatementRunnerResult;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbViewSourceTest
	extends WbTestCase
{

	public WbViewSourceTest()
	{
		super("WbViewSourceTest");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection conn = util.getConnection();
		TestUtil.executeScript(conn,
			"create table foo (id integer not null primary key);\n" +
			"create view v_foo as select id, id * 42 as id2 from foo;\n" +
			"commit;");

		WbViewSource stmt = new WbViewSource();
		stmt.setConnection(conn);
		StatementRunnerResult result = stmt.execute("WbViewSource v_foo");
		assertTrue(result.isSuccess());
		String sql = result.getMessageBuffer().toString();
		assertTrue(sql.contains("VIEW PUBLIC.V_FOO"));
		assertTrue(sql.contains("ID * 42"));
	}

}
