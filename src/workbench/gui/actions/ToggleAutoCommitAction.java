/*
 * ToggleAutoCommitAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;

/**
 * An action to toggle the auto commit attribute of the 
 * given {@link workbench.db.WbConnection}
 * @author  Thomas Kellerer
 */
public class ToggleAutoCommitAction 
	extends CheckBoxAction
	implements PropertyChangeListener
{
	private WbConnection connection;
	
	public ToggleAutoCommitAction()
	{
		super("MnuTxtToggleAutoCommit", null);
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
			this.connection.toggleAutoCommit();
			checkState();
		}
	}

	private void checkState()
	{
		if (this.connection != null)
		{
			this.setEnabled(true);
			this.setSwitchedOn(this.connection.getAutoCommit());
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
