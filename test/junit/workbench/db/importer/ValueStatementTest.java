/*
 * ValueStatementTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.db.importer;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.WbConnection;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ValueStatementTest
	extends WbTestCase
{

	public ValueStatementTest()
	{
		super("ValueStatementTest");
	}

	@Test
	public void testStatementParsing()
	{
		String sql = "select max(id) from the_table where some_col = $2 and other_col = $14";
		ValueStatement stmt = new ValueStatement(sql);
		assertEquals(1, stmt.getIndexInStatement(2));
		assertEquals(2, stmt.getIndexInStatement(14));
		assertEquals("select max(id) from the_table where some_col = ? and other_col = ?", stmt.getSelectSQL());
		Set<Integer> indexes = stmt.getInputColumnIndexes();
		assertEquals(2, indexes.size());
		assertTrue(indexes.contains(new Integer(2)));
		assertTrue(indexes.contains(new Integer(14)));
	}

	@Test
	public void testGetValue()
		throws Exception
	{
		TestUtil util = getTestUtil();

		String sql = "select max(id) from person where first_name = $7";
		ValueStatement stmt = new ValueStatement(sql);
		assertEquals(1, stmt.getIndexInStatement(7));
		String script = "CREATE TABLE person (id integer, first_name varchar(50), last_name varchar(50));\n" +
			"INSERT INTO person VALUES (1, 'Arthur', 'Dent');\n" +
			"INSERT INTO person VALUES (2, 'Zaphod', 'Beeblebrox');\n" +
			"COMMIT\n";
		WbConnection con = util.getConnection();
		try
		{
			TestUtil.executeScript(con, script);
			Map<Integer, Object> data = new HashMap<Integer, Object>();
			data.put(7, "Arthur");
			Object id = stmt.getDatabaseValue(con, data);
			assertNotNull(id);
			assertEquals(new Integer(1), id);
			stmt.done();
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
