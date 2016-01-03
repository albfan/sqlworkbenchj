/*
 * DriverlistEditorPanel.java
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
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.interfaces.FileActions;
import workbench.interfaces.ListSelectionControl;
import workbench.interfaces.Validator;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.DbDriver;

import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbTraversalPolicy;

/**
 * @author Thomas Kellerer
 */
public class DriverlistEditorPanel
	extends JPanel
	implements FileActions, Validator, ListSelectionControl
{
	private DriverListModel model;
	private JToolBar toolbar;
	private int lastIndex = -1;

	private DriverEditorPanel driverEditor;
	private JList driverList;

	public DriverlistEditorPanel()
	{
		super();
		initComponents();
		this.fillDriverList();
		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(driverList);
		policy.addComponent(driverEditor);
		policy.setDefaultComponent(driverList);
	}

	private void fillDriverList()
	{
		this.model = new DriverListModel(ConnectionMgr.getInstance().getDrivers());
		this.driverList.setModel(this.model);
		if (this.model.getSize() > 0)
		{
			this.driverList.setSelectedIndex(0);
		}
	}

	private void initComponents()
	{
		driverList = new JList();
		driverList.setSelectionModel(new VetoableListSelectionModel(this));
		driverEditor = new DriverEditorPanel();
		driverEditor.setValidator(this);

		setLayout(new BorderLayout());

		driverList.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent evt)
			{
				driverListValueChanged(evt);
			}
		});

		JScrollPane scroll = new JScrollPane(driverList);

		this.toolbar = new WbToolbar();
		toolbar.setBorder(DividerBorder.BOTTOM_DIVIDER);
		this.toolbar.add(new NewListEntryAction(this));
		this.toolbar.add(new DeleteListEntryAction(this));

		add(toolbar, BorderLayout.NORTH);
		add(scroll, BorderLayout.WEST);
		add(driverEditor, BorderLayout.CENTER);
		this.setMinimumSize(new Dimension(200, 400));
	}

	private void driverListValueChanged(ListSelectionEvent evt)
	{
		if (evt.getSource() == this.driverList)
		{
			if (lastIndex > -1)
			{
				DbDriver current = this.driverEditor.getDriver();
				this.model.putDriver(lastIndex, current);
			}
			int index = this.driverList.getSelectedIndex();
			if (index > -1)
			{
				DbDriver newDriver = this.model.getDriver(index);
				this.driverEditor.setDriver(newDriver);
			}
			lastIndex = index;
		}
	}

	public DbDriver getSelectedDriver()
	{
		this.updateUI();
		this.driverEditor.updateDriver();
		int index = driverList.getSelectedIndex();
		DbDriver drv = this.model.getDriver(index);
		return drv;
	}

	public void selectDriver(String aDriverName)
	{
		if (aDriverName == null) return;

		try
		{
			int count = this.model.getSize();

			for (int i=0; i < count; i++)
			{
				DbDriver drv = this.model.getDriver(i);
				if (drv.getName().equals(aDriverName))
				{
					this.driverList.setSelectedIndex(i);
					this.driverList.ensureIndexIsVisible(i);
					break;
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DriverListEditorPanel.selectDriver()", "Error selecting item", e);
			driverList.setSelectedIndex(0);
		}
	}

	/**
	 *	Remove an item from the listmodel
	 */
	@Override
	public void deleteItem() throws Exception
	{
		int index = this.driverList.getSelectedIndex();
		this.driverList.clearSelection();
		this.driverEditor.reset();
		this.model.deleteDriver(index);

		// check if the last driver was deleted
		if (index > this.model.getSize() - 1) index--;

		this.driverList.setSelectedIndex(index);
		this.driverList.repaint();
	}

	/**
	 *	Create a new profile. This will only be
	 *	created in the ListModel.
	 */
	@Override
	public void newItem(boolean copyCurrent) throws Exception
	{
		DbDriver drv;
		if (copyCurrent)
		{
			drv = this.getSelectedDriver().createCopy();
		}
		else
		{
			drv = new DbDriver();
		}
		drv.setName(ResourceMgr.getString("TxtEmptyDriverName"));
		this.model.addDriver(drv);
		this.selectDriver(drv.getName());
		this.driverList.updateUI();
	}

	@Override
	public void saveItem()
		throws Exception
	{
		// validateName() will call isValid() in turn, but will also display an error message if needed
		if (driverEditor.validateName())
		{
			ConnectionMgr conn = ConnectionMgr.getInstance();
			this.driverEditor.updateDriver();
			conn.setDrivers(this.model.getValues());
			conn.saveDrivers();
		}
	}

	@Override
	public boolean isValid(String name)
	{
		if (name == null) return false;
		name = name.trim();
		int selected = driverList.getSelectedIndex();
		int count = model.getValues().size();
		for (int index = 0; index < count; index ++)
		{
			if (index != selected)
			{
				if (name.equalsIgnoreCase(model.getDriver(index).getName()))
				{
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean canChangeSelection()
	{
		return driverEditor.validateName();
	}

}
