/*
 * AddMacroAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;
import workbench.sql.macros.MacroManager;
import workbench.util.StringUtil;

/**
 * Action to add a new macro.
 * @see workbench.sql.MacroManager
 * @author  support@sql-workbench.net
 */
public class AddMacroAction extends WbAction
	implements TextSelectionListener
{
	private EditorPanel client;

	public AddMacroAction()
	{
		super();
		this.setIcon(null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_MACRO);
		this.initMenuDefinition("MnuTxtAddMacro", null);
		setEnabled(false);
	}

	public void setClient(EditorPanel panel)
	{
		if (this.client != null)
		{
			this.client.removeSelectionListener(this);
		}
		this.client = panel;
		this.client.addSelectionListener(this);
		this.setEnabled(client.isTextSelected());
	}
	
	public void executeAction(ActionEvent e)
	{
		String text = client.getSelectedText();
		if (StringUtil.isBlank(text))
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		
		String name = WbSwingUtilities.getUserInput(client, ResourceMgr.getString("TxtGetMacroNameWindowTitle"), ResourceMgr.getString("TxtEmptyMacroName"));
		if (name != null)
		{
			MacroManager.getInstance().getMacros().addMacro((String)null, name, text);
		}
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		boolean selected = (newStart > -1 && newEnd > newStart);
		this.setEnabled(selected);
	}
}
