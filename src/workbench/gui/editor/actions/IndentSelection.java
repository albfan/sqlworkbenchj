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
import workbench.gui.actions.WbAction;
import workbench.gui.editor.JEditTextArea;
import workbench.gui.editor.TextIndenter;
import workbench.resource.ResourceMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class IndentSelection
	extends WbAction
{
	private JEditTextArea area;

	public IndentSelection(JEditTextArea edit)
	{
		super();
		initMenuDefinition("MnuTxtIndent");
		setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		area = edit;
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		TextIndenter indenter = new TextIndenter(area);
		indenter.indentSelection();
	}
}
