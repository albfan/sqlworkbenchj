/*
 * ViewToolbar.java
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
 * Toggle the display of the toolbar in the main window
 * @author Petr Novotnik
 */
public class CheckBoxAction
	extends WbAction
{
	private boolean switchedOn = false;
	private String settingsProperty;
	private JCheckBoxMenuItem toggleMenu;

	public CheckBoxAction(String resourceKey, String prop)
	{
		super();
		this.initMenuDefinition(resourceKey);
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
		this.settingsProperty = prop;
		if (prop != null)
		{
			this.switchedOn = Settings.getInstance().getBoolProperty(settingsProperty);
		}
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
		if (this.settingsProperty != null)
		{
			Settings.getInstance().setProperty(settingsProperty, this.switchedOn);
		}
	}

	public void addToMenu(JMenu aMenu)
	{
		if (this.toggleMenu == null)
		{
			this.toggleMenu = new JCheckBoxMenuItem();
			this.toggleMenu.setAction(this);
			String text = this.getValue(Action.NAME).toString();
			int pos = text.indexOf('&');
			if (pos > -1)
			{
				char mnemonic = text.charAt(pos + 1);
				text = text.substring(0, pos) + text.substring(pos + 1);
				this.toggleMenu.setMnemonic((int) mnemonic);
			}
			this.toggleMenu.setText(text);
			this.toggleMenu.setSelected(this.switchedOn);
		}
		aMenu.add(this.toggleMenu);
	}
}