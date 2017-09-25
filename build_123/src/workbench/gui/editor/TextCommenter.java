/*
 * TextCommenter.java
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
package workbench.gui.editor;

import javax.swing.text.BadLocationException;

import workbench.log.LogMgr;


/**
 *
 * @author Thomas Kellerer
 */
public class TextCommenter
{
  private static enum CommentType
  {
    comment,
    uncomment,
    toggle;
  };

	private JEditTextArea editor;

	public TextCommenter(JEditTextArea client)
	{
		this.editor = client;
	}

	public void toggleComment()
	{
		String commentChar = editor.getCommentChar();
    doComment(commentChar, CommentType.toggle);
	}

	public void commentSelection()
	{
		String commentChar = editor.getCommentChar();
    doComment(commentChar, CommentType.comment);
	}

	public void unCommentSelection()
	{
		doComment(editor.getCommentChar(), CommentType.uncomment);
	}

	private void doComment(String commentChar, CommentType type)
	{
		int startline = editor.getSelectionStartLine();
    int endline = getLastRelevantSelectionLine();

		if (commentChar == null) commentChar = "--";

    String commentToUse = commentChar;

		int cLength = commentChar.length();

		int pos = editor.getSelectionEnd(endline) - editor.getLineStartOffset(endline);
		SyntaxDocument document = editor.getDocument();

    if (commentChar.equals("--"))
		{
			// workaround for an Oracle bug, where a comment like
			//
			// --commit;
			//
			// would not be treated correctly when sent to the database.
			// Apparently Oracle requires a blank after the two dashes.
			//
			// Adding the blank shouldn't do any harm for other databases
			commentToUse = "-- ";
		}

		boolean ansiComment = "--".equals(commentChar);

		try
		{
			document.beginCompoundEdit();
			for (int line = startline; line <= endline; line ++)
			{
				String text = editor.getLineText(line);
        boolean isCommented = text.trim().startsWith(commentChar);

				int lineStart = editor.getLineStartOffset(line);
        if (type == CommentType.comment || (type == CommentType.toggle && isCommented == false))
				{
					document.insertString(lineStart, commentToUse, null);
				}
				else
				{
					pos = text.indexOf(commentChar);
					if (pos > -1)
					{
						int commentLength = cLength;
						// remove the blank following the comment character to cater
						// for the blank that was inserted by commenting the lines (see above)
						if (ansiComment && text.length() > pos + cLength && Character.isWhitespace(text.charAt(pos + cLength)))
						{
							commentLength ++;
						}
						document.remove(lineStart, pos + commentLength);
					}
				}
			}
		}
		catch (BadLocationException e)
		{
			LogMgr.logError("TextManipulator.doComment()", "Error when processing comment", e);
		}
		finally
		{
			document.endCompoundEdit();
		}
	}

	private int getLastRelevantSelectionLine()
	{
		int startline = editor.getSelectionStartLine();
		int endline = editor.getSelectionEndLine();
		int lastLineStart = editor.getLineStartOffset(endline);
		if (lastLineStart == editor.getSelectionEnd() && endline > startline)
		{
			// ignore the last selection line (of a multiline selection) if there isn't something selected
			endline--;
		}
		return endline;
	}
}
