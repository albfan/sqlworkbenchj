/*
 * OpenEditWindowAction.java
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
import javax.swing.KeyStroke;
import workbench.gui.components.WbTextCellEditor;
import workbench.gui.sql.SqlPanel;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.MacroManager;

/**
 *
 * @author support@sql-workbench.net
 */
public class OpenEditWindowAction
	extends WbAction
{
	private WbTextCellEditor editor;
	
	public OpenEditWindowAction(WbTextCellEditor ed)
	{
		super();
		this.editor = ed;
		this.setDefaultAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		this.setIcon(null);
	}
	
	public void executeAction(ActionEvent e)
	{
		editor.openEditWindow();
	}
	
}
