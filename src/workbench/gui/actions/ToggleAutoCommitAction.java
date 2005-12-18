/*
 * CommitAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;

/**
 *	An action to toggle the auto commit attribute of the 
 * given {@link workbench.db.WbConnection}
 *	@author  support@sql-workbench.net
 */
public class ToggleAutoCommitAction 
	extends WbAction
	implements PropertyChangeListener
{
	private WbConnection connection;
	private JCheckBoxMenuItem toggleMenu;
	
	public ToggleAutoCommitAction()
	{
		super();
		this.initMenuDefinition("MnuTxtToggleAutoCommit");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

	public void setConnection(WbConnection conn)
	{
		if (this.connection != null)
		{
			this.connection.removeChangeListener(this);
		}
		this.connection = conn;
		if (this.connection != null)
		{
			this.connection.addChangeListener(this);
		}
		this.checkState();
	}
	
	public void executeAction(ActionEvent e)
	{
		if (this.connection != null && this.isEnabled()) 
		{
			try
			{
				this.connection.toggleAutoCommit();
				boolean flag = this.connection.getAutoCommit();
				if (this.toggleMenu != null) this.toggleMenu.setSelected(flag);
			}
			finally
			{
			}
		}
	}

	public void addToMenu(JMenu aMenu)
	{
		if (this.toggleMenu == null)
		{
			this.toggleMenu= new JCheckBoxMenuItem();
			this.toggleMenu.setAction(this);
			String text = this.getValue(Action.NAME).toString();
			int pos = text.indexOf('&');
			if (pos > -1)
			{
				char mnemonic = text.charAt(pos + 1);
				text = text.substring(0, pos) + text.substring(pos + 1);
				this.toggleMenu.setMnemonic((int)mnemonic);
			}
			this.toggleMenu.setText(text);
			this.checkState();
		}
		aMenu.add(this.toggleMenu);
	}	
	
	private void checkState()
	{
		if (this.connection != null && this.toggleMenu != null)
		{
			this.setEnabled(true);
			this.toggleMenu.setSelected(this.connection.getAutoCommit());
		}
		else
		{
			this.setEnabled(false);
		}
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == this.connection && WbConnection.PROP_AUTOCOMMIT.equals(evt.getPropertyName()))
		{
			this.checkState();
		}		
	}
}
