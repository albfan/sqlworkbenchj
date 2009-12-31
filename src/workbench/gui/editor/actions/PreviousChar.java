/*
 * PreviousChar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;

/**
 *
 * @author Thomas Kellerer
 */
public class PreviousChar
	extends EditorAction
{
	protected boolean select;

	public PreviousChar()
	{
		super("TxtEdPrevChar", KeyEvent.VK_LEFT, 0);
		select = false;
	}

	public PreviousChar(String resourceKey, int key, int modifier)
	{
		super(resourceKey, key, modifier);
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		int caret = textArea.getCaretPosition();
		if (caret == 0)
		{
			textArea.getToolkit().beep();
			if (!select)
			{
				textArea.selectNone();
			}
			return;
		}

		if (select)
		{
			textArea.select(textArea.getMarkPosition(), caret - 1);
		}
		else
		{
			textArea.setCaretPosition(caret - 1);
		}
	}
}
