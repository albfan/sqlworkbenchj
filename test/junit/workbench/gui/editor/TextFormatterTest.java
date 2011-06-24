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
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import workbench.interfaces.SqlTextContainer;
import workbench.sql.DelimiterDefinition;


/**
 *
 * @author Thomas Kellerer
 */
public class TextFormatterTest
{
	private String editorText;

	public TextFormatterTest()
	{
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
				return editorText;
			}

			@Override
			public String getText()
			{
				return editorText;
			}

			@Override
			public String getSelectedText()
			{
				return null;
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
				return 0;
			}

			@Override
			public int getSelectionStart()
			{
				return 0;
			}

			@Override
			public int getSelectionEnd()
			{
				return 0;
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
				return false;
			}
		};

		editorText = "update foo set bar = 1; update bar set foo = 2;";
		
		TextFormatter instance = new TextFormatter("postgresql");
		instance.formatSql(editor, DelimiterDefinition.DEFAULT_ORA_DELIMITER, "--");
		fail("The test case is a prototype.");
	}
}
