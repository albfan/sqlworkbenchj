/*
 * ViewMessageLogAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;

/**
 * Select the messages panel of the SqlPanel
 * @author Thomas Kellerer
 */
public class ViewMessageLogAction
	extends WbAction
{
	private SqlPanel panel;

	/**
	 * Creates a new instance of ViewMessageLogAction
	 */
	public ViewMessageLogAction(SqlPanel p)
	{
		super();
		this.panel = p;
		this.initMenuDefinition("MnuTxtSelectMsgLog");
		this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
	}

	public void executeAction(ActionEvent e)
	{
		panel.showLogPanel();
	}

}
