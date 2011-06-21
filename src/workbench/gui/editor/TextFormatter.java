/*
 * TextFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import workbench.gui.sql.EditorPanel;
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

	public void formatSql(EditorPanel editor, DelimiterDefinition alternateDelimiter, String lineComment)
	{
		if (!editor.isEditable()) return;

		String sql = editor.getSelectedStatement();
		ScriptParser parser = new ScriptParser();
		parser.setAlternateDelimiter(alternateDelimiter);
		parser.setReturnStartingWhitespace(true);
		parser.setAlternateLineComment(lineComment);
		parser.setScript(sql);

		String delimit = parser.getDelimiterString();
		int pos = StringUtil.findFirstNonWhitespace(editor.getText(), editor.getSelectionEnd());
		int delimitPos = editor.getText().indexOf(delimit, editor.getSelectionEnd());
		boolean isSelected = editor.isTextSelected();
		boolean delimiterAfterSelection = (pos == delimitPos);

		int count = parser.getSize();
		if (count < 1) return;

		StringBuilder newSql = new StringBuilder(sql.length() + 100);

		for (int i=0; i < count; i++)
		{
			String command = parser.getCommand(i);

			// no need to format "empty" strings
			if (StringUtil.isBlank(command))
			{
				newSql.append(command);
				continue;
			}

			// only add the delimiter to the last statement if there is no
			// delimiter following directly after the currently selected text
			if (!isSelected && ((i == count - 1 && !delimiterAfterSelection) || i < count - 1))
			{
				command += delimit;
			}

			SqlFormatter f = new SqlFormatter(command, Settings.getInstance().getFormatterMaxSubselectLength(), dbId);

			try
			{
				String formattedSql = f.getFormattedSql();
				newSql.append(formattedSql);
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
