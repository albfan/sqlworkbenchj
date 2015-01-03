/*
 * MacroManagerGui.java
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
package workbench.gui.macros;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import workbench.interfaces.FileActions;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CollapseTreeAction;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.ExpandTreeAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.profiles.NewGroupAction;

import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroGroup;

import workbench.util.StringUtil;

/**
 * Displays all defined macros and lets the user add, edit and delete macros.
 * It uses {@link workbench.sql.macros.MacroManager} to retrieve and store
 * the macros.
 *
 * @author Thomas Kellerer
 */
public class MacroManagerGui
	extends JPanel
	implements FileActions, TreeSelectionListener, PropertyChangeListener, TreeModelListener
{
	private JToolBar toolbar;
	private JSplitPane splitPane;
	private MacroDefinitionPanel macroPanel;
	private MacroGroupPanel groupPanel;
	private MacroTree macroTree;
	public MacroManagerGui(int macroId)
	{
		super();
		this.macroTree = new MacroTree(macroId);
		this.setLayout(new BorderLayout());

		this.toolbar = new WbToolbar();
		this.toolbar.add(new NewListEntryAction(this));
		this.toolbar.add(new NewGroupAction(macroTree, "LblNewMacroGroup"));
		this.toolbar.addSeparator();

		DeleteListEntryAction deleteAction = new DeleteListEntryAction(this);
		this.toolbar.add(deleteAction);
		this.toolbar.addSeparator();
		this.toolbar.add(new ExpandTreeAction(macroTree));
		this.toolbar.add(new CollapseTreeAction(macroTree));

		macroTree.addPopupAction(deleteAction, true);

		JPanel treePanel = new JPanel();
		treePanel.setLayout(new BorderLayout());
		treePanel.add(this.toolbar, BorderLayout.NORTH);

		splitPane = new WbSplitPane();
		splitPane.setDividerLocation(140);

		JScrollPane scroll = new JScrollPane(this.macroTree);
		scroll.setBorder(WbSwingUtilities.EMPTY_BORDER);
		treePanel.add(scroll, java.awt.BorderLayout.CENTER);

		splitPane.setLeftComponent(treePanel);

		macroPanel = new MacroDefinitionPanel(this);
		groupPanel = new MacroGroupPanel(this);

		JPanel dummy = new JPanel();
		dummy.setMinimumSize(macroPanel.getMinimumSize());
		dummy.setPreferredSize(macroPanel.getPreferredSize());
		splitPane.setRightComponent(dummy);

		add(splitPane, BorderLayout.CENTER);
		macroTree.addTreeSelectionListener(this);
		ToolTipManager.sharedInstance().registerComponent(macroTree);

		Dimension panelMinSize = macroPanel.getMinimumSize();
		Dimension treeMinSize = macroTree.getMinimumSize();
		int height = Math.max(panelMinSize.height, treeMinSize.height);
		int width = panelMinSize.width + treeMinSize.width;
		Dimension minSize = new Dimension(width, height);
		splitPane.setMinimumSize(minSize);
		Dimension minSize2 = new Dimension(minSize.width + 15, minSize.height  + 15);
		this.setMinimumSize(minSize2);
	}

	public void dispose()
	{
		ToolTipManager.sharedInstance().unregisterComponent(macroTree);
	}

	public void setCurrentConnection(WbConnection conn)
	{
		macroPanel.setCurrentConnection(conn);
	}

	public void addTreeSelectionListener(TreeSelectionListener l)
	{
		macroTree.addTreeSelectionListener(l);
	}

	public MacroDefinition getSelectedMacro()
	{
		return macroTree.getSelectedMacro();
	}

	@Override
	public void deleteItem()
		throws Exception
	{
		macroTree.deleteSelection();
		macroTree.repaint();
	}

	@Override
	public void newItem(boolean copyCurrent) throws Exception
	{
		boolean ok = macroTree.addMacro(copyCurrent);
		if (ok)
		{
			macroPanel.selectMacroName();
		}
	}

	private void selectListLater()
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				macroTree.requestFocusInWindow();
			}
		});
	}

	@Override
	public void saveItem() throws Exception
	{
		macroPanel.applyChanges();
		macroTree.saveChanges();
	}

	public void saveSettings()
	{
		int location = this.splitPane.getDividerLocation();
		Settings.getInstance().setProperty(this.getClass().getName() + ".divider", location);
		MacroDefinition macro = getSelectedMacro();
		Settings.getInstance().setProperty(this.getClass().getName() + ".lastmacro", macro == null ? "" : macro.getName());
		MacroGroup group = macroTree.getCurrentGroup();
		Settings.getInstance().setProperty(this.getClass().getName() + ".lastmacrogroup", group == null ? "" : group.getName());

		List<String> expandedGroups = macroTree.getExpandedGroupNames();
		Settings.getInstance().setProperty(this.getClass().getName() + ".expandedgroups", StringUtil.listToString(expandedGroups, ',', true));
	}

	public void restoreSettings()
	{
		int location = Settings.getInstance().getIntProperty(this.getClass().getName() + ".divider", 190);
		this.splitPane.setDividerLocation(location);
		String macro = Settings.getInstance().getProperty(this.getClass().getName() + ".lastmacro", null);
		String group = Settings.getInstance().getProperty(this.getClass().getName() + ".lastmacrogroup", null);
		macroTree.selectMacro(group, macro);
		String groups = Settings.getInstance().getProperty(this.getClass().getName() + ".expandedgroups", null);
		List<String> l = StringUtil.stringToList(groups, ",", true, true);
		macroTree.expandGroups(l);
		this.selectListLater();
	}

	private void showGroup(final MacroGroup group)
	{
		macroPanel.setMacro(null);
		groupPanel.setMacroGroup(group);
		changePanel(groupPanel);
	}

	private void showMacro(final MacroDefinition entry)
	{
		groupPanel.setMacroGroup(null);
		macroPanel.setMacro(entry);
		changePanel(macroPanel);
	}

	private void changePanel(JPanel newPanel)
	{
		int location = splitPane.getDividerLocation();
		splitPane.setRightComponent(newPanel);
		splitPane.setDividerLocation(location);
	}

	@Override
	public void treeNodesChanged(TreeModelEvent e)
	{

	}

	@Override
	public void treeNodesInserted(TreeModelEvent e)
	{

	}

	@Override
	public void treeNodesRemoved(TreeModelEvent e)
	{

	}

	@Override
	public void treeStructureChanged(TreeModelEvent e)
	{

	}


	@Override
	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		this.macroTree.repaint();
	}

	@Override
	public void valueChanged(TreeSelectionEvent e)
	{
		TreePath path = e.getPath();
		if (path == null) return;
		MacroTreeNode node = (MacroTreeNode)path.getLastPathComponent();
		if (node.getAllowsChildren())
		{
			MacroGroup group = (MacroGroup)node.getDataObject();
			showGroup(group);
		}
		else
		{
			MacroDefinition macro = (MacroDefinition)node.getDataObject();
			showMacro(macro);
		}
	}
}
