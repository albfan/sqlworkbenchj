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
import javax.swing.KeyStroke;
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.TextUtilities;
import workbench.resource.PlatformShortcuts;

/**
 *
 * @author support@sql-workbench.net
 */
public class PrevWord
	extends EditorAction
{
	protected boolean select;

	public PrevWord()
	{
		super("TxtEdPrvWord", PlatformShortcuts.getDefaultPrevWord(false));
		select = false;
	}

	public PrevWord(String resourceKey, KeyStroke key)
	{
		super(resourceKey, key);
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		int caret = textArea.getCaretPosition();
		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		caret -= lineStart;

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

		if (select)
		{
			textArea.select(textArea.getMarkPosition(), lineStart + caret);
		}
		else
		{
			textArea.setCaretPosition(lineStart + caret);
		}
	}
}
