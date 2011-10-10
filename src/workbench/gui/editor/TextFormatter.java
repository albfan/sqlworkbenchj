/*
 * TextFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import workbench.interfaces.SqlTextContainer;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptParser;
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

	public void formatSql(SqlTextContainer editor, DelimiterDefinition alternateDelimiter, String lineComment)
	{
		if (!editor.isEditable()) return;

		String sql = editor.getSelectedStatement();
		ScriptParser parser = new ScriptParser();
		parser.setAlternateDelimiter(alternateDelimiter);
		parser.setReturnStartingWhitespace(true);
		parser.setAlternateLineComment(lineComment);
		parser.setScript(sql);


		DelimiterDefinition delimiter = parser.getDelimiter();

		boolean isSelected = editor.isTextSelected();
		boolean selectionWithDelimiter = sql.trim().endsWith(delimiter.getDelimiter());

		int count = parser.getSize();
		if (count < 1) return;

		StringBuilder newSql = new StringBuilder(sql.length() + 100);
		boolean needDelimiter = (count > 1) || (count == 1 && isSelected && selectionWithDelimiter);

		for (int i=0; i < count; i++)
		{
			String command = parser.getCommand(i);

			// no need to format "empty" strings
			if (StringUtil.isBlank(command))
			{
				newSql.append(command);
				continue;
			}


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
				if (count > 1 && i < count - 2)
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

		if (editor.isTextSelected())
		{
			editor.setSelectedText(newSql.toString());
		}
		else
		{
			editor.setText(newSql.toString());
		}
	}
}
