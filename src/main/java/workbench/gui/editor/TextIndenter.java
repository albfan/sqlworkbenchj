/*
 * TextIndenter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.editor;

import javax.swing.text.BadLocationException;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TextIndenter
{
	private JEditTextArea editor;

	public TextIndenter(JEditTextArea client)
	{
		this.editor = client;
	}

	public void indentSelection()
	{
		this.shiftSelection(true);
	}

	public void unIndentSelection()
	{
		this.shiftSelection(false);
	}

	private void shiftSelection(boolean indent)
	{
		int startline = editor.getSelectionStartLine();
		int realEndline = editor.getSelectionEndLine();
		int endline = realEndline;
		int tabSize = editor.getTabSize();

		StringBuilder buff = new StringBuilder(tabSize);
		for (int i=0; i < tabSize; i++) buff.append(' ');
		String spacer = buff.toString();

		boolean useTab = Settings.getInstance().getEditorUseTabCharacter();

		int pos = editor.getSelectionEnd(endline) - editor.getLineStartOffset(endline);
		if (pos == 0) endline --;
		SyntaxDocument document = editor.getDocument();

		try
		{
			document.beginCompoundEdit();
			for (int line = startline; line <= endline; line ++)
			{
				String text = editor.getLineText(line);
				if (StringUtil.isBlank(text)) continue;
				int lineStart = editor.getLineStartOffset(line);
				if (indent)
				{
					if (useTab)
					{
						document.insertString(lineStart, "\t", null);
					}
					else
					{
						document.insertString(lineStart, spacer, null);
					}
				}
				else
				{
					char c = text.charAt(0);
					if (c == '\t')
					{
						document.remove(lineStart, 1);
					}
					else
					{
						int numChars = 0;
						while (Character.isWhitespace(text.charAt(numChars)) && numChars < tabSize) numChars ++;
						document.remove(lineStart, numChars);
					}
				}
			}
		}
		catch (BadLocationException e)
		{
			LogMgr.logError("TextIndenter.shiftSelection()", "Error when shifting selection", e);
		}
		finally
		{
			document.endCompoundEdit();
		}
	}

}
