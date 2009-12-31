/*
 * LineStart.java
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
import javax.swing.KeyStroke;
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.resource.PlatformShortcuts;

/**
 *
 * @author Thomas Kellerer
 */
public class LineStart
	extends EditorAction
{
	protected boolean select;
	
	public LineStart()
	{
		super("TxtEdLineStart", PlatformShortcuts.getDefaultStartOfLine(false));
		select = false;
	}

	protected LineStart(String resourceKey, KeyStroke key)
	{
		super(resourceKey, key);
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);

		int caret = textArea.getCaretPosition();

		int firstLine = textArea.getFirstLine();

		int firstOfLine = textArea.getLineStartOffset(textArea.getCaretLine());
		int firstVisibleLine = (firstLine == 0 ? 0 : firstLine + textArea.getElectricScroll());
		int firstVisible = textArea.getLineStartOffset(firstVisibleLine);

		if (caret == 0)
		{
			textArea.getToolkit().beep();
			if (!select)
			{
				textArea.selectNone();
			}
			return;
		}
		else if (!Boolean.TRUE.equals(textArea.getClientProperty(InputHandler.SMART_HOME_END_PROPERTY)))
		{
			caret = firstOfLine;
		}
		else if (caret == firstVisible)
		{
			caret = 0;
		}
		else if (caret == firstOfLine)
		{
			caret = firstVisible;
		}
		else
		{
			caret = firstOfLine;
		}

		if (select)
		{
			textArea.select(textArea.getMarkPosition(), caret);
		}
		else
		{
			textArea.selectNone();
			textArea.setCaretPosition(caret);
		}
	}
}
