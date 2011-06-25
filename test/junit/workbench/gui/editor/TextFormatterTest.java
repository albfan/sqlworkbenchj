/*
 * TextFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import org.junit.AfterClass;

import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import workbench.interfaces.SqlTextContainer;
import workbench.sql.DelimiterDefinition;


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

		editorText = "update foo set bar = 1;\nupdate bar set foo = 2; ";
		selectionStart = 0;
		selectionEnd = 0;

		TextFormatter instance = new TextFormatter("postgresql");
		instance.formatSql(editor, DelimiterDefinition.DEFAULT_ORA_DELIMITER, "--");

		String expected =
			"UPDATE foo\n" +
			"   SET bar = 1;\n" +
			"UPDATE bar\n" +
			"   SET foo = 2;";

		assertEquals(expected, editorText.trim());

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
			"UPDATE foo\n" +
			"   SET bar = 2\n" +
			"WHERE id = 2\n" +
			"/";

//		System.out.println("formatted:\n" + editorText);
		assertEquals(expected, editorText.trim());

	}
}
