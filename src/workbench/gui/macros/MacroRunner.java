/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.gui.macros;

import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.sql.ScriptParser;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class MacroRunner
{
	public void runMacro(MacroDefinition macro, SqlPanel panel, boolean replaceText)
	{
		if (macro == null) return;

		MacroManager mgr = MacroManager.getInstance();
		String sql = macro.getText();
		EditorPanel editor = panel.getEditor();

		if (StringUtil.isBlank(sql)) return;

		if (mgr.hasSelectedKey(sql))
		{
			String selected = editor.getSelectedText();
			if (selected == null)
			{
				WbSwingUtilities.showErrorMessage(panel, ResourceMgr.getString("ErrNoSelection4Macro"));
				return;
			}
			sql = mgr.replaceSelected(sql, selected);
		}

		if (mgr.hasCurrentKey(sql))
		{
			String current = getStatementAtCursor(panel);
			if (current == null)
			{
				WbSwingUtilities.showErrorMessage(panel, ResourceMgr.getString("ErrNoCurrent4Macro"));
				return;
			}
			sql = mgr.replaceCurrent(sql, current);
		}

		if (mgr.hasTextKey(sql))
		{
			sql = mgr.replaceEditorText(sql, editor.getText());
		}
		panel.executeMacroSql(macro.getText(), replaceText);
	}

	protected String getStatementAtCursor(SqlPanel panel)
	{
		ScriptParser parser = panel.createScriptParser();
		parser.setScript(panel.getEditor().getText());
		int index = parser.getCommandIndexAtCursorPos(panel.getEditor().getCaretPosition());
		String currentStatement = parser.getCommand(index);
		return currentStatement;
	}
}
