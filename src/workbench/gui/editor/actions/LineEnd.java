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
import workbench.resource.PlatformShortcuts;

/**
 *
 * @author support@sql-workbench.net
 */
public class LineEnd
	extends EditorAction
{
	protected boolean select;

	public LineEnd()
	{
		super("TxtEdLineEnd", PlatformShortcuts.getDefaultEndOfLine(false));
		select = false;
	}

	protected LineEnd(String resourceKey, KeyStroke key)
	{
		super(resourceKey, key);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);

		int line = textArea.getCaretLine();
		int caret = textArea.getCaretPosition();

		int lastOfLine = textArea.getLineEndOffset(line) - 1;
		int lastVisibleLine = textArea.getFirstLine() + textArea.getVisibleLines();
		if (lastVisibleLine >= textArea.getLineCount())
		{
			lastVisibleLine = Math.min(textArea.getLineCount() - 1, lastVisibleLine);
		}
		else
		{
			lastVisibleLine -= (textArea.getElectricScroll() + 1);
		}

		int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;
		int lastDocument = textArea.getDocumentLength();

		if (caret == lastDocument)
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
			caret = lastOfLine;
		}
		else if (caret == lastVisible)
		{
			caret = lastDocument;
		}
		else if (caret == lastOfLine)
		{
			caret = lastVisible;
		}
		else
		{
			caret = lastOfLine;
		}

		if (select)
		{
			textArea.select(textArea.getMarkPosition(), caret);
		}
		else
		{
			textArea.setCaretPosition(caret);
		}
	}
}
