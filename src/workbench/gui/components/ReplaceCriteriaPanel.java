/*
 * ReplaceCriteriaPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import workbench.interfaces.Replaceable;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  thomas
 */
public class ReplaceCriteriaPanel extends JPanel
	implements ActionListener
{
	private static final String PROP_KEY_CASE = "workbench.sql.replace.ignoreCase";
	private JCheckBox ignoreCase;
	
	private JTextField criteria;
	private JTextField newValue;
//	private Replaceable client;
	
	public ReplaceCriteriaPanel(Replaceable aClient)
	{
		this(aClient, null);
	}
	
	public ReplaceCriteriaPanel(Replaceable aClient, String initialValue)
	{
//		this.client = aClient;
		this.ignoreCase = new JCheckBox(ResourceMgr.getString("LabelSearchIgnoreCase"));
		this.ignoreCase.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_CASE, true));
	
		JLabel label = new JLabel(ResourceMgr.getString("LabelSearchCriteria"));
		this.criteria = new JTextField();
		this.criteria.setColumns(40);
		this.criteria.setText(initialValue);
		if (initialValue != null)
		{
			this.criteria.selectAll();
		}
		this.criteria.addMouseListener(new TextComponentMouseListener());
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BorderLayout(5,0));
		searchPanel.add(label, BorderLayout.WEST);
		searchPanel.add(this.criteria, BorderLayout.CENTER);
		
		label = new JLabel(ResourceMgr.getString("LabelReplaceNewValue"));
		this.newValue = new JTextField();
		this.newValue.setColumns(40);
		JPanel replacePanel = new JPanel();
		replacePanel.setLayout(new BorderLayout(5,0));
		replacePanel.add(label, BorderLayout.WEST);
		replacePanel.add(this.newValue, BorderLayout.CENTER);
		

		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2,1));
		p.add(searchPanel);
		p.add(replacePanel);
		this.setLayout(new BorderLayout());
		this.add(p,BorderLayout.CENTER);
		this.add(this.ignoreCase, BorderLayout.SOUTH);
	}

	public String getCriteria()
	{
		return this.criteria.getText();
	}
	
	public boolean getIgnoreCase()
	{
		return this.ignoreCase.isSelected();
	}

	public void setSearchCriteria(String aValue)
	{
		this.criteria.setText(aValue);
		if (aValue != null)
		{
			this.criteria.selectAll();
		}
	}

	public void setNewValue(String aValue)
	{
		this.newValue.setText(aValue);
		if (aValue != null)
		{
			this.newValue.selectAll();
		}
	}
	
	public String getNewValue()
	{
		return this.newValue.getText();
	}
	
	public boolean showReplaceDialog(Component caller)
	{
		EventQueue.invokeLater(new Runnable() {
			public void run()
			{
				criteria.grabFocus();
			}
		});
		JDialog d = new JDialog();
		d.getContentPane().add(this, BorderLayout.CENTER);
		d.show();
		return true;
	}
	
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
	}

}
