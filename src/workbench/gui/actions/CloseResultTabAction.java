/*
 * CloseResultTabAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JTabbedPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 * An action to close the currently selected result tab of a SqlPanel.
 * 
 * @author  support@sql-workbench.net
 */
public class CloseResultTabAction
	extends WbAction
	implements ChangeListener
{
	private SqlPanel client;
	
	public CloseResultTabAction(SqlPanel aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCloseResultTab", KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setIcon(null);
		this.client.addResultTabChangeListener(this);
		this.setEnabled(false);
	}
	
	public void executeAction(ActionEvent e)
	{
		client.closeCurrentResult();
	}
	
	public void stateChanged(ChangeEvent evt)
	{
		try
		{
			JTabbedPane tab = (JTabbedPane)evt.getSource();
			int index = tab.getSelectedIndex();
			boolean resultTab = (index == tab.getTabCount() -1);
			this.setEnabled(!resultTab);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
