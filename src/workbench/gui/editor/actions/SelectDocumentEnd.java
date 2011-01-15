/*
 * SelectDocumentEnd.java
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
import workbench.gui.editor.InputHandler;
import workbench.gui.editor.JEditTextArea;
import workbench.resource.PlatformShortcuts;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectDocumentEnd
	extends EditorAction
{
	public SelectDocumentEnd()
	{
		super("TxtEdDocEndSel", PlatformShortcuts.getDefaultEndOfDoc(true));
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		JEditTextArea textArea = InputHandler.getTextArea(evt);
		textArea.select(textArea.getMarkPosition(), textArea.getDocumentLength());
	}
}
