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
package workbench.sql;


import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PgSqlParserTest
{

	public PgSqlParserTest()
	{
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void testGetNextCommand()
		throws Exception
	{
		String sql =
			"select * from foo;\n" +
			"commit;\n" +
			"delete from bar;";

		PgSqlParser parser = new PgSqlParser(sql);
		List<String> script = getCommands(parser);
		assertEquals(3, script.size());
		assertEquals("commit", script.get(1));

		sql =
			"creeate or replace function foo()\n" +
			"  returns integer \n" +
			"as \n" +
			"$body$\n" +
			"  declare l_value integer;\n" +
			"begin \n" +
			"   l_value := 42; \n" +
			"   return l_value; \n" +
			"end;\n" +
			"$body$\n"+
			"language plpgsql;";

		parser = new PgSqlParser(sql);
		script = getCommands(parser);
		assertEquals(1, script.size());
	}

	private List<String> getCommands(PgSqlParser parser)
	{
		List<String> result = new ArrayList<>();
		while (parser.hasMoreCommands())
		{
			ScriptCommandDefinition cmd = parser.getNextCommand();
			if (cmd != null) result.add(cmd.getSQL());
		}
		return result;
	}

}
