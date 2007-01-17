/*
 * CleanJavaCodeAction.java
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.interfaces.TextContainer;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 * Action to convert Java code into a SQL statement
 * 
 * @see workbench.util.StringUtil#cleanJavaString(String)
 * @author support@sql-workbench.net
 */
public class CleanJavaCodeAction extends WbAction
{
	protected TextContainer client;

	public CleanJavaCodeAction(TextContainer aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCleanJavaCode", KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

	public void executeAction(ActionEvent e)
	{
		boolean selected = true;
		String code = this.client.getSelectedText();
		if (code == null || code.length() == 0)
		{
			code = this.client.getText();
			selected = false;
		}
		final String sql = StringUtil.cleanJavaString(code);
		if (sql != null && sql.length() > 0)
		{
			final boolean sel = selected;
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					if (sel)
						client.setSelectedText(sql);
					else
						client.setText(sql);
				}
			});
		}
	}
}
