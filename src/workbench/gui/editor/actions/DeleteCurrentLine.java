/*
 * DeleteWord.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.text.BadLocationException;
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class DeleteCurrentLine
	extends EditorAction
{
	public DeleteCurrentLine()
	{
		super("TxtEdDelLine", null);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		if (textArea == null) return;

		int line = textArea.getCaretLine();

		int lineStart = textArea.getLineStartOffset(line);
		int len = textArea.getLineLength(line);
		if (line < textArea.getLineCount() - 1)
		{
			 len += Settings.getInstance().getInternalEditorLineEnding().length();
		}
		try
		{
			textArea.getDocument().remove(lineStart, len);
		}
		catch (BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}
}
