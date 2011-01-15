/*
 * AppendResultsAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 *	Action to toggle the if running statements should replace the current
 *  results or simply add a new result tab to SqlPanel
 * 
 *	@author  Thomas Kellerer
 */
public class AppendResultsAction 
	extends CheckBoxAction
{
	private SqlPanel client;
	private JToggleButton toggleButton;
	
	public AppendResultsAction(SqlPanel panel)
	{
		super("MnuTxtToggleAppendResults", null);
		this.client = panel;
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setSwitchedOn(client.getAppendResults());
		this.setEnabled(false);
	}

	public void setSwitchedOn(boolean aFlag)
	{
		super.setSwitchedOn(aFlag);
		if (this.toggleButton != null)
		{
			this.toggleButton.setSelected(this.isSwitchedOn());
		}
		client.setAppendResults(this.isSwitchedOn());
	}

	public JToggleButton getButton()
	{
		if (this.toggleButton == null)
		{
			this.toggleButton = new JToggleButton(this);
			this.toggleButton.setText(null);
			this.toggleButton.setMargin(WbToolbarButton.MARGIN);
			this.toggleButton.setIcon(ResourceMgr.getImage("AppendResult"));
			this.toggleButton.setSelected(this.isSwitchedOn());
		}
		return this.toggleButton;
	}
	
	public void addToToolbar(JToolBar aToolbar)
	{
		aToolbar.add(this.getButton());
	}
	
}
