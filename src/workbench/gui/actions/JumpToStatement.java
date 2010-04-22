/*
 * JumpToStatement
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.sql.ScriptParser;

/**
 *
 * @author Thomas Kellerer
 */
public class JumpToStatement
	extends WbAction
{
	private SqlPanel client;

	public JumpToStatement(SqlPanel panel)
	{
		super();
		initMenuDefinition("MnuTxtJumpToStatementNr");
		setDefaultAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK));
		setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		initializeShortcut();
		client = panel;
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		ScriptParser p = client.createScriptParser();
		p.setScript(client.getText());
		//int count = p.getSize();
		String value = WbSwingUtilities.getUserInput(client, ResourceMgr.getString("TxtJumpTo"), "1", false, 10);
		if (value != null)
		{
			int stmt = Integer.valueOf(value);
			final int pos = p.getStartPosForCommand(stmt - 1);
			EventQueue.invokeLater( new Runnable()
			{
				@Override
				public void run()
				{
					client.getEditor().setCaretPosition(pos);
				}
			});
		}
	}

}
