/*
 * CheckBoxAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

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

	public CheckBoxAction(String resourceKey)
	{
		this(resourceKey, null);
	}

	public CheckBoxAction(String resourceKey, String prop)
	{
		super();
		this.initMenuDefinition(resourceKey);
		this.setIcon(null);
		this.settingsProperty = prop;
		if (prop != null)
		{
			this.switchedOn = Settings.getInstance().getBoolProperty(settingsProperty);
		}
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		this.setSwitchedOn(!this.switchedOn);
	}

	public boolean isSwitchedOn()
	{
		return this.switchedOn;
	}

	public void setSwitchedOn(boolean flag)
	{
		if (flag == this.switchedOn) return;

		this.switchedOn = flag;
		if (this.toggleMenu != null)
		{
			WbSwingUtilities.invoke(() ->
      {
        toggleMenu.setSelected(switchedOn);
      });
		}
		if (this.settingsProperty != null)
		{
			Settings.getInstance().setProperty(settingsProperty, this.switchedOn);
		}
	}

	@Override
	public JMenuItem getMenuItem()
	{
		if (this.toggleMenu == null)
		{
			createMenuItem();
		}
		return toggleMenu;
	}

	private void createMenuItem()
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

	@Override
	public void addToMenu(JMenu aMenu)
	{
		if (this.toggleMenu == null)
		{
			createMenuItem();
		}
		aMenu.add(this.toggleMenu);
	}

	@Override
	public void setAccelerator(KeyStroke key)
	{
		super.setAccelerator(key);
		if (this.toggleMenu != null)
		{
			toggleMenu.setAccelerator(key);
		}
	}
}
