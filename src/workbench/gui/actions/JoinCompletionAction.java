/*
 * JoinCompletionAction.java
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
import java.awt.event.KeyEvent;
import java.sql.SQLException;

import javax.swing.KeyStroke;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.StatusBar;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.ScriptParser;
import workbench.sql.fksupport.JoinCreator;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Action do an automatic completion of a join in a SQL statement.
 *
 * @author Thomas Kellerer
 */
public class JoinCompletionAction
	extends WbAction
{
	protected SqlPanel client;

	public JoinCompletionAction(SqlPanel panel)
	{
		super();
		this.client = panel;
		this.initMenuDefinition("MnuTxtAutoCompleteJoin", KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.ALT_MASK));
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		WbConnection conn = client.getConnection();
		if (conn == null) return;

		ScriptParser parser = client.createScriptParser();

		EditorPanel editor = client.getEditor();
		parser.setScript(editor.getText());
		int cursorPos = editor.getCaretPosition();

		int index = parser.getCommandIndexAtCursorPos(cursorPos);
		int commandCursorPos = parser.getIndexInCommand(index, cursorPos);
		String sql = parser.getCommand(index, false);

		if (sql == null)
		{
			LogMgr.logWarning("JoinCompletionAction.executeAction()", "No SQL found!");
			return;
		}

		String verb = SqlUtil.getSqlVerb(sql);
		if (!"SELECT".equalsIgnoreCase(verb))
		{
			String msg = "'" + verb + "' " + ResourceMgr.getString("MsgCompletionNotSupported");
			setStatusMessage(msg, 2500);
			return;
		}

		try
		{
			setStatusMessage(ResourceMgr.getString("MsgCompletionRetrievingObjects"), 0);
			JoinCreator creator = new JoinCreator(sql, commandCursorPos, conn);
			String condition = creator.getJoinCondition();
			if (StringUtil.isNonBlank(condition))
			{
				editor.insertText(condition + " ");
				setStatusMessage("", 0);
			}
			else
			{
				setStatusMessage(ResourceMgr.getString("MsgCompletionNothingFound"), 2500);
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logWarning("JoinCompletionAction.executeAction()", "Error retrieving condition", ex);
			setStatusMessage("", 0);
		}
	}

	private void setStatusMessage(final String msg, final int duration)
	{
		final StatusBar statusbar = client.getStatusBar();
		if (statusbar == null)
		{
			return;
		}

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (StringUtil.isEmptyString(msg))
				{
					statusbar.clearStatusMessage();
				}
				else if (duration > 0)
				{
					statusbar.setStatusMessage(msg, duration);
				}
				else
				{
					statusbar.setStatusMessage(msg);
				}
				statusbar.doRepaint();
			}
		});
	}
}
