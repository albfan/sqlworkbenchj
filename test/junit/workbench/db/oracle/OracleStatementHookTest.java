/*
 * OracleStatementHookTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.oracle;

import static org.junit.Assert.*;
import org.junit.Test;

import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleStatementHookTest
	extends WbTestCase
{
	public OracleStatementHookTest()
	{
	}

	/**
	 * Test of injectHint method, of class OracleStatementHook.
	 */
	@Test
	public void testInjectHint()
	{
		String sql = "select * from foo";
		OracleStatementHook hook = new OracleStatementHook();
		String expResult = "select /*+ gather_plan_statistics */ * from foo";
		String result = hook.injectHint(sql);
		assertEquals(expResult, result);

		sql = "select /*+qb_name(foo) */ * from foo";
		expResult = "select /*+ gather_plan_statistics qb_name(foo) */ * from foo";
		result = hook.injectHint(sql);
		assertEquals(expResult, result);

		sql = "select /*+ qb_name(foo) */ * from foo";
		expResult = "select /*+ gather_plan_statistics  qb_name(foo) */ * from foo";
		result = hook.injectHint(sql);
		assertEquals(expResult, result);

		sql = "select /* do stuff */ * from foo";
		expResult = "select /*+ gather_plan_statistics */ /* do stuff */ * from foo";
		result = hook.injectHint(sql);
		assertEquals(expResult, result);

		sql = "select /*+ gather_plan_statistics */ * from foo";
		expResult = "select /*+ gather_plan_statistics */ * from foo";
		result = hook.injectHint(sql);
		assertEquals(expResult, result);

		sql = "with foobar as (\n" +
			"select * \n" +
			"from foo)\n" +
			"select * from foobar";
		result = hook.injectHint(sql);
		assertTrue(result.startsWith("with /*+ gather_plan_statistics */ foobar as"));
	}

}
