/*
 * DelPrevWord.java
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
public class DelPrevWord
	extends EditorAction
{
	public DelPrevWord()
	{
		super("TxtEdDelPrvWord", KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_MASK);
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

		if (caret == 0)
		{
			if (lineStart == 0)
			{
				textArea.getToolkit().beep();
				return;
			}
			caret--;
		}
		else
		{
			caret = TextUtilities.findWordStart(lineText, caret);
		}

		try
		{
			textArea.getDocument().remove(caret + lineStart, start - (caret + lineStart));
		}
		catch (BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}
}
