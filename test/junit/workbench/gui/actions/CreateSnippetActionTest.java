/*
 * CreateSnippetActionTest.java
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
package workbench.gui.actions;

import org.junit.Assume;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class CreateSnippetActionTest
	extends WbTestCase
{

	public CreateSnippetActionTest()
	{
		super("SnippetTest");
	}

	@Test
	public void testMakeJavaString()
	{
		Assume.assumeTrue(!java.awt.GraphicsEnvironment.isHeadless());
		String input =
			"SELECT * \n" +
			"FROM some_table;";

		CreateSnippetAction action = new CreateSnippetAction(null);
		String code = action.makeJavaString(input, true);
		assertFalse(code.contains("_table;"));

		code = action.makeJavaString(input, false);
		assertTrue(code.contains("_table;"));

		input =
			"CREATE OR REPLACE FUNCTION update_person2() \n" +
			"RETURNS trigger AS \n" +
			"$body$\n"+
			"BEGIN\n"+
			"   if new.comment IS NULL then\n"+
			"      new.comment = 'n/a';\n" +
			"   end if;\n"+
			"   RETURN NEW;\n"+
			"END;\n"+
			"$body$ \n"+
			"LANGUAGE plpgsql;\n"+
			"/\n";

		code = action.makeJavaString(input, true);
		assertTrue(code.contains("\"   end if \\n\" +"));

		code = action.makeJavaString(input, false);
		assertTrue(code.contains("\"   end if; \\n\" +"));
	}
}
