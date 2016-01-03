/*
 * TextFormatter.java
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

import workbench.interfaces.SqlTextContainer;
import workbench.log.LogMgr;

import workbench.gui.WbSwingUtilities;

import workbench.sql.DelimiterDefinition;
import workbench.sql.formatter.SqlFormatter;
import workbench.sql.formatter.SqlFormatterFactory;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

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

	public void formatSql(final SqlTextContainer editor, DelimiterDefinition alternateDelimiter)
  {
    String sql = editor.getSelectedStatement();
    boolean isSelected = editor.isTextSelected();

    SqlFormatter f = SqlFormatterFactory.createFormatter(dbId);

    String text = null;

    if (f.supportsMultipleStatements())
    {
      text = f.getFormattedSql(sql);
    }
    else
    {
      text = doFormat(sql, f, alternateDelimiter, isSelected);
    }

    updateEditor(editor, text);
  }

  private void updateEditor(final SqlTextContainer editor, final String text)
  {
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
            // the editor will refuse to execute setSelectedText() if it's not editable
            editor.setEditable(true);
            editor.setSelectedText(text);
          }
          finally
          {
            editor.setEditable(editable);
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

  private String doFormat(String sql, SqlFormatter formatter, DelimiterDefinition alternateDelimiter, boolean isSelected)
	{
		ScriptParser parser = new ScriptParser(ParserType.getTypeFromDBID(dbId));
		parser.setAlternateDelimiter(alternateDelimiter);
		parser.setReturnStartingWhitespace(true);
		parser.setScript(sql);

		int count = parser.getSize();
		if (count < 1) return null;

		StringBuilder newSql = new StringBuilder(sql.length() + 100);
		boolean needDelimiter = false;
		boolean addNewLine = false;

		for (int i=0; i < count; i++)
		{
			String command = parser.getCommand(i);

			DelimiterDefinition delimiter = parser.getDelimiterUsed(i);
			if (delimiter == null)
			{
				delimiter = parser.getDelimiter();
			}

			// no need to format "empty" strings
			if (StringUtil.isBlank(command))
			{
				newSql.append(command);
				continue;
			}

			boolean isEmpty = isEmpty(command);

			needDelimiter = (count > 1) || (isSelected && delimiter.terminatesScript(sql, false));

			addNewLine = (i < count);

			try
			{
				String formattedSql = formatter.getFormattedSql(command);
				newSql.append(formattedSql.trim());
				if (needDelimiter && !isEmpty)
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

		if (newSql.length() == 0) return null;

		return newSql.toString();
	}

	private boolean isEmpty(String sql)
	{
		SQLLexer lexer = SQLLexerFactory.createLexerForDbId(dbId, sql);
		SQLToken token = lexer.getNextToken(false, false);
		return token == null;
	}

}
