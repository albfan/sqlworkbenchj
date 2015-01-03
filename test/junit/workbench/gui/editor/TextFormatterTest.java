/*
 * TextFormatterTest.java
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
package workbench.gui.editor;

import workbench.WbTestCase;
import workbench.interfaces.SqlTextContainer;

import workbench.sql.DelimiterDefinition;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class TextFormatterTest
	extends WbTestCase
{
	private String editorText;
	private int selectionStart;
	private int selectionEnd;

	public TextFormatterTest()
	{
		super("TextFormatterTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
	}

	@Test
	public void testFormatSql()
	{
		// <editor-fold desc="Editor Mock" defaultstate="collapsed">
		SqlTextContainer editor = new SqlTextContainer()
		{
			@Override
			public String getSelectedStatement()
			{
				String text = this.getSelectedText();
				if (text == null || text.length() == 0)
				{
					return this.getText();
				}
				else
				{
					return text;
				}
			}

			@Override
			public String getText()
			{
				return editorText;
			}

			@Override
			public String getSelectedText()
			{
				return editorText.substring(getSelectionStart(), getSelectionEnd());
			}

			@Override
			public void setSelectedText(String text)
			{
				editorText = text;
			}

			@Override
			public void setText(String text)
			{
				editorText = text;
			}

			@Override
			public void setCaretPosition(int pos)
			{
			}

			@Override
			public int getCaretPosition()
			{
				return selectionEnd;
			}

			@Override
			public int getSelectionStart()
			{
				return selectionStart;
			}

			@Override
			public int getSelectionEnd()
			{
				return selectionEnd;
			}

			@Override
			public void select(int start, int end)
			{
			}

			@Override
			public void setEditable(boolean flag)
			{
			}

			@Override
			public boolean isEditable()
			{
				return true;
			}

			@Override
			public boolean isTextSelected()
			{
				return getSelectionStart() < getSelectionEnd();
			}
		};
		// </editor-fold>

		editorText = "update foo set bar = 1;\nupdate bar set foo = 2; ";
		selectionStart = 0;
		selectionEnd = 0;

		TextFormatter instance = new TextFormatter("postgresql");
		instance.formatSql(editor, DelimiterDefinition.DEFAULT_ORA_DELIMITER, "--");

		String expected =
			"UPDATE foo\n" +
			"   SET bar = 1;\n\n" +
			"UPDATE bar\n" +
			"   SET foo = 2;";

		String formatted = editorText.trim();
//		System.out.println("expected: \n" + expected + "\n------------- formatted: --------- \n" + formatted);
		assertEquals(expected, formatted);

		editorText = "update foo set bar = 1;\nupdate bar set foo = 2; ";
		selectionStart = 0;
		selectionEnd = editorText.length();
		instance.formatSql(editor, DelimiterDefinition.DEFAULT_ORA_DELIMITER, "--");
//		System.out.println("formatted:\n" + editorText);
		assertEquals(expected, editorText.trim());

		editorText = "update foo set bar = 1;";
		selectionStart = 0;
		selectionEnd = editorText.length() - 1;
//		System.out.println("selected: " + editor.getSelectedStatement());
		instance.formatSql(editor, DelimiterDefinition.DEFAULT_ORA_DELIMITER, "--");
		expected =
			"UPDATE foo\n" +
			"   SET bar = 1";

//		System.out.println("formatted:\n" + editorText);
		assertEquals(expected, editorText.trim());

		editorText = "update foo set bar = 1;";
		selectionStart = 0;
		selectionEnd = editorText.length();
//		System.out.println("selected: " + editor.getSelectedStatement());
		instance.formatSql(editor, DelimiterDefinition.DEFAULT_ORA_DELIMITER, "--");
		expected =
			"UPDATE foo\n" +
			"   SET bar = 1;";

//		System.out.println("formatted:\n" + editorText);
		assertEquals(expected, editorText.trim());

		instance = new TextFormatter("oracle");

		editorText = "update foo set bar = 1 where id = 1\n/\nupdate foo set bar = 2 where id = 2\n/\n";
		selectionStart = 0;
		selectionEnd = 0;
//		System.out.println("selected: " + editor.getSelectedStatement());
		instance.formatSql(editor, DelimiterDefinition.DEFAULT_ORA_DELIMITER, "--");
		expected =
			"UPDATE foo\n" +
			"   SET bar = 1\n" +
			"WHERE id = 1\n" +
			"/\n" +
			"\n" +
			"UPDATE foo\n" +
			"   SET bar = 2\n" +
			"WHERE id = 2\n" +
			"/";

		formatted = editorText.trim();
//		System.out.println("expected: \n" + expected + "\n------------- formatted: --------- \n" + formatted);
		assertEquals(expected, editorText.trim());

	}
}
