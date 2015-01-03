/*
 * OracleExplainAnalyzerTest.java
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
package workbench.gui.completion;

import workbench.util.CollectionUtil;
import java.util.List;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleExplainAnalyzerTest
	extends WbTestCase
{

	public OracleExplainAnalyzerTest()
	{
		super("OracleExplainAnalyzerTest");
	}

	@Test
	public void testGetStatementStart()
	{
		String sql = "explain plan for select * from person";
		OracleExplainAnalyzer explain = new OracleExplainAnalyzer(null, sql, 8);
		String explained = explain.getExplainedStatement();
		assertEquals("select * from person", explained);

		explain.checkContext();
		List options = explain.getData();
		assertNotNull(options);
		List<String> expectedOptions = CollectionUtil.arrayList("SET STATEMENT_ID=", "INTO");
		for (String option : expectedOptions)
		{
			assertTrue(options.contains(option));
		}

		sql = "explain set statement_id = 'wb' plan for select * from person";
		explain = new OracleExplainAnalyzer(null, sql, sql.indexOf("'wb'") + 4);
		explain.checkContext();
		options = explain.getData();
		assertNotNull(options);
		expectedOptions = CollectionUtil.arrayList("INTO");
		for (String option : expectedOptions)
		{
			assertTrue(options.contains(option));
		}

		sql = "explain set statement_id = 'wb' into plan for select * from person";
		explain = new OracleExplainAnalyzer(null, sql, sql.indexOf("into") + 5);
		explain.checkContext();
		assertEquals(BaseAnalyzer.CONTEXT_TABLE_LIST, explain.getContext());
	}
}
