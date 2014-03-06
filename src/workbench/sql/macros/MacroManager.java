/*
 * MacroManager.java
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
package workbench.sql.macros;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.KeyStroke;

import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.resource.StoreableKeyStroke;

import workbench.util.CaseInsensitiveComparator;

/**
 * A singleton class to load and save SQL macros (aliases)
 *
 * @author Thomas Kellerer
 */
public class MacroManager
{
	private final MacroStorage storage;

	/**
	 * Thread safe singleton instance.
	 */
	protected static class InstanceHolder
	{
		protected static MacroManager instance = new MacroManager();
	}

	private MacroManager()
	{
		long start = System.currentTimeMillis();
		storage = new MacroStorage();
		storage.loadMacros(getDefaultMacroFile(), false);
		long duration = System.currentTimeMillis() - start;
		LogMgr.logTrace("MacroManager.init<>", "Loading macros took " + duration + "ms");
	}

	private File getDefaultMacroFile()
	{
		File f = new File(Settings.getInstance().getMacroStorage());
		return f;
	}

	public void save()
	{
		storage.saveMacros();
	}

	public void save(File macroFile)
	{
		storage.saveMacros(macroFile);
	}

	public void loadMacros(File macroFile)
	{
		storage.loadMacros(macroFile, true);
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

	public Map<String, MacroDefinition> getExpandableMacros()
	{
		Map<String, MacroDefinition> result = new TreeMap<String, MacroDefinition>(CaseInsensitiveComparator.INSTANCE);
		List<MacroGroup> groups = this.storage.getGroups();
		for (MacroGroup group : groups)
		{
			for (MacroDefinition def : group.getMacros())
			{
				if (def.getExpandWhileTyping())
				{
					result.put(def.getName(), def);
				}
			}
		}
		return result;
	}

}
