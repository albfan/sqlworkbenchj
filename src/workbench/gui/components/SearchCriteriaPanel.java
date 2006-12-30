/*
 * SearchCriteriaPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import workbench.interfaces.ValidatingComponent;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class SearchCriteriaPanel
	extends JPanel
	implements ValidatingComponent
{
	private static final String PROP_CLASS = "workbench.sql.search";
	private static final String PROP_KEY_CASE = PROP_CLASS + ".ignoreCase";
	private static final String PROP_KEY_WHOLE_WORD = PROP_CLASS + ".wholeWord";
	private static final String PROP_KEY_REGEX = PROP_CLASS + ".useRegEx";
	private static final String PROP_KEY_CRIT = PROP_CLASS + ".lastValue";
	private JCheckBox ignoreCase;
	private JCheckBox wholeWord;
	private JCheckBox useRegEx;
	
	protected JTextField criteria;
	protected JLabel label;
	
	public SearchCriteriaPanel()
	{
		this(null);
	}
	
	public SearchCriteriaPanel(String initialValue)
	{
		this.ignoreCase = new JCheckBox(ResourceMgr.getString("LblSearchIgnoreCase"));
		this.ignoreCase.setToolTipText(ResourceMgr.getDescription("LblSearchIgnoreCase"));
		this.ignoreCase.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_CASE, true));
		
		this.wholeWord = new JCheckBox(ResourceMgr.getString("LblSearchWordsOnly"));
		this.wholeWord.setToolTipText(ResourceMgr.getDescription("LblSearchWordsOnly"));
		this.wholeWord.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_WHOLE_WORD, false));

		this.useRegEx = new JCheckBox(ResourceMgr.getString("LblSearchRegEx"));
		this.useRegEx.setToolTipText(ResourceMgr.getDescription("LblSearchRegEx"));
		this.useRegEx.setSelected(Settings.getInstance().getBoolProperty(PROP_KEY_REGEX, false));
		
		this.label = new JLabel(ResourceMgr.getString("LblSearchCriteria"));
		this.criteria = new JTextField();
		this.criteria.setColumns(40);
		if (initialValue != null)
		{
			this.criteria.setText(initialValue);
			this.criteria.selectAll();
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
		String title = ResourceMgr.getString("TxtWindowTitleSearchText");
		boolean result = ValidatingDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(caller), this, title);
		
		Settings.getInstance().setProperty(PROP_KEY_CASE, this.getIgnoreCase());
		Settings.getInstance().setProperty(PROP_KEY_CRIT, this.getCriteria());
		Settings.getInstance().setProperty(PROP_KEY_WHOLE_WORD, this.getWholeWordOnly());
		Settings.getInstance().setProperty(PROP_KEY_REGEX, this.getUseRegex());
		return result;
	}

	public boolean validateInput()
	{
		return true;
	}

	public void componentDisplayed()
	{
		criteria.requestFocusInWindow();
	}
	
}
