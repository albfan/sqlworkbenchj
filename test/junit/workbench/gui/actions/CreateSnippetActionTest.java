/*
 * CreateSnippetActionTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

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
