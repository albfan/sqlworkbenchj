/*
 * DocumentEnd.java
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
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.resource.PlatformShortcuts;

/**
 *
 * @author Thomas Kellerer
 */
public class DocumentEnd
	extends EditorAction
{
	public DocumentEnd()
	{
		super("TxtEdDocEnd", PlatformShortcuts.getDefaultEndOfDoc(false));
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		textArea.selectNone();
		textArea.setCaretPosition(textArea.getDocumentLength());
	}
}
