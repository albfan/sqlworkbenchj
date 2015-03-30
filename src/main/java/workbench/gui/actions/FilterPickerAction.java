/*
 * FilterPickerAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import workbench.log.LogMgr;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DropDownButton;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbTable;
import workbench.gui.filter.FilterDefinitionManager;

import workbench.storage.filter.FilterExpression;

import workbench.util.WbFile;

/**
 *	Select a different filter for a table.
 *	@author  Thomas Kellerer
 */
public class FilterPickerAction
		extends WbAction
		implements PropertyChangeListener, ActionListener
{
	private WbTable client;
	private DropDownButton dropDownButton;

	public FilterPickerAction(WbTable aClient)
	{
		super();
		this.setClient(aClient);
		this.initMenuDefinition("MnuTxtPickFilter");
		this.setIcon("dropdown");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		FilterDefinitionManager mgr = FilterDefinitionManager.getInstance();
		mgr.addPropertyChangeListener(this);
		checkEnabled();
		buildPopup();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (this.client == null) return;

		if (e.getSource() instanceof JMenuItem)
		{
			String file = e.getActionCommand();
			try
			{
				FilterExpression f = FilterDefinitionManager.getInstance().loadFilter(file);
				this.client.applyFilter(f);

				String tooltip = "<html>" + ResourceMgr.getDescription("MnuTxtPickFilter");
				tooltip += "<br>(" + file + ")</html>";
				dropDownButton.setToolTipText(tooltip);
			}
			catch (Exception ex)
			{
				LogMgr.logError("FilterPickerAction.actionPerformed()", "Error loading filter", ex);
				Window w = SwingUtilities.getWindowAncestor(this.client);
				WbSwingUtilities.showErrorMessage(w, "Could not load filter: " + ex.getMessage());
				dropDownButton.setToolTipText(ResourceMgr.getDescription("MnuTxtPickFilter"));
			}
		}
	}

	private void checkEnabled()
	{
		if (this.client == null)
		{
			this.setEnabled(false);
		}
		else
		{
			int availableFilters = FilterDefinitionManager.getInstance().getEntries().size();
			this.setEnabled(availableFilters > 0);
		}
	}


	public final void setClient(WbTable c)
	{
		this.client = c;
		checkEnabled();
	}

	@Override
	public JButton getToolbarButton(boolean createNew)
	{
    JButton result = null;
		if (this.dropDownButton == null || createNew)
		{
			ImageIcon icon = IconMgr.getInstance().getToolbarIcon("dropdown");
			DropDownButton b = new DropDownButton(icon);
			b.setAction(this);
			b.setText(null);
			b.setMnemonic(0);
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			Dimension d = new Dimension(w, h);
			b.setMaximumSize(d);
			b.setPreferredSize(d);
			b.setMinimumSize(d);
      if (this.dropDownButton == null) this.dropDownButton = b;
      result = b;
		}
    else
    {
      result = this.dropDownButton;
    }
		return result;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		int availableFilters = FilterDefinitionManager.getInstance().getEntries().size();
		this.setEnabled(availableFilters > 0);
		buildPopup();
	}

	private void buildPopup()
	{
		List<WbFile> entries = FilterDefinitionManager.getInstance().getEntries();
		if (entries == null || entries.isEmpty()) return;

		JMenu menu = new JMenu("filters");
		for (WbFile f : entries)
		{
			JMenuItem item = new WbMenuItem(f.getName());
			item.setToolTipText(f.getFullPath());
			item.setActionCommand(f.getFullPath());
			item.addActionListener(this);
			menu.add(item);
		}
		if (this.dropDownButton == null)
		{
			this.getToolbarButton();
		}
		dropDownButton.setDropDownMenu(menu.getPopupMenu());
	}

	@Override
	public void dispose()
	{
		super.dispose();
		if (this.dropDownButton != null)
		{
			this.dropDownButton.dispose();
			this.dropDownButton = null;
		}
	}

}
