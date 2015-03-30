/*
 * InsertTipProvider.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import workbench.resource.ResourceMgr;

import workbench.gui.completion.InsertColumnMatcher;
import workbench.gui.completion.ParameterTipProvider;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;

import workbench.sql.parser.ScriptParser;

/**
 * A class to provide tooltips for an INSERT statement.
 *
 * @author Thomas Kellerer
 */
public class InsertTipProvider
	implements ParameterTipProvider
{
	private SqlPanel sqlPanel;
	private int lastCommandStart = Integer.MAX_VALUE;
	private int lastCommandEnd = -1;
	private String lastCommand;
	private long lastParseTime = 0;

	public InsertTipProvider(SqlPanel panel)
	{
		this.sqlPanel = panel;
	}

	@Override
	public String getCurrentTooltip()
	{
		EditorPanel editor = sqlPanel.getEditor();
		int currentPosition = editor.getCaretPosition();

		// Cache the last used statement (and it's bounds), so that we do not need
		// to parse the whole editor script each time a tooltip is requested
		if (editor.isModifiedAfter(lastParseTime) || currentPosition < lastCommandStart || currentPosition > lastCommandEnd)
		{
			lastParseTime = System.currentTimeMillis();
			ScriptParser parser = ScriptParser.createScriptParser(sqlPanel.getConnection());
			parser.setScript(editor.getText());
			int index = parser.getCommandIndexAtCursorPos(editor.getCaretPosition());
			if (index < 0)
			{
				return null;
			}
			lastCommand = parser.getCommand(index);
			lastCommandStart = parser.getStartPosForCommand(index);
			lastCommandEnd = parser.getEndPosForCommand(index);
		}

		if (lastCommand == null)
		{
			lastParseTime = 0;
			lastCommandStart = Integer.MAX_VALUE;
			lastCommandEnd = 0;
			return null;
		}

		int positionInStatement = currentPosition - lastCommandStart;
		InsertColumnMatcher matcher = new InsertColumnMatcher(sqlPanel.getConnection(), lastCommand);
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
