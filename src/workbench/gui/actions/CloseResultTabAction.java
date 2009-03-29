/*
 * CloseResultTabAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import javax.swing.KeyStroke;
import workbench.gui.sql.ResultHandler;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * An action to close the currently selected result tab of a SqlPanel.
 *
 * @author  support@sql-workbench.net
 */
public class CloseResultTabAction
	extends WbAction
{
	private JTabbedPane resultTab;
	private ResultHandler client;

	public CloseResultTabAction(JTabbedPane tabPane, ResultHandler closer)
	{
		super();
		this.resultTab = tabPane;
		client = closer;
		this.initMenuDefinition("MnuTxtCloseResultTab", KeyStroke.getKeyStroke(KeyEvent.VK_K, PlatformShortcuts.getDefaultModifier() | InputEvent.SHIFT_MASK ));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		client.closeCurrentResult();
	}

	@Override
	public boolean isEnabled()
	{
		if (resultTab == null) return false;
		int index = resultTab.getSelectedIndex();
		return (index < resultTab.getTabCount() -1);
	}

}
