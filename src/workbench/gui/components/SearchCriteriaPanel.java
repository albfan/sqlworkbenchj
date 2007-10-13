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
import java.awt.Window;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import workbench.gui.WbSwingUtilities;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * A search dialog panel.
 * 
 * @see workbench.gui.components.TableReplacer
 * @see workbench.gui.editor.SearchAndReplace
 * 
 * @author support@sql-workbench.net
 */
public class SearchCriteriaPanel
	extends JPanel
	implements ValidatingComponent
{
	private String caseProperty;
	private String wordProperty;
	private String regexProperty;
	private String criteriaProperty;
	
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
		this(initialValue, "workbench.sql.search");
	}
	
	public SearchCriteriaPanel(String initialValue, String settingsKey)
	{
		caseProperty = settingsKey + ".ignoreCase";
		wordProperty = settingsKey + ".wholeWord";
		regexProperty = settingsKey + ".useRegEx";
		criteriaProperty = settingsKey + ".lastValue";
		
		this.ignoreCase = new JCheckBox(ResourceMgr.getString("LblSearchIgnoreCase"));
		this.ignoreCase.setName("ignorecase");
		this.ignoreCase.setToolTipText(ResourceMgr.getDescription("LblSearchIgnoreCase"));
		this.ignoreCase.setSelected(Settings.getInstance().getBoolProperty(caseProperty, true));
		
		this.wholeWord = new JCheckBox(ResourceMgr.getString("LblSearchWordsOnly"));
		this.wholeWord.setToolTipText(ResourceMgr.getDescription("LblSearchWordsOnly"));
		this.wholeWord.setSelected(Settings.getInstance().getBoolProperty(wordProperty, false));
		this.wholeWord.setName("wholeword");

		this.useRegEx = new JCheckBox(ResourceMgr.getString("LblSearchRegEx"));
		this.useRegEx.setToolTipText(ResourceMgr.getDescription("LblSearchRegEx"));
		this.useRegEx.setSelected(Settings.getInstance().getBoolProperty(regexProperty, false));
		this.useRegEx.setName("regex");
		
		this.label = new JLabel(ResourceMgr.getString("LblSearchCriteria"));
		this.criteria = new JTextField();
		this.criteria.setName("searchtext");
		
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
		return showFindDialog(caller, ResourceMgr.getString("TxtWindowTitleSearchText"));
	}
	
	public boolean showFindDialog(Component caller, String title)
	{
		Window w = WbSwingUtilities.getWindowAncestor(caller);
		boolean result = ValidatingDialog.showConfirmDialog(w, this, title, caller, 0, false);
		
		Settings.getInstance().setProperty(caseProperty, this.getIgnoreCase());
		Settings.getInstance().setProperty(criteriaProperty, this.getCriteria());
		Settings.getInstance().setProperty(wordProperty, this.getWholeWordOnly());
		Settings.getInstance().setProperty(regexProperty, this.getUseRegex());
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
