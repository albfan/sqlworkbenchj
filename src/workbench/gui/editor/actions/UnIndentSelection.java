/*
 * UnIndentSelection.java
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
import workbench.gui.actions.WbAction;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.TextIndenter;
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class UnIndentSelection
	extends WbAction
{
	private JEditTextArea area;

	public UnIndentSelection(JEditTextArea edit)
	{
		super();
		initMenuDefinition("MnuTxtUnIndent");
		setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		area = edit;
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		TextIndenter indenter = new TextIndenter(area);
		indenter.unIndentSelection();
	}
}
