/*
 * LnFOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.lnf.LnFDefinition;
import workbench.gui.lnf.LnFLoader;
import workbench.gui.lnf.LnFManager;
import workbench.interfaces.FileActions;
import workbench.interfaces.Restoreable;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class LnFOptionsPanel 
	extends JPanel
	implements Restoreable, ListSelectionListener, FileActions, 
	           PropertyChangeListener, ActionListener
{
	public WbSplitPane splitPane;
	private JPanel listPanel;
	private JList lnfList;
	private LnFDefinitionPanel definitionPanel;
	private LnFManager manager = new LnFManager();
	private WbToolbar toolbar;
	private JComboBox lnfSelector;
	private JLabel currentLabel;
	private WbButton switchLnFButton;
	private String newLnFClass;
	
	public LnFOptionsPanel()
	{
		setLayout(new BorderLayout());
		
		splitPane = new WbSplitPane();
		listPanel = new JPanel();
		lnfList = new JList();
		lnfList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane scroll = new JScrollPane(lnfList);

		this.toolbar = new WbToolbar();
		this.toolbar.addDefaultBorder();
		this.toolbar.add(new NewListEntryAction(this));
		this.toolbar.add(new DeleteListEntryAction(this));
		
		listPanel.setLayout(new BorderLayout());
		listPanel.add(scroll, BorderLayout.CENTER);
		listPanel.add(this.toolbar, BorderLayout.NORTH);
		Dimension d = toolbar.getPreferredSize();
		
		JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,0,2));
		switchLnFButton = new WbButton();
		switchLnFButton.setResourceKey("LblSwitchLnF");
		switchLnFButton.addActionListener(this);
		
		switchPanel.add(switchLnFButton);
		
		listPanel.add(switchPanel, BorderLayout.SOUTH);
		
		splitPane.setLeftComponent(listPanel);
		
		JPanel infoPanel = new JPanel(new BorderLayout());
		
		definitionPanel = new LnFDefinitionPanel();
		definitionPanel.setPropertyListener(this);
		infoPanel.add(definitionPanel, BorderLayout.CENTER);

		currentLabel = new JLabel();
		
		currentLabel.setMinimumSize(new Dimension(50, (int)d.getHeight()));
		currentLabel.setPreferredSize(d);
		infoPanel.add(currentLabel, BorderLayout.NORTH);
		
		splitPane.setRightComponent(infoPanel);
		
		add(splitPane, java.awt.BorderLayout.CENTER);

		ListModel model = new AbstractListModel()
		{
			public Object getElementAt(int index)
			{
				return manager.getAvailableLookAndFeels().get(index);
			}
			
			public int getSize()
			{
				return manager.getAvailableLookAndFeels().size();
			}
		};
		lnfList.setModel(model);
		lnfList.addListSelectionListener(this);
		LnFDefinition clnf = manager.getCurrentLnF();
		lnfList.setSelectedValue(clnf, true);
		if (clnf != null) setCurrentInfo(clnf.getName());
		restoreSettings();
	}
	
	private void setCurrentInfo(String name)
	{
		currentLabel.setText("  " + ResourceMgr.getString("LblCurrLnf") + " " + name);
	}
	
	public void saveSettings()
	{
		int divider = splitPane.getDividerLocation();
		Settings.getInstance().setProperty(this.getClass().getName() + ".divider", divider);
		manager.saveLookAndFeelDefinitions();
		if (this.newLnFClass != null)
		{
			Settings.getInstance().setLookAndFeelClass(newLnFClass);
		}
	}
	
	public void restoreSettings()
	{
		int divider = Settings.getInstance().getIntProperty(this.getClass().getName() + ".divider", 120);
		splitPane.setDividerLocation(divider);
	}
	
  public void valueChanged(ListSelectionEvent evt)
  {
		//if (evt.getValueIsAdjusting()) return;
		int index = lnfList.getSelectedIndex();
		LnFDefinition def = (LnFDefinition)manager.getAvailableLookAndFeels().get(index);
		definitionPanel.setDefinition(def);
  }

	public void saveItem() throws Exception
	{
	}

	public void deleteItem() throws Exception
	{
		int index = lnfList.getSelectedIndex();
		if (index > -1)
		{
			manager.removeDefinition(index);
		}
		if (lnfList.getModel().getSize() == 0)
		{
			definitionPanel.setDefinition(null);
		}
		lnfList.repaint();
	}

	public void newItem(boolean copyCurrent) throws Exception
	{
		try
		{
			LnFDefinition def = null;
			if (copyCurrent)
			{
				int index = lnfList.getSelectedIndex();
				LnFDefinition current = (LnFDefinition)manager.getAvailableLookAndFeels().get(index);
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
			e.printStackTrace();
		}
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("name"))
		{
			lnfList.repaint();
		}
	}

	private boolean testLnF(LnFDefinition lnf)
	{
		try
		{
			LnFLoader loader = new LnFLoader(lnf);
			return loader.isAvailable();
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() != switchLnFButton) return;
		int index = lnfList.getSelectedIndex();
		LnFDefinition lnf = (LnFDefinition)manager.getAvailableLookAndFeels().get(index);
		if (testLnF(lnf))
		{
			this.newLnFClass = lnf.getClassName();
			this.currentLabel.setText(lnf.getName());
			WbSwingUtilities.showMessage(this, ResourceMgr.getString("MsgLnFChanged"));
		}
		else
		{
			this.newLnFClass = null;
			WbSwingUtilities.showMessage(this, ResourceMgr.getString("MsgLnFNotLoaded"));
		}
	}
	
}
