/*
 * LnFOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.WbToolbar;
import workbench.gui.lnf.LnFDefinition;
import workbench.gui.lnf.LnFManager;
import workbench.interfaces.FileActions;
import workbench.interfaces.Restoreable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class LnFOptionsPanel
	extends JPanel
	implements Restoreable, ListSelectionListener, FileActions,
	           PropertyChangeListener
{
	private JList lnfList;
	private LnFDefinitionPanel definitionPanel;
	protected LnFManager manager = new LnFManager();
	private WbToolbar toolbar;
	private DeleteListEntryAction deleteEntry = null;

	public LnFOptionsPanel()
	{
		super();
		setLayout(new BorderLayout());

		lnfList = new JList();
		lnfList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lnfList.setBorder(new EmptyBorder(2,1,2,1));

		lnfList.setMinimumSize(new Dimension(100, 100));
		JScrollPane scroll = new JScrollPane(lnfList);

		deleteEntry = new DeleteListEntryAction(this);
		this.toolbar = new WbToolbar();
		this.toolbar.add(new NewListEntryAction(this));
		this.toolbar.add(deleteEntry);
		toolbar.setBorder(DividerBorder.BOTTOM_DIVIDER);

		definitionPanel = new LnFDefinitionPanel();
		definitionPanel.setPropertyListener(this);

		add(scroll, BorderLayout.WEST);
		add(toolbar, BorderLayout.NORTH);
		add(definitionPanel, java.awt.BorderLayout.CENTER);

		ListModel model = new LnfList();
		lnfList.setModel(model);
		lnfList.addListSelectionListener(this);
	}

	@Override
	public void saveSettings()
	{
		manager.saveLookAndFeelDefinitions();
	}

	@Override
	public void restoreSettings()
	{
		LnFDefinition clnf = manager.getCurrentLnF();
		lnfList.setSelectedValue(clnf, true);
		definitionPanel.setCurrentLookAndFeeld(clnf);
	}

	@Override
	public void valueChanged(ListSelectionEvent evt)
	{
		LnFDefinition def = (LnFDefinition)lnfList.getSelectedValue();
		definitionPanel.setDefinition(def);
		if (def != null)
		{
			this.deleteEntry.setEnabled(!def.isBuiltInLnF());
		}
	}

	@Override
	public void saveItem() throws Exception
	{
	}

	@Override
	public void deleteItem() throws Exception
	{
		LnFDefinition def = (LnFDefinition)lnfList.getSelectedValue();
		int index = lnfList.getSelectedIndex();
		if (def != null)
		{
			manager.removeDefinition(def);
		}
		if (lnfList.getModel().getSize() == 0)
		{
			definitionPanel.setDefinition(null);
		}
		if (index >= lnfList.getModel().getSize())
		{
			index = lnfList.getModel().getSize() - 1;
		}
		lnfList.setSelectedIndex(index);
		valueChanged(null);
		lnfList.repaint();
	}

	@Override
	public void newItem(boolean copyCurrent) throws Exception
	{
		try
		{
			LnFDefinition def = null;
			if (copyCurrent)
			{
				int index = lnfList.getSelectedIndex();
				LnFDefinition current = manager.getAvailableLookAndFeels().get(index);
				def = current.createCopy();
			}
			else
			{
				String d = ResourceMgr.getString("TxtLnFSample");
				def = new LnFDefinition(d);
			}
			int index = manager.addDefinition(def);
			lnfList.setSelectedIndex(index);
			definitionPanel.setDefinition(def);
			lnfList.updateUI();
		}
		catch (Exception e)
		{
			LogMgr.logError("LnFOptionsPanel.newItem()", "Error creating new item", e);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("name"))
		{
			lnfList.repaint();
		}
	}

	class LnfList
		extends AbstractListModel
	{
		@Override
		public Object getElementAt(int index)
		{
			return manager.getAvailableLookAndFeels().get(index);
		}

		@Override
		public int getSize()
		{
			return manager.getAvailableLookAndFeels().size();
		}
	}

}
