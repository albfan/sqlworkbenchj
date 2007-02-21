/*
 * ExternalToolsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.interfaces.FileActions;
import workbench.interfaces.Restoreable;
import workbench.resource.Settings;
import workbench.util.ToolDefinition;

/**
 *
 * @author support@sql-workbench.net
 */
public class ExternalToolsPanel 
	extends JPanel
	implements Restoreable, ListSelectionListener, FileActions, 
	           PropertyChangeListener
{
	public WbSplitPane splitPane;
	private JPanel listPanel;
	private JList toolList;
	private ToolDefinitionPanel definitionPanel;
	private WbToolbar toolbar;
	private JComboBox lnfSelector;
	private DefaultListModel tools;
	
	public ExternalToolsPanel()
	{
		setLayout(new BorderLayout());
		
		splitPane = new WbSplitPane();
		listPanel = new JPanel();
		toolList = new JList();
		toolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane scroll = new JScrollPane(toolList);

		this.toolbar = new WbToolbar();
		this.toolbar.addDefaultBorder();
		this.toolbar.add(new NewListEntryAction(this));
		this.toolbar.add(new DeleteListEntryAction(this));
		setBorder(DividerBorder.BOTTOM_DIVIDER);
		
		listPanel.setLayout(new BorderLayout());
		listPanel.add(scroll, BorderLayout.CENTER);
		listPanel.add(this.toolbar, BorderLayout.NORTH);
		
		splitPane.setLeftComponent(listPanel);
		
		definitionPanel = new ToolDefinitionPanel();
		splitPane.setRightComponent(definitionPanel);
		
		add(splitPane, java.awt.BorderLayout.CENTER);

		tools = new DefaultListModel();
		ToolDefinition[] t = Settings.getInstance().getAllExternalTools();
		for (int i = 0; i < t.length; i++)
		{
			tools.addElement(t[i]);
		}
		toolList.setModel(tools);
		toolList.addListSelectionListener(this);
		toolList.setSelectedIndex(0);
		restoreSettings();
	}
	
	public void saveSettings()
	{
		int divider = splitPane.getDividerLocation();
		Settings.getInstance().setProperty(this.getClass().getName() + ".divider", divider);
		List l = new LinkedList();
		Enumeration e = this.tools.elements();
		while (e.hasMoreElements())
		{
			l.add(e.nextElement());
		}
		Settings.getInstance().setExternalTool(l);
	}
	
	public void restoreSettings()
	{
		int divider = Settings.getInstance().getIntProperty(this.getClass().getName() + ".divider", 120);
		splitPane.setDividerLocation(divider);
	}
	
	public void valueChanged(ListSelectionEvent evt)
	{
		ToolDefinition def = (ToolDefinition)toolList.getSelectedValue();
		definitionPanel.setDefinition(def);
	}

	public void saveItem() throws Exception
	{
	}

	public void deleteItem() throws Exception
	{
		int index = toolList.getSelectedIndex();
		if (index > -1)
		{
			tools.remove(index);
		}
		if (toolList.getModel().getSize() == 0)
		{
			definitionPanel.setDefinition(null);
		}
		toolList.repaint();
	}

	public void newItem(boolean copyCurrent) throws Exception
	{
		try
		{
			ToolDefinition tool = new ToolDefinition("path_to_program", "New Tool");
			tools.addElement(tool);
			toolList.setSelectedIndex(tools.size()-1);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("name"))
		{
			toolList.repaint();
		}
	}

}
