/*
 * FilterDataAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DropDownButton;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbTable;
import workbench.gui.filter.FilterDefinitionManager;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.filter.FilterExpression;

/**
 *	Filter data from a WbTable 
 *	@author  support@sql-workbench.net
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
		this.setMenuText("");
		this.setIcon(ResourceMgr.getImage("dropdown"));
		initializeShortcut();
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
			}
			catch (Exception ex)
			{
				LogMgr.logError("FilterPickerAction.actionPerformed()", "Error loading filter", ex);
				Window w = SwingUtilities.getWindowAncestor(this.client);
				WbSwingUtilities.showErrorMessage(w, "Could not load filter: " + ex.getMessage());
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
    JButton result;
		if (this.dropDownButton == null || createNew)
		{
      DropDownButton b = new DropDownButton(ResourceMgr.getImage("dropdown"))
			{
				public void setBorder(Border b)
				{
					super.setBorder(WbSwingUtilities.EMPTY_BORDER);
				}
			};
			b.setAction(this);
			b.setMnemonic(0);
			b.setMaximumSize(new Dimension(8,16));
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
		List entries = FilterDefinitionManager.getInstance().getEntries();
		if (entries == null || entries.size() == 0) return;
		
		JMenu menu = new JMenu("filters");
		Iterator itr = entries.iterator();
		while (itr.hasNext())
		{
			String file = (String)itr.next();
			File f = new File(file);
			if (f.exists())
			{
				JMenuItem item = new WbMenuItem(f.getName());
				item.setToolTipText(f.getAbsolutePath());
				item.setActionCommand(f.getAbsolutePath());
				item.addActionListener(this);
				menu.add(item);			
			}
			else
			{
				LogMgr.logDebug("FilterPickerAction.buildPopup()", "Filter file '" + file + "' ignored because file was not found");
			}
		}
		if (this.dropDownButton == null)
		{
			this.getToolbarButton();
		}
		dropDownButton.setDropDownMenu(menu.getPopupMenu());
	}
}
