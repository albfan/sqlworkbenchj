/*
 * SearchAndReplaceTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.interfaces.TextContainer;

/**
 *
 * @author Thomas Kellerer
 */
public class SearchAndReplaceTest
	extends WbTestCase
{

	public SearchAndReplaceTest()
	{
		super("SearchAndReplaceTest");
	}

	@Test
	public void testCreateSearchPattern()
	{
		String input = "thetext";
		String expression = SearchAndReplace.getSearchExpression(input, false, false, false);
		assertEquals("Wrong expression", "(" + input + ")", expression);

		expression = SearchAndReplace.getSearchExpression(input, false, true, false);
		assertEquals("Wrong expression", "\\b(" + input + ")\\b", expression);

		expression = SearchAndReplace.getSearchExpression(input, true, true, false);
		assertEquals("Wrong expression", "(?i)\\b(" + input + ")\\b", expression);

		expression = SearchAndReplace.getSearchExpression(input, true, true, true);
		assertEquals("Wrong expression", "(?i)\\b" + input + "\\b", expression);

		expression = SearchAndReplace.getSearchExpression(input, true, false, true);
		assertEquals("Wrong expression", "(?i)" + input, expression);

		expression = SearchAndReplace.getSearchExpression(input, false, false, true);
		assertEquals("Wrong expression", input, expression);
	}

	@Test
	public void testReplace()
	{
		DummyContainer container = new DummyContainer();
		container.setText("go\ngo\ngo\n");
		SearchAndReplace replace = new SearchAndReplace(null, container);
		replace.replaceAll("go$", ";", false, true, false, true);
		assertEquals(";\n;\n;\n", container.getText());

		container.setText("foo go\nfoo go\nfoo go\n");
		replace.replaceAll("go$", ";", false, true, false, true);
		assertEquals("foo ;\nfoo ;\nfoo ;\n", container.getText());
	}

	@Test
	public void testFind()
	{
		DummyContainer editor = new DummyContainer();
		editor.setText("foobar\nfoobar\nbar\n");
		SearchAndReplace replace = new SearchAndReplace(null, editor);
		editor.setCaretPosition(0);
		int index = replace.findFirst("bar$", true, false, true);
		editor.setCaretPosition(index);
		assertEquals(3, index);
		index = replace.findNext();
		assertEquals(10, index);
	}


	private static class DummyContainer
		implements TextContainer
	{
		private String text;
		private int caretPosition;

		@Override
		public String getText()
		{
			return text;
		}

		@Override
		public String getSelectedText()
		{
			return text;
		}

		@Override
		public void setSelectedText(String aText)
		{
		}

		@Override
		public void setText(String aText)
		{
			this.text = aText;
		}

		@Override
		public void setCaretPosition(int pos)
		{
			caretPosition = pos;
		}

		@Override
		public int getCaretPosition()
		{
			return caretPosition;
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
	}
}

