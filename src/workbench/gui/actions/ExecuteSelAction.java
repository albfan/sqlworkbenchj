/*
 * ExecuteSelAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.SqlPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.GuiSettings;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Run the selected text as a script in the current SQL Panel
 *
 * @see workbench.gui.sql.SqlPanel#runSelectedStatement()
 * @author  Thomas Kellerer
 */
public class ExecuteSelAction
	extends WbAction
	implements TextSelectionListener
{
	private SqlPanel target;
	private boolean isEnabled;
	private boolean checkSelection;
	
	public ExecuteSelAction(SqlPanel aPanel)
	{
		super();
		this.target = aPanel;
		this.initMenuDefinition("MnuTxtExecuteSel", KeyStroke.getKeyStroke(KeyEvent.VK_E, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("ExecuteSel");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setAlternateAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
		this.setEnabled(false);
		if (GuiSettings.getExecuteOnlySelected())
		{
			checkSelection = true;
			target.getEditor().addSelectionListener(this);
			checkSelection();
		}
	}

	public void executeAction(ActionEvent e)
	{
		if (isEnabled())
		{
			target.runSelectedStatement();
		}
	}

	@Override
	public void setEnabled(boolean flag)
	{
		super.setEnabled(flag);
		isEnabled = flag;
		checkSelection();
	}

	@Override
	public void selectionChanged(int newStart, int newEnd)
	{
		if (isEnabled)
		{
			super.setEnabled(newStart < newEnd);
		}
	}

	public void checkSelection()
	{
		if (checkSelection && isEnabled)
		{
			if (target == null) return;
			if (target.getEditor() == null) return;

			int start = target.getEditor().getSelectionStart();
			int end = target.getEditor().getSelectionEnd();
			super.setEnabled(start < end);
		}
	}
}
