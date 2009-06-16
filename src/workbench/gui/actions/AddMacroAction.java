/*
 * AddMacroAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.macros.AddMacroPanel;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.ResourceMgr;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;
import workbench.sql.macros.MacroManager;
import workbench.util.StringUtil;

/**
 * Action to add a new macro.
 *
 * @see workbench.sql.macros.MacroManager
 * @author  support@sql-workbench.net
 */
public class AddMacroAction
	extends WbAction
	implements TextSelectionListener
{
	private EditorPanel client;

	public AddMacroAction()
	{
		super();
		this.setIcon(null);
		this.setMenuItemName(ResourceMgr.MNU_TXT_MACRO);
		this.initMenuDefinition("MnuTxtAddMacro");
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

		AddMacroPanel panel = new AddMacroPanel();

		ValidatingDialog dialog = ValidatingDialog.createDialog(
			SwingUtilities.getWindowAncestor(client),
			panel,
			ResourceMgr.getString("TxtGetMacroNameWindowTitle"), null, 0, true);

		dialog.addWindowListener(panel);
		dialog.setVisible(true);

		if (!dialog.isCancelled())
		{
			MacroGroup group = panel.getSelectedGroup();
			String name = panel.getMacroName();
			if (StringUtil.isNonBlank(name) && group != null)
			{
				MacroManager.getInstance().getMacros().addMacro(group, new MacroDefinition(name, text));
			}
		}
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		boolean selected = (newStart > -1 && newEnd > newStart);
		this.setEnabled(selected);
	}
}
