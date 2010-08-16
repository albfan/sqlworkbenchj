/*
 * RemoveTabAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.gui.MainWindow;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class RemoveTabAction
	extends WbAction
	implements ChangeListener
{
	private MainWindow client;

	public RemoveTabAction(MainWindow aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtRemoveTab", KeyStroke.getKeyStroke(KeyEvent.VK_K, PlatformShortcuts.getDefaultModifier()));
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
		client.addTabChangeListener(this);
		setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		client.removeCurrentTab();
	}

	public void stateChanged(ChangeEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				setEnabled(client.canCloseTab());
			}
		});
	}
}
