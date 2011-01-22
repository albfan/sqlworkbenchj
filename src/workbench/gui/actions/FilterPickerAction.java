/*
 * FilterPickerAction.java
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

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DropDownButton;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbTable;
import workbench.gui.filter.FilterDefinitionManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
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


	public void setClient(WbTable c)
	{
		this.client = c;
		checkEnabled();
	}

	public JButton getToolbarButton(boolean createNew)
	{
    JButton result = null;
		if (this.dropDownButton == null || createNew)
		{
      DropDownButton b = new DropDownButton(ResourceMgr.getImage("dropdown"));
			b.setAction(this);
			b.setText(null);
			b.setMnemonic(0);
			Dimension d = new Dimension(12,24);
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
}
