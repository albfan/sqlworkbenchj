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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.gui.sql.ResultCloser;
import workbench.resource.PlatformShortcuts;
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
	private JTabbedPane resultTab;
	private ResultCloser client;

	public CloseResultTabAction(JTabbedPane tabPane, ResultCloser closer)
	{
		super();
		this.resultTab = tabPane;
		client = closer;
		this.initMenuDefinition("MnuTxtCloseResultTab", KeyStroke.getKeyStroke(KeyEvent.VK_K, PlatformShortcuts.getDefaultModifier() | InputEvent.SHIFT_MASK ));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setIcon(null);
		this.resultTab.addChangeListener(this);
		checkEnabled();
	}

	public void executeAction(ActionEvent e)
	{
		client.closeCurrentResult();
	}

	private void checkEnabled()
	{
		int index = resultTab.getSelectedIndex();
		this.setEnabled(index < resultTab.getTabCount() -1);
	}

	public void stateChanged(ChangeEvent evt)
	{
		checkEnabled();
	}

}
