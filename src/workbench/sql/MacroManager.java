/*
 * MacroManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;
import workbench.gui.macros.MacroEntry;

import workbench.gui.macros.MacroManagerDialog;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.MacroChangeListener;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 * A class to manage, store and apply SQL macros (alias) 
 * @author support@sql-workbench.net
 */
public class MacroManager
{
	private static MacroManager instance = new MacroManager();
	private HashMap<String, String> macros;
	private boolean modified = false;
	private List<MacroChangeListener> changeListeners = null;
	private boolean errorDuringLoad = false;
	private String selectedTextKey = Settings.getInstance().getProperty("workbench.macro.key.selectioin", "${selection}$");
	private String selectedStatementKey = Settings.getInstance().getProperty("workbench.macro.key.selectedstmt", "${selected_statement}$");
	private String currentStatementKey = Settings.getInstance().getProperty("workbench.macro.key.currentstatement", "${current_statement}$");
	private String editorTextKey = Settings.getInstance().getProperty("workbench.macro.key.editortext", "${text}$");
	
	public static MacroManager getInstance()
	{
		return instance;
	}

	public boolean hadLoadErrors()
	{
		return this.errorDuringLoad;
	}

	public synchronized String getMacroText(String aKey)
	{
		loadIfNecessary();
		if (aKey == null) return null;
		String sql = this.macros.get(aKey.toLowerCase());
		return sql;
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
	
	public synchronized void removeMacro(String aKey)
	{
		loadIfNecessary();
		if (aKey == null) return;
		this.macros.remove(aKey);
		this.modified = true;
		this.fireMacroListChange();
	}

	public synchronized List<String> getMacroList()
	{
		loadIfNecessary();
		Set<String> keys = this.macros.keySet();
		ArrayList<String> result = new ArrayList<String>(keys);
		return result;
	}

	public synchronized String[] getMacroNames()
	{
		loadIfNecessary();
		Set keys = this.macros.keySet();
		String[] result = new String[keys.size()];
		Iterator itr = keys.iterator();
		for (int i=0; itr.hasNext(); i++)
		{
			result[i] = (String)itr.next();
		}
		return result;
	}

	public synchronized void setMacro(String aKey, String aText)
	{
		loadIfNecessary();
		if (aKey == null || aKey.trim().length() == 0) return;
		this.macros.put(aKey.toLowerCase(), aText);
		this.modified = true;
		this.fireMacroListChange();
	}
	
	public synchronized void setMacros(Collection<MacroEntry> newMacros)
	{
		if (newMacros == null) return;
		this.macros = new HashMap<String, String>(); // clear out the old entries
		
		for (MacroEntry entry : newMacros)
		{
			this.macros.put(entry.getName().toLowerCase(), entry.getText());
		}
		this.modified = true;
		this.fireMacroListChange();
	}

	public boolean isModified() { return this.modified; }

	public synchronized void clearAll()
	{
		if (this.macros != null)
		{
			this.macros.clear();
		}
		else
		{
			this.macros = new HashMap<String, String>();
		}
		this.modified = true;
		this.fireMacroListChange();
	}

	public synchronized void addChangeListener(MacroChangeListener aListener)
	{
		if (this.changeListeners == null)
		{
			this.changeListeners = new ArrayList<MacroChangeListener>();
		}
		this.changeListeners.add(aListener);
	}

	public void selectAndRun(SqlPanel aPanel)
	{
		loadIfNecessary();
		Window w = SwingUtilities.getWindowAncestor(aPanel);
		Frame parent = null;
		if (w instanceof Frame)
		{
			parent = (Frame)w;
		}
		MacroManagerDialog d = new MacroManagerDialog(parent, aPanel);
		d.setVisible(true);
	}

	public synchronized void saveMacros()
	{
		if (this.macros != null && this.modified)
		{
			WbPersistence writer = new WbPersistence(this.getMacroFile().getAbsolutePath());
			try 
			{ 
				writer.writeObject(this.macros); 
			} 
			catch (Exception th) 
			{
				LogMgr.logError("MacroManager.saveMacros()", "Error saving macros", th);
			}
			this.modified = false;
			this.errorDuringLoad = false;
		}
	}
	
	private void loadIfNecessary()
	{
		if (this.macros == null) this.loadMacros();
	}

	private MacroManager()
	{
	}

	private File getMacroFile()
	{
		String configDir = Settings.getInstance().getConfigDir();
		File f = new File(configDir, "WbMacros.xml");
		return f;
	}

	private void loadMacros()
	{
		try
		{
			WbPersistence reader = new WbPersistence(this.getMacroFile().getAbsolutePath());
			Object o = reader.readObject();
			if (o instanceof HashMap)
			{
				this.macros = (HashMap)o;
			}
			else
			{
				this.macros = new HashMap<String, String>(20);
			}
		}
		catch (FileNotFoundException fne)
		{
			this.errorDuringLoad = false;
			this.macros = new HashMap<String, String>(20);
		}
		catch (Exception e)
		{
			LogMgr.logError("MacroManager.loadMacros()", "Error loading macro file", e);
			this.macros = new HashMap<String, String>(20);
			this.errorDuringLoad = true;
		}
		this.modified = false;
	}

	private void fireMacroListChange()
	{
		if (this.changeListeners == null) return;
		for (MacroChangeListener listener : this.changeListeners)
		{
			if (listener != null)
			{
				listener.macroListChanged();
			}
		}
	}

}
