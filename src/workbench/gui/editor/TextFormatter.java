/*
 * TextFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import workbench.interfaces.SqlTextContainer;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

import workbench.sql.DelimiterDefinition;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;
import workbench.sql.formatter.SqlFormatter;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TextFormatter
{

	private String dbId;

	public TextFormatter(String id)
	{
		this.dbId = id;
	}

	public void formatSql(final SqlTextContainer editor, DelimiterDefinition alternateDelimiter, String lineComment)
	{
		String sql = editor.getSelectedStatement();
		ScriptParser parser = new ScriptParser(ParserType.getTypeFromDBID(dbId));
		parser.setAlternateDelimiter(alternateDelimiter);
		parser.setReturnStartingWhitespace(true);
		parser.setScript(sql);

		boolean isSelected = editor.isTextSelected();

		int count = parser.getSize();
		if (count < 1) return;

		StringBuilder newSql = new StringBuilder(sql.length() + 100);
		boolean needDelimiter = false;
		boolean addNewLine = false;
		for (int i=0; i < count; i++)
		{
			String command = parser.getCommand(i);

			DelimiterDefinition delimiter = parser.getDelimiterUsed(i);

			// no need to format "empty" strings
			if (StringUtil.isBlank(command))
			{
				newSql.append(command);
				continue;
			}

			needDelimiter = (count > 1) || (isSelected && delimiter.terminatesScript(sql, false));

			addNewLine = (i < count);

			SqlFormatter f = new SqlFormatter(command, Settings.getInstance().getFormatterMaxSubselectLength(), dbId);

			try
			{
				String formattedSql = f.getFormattedSql();
				newSql.append(formattedSql.trim());
				if (needDelimiter)
				{
					if (delimiter.isSingleLine())
					{
						newSql.append('\n');
					}
					newSql.append(delimiter.getDelimiter());
				}
				newSql.append('\n');

				// add a blank line between the statements, but not for the last one
				if (addNewLine)
				{
					newSql.append('\n');
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("EditorPanel.reformatSql()", "Error when formatting SQL", e);
			}
		}

		if (newSql.length() == 0) return;

		final String text = newSql.toString();

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (editor.isTextSelected())
				{
					boolean editable = editor.isEditable();
					try
					{
						if (!editable)
						{
							// the editor will refuse to execute setSelectedText() if it's not editable
							editor.setEditable(true);
						}
						editor.setSelectedText(text);
					}
					finally
					{
						if (!editable)
						{
							editor.setEditable(false);
						}
					}
				}
				else
				{
					// setText() is always allowed, even if the editor is not editable
					editor.setText(text);
				}
			}
		});
	}
}
