/*
 * CheckPreparedStatementsAction.java
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

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * Action to toggle the detection of prepared statements during SQL execution
 * 
 * @see workbench.resource.Settings#setCheckPreparedStatements(boolean)
 * 
 * @author  Thomas Kellerer
 */
public class CheckPreparedStatementsAction 
	extends WbAction
	implements PropertyChangeListener
{
	private boolean switchedOn = false;
	private JCheckBoxMenuItem toggleMenu;
	private boolean inSetter = false;

	public CheckPreparedStatementsAction()
	{
		super();
		this.initMenuDefinition("MnuTxtCheckPrepared");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.switchedOn = Settings.getInstance().getCheckPreparedStatements();
		Settings.getInstance().addPropertyChangeListener(this, "workbench.sql.checkprepared");
	}

	public void executeAction(ActionEvent e)
	{
		this.setSwitchedOn(!this.switchedOn);
	}

	public boolean isSwitchedOn() { return this.switchedOn; }

	private void setSwitchedOn(boolean aFlag)
	{
		try
		{
			this.switchedOn = aFlag;
			if (this.toggleMenu != null) this.toggleMenu.setSelected(aFlag);
			inSetter = true;
			Settings.getInstance().setCheckPreparedStatements(this.switchedOn);
		}
		finally
		{
			inSetter = false;
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
			this.toggleMenu.setSelected(this.switchedOn);
		}
		aMenu.add(this.toggleMenu);
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (!inSetter)
		{
			this.switchedOn = Settings.getInstance().getCheckPreparedStatements();
			if (this.toggleMenu != null) this.toggleMenu.setSelected(this.switchedOn);
		}
	}
	
}
