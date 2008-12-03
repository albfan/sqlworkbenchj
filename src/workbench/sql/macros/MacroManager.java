/*
 * MacroManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.macros;

import java.awt.Frame;
import java.awt.Window;

import java.io.File;
import javax.swing.SwingUtilities;

import workbench.gui.macros.MacroManagerDialog;
import workbench.gui.sql.SqlPanel;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * A class to manage, store and apply SQL macros (aliases)
 *
 * @author support@sql-workbench.net
 */
public class MacroManager
{
	private MacroStorage storage;

	private String selectedTextKey = Settings.getInstance().getProperty("workbench.macro.key.selection", "${selection}$");
	private String selectedStatementKey = Settings.getInstance().getProperty("workbench.macro.key.selectedstmt", "${selected_statement}$");
	private String currentStatementKey = Settings.getInstance().getProperty("workbench.macro.key.currentstatement", "${current_statement}$");
	private String editorTextKey = Settings.getInstance().getProperty("workbench.macro.key.editortext", "${text}$");

	protected static class InstanceContainer
	{
		protected static MacroManager instance = new MacroManager();
	}

	private MacroManager()
	{
		storage = new MacroStorage();
		storage.loadMacros(getMacroFile());
	}

	public File getMacroFile()
	{
		File f = new File(Settings.getInstance().getConfigDir(), "WbMacros.xml");
		return f;
	}

	public static MacroManager getInstance()
	{
		return InstanceContainer.instance;
	}

	public synchronized String getMacroText(String key)
	{
		if (key == null) return null;
		MacroDefinition macro = storage.getMacro(key);
		if (macro == null) return null;
		return macro.getText();
	}

	public synchronized boolean hasTextKey(String sql)
	{
		if (sql == null) return false;
		return (sql.indexOf(editorTextKey) > - 1);
	}

	public synchronized boolean hasSelectedKey(String sql)
	{
		if (sql == null) return false;
		return (sql.indexOf(selectedTextKey) > - 1) || (sql.indexOf(selectedStatementKey) > -1);
	}

	public synchronized boolean hasCurrentKey(String sql)
	{
		if (sql == null) return false;
		return (sql.indexOf(currentStatementKey) > - 1);
	}

	public synchronized String replaceCurrent(String sql, String statementAtCursor)
	{
		if (statementAtCursor == null || sql == null) return sql;
		return StringUtil.replace(sql, currentStatementKey, statementAtCursor);
	}

	public synchronized String replaceEditorText(String sql, String text)
	{
		if (text == null || sql == null) return sql;
		return StringUtil.replace(sql, currentStatementKey, text);
	}

	public synchronized String replaceSelected(String sql, String selectedText)
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

	public void selectAndRun(SqlPanel aPanel)
	{
		Window w = SwingUtilities.getWindowAncestor(aPanel);
		Frame parent = null;
		if (w instanceof Frame)
		{
			parent = (Frame)w;
		}
		MacroManagerDialog d = new MacroManagerDialog(parent, aPanel);
		d.setVisible(true);
	}

	public MacroStorage getMacros()
	{
		return this.storage;
	}

	public void save()
	{
		storage.saveMacros(getMacroFile());
	}

}
