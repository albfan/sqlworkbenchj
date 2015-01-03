/*
 * CteParserTest.java
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

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class CteParserTest
{
	public CteParserTest()
	{
	}

	@Before
	public void setUp()
	{
	}

	@Test
	public void testParseSql()
	{

	}

	@Test
	public void testColDefs()
	{
		String cte =
			"with cte (one, two) as (" +
			"  select x,y from bar " +
			"), " +
			"other as ( " +
			"   select c.x as x2, c.y as y2, f.a \n" +
			"   from cte c \n" +
			"     join foo f on c.x = f.id \n" +
			") " +
			"select * from other";

		CteParser analyzer = new CteParser(cte);
		List<CteDefinition> result = analyzer.getCteDefinitions();
		assertNotNull(result);
		assertEquals(2, result.size());
		CteDefinition t1 = result.get(0);
		assertEquals("cte", t1.getName());
		assertEquals(2, t1.getColumns().size());
		assertEquals("one", t1.getColumns().get(0).getColumnName());
		assertEquals("two", t1.getColumns().get(1).getColumnName());
		assertEquals("select x,y from bar", t1.getInnerSql().trim());
		assertEquals("select * from other", analyzer.getBaseSql().trim());
		assertEquals(t1.getStartInStatement(), cte.indexOf("as (") + 4);
		assertEquals("select x,y from bar", cte.substring(t1.getStartInStatement(), t1.getEndInStatement()).trim());
	}

	@Test
	public void testSplitCtes()
	{
		String cte = "with cte as (" +
			"  select x,y from bar " +
			"), " +
			"other as ( " +
			"   select c.x as x2, c.y as y2, f.a " +
			"   from cte c " +
			"     join foo f on c.x = f.id " +
			") " +
			"select * from other";

		CteParser analyzer = new CteParser(cte);
		List<CteDefinition> result = analyzer.getCteDefinitions();
		assertNotNull(result);
		assertEquals(2, result.size());
		CteDefinition t1 = result.get(0);
		assertEquals("cte", t1.getName());
		assertEquals(2, t1.getColumns().size());
		assertEquals("x", t1.getColumns().get(0).getColumnName());
		assertEquals("y", t1.getColumns().get(1).getColumnName());

//		System.out.println(result.get(1));
//		System.out.println(cte.substring(result.get(1).getStartInStatement(), result.get(1).getEndInStatement()));

		CteDefinition t2 = result.get(1);
		assertEquals("other", t2.getName());
		assertEquals(3, t2.getColumns().size());
		assertEquals("x2", t2.getColumns().get(0).getColumnName());
		assertEquals("y2", t2.getColumns().get(1).getColumnName());
		assertEquals("a", t2.getColumns().get(2).getColumnName());
	}

	@Test
	public void testRecursive()
	{
		String cte =
			"with recursive cte as (" +
			"  select x, y from bar " +
			") " +
			"select * from cte";

		CteParser analyzer = new CteParser(cte);
		List<CteDefinition> result = analyzer.getCteDefinitions();
		assertNotNull(result);
		assertEquals(1, result.size());
		CteDefinition t1 = result.get(0);
		assertEquals("cte", t1.getName());
		assertEquals(2, t1.getColumns().size());
		assertEquals("x", t1.getColumns().get(0).getColumnName());
		assertEquals("y", t1.getColumns().get(1).getColumnName());
	}

	@Test
	public void testWriteable()
	{
		String cte =
			"with new_rows (id, nr) as (" +
			"  insert into foo values (1,2) returning * " +
			") " +
			"select * from new_rows";

		CteParser analyzer = new CteParser(cte);
		List<CteDefinition> result = analyzer.getCteDefinitions();
		assertNotNull(result);
		assertEquals(1, result.size());
		CteDefinition t1 = result.get(0);
		assertEquals("new_rows", t1.getName());
		assertEquals(2, t1.getColumns().size());
		assertEquals("id", t1.getColumns().get(0).getColumnName());
		assertEquals("nr", t1.getColumns().get(1).getColumnName());
		assertEquals("insert into foo values (1,2) returning *", t1.getInnerSql().trim());
	}

}
