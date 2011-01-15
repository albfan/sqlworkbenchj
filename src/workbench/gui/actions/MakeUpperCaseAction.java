/*
 * MakeUpperCaseAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.gui.sql.EditorPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Make current selection upper case
 * 
 * @see workbench.gui.sql.EditorPanel#toUpperCase()
 * @author  Thomas Kellerer
 */
public class MakeUpperCaseAction
	extends WbAction
	implements TextSelectionListener
{
	private EditorPanel client;

	public MakeUpperCaseAction(EditorPanel aClient)
	{
		super();
		this.client = aClient;
		this.client.addSelectionListener(this);
		this.initMenuDefinition("MnuTxtMakeUpperCase", KeyStroke.getKeyStroke(KeyEvent.VK_U, PlatformShortcuts.getDefaultModifier()));
		this.setMenuItemName(ResourceMgr.MNU_TXT_EDIT);
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.toUpperCase();
	}

	public void selectionChanged(int newStart, int newEnd)
	{
		this.setEnabled(newEnd > newStart);
	}
}
