/*
 * InsertTipProvider
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.editor;

import workbench.gui.completion.InsertColumnMatcher;
import workbench.gui.completion.ParameterTipProvider;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.sql.ScriptParser;

/**
 *
 * @author Thomas Kellerer
 */
public class InsertTipProvider
	implements ParameterTipProvider
{
	private SqlPanel sqlPanel;

	public InsertTipProvider(SqlPanel panel)
	{
		this.sqlPanel = panel;
	}

	@Override
	public String getCurrentTooltip()
	{
		ScriptParser parser = sqlPanel.createScriptParser();
		EditorPanel editor = sqlPanel.getEditor();
		parser.setScript(editor.getText());
		int index = parser.getCommandIndexAtCursorPos(editor.getCaretPosition());
		if (index < 0)
		{
			return null;
		}
		String currentStatement = parser.getCommand(index);

		if (currentStatement == null)
		{
			return null;
		}
		int statementStart = parser.getStartPosForCommand(index);
		int positionInStatement = editor.getCaretPosition() - statementStart;
		InsertColumnMatcher matcher = new InsertColumnMatcher(currentStatement);
		String tip = matcher.getTooltipForPosition(positionInStatement);
		if (tip == null)
		{
			if (matcher.inColumnList(positionInStatement))
			{
				tip = ResourceMgr.getString("ErrNoInsertVal");
			}
			if (matcher.inValueList(positionInStatement))
			{
				tip = ResourceMgr.getString("ErrNoInsertCol");
			}
		}
		return tip;
	}

}
