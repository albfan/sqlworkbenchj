/*
 * SearchCriteriaPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  thomas
 */
public class SearchCriteriaPanel
	extends JPanel
{
	private static final String PROP_CLASS = "workbench.sql.search";
	private static final String PROP_KEY_CASE = PROP_CLASS + ".ignoreCase";
	private static final String PROP_KEY_WHOLE_WORD = PROP_CLASS + ".wholeWord";
	private static final String PROP_KEY_REGEX = PROP_CLASS + ".useRegEx";
	private static final String PROP_KEY_CRIT = PROP_CLASS + ".lastValue";
	private JCheckBox ignoreCase;
	private JCheckBox wholeWord;
	private JCheckBox useRegEx;
	
	private JTextField criteria;
	private JLabel label;
	
	public SearchCriteriaPanel()
	{
		this(null);
	}
	
	public SearchCriteriaPanel(String initialValue)
	{
		this.ignoreCase = new JCheckBox(ResourceMgr.getString("LabelSearchIgnoreCase"));
		this.ignoreCase.setToolTipText(ResourceMgr.getDescription("LabelSearchIgnoreCase"));
		this.ignoreCase.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_CASE, true));
		
		this.wholeWord = new JCheckBox(ResourceMgr.getString("LabelSearchWordsOnly"));
		this.wholeWord.setToolTipText(ResourceMgr.getDescription("LabelSearchWordsOnly"));
		this.wholeWord.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_WHOLE_WORD, false));

		this.useRegEx = new JCheckBox(ResourceMgr.getString("LabelSearchRegEx"));
		this.useRegEx.setToolTipText(ResourceMgr.getDescription("LabelSearchRegEx"));
		this.useRegEx.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_REGEX, false));
		
		this.label = new JLabel(ResourceMgr.getString("LabelSearchCriteria"));
		this.criteria = new JTextField();
		this.criteria.setColumns(40);
		if (initialValue != null)
		{
			this.criteria.setText(initialValue);
			this.criteria.selectAll();
			//initialValue = Settings.getInstance().getProperty(PROP_CLASS, PROP_KEY_CRIT, null);
		}
		
		this.criteria.addMouseListener(new TextComponentMouseListener());
		
		this.setLayout(new BorderLayout());
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout(5,0));
		p.add(this.label, BorderLayout.WEST);
		p.add(this.criteria, BorderLayout.CENTER);
		this.add(p,BorderLayout.CENTER);
		p = new JPanel();
		p.setLayout(new GridLayout(3,1));
		p.add(this.ignoreCase);
		p.add(this.wholeWord);
		p.add(this.useRegEx);
		
		this.add(p, BorderLayout.SOUTH);
	}

	public String getCriteria()
	{
		return this.criteria.getText();
	}
	
	public boolean getWholeWordOnly()
	{
		return this.wholeWord.isSelected();
	}
	
	public boolean getIgnoreCase()
	{
		return this.ignoreCase.isSelected();
	}

	public boolean getUseRegex()
	{
		return this.useRegEx.isSelected();
	}
	
	public void setSearchCriteria(String aValue)
	{
		this.criteria.setText(aValue);
		if (aValue != null)
		{
			this.criteria.selectAll();
		}
	}
	
	public boolean showFindDialog(Component caller)
	{
		EventQueue.invokeLater(new Runnable() {
			public void run()
			{
				criteria.grabFocus();
			}
		});
		String title = ResourceMgr.getString("TxtWindowTitleSearchText");
		int choice = JOptionPane.showConfirmDialog(caller, this, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		Settings.getInstance().setProperty(PROP_KEY_CASE, this.getIgnoreCase());
		Settings.getInstance().setProperty(PROP_KEY_CRIT, this.getCriteria());
		Settings.getInstance().setProperty(PROP_KEY_WHOLE_WORD, this.getWholeWordOnly());
		Settings.getInstance().setProperty(PROP_KEY_REGEX, this.getUseRegex());
		return (choice == JOptionPane.OK_OPTION);
	}
	
}
