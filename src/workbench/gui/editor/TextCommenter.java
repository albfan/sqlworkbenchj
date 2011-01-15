/*
 * TextCommenter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import javax.swing.text.BadLocationException;
import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TextCommenter
{
	private JEditTextArea editor;

	public TextCommenter(JEditTextArea client)
	{
		this.editor = client;
	}

	public void commentSelection()
	{
		String commentChar = editor.getCommentChar();
		boolean isCommented = this.isSelectionCommented(commentChar);
		// Comment Selection acts as a toggle.
		// if the complete selection is already commented
		// the comments will be removed.
		doComment(commentChar, !isCommented);
	}

	public void unCommentSelection()
	{
		doComment(editor.getCommentChar(), false);
	}

	private void doComment(String commentChar, boolean comment)
	{
		int startline = editor.getSelectionStartLine();
		int endline = editor.getSelectionEndLine();

		if (commentChar == null) commentChar = "--";

		int cLength = commentChar.length();

		int pos = editor.getSelectionEnd(endline) - editor.getLineStartOffset(endline);
		SyntaxDocument document = editor.getDocument();

		try
		{
			document.beginCompoundEdit();
			for (int line = startline; line <= endline; line ++)
			{
				String text = editor.getLineText(line);
				if (StringUtil.isBlank(text)) continue;
				int lineStart = editor.getLineStartOffset(line);
				if (comment)
				{
					document.insertString(lineStart, commentChar, null);
				}
				else
				{
					pos = text.indexOf(commentChar);
					if (pos > -1)
					{
						document.remove(lineStart, pos + cLength);
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

	protected boolean isSelectionCommented(String commentChar)
	{
		int startline = editor.getSelectionStartLine();
		int endline = editor.getSelectionEndLine();
		if (commentChar == null) commentChar = "--";

		for (int line = startline; line <= endline; line ++)
		{
			String text = editor.getLineText(line);
			if (StringUtil.isBlank(text)) continue;
			if (!text.startsWith(commentChar)) return false;
		}
		return true;
	}

}
