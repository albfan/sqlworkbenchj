/*
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 *
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

/**
 *
 * @author support@sql-workbench.net
 */
public class DeleteChar
	extends EditorAction
{
	public DeleteChar()
	{
		super("TxtEdDelChar", KeyEvent.VK_DELETE, 0);
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);

		if (!textArea.isEditable())
		{
			textArea.getToolkit().beep();
			return;
		}

		if (textArea.getSelectionStart() != textArea.getSelectionEnd())
		{
			textArea.setSelectedText("");
		}
		else
		{
			int caret = textArea.getCaretPosition();
			if (caret == textArea.getDocumentLength())
			{
				textArea.getToolkit().beep();
				return;
			}
			try
			{
				textArea.getDocument().remove(caret, 1);
			}
			catch (BadLocationException bl)
			{
				bl.printStackTrace();
			}
		}
	}
}
