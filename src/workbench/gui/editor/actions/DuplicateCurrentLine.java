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
public class DuplicateCurrentLine
	extends EditorAction
{
	public DuplicateCurrentLine()
	{
		super("TxtEdDupLine", null);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		if (textArea == null) return;

		int line = textArea.getCaretLine();
		String lineText = textArea.getLineText(line) + Settings.getInstance().getInternalEditorLineEnding();

		int lineEnd = textArea.getLineEndOffset(line);

		try
		{
			textArea.getDocument().insertString(lineEnd, lineText, null);
		}
		catch (BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}
}
