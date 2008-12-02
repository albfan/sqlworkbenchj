/*
 * MacroManagerGui.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.actions.SaveListFileAction;
import workbench.gui.components.StringPropertyEditor;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.profiles.NewGroupAction;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.FileActions;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.macros.MacroDefinition;

/**
 * Displays all defined macros and lets the user add, edit and delete macros.
 * It uses {@link workbench.sql.MacroManager} to retrieve and store
 * the macros.
 *
 * @author support@sql-workbench.net
 */
public class MacroManagerGui
	extends JPanel
	implements FileActions, TreeSelectionListener, PropertyChangeListener, TreeModelListener
{
	private JToolBar toolbar;
	private JSplitPane jSplitPane1;
	private MacroTree macroTree;
	private EditorPanel macroEditor;
	private StringPropertyEditor macroNameField;
	private MacroDefinition currentMacro;

	public MacroManagerGui()
	{
		super();
		this.macroTree = new MacroTree();

		this.toolbar = new WbToolbar();
		this.toolbar.add(new NewListEntryAction(this));
		this.toolbar.add(new NewGroupAction(macroTree, "LblNewMacroGroup"));
		this.toolbar.addSeparator();
		this.toolbar.add(new DeleteListEntryAction(this));
		this.toolbar.addSeparator();
		this.toolbar.add(new SaveListFileAction(this));

		JPanel treePanel = new JPanel();
		treePanel.setLayout(new BorderLayout());
		treePanel.add(this.toolbar, BorderLayout.NORTH);

		this.jSplitPane1 = new WbSplitPane();


		this.setLayout(new BorderLayout());

		jSplitPane1.setDividerLocation(100);

		JScrollPane scroll = new JScrollPane(this.macroTree);
		scroll.setBorder(WbSwingUtilities.EMPTY_BORDER);
		treePanel.add(scroll, java.awt.BorderLayout.CENTER);

		macroEditor = EditorPanel.createSqlEditor();
		macroEditor.showFindOnPopupMenu();
		macroEditor.showFormatSql();

		jSplitPane1.setLeftComponent(treePanel);

		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BorderLayout());
		JLabel l = new JLabel(ResourceMgr.getString("LblMacroName"));
		l.setBorder(new CompoundBorder(new EmptyBorder(0,5,0,5), l.getBorder()));
		this.macroNameField = new StringPropertyEditor(); 
		this.macroNameField.setColumns(40);
		this.macroNameField.setImmediateUpdate(true);
		this.macroNameField.addPropertyChangeListener(this);
		//this.macroNameField.setBorder(new CompoundBorder(new EmptyBorder(0,0,0,5), this.macroNameField.getBorder()));

		namePanel.add(l, BorderLayout.WEST);
		namePanel.add(this.macroNameField, BorderLayout.CENTER);

		// Create some visiual space above and below the entry field
		JPanel p = new JPanel();
		namePanel.add(p, BorderLayout.SOUTH);
		p = new JPanel();
		namePanel.add(p, BorderLayout.NORTH);

		JPanel macroPanel = new JPanel(new BorderLayout());
		macroPanel.setLayout(new BorderLayout());
		macroPanel.add(namePanel, BorderLayout.NORTH);
		macroPanel.add(macroEditor, BorderLayout.CENTER);
		jSplitPane1.setRightComponent(macroPanel);

		add(jSplitPane1, BorderLayout.CENTER);
		macroTree.addTreeSelectionListener(this);

		WbTraversalPolicy policy = new WbTraversalPolicy();
		policy.addComponent(macroTree);
		policy.addComponent(macroNameField);
		policy.addComponent(macroEditor);
		policy.setDefaultComponent(macroTree);
		this.setFocusTraversalPolicy(policy);
	}


	public MacroDefinition getSelectedMacro()
	{
		return macroTree.getSelectedMacro();
	}

	public void deleteItem()
		throws Exception
	{
	}

	/**
	 *	Create a new profile. This will only be
	 *	created in the ListModel.
	 */
	public void newItem(boolean copyCurrent) throws Exception
	{
	}

	private void selectListLater()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				macroTree.requestFocusInWindow();
			}
		});
	}

	public void saveItem() throws Exception
	{
		macroTree.saveChanges();
	}

	public void saveSettings()
	{
		int location = this.jSplitPane1.getDividerLocation();
		Settings.getInstance().setProperty(this.getClass().getName() + ".divider", location);
		MacroDefinition macro = getSelectedMacro();
		Settings.getInstance().setProperty(this.getClass().getName() + ".lastmacro", macro == null ? "" : macro.getName());
		String group = macroTree.getGroupForSelectedMacro();
		Settings.getInstance().setProperty(this.getClass().getName() + ".lastmacrogroup", group);
	}

	public void restoreSettings()
	{
		int location = Settings.getInstance().getIntProperty(this.getClass().getName() + ".divider", 140);
		this.jSplitPane1.setDividerLocation(location);
		String macro = Settings.getInstance().getProperty(this.getClass().getName() + ".lastmacro", null);
		String group = Settings.getInstance().getProperty(this.getClass().getName() + ".lastmacrogroup", null);
		this.selectMacro(group, macro);
		this.selectListLater();
	}

	private void selectMacro(String group, String macro)
	{
		macroTree.selectMacro(group, macro);
	}

	private void showMacro(final MacroDefinition entry)
	{
		if (this.currentMacro != null && macroEditor.isModified())
		{
			currentMacro.setText(macroEditor.getText());
		}
		currentMacro = entry;
		if (entry != null)
		{
			macroEditor.setEnabled(true);
			macroEditor.setVisible(true);
			macroNameField.setEnabled(true);
			macroNameField.setSourceObject(entry, "name", entry.getName());
			macroNameField.setImmediateUpdate(true);
			macroEditor.setText(entry.getText());
			macroEditor.setCaretPosition(0);
		}
		else
		{
			macroNameField.setSourceObject(null, "name");
			macroEditor.setText("");
			macroEditor.setEnabled(false);
			macroNameField.setEnabled(false);
			macroEditor.setVisible(false);
		}
	}

	public void treeNodesChanged(TreeModelEvent e)
	{
		
	}

	public void treeNodesInserted(TreeModelEvent e)
	{

	}

	public void treeNodesRemoved(TreeModelEvent e)
	{

	}

	public void treeStructureChanged(TreeModelEvent e)
	{

	}


	public void propertyChange(java.beans.PropertyChangeEvent evt)
	{
		this.macroTree.repaint();
	}

	public void valueChanged(TreeSelectionEvent e)
	{
		TreePath path = e.getPath();
		if (path == null) return;
		MacroTreeNode node = (MacroTreeNode)path.getLastPathComponent();
		if (!node.getAllowsChildren())
		{
			MacroDefinition macro = (MacroDefinition)node.getDataObject();
			showMacro(macro);
		}
		else
		{
			showMacro(null);
		}
	}
}
