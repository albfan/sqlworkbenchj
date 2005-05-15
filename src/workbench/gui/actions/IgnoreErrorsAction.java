/*
 * IgnoreErrorsAction.java
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

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import workbench.gui.components.WbToolbarButton;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *	Action to ignore errors when executing scripts
 *	@author  support@sql-workbench.net
 */
public class IgnoreErrorsAction extends WbAction
{
	private Border originalBorder;

	private boolean switchedOn = false;
	private JCheckBoxMenuItem toggleMenu;
	private JToggleButton toggleButton;

	public IgnoreErrorsAction()
	{
		super();
		this.initMenuDefinition("MnuTxtIgnoreErrors");
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.switchedOn = Settings.getInstance().getIgnoreErrors();
	}

	public void executeAction(ActionEvent e)
	{
		this.setSwitchedOn(!this.switchedOn);
	}

	public JToggleButton createButton()
	{
		this.toggleButton = new JToggleButton(this);
		this.toggleButton.setText(null);
		this.toggleButton.setMargin(WbToolbarButton.MARGIN);
		this.toggleButton.setIcon(ResourceMgr.getPicture("IgnoreError"));
		this.toggleButton.setSelected(this.switchedOn);
		return this.toggleButton;
	}
	
	public void addToToolbar(JToolBar aToolbar)
	{
		if (this.toggleButton == null) this.createButton();
		aToolbar.add(this.toggleButton);
	}
	
	public boolean isSwitchedOn() { return this.switchedOn; }

	public void setSwitchedOn(boolean aFlag)
	{
		this.switchedOn = aFlag;
		if (this.toggleMenu != null) this.toggleMenu.setSelected(aFlag);
		if (this.toggleButton != null) this.toggleButton.setSelected(aFlag);
		Settings.getInstance().setIgnoreErrors(this.switchedOn);
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
	
}
