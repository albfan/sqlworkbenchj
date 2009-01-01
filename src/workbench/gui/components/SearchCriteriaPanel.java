/*
 * SearchCriteriaPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
	implements ValidatingComponent, ActionListener
{
	private String caseProperty;
	private String wordProperty;
	private String regexProperty;
	private String highlightProperty;
	private String baseProperty;
	private JCheckBox ignoreCase;
	private JCheckBox wholeWord;
	private JCheckBox useRegEx;
	private JCheckBox highlightAll;
	protected HistoryTextField criteria;
	protected JLabel label;

	public SearchCriteriaPanel()
	{
		this(null);
	}

	public SearchCriteriaPanel(String initialValue)
	{
		this(initialValue, "workbench.sql.search", false);
	}

	public SearchCriteriaPanel(String initialValue, String settingsKey, boolean showHighlight)
	{
		super();
		baseProperty = settingsKey;
		caseProperty = settingsKey + ".ignoreCase";
		wordProperty = settingsKey + ".wholeWord";
		regexProperty = settingsKey + ".useRegEx";

		if (showHighlight)
		{
			highlightProperty = settingsKey + ".highlight";
		}

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

		if (showHighlight)
		{
			this.highlightAll = new JCheckBox(ResourceMgr.getString("LblHighlightAll"));
			this.highlightAll.setToolTipText(ResourceMgr.getDescription("LblHighlightAll"));
			this.highlightAll.setSelected(Settings.getInstance().getBoolProperty(highlightProperty, false));
		}

		this.label = new JLabel(ResourceMgr.getString("LblSearchCriteria"));
		this.criteria = new HistoryTextField("search");
		this.criteria.setName("searchtext");

		criteria.restoreSettings(Settings.getInstance(), baseProperty);

		if (initialValue != null)
		{
			this.criteria.setText(initialValue);
		}
		criteria.selectAll();
		criteria.setColumns(40);

		criteria.addMouseListener(new TextComponentMouseListener());

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 5, 0, 10);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		add(this.label, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx++;
		c.weightx = 1.0;
		c.weighty = 0.0;
		add(this.criteria, c);

		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		add(this.ignoreCase, c);

		if (showHighlight)
		{
			c.gridy++;
			add(this.highlightAll, c);
		}

		c.gridy++;
		add(this.wholeWord, c);

		c.gridy++;
		c.weighty = 1.0;
		add(this.useRegEx, c);
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

	public boolean getHighlightAll()
	{
		if (this.highlightAll == null)
		{
			return false;
		}
		return this.highlightAll.isSelected();
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

		final ValidatingDialog dialog = ValidatingDialog.createDialog(w, this, title, caller, 0, false);

		// The criteria is a JComboBox that is editable. When entering new text
		// and pressing the ENTER key, the ComboBox will not trigger the OK
		// button of the dialog, but simply "apply" the new value to the
		// list. So the user would need to hit enter twice if this action listenter was not installed
		criteria.addActionListener(new ActionListener()
		{
			private String lastCmd;

			public void actionPerformed(ActionEvent e)
			{
				// If the last action as comboBoxChanged and we now receive a comboBoxEdited
				// the user selected a new item from the list by using the cursor keys.
				// In that case we do not want to close the dialog because the user should
				// first apply his/her selection with another enter key
				if (e.getActionCommand().equals("comboBoxEdited") && !"comboBoxChanged".equals(lastCmd))
				{
					dialog.approveAndClose();
				}
				lastCmd = e.getActionCommand();
			}
		});
		dialog.setVisible(true);

		boolean result = !dialog.isCancelled();

		criteria.addToHistory(criteria.getText());

		criteria.saveSettings(Settings.getInstance(), baseProperty);
		Settings.getInstance().setProperty(caseProperty, this.getIgnoreCase());
		Settings.getInstance().setProperty(wordProperty, this.getWholeWordOnly());
		Settings.getInstance().setProperty(regexProperty, this.getUseRegex());
		if (this.highlightProperty != null)
		{
			Settings.getInstance().setProperty(highlightProperty, getHighlightAll());
		}
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

	public void actionPerformed(ActionEvent e)
	{
	}
}
