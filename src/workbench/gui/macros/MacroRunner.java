/*
 * MacroRunner.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.macros.MacroDefinition;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroRunner
{
	private String selectedTextKey = Settings.getInstance().getProperty("workbench.macro.key.selection", "${selection}$");
	private String selectedStatementKey = Settings.getInstance().getProperty("workbench.macro.key.selectedstmt", "${selected_statement}$");
	private String currentStatementKey = Settings.getInstance().getProperty("workbench.macro.key.currentstatement", "${current_statement}$");
	private String editorTextKey = Settings.getInstance().getProperty("workbench.macro.key.editortext", "${text}$");

	public void runMacro(MacroDefinition macro, MacroClient client, boolean replaceText)
	{
		if (macro == null) return;
		if (client == null) return;

		String sql = macro.getText();

		if (StringUtil.isBlank(sql)) return;

		if (hasSelectedKey(sql))
		{
			String selected = client.getSelectedText();
			if (selected == null)
			{
				WbSwingUtilities.showErrorMessage(client.getPanel(), ResourceMgr.getString("ErrNoSelection4Macro"));
				return;
			}
			sql = replaceSelected(sql, selected);
		}

		if (hasCurrentKey(sql))
		{
			String current = client.getStatementAtCursor();
			if (current == null)
			{
				WbSwingUtilities.showErrorMessage(client.getPanel(), ResourceMgr.getString("ErrNoCurrent4Macro"));
				return;
			}
			sql = replaceCurrent(sql, current);
		}

		if (hasTextKey(sql))
		{
			sql = replaceEditorText(sql, client.getText());
		}
		client.executeMacroSql(sql, replaceText);
	}

	protected boolean hasTextKey(String sql)
	{
		if (sql == null) return false;
		return (sql.indexOf(editorTextKey) > - 1);
	}

	protected boolean hasSelectedKey(String sql)
	{
		if (sql == null) return false;
		return (sql.indexOf(selectedTextKey) > - 1) || (sql.indexOf(selectedStatementKey) > -1);
	}

	protected boolean hasCurrentKey(String sql)
	{
		if (sql == null) return false;
		return (sql.indexOf(currentStatementKey) > - 1);
	}

	protected String replaceCurrent(String sql, String statementAtCursor)
	{
		if (statementAtCursor == null || sql == null) return sql;
		return StringUtil.replace(sql, currentStatementKey, statementAtCursor);
	}

	protected String replaceEditorText(String sql, String text)
	{
		if (text == null || sql == null) return sql;
		return StringUtil.replace(sql, editorTextKey, text);
	}

	protected String replaceSelected(String sql, String selectedText)
	{
		if (selectedText == null || sql == null) return sql;

		if (sql.indexOf(selectedTextKey) > -1)
		{
			return StringUtil.replace(sql, selectedTextKey, selectedText);
		}
		else if (sql.indexOf(selectedStatementKey) > -1)
		{
			String stmt = selectedText.trim();
			if (stmt.endsWith(";"))
			{
				stmt = stmt.substring(0, stmt.length() - 1);
			}
			return StringUtil.replace(sql, selectedStatementKey, stmt);
		}
		return sql;
	}

}
