/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

package workbench.sql.parser;

import workbench.sql.parser.IteratingScriptParser;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptCommandDefinition;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class IteratingScriptParserTest
{

	public IteratingScriptParserTest()
	{
	}

	@Test
	public void testEscapedQuotes()
	{
		IteratingScriptParser parser = new IteratingScriptParser();
		parser.setCheckEscapedQuotes(true);
		parser.setScript(
			"insert into foo (data) values ('foo\\'s data1');\n" +
			"insert into foo (data) values ('foo\\'s data2');" +
			"commit;\n");
		parser.setStoreStatementText(true);
		ScriptCommandDefinition c = null;
		int index = 0;

		List<ScriptCommandDefinition> commands = new ArrayList<>();
		while ((c = parser.getNextCommand()) != null)
		{
			c.setIndexInScript(index);
			commands.add(c);
		}
		assertEquals(3, commands.size());
		assertEquals("commit", commands.get(2).getSQL());
	}

	@Test
	public void testTrailingWhitespace()
		throws Exception
	{
		String sql =
			"1\n" +
			"/ \n" +
			"\n" +
			"-- some comment \n" +
			"2\n" +
			"/\n";
		IteratingScriptParser p = new IteratingScriptParser();
		p.setDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		p.setScript(sql);
		p.setStoreStatementText(true);

		ScriptCommandDefinition c = p.getNextCommand();
		assertNotNull(c);
		System.out.println("first: " + c.getSQL());
		assertEquals("1", c.getSQL());
		c = p.getNextCommand();
		assertNotNull(c);
		assertEquals("-- some comment \n2", c.getSQL());
	}

}
