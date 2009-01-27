/*
 * ExpandEditorAction.java
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

import javax.swing.KeyStroke;
import workbench.gui.sql.SplitPaneExpander;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

/**
 * Expand the editor in a SqlPanel to the full window size.
 *
 * @author  support@sql-workbench.net
 */
public class ExpandEditorAction extends WbAction
{
	private SplitPaneExpander client;

	public ExpandEditorAction(SplitPaneExpander aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtExpandEditor", KeyStroke.getKeyStroke(KeyEvent.VK_D, PlatformShortcuts.getDefaultModifier() | InputEvent.SHIFT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		this.client.toggleUpperComponentExpand();
	}
}
