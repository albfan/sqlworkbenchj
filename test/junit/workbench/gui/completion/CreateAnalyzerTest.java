/*
 * CreateAnalyzerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class CreateAnalyzerTest
	extends WbTestCase
{

	public CreateAnalyzerTest()
	{
		super("CreateAnalyzerTest");
	}

	@Test
	public void textAnalyzer()
	{
		CreateAnalyzer analyzer = new CreateAnalyzer(null, "create index foo on bar ( );", 25);
		analyzer.checkContext();
		assertEquals(BaseAnalyzer.CONTEXT_COLUMN_LIST, analyzer.getContext());
		assertEquals("bar", analyzer.getTableForColumnList().getTableName());

		analyzer = new CreateAnalyzer(null, "create index foo on bar (lower(x),  );", 34);
		analyzer.checkContext();
		assertEquals(BaseAnalyzer.CONTEXT_COLUMN_LIST, analyzer.getContext());
		assertEquals("bar", analyzer.getTableForColumnList().getTableName());

		analyzer = new CreateAnalyzer(null, "create index foo on ", 20);
		analyzer.checkContext();
		assertEquals(BaseAnalyzer.CONTEXT_TABLE_LIST, analyzer.getContext());

		analyzer = new CreateAnalyzer(null, "create ", 7);
		analyzer.retrieveObjects();
		assertEquals(BaseAnalyzer.CONTEXT_KW_LIST, analyzer.getContext());
		List data = analyzer.getData();
		assertNotNull(data);
		assertTrue(data.contains("INDEX"));
		assertTrue(data.contains("TABLE"));
	}

}
