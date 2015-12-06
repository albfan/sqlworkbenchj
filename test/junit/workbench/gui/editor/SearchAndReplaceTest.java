/*
 * SearchAndReplaceTest.java
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
import workbench.interfaces.TextContainer;

import org.junit.Test;

import static org.junit.Assert.*;

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

  // <editor-fold desc="Editor Mock" defaultstate="collapsed">
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

    @Override
    public int getLineOfOffset(int offset)
    {
      return -1;
    }

    @Override
    public int getStartInLine(int offset)
    {
      return -1;
    }

    @Override
    public String getLineText(int line)
    {
      return null;
    }

    @Override
    public int getLineCount()
    {
      return 1;
    }

    @Override
    public String getWordAtCursor(String wordChars)
    {
      return null;
    }
	}
		// </editor-fold>
}

