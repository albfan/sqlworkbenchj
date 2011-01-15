/*
 * DeleteWord.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.text.BadLocationException;
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.TextUtilities;

/**
 *
 * @author Thomas Kellerer
 */
public class DeleteWord
	extends EditorAction
{
	public DeleteWord()
	{
		super("TxtEdDelWord", KeyEvent.VK_DELETE, KeyEvent.CTRL_MASK);
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		int start = textArea.getSelectionStart();
		if (start != textArea.getSelectionEnd())
		{
			textArea.setSelectedText("");
		}

		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		int caret = start - lineStart;

		String lineText = textArea.getLineText(textArea.getCaretLine());

		if (caret == lineText.length())
		{
			if (lineStart + caret == textArea.getDocumentLength())
			{
				textArea.getToolkit().beep();
				return;
			}
			caret++;
		}
		else
		{
			caret = TextUtilities.findWordEnd(lineText, caret);
		}

		try
		{
			textArea.getDocument().remove(start,
				(caret + lineStart) - start);
		}
		catch (BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}
}
