/*
 * FindPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.FindDataAction;
import workbench.gui.actions.FindDataAgainAction;
import workbench.gui.actions.WbAction;
import workbench.interfaces.Searchable;

/**
 * A small panel with a find and find next button, and a criteria field 
*  which provides a quick search facitiliy for a WbTable component
 * @author  info@sql-workbench.net
 */
public class FindPanel 
	extends JPanel 
	implements Searchable
{
	private WbTable searchTable;
	private JTextField findField;
	public WbToolbar toolbar;
	private FindDataAction findAction;
	private FindDataAgainAction findAgainAction;

	public FindPanel(WbTable aTable)
	{
		GridBagConstraints gridBagConstraints;
		this.searchTable = aTable;
		this.setLayout(new GridBagLayout());
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);

		this.findField = new JTextField();
		this.findField.addMouseListener(new TextComponentMouseListener());

		this.toolbar = new WbToolbar();
		this.findAction = new FindDataAction(this);
		this.findAgainAction = new FindDataAgainAction(this);
		this.toolbar.add(this.findAction);
		this.toolbar.add(this.findAgainAction);
		this.toolbar.setMargin(new Insets(0,0,0,0));
		this.toolbar.setRollover(true);
		//Border b = new CompoundBorder(new EmptyBorder(1,0,1,0), new EtchedBorder());
		//toolbar.setBorder(b);
		toolbar.setBorderPainted(true);

		Dimension d = new Dimension(32768, 20);
		this.setMaximumSize(d);
		this.findField.setMaximumSize(d);
		this.findField.setPreferredSize(new Dimension(50,20));
		this.toolbar.setMaximumSize(d);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		this.add(toolbar, gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;

		this.add(findField, gridBagConstraints);

		InputMap im = new ComponentInputMap(this);
		ActionMap am = new ActionMap();
		this.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, im);
		this.setActionMap(am);

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), this.findAction.getActionName());
		im.put(this.findAction.getAccelerator(), this.findAction.getActionName());
		am.put(this.findAction.getActionName(), this.findAction);

		im.put(this.findAgainAction.getAccelerator(), this.findAgainAction.getActionName());
		am.put(this.findAgainAction.getActionName(), this.findAgainAction);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(findField);
		pol.addComponent(findField);
		pol.addComponent(findAction.getToolbarButton());
		pol.addComponent(findAgainAction.getToolbarButton());
		this.setFocusTraversalPolicy(pol);
	}

	public void setFocusToEntryField()
	{
		this.findField.grabFocus();
	}

	public int find()
	{
		if (this.searchTable.getRowCount() <= 0) return -1;
		Window parent = SwingUtilities.getWindowAncestor(this);
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		int row = this.searchTable.search(this.findField.getText().trim());
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		return row;
	}

	public int findNext()
	{
		if (this.searchTable.getRowCount() <= 0) return -1;
		if (!this.searchTable.canSearchAgain()) return -1;
		Window parent = SwingUtilities.getWindowAncestor(this);
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		int row = this.searchTable.searchNext();
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		return row;
	}

	public void setSearchString(String aText)
	{
		this.findField.setText(aText);
	}
	
	public String getSearchString()
	{
		return this.findField.getText();
	}

	public void addToToolbar(WbAction anAction)
	{
		this.addToToolbar(anAction, false, true);
	}
	
	public void addToToolbar(WbAction anAction, boolean atFront, boolean withSep)
	{
		JButton button = anAction.getToolbarButton();
		if (atFront)
		{
			this.toolbar.add(button, 0);
			if (withSep) this.toolbar.addSeparator(1);
		}
		else
		{
			this.toolbar.addSeparator();
			if (withSep) this.toolbar.add(button);
		}
	}
}
