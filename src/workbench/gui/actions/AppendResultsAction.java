/*
 * AppendResultsAction.java
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
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	Action to toggle the if running statements should replace the current
 *  results or simply add a new result tab to SqlPanel
 * 
 *	@author  support@sql-workbench.net
 */
public class AppendResultsAction 
	extends WbAction
{
	private boolean switchedOn = false;
	private JCheckBoxMenuItem toggleMenu;
	private SqlPanel client;
	private JToggleButton toggleButton;
	
	public AppendResultsAction(SqlPanel panel)
	{
		super();
		this.initMenuDefinition("MnuTxtToggleAppendResults");
		client = panel;
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setSwitchedOn(client.getAppendResults());
	}

	public void executeAction(ActionEvent e)
	{
		this.setSwitchedOn(!this.switchedOn);
	}

	public boolean isSwitchedOn() 
	{ 
		return this.switchedOn; 
	}

	public void setSwitchedOn(boolean aFlag)
	{
		this.switchedOn = aFlag;
		if (this.toggleMenu != null) this.toggleMenu.setSelected(aFlag);
		if (this.toggleButton != null) this.toggleButton.setSelected(aFlag);

		client.setAppendResults(this.switchedOn);
	}

	public JToggleButton createButton()
	{
		this.toggleButton = new JToggleButton(this);
		this.toggleButton.setText(null);
		this.toggleButton.setMargin(WbToolbarButton.MARGIN);
		this.toggleButton.setIcon(ResourceMgr.getImage("AppendResult"));
		this.toggleButton.setSelected(this.switchedOn);
		return this.toggleButton;
	}
	
	public void addToToolbar(JToolBar aToolbar)
	{
		if (this.toggleButton == null) this.createButton();
		aToolbar.add(this.toggleButton);
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
