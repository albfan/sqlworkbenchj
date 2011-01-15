/*
 * MacroManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.macros;

import java.io.File;
import java.util.List;
import javax.swing.KeyStroke;

import workbench.resource.Settings;
import workbench.resource.StoreableKeyStroke;

/**
 * A singleton class to load and save SQL macros (aliases)
 *
 * @author Thomas Kellerer
 */
public class MacroManager
{
	private MacroStorage storage;

	/**
	 * Thread safe singleton instance.
	 */
	protected static class InstanceHolder
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
		return InstanceHolder.instance;
	}

	public synchronized String getMacroText(String key)
	{
		if (key == null) return null;
		MacroDefinition macro = storage.getMacro(key);
		if (macro == null) return null;
		return macro.getText();
	}

	public MacroStorage getMacros()
	{
		return this.storage;
	}

	public MacroDefinition getMacroForKeyStroke(KeyStroke key)
	{
		if (key == null) return null;

		StoreableKeyStroke sk = new StoreableKeyStroke(key);
		List<MacroGroup> groups = this.storage.getGroups();
		for (MacroGroup group : groups)
		{
			for (MacroDefinition def : group.getMacros())
			{
				StoreableKeyStroke macroKey = def.getShortcut();
				if (macroKey != null && macroKey.equals(sk)) return def;
			}
		}
		return null;
	}
	
	public void save()
	{
		storage.saveMacros(getMacroFile());
	}

}
