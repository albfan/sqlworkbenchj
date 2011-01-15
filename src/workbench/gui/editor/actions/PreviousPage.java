/*
 * PreviousPage.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
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
public class PreviousPage
	extends EditorAction
{
	protected boolean select;

	public PreviousPage()
	{
		super("TxtEdPrvPage", KeyEvent.VK_PAGE_UP, 0);
		select = false;
	}

	public PreviousPage(String resourceKey, int key, int modifier)
	{
		super(resourceKey, key, modifier);
	}

	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		int firstLine = textArea.getFirstLine();
		int visibleLines = textArea.getVisibleLines();
		int line = textArea.getCaretLine();

		if (firstLine < visibleLines)
		{
			firstLine = visibleLines;
		}

		textArea.setFirstLine(firstLine - visibleLines);

		int caret = textArea.getLineStartOffset(Math.max(0, line - visibleLines));

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
