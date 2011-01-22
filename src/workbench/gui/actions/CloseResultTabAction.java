/*
 * CloseResultTabAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import workbench.gui.sql.SqlPanel;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * An action to close the currently selected result tab of a SqlPanel.
 *
 * @author  Thomas Kellerer
 */
public class CloseResultTabAction
	extends WbAction
{
	private SqlPanel panel;

	public CloseResultTabAction(SqlPanel sqlPanel)
	{
		super();
		panel = sqlPanel;
		this.initMenuDefinition("MnuTxtCloseResultTab", KeyStroke.getKeyStroke(KeyEvent.VK_K, PlatformShortcuts.getDefaultModifier() | InputEvent.SHIFT_MASK ));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setIcon(null);
		this.setEnabled(panel.getCurrentResult() != null);
	}

	public void executeAction(ActionEvent e)
	{
		panel.closeCurrentResult();
	}

}
