/*
 * ViewLineNumbers.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class ViewLineNumbers extends WbAction
{
	private boolean switchedOn = false;
	private JCheckBoxMenuItem toggleMenu;

	public ViewLineNumbers()
	{
		super();
		this.initMenuDefinition("MnuTxtShowLineNumbers");
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
		this.switchedOn = Settings.getInstance().getShowLineNumbers();
	}

	public void executeAction(ActionEvent e)
	{
		this.setSwitchedOn(!this.switchedOn);
	}

	public boolean isSwitchedOn() { return this.switchedOn; }

	public void setSwitchedOn(boolean aFlag)
	{
		this.switchedOn = aFlag;
		if (this.toggleMenu != null) this.toggleMenu.setSelected(aFlag);
		Settings.getInstance().setShowLineNumbers(this.switchedOn);
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
			//this.toggleMenu.setIconTextGap(0);
			//this.toggleMenu.setIcon(ResourceMgr.getImage("blank"));
			this.toggleMenu.setSelected(this.switchedOn);
		}
		aMenu.add(this.toggleMenu);
	}

}
