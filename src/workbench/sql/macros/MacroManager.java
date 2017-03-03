/*
 * MacroManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.sql.macros;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.KeyStroke;

import workbench.interfaces.MacroChangeListener;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.resource.StoreableKeyStroke;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.WbFile;

/**
 * A singleton class to load and saveAs SQL macros (aliases)
 *
 * @author Thomas Kellerer
 */
public class MacroManager
{
	public static final int DEFAULT_STORAGE = Integer.MIN_VALUE;

	private final Map<String, MacroStorage> allMacros = new HashMap<>();
	private final Map<Integer, String> macroClients = new HashMap<>();

	/**
	 * Thread safe singleton instance.
	 */
	private static class InstanceHolder
	{
		protected static MacroManager instance = new MacroManager();
	}

	private MacroManager()
	{
    WbFile file = getDefaultMacroFile();
		long start = System.currentTimeMillis();
		MacroStorage storage = new MacroStorage(file);
		long duration = System.currentTimeMillis() - start;
		allMacros.put(file.getFullPath(), storage);
		LogMgr.logDebug("MacroManager.init<>", "Loading default macros took " + duration + "ms");
    dumpMacroInfo();
	}

	public static MacroManager getInstance()
	{
		return InstanceHolder.instance;
	}

	public static WbFile getDefaultMacroFile()
	{
		return new WbFile(Settings.getInstance().getMacroStorage());
	}

	public synchronized void save()
	{
		for (MacroStorage storage : allMacros.values())
		{
			storage.saveMacros();
		}
	}

	public void saveAs(int clientId, WbFile macroFile)
	{
		MacroStorage storage = allMacros.get(getClientfilename(clientId));
		if (storage != null)
		{
			storage.saveMacros(macroFile);
			macroClients.put(clientId, macroFile.getFullPath());
		}
	}

	/**
	 * Save the macro definitions currently used by the given client.
	 *
	 * @param clientId the client ID
	 */
	public void save(int clientId)
	{
		String fname = getClientfilename(clientId);
		MacroStorage storage = allMacros.get(fname);
		if (storage != null)
		{
			storage.saveMacros(new WbFile(fname));
		}
	}

	private String getClientfilename(int clientId)
	{
		String clientFilename = macroClients.get(clientId);
		if (clientFilename == null)
		{
			return getDefaultMacroFile().getFullPath();
		}
		return clientFilename;
	}

	public void loadDefaultMacros(int clientId)
	{
		loadMacros(clientId, getDefaultMacroFile());
	}

	public synchronized void loadMacros(int clientId, WbFile macroFile)
	{
		String newFilename = macroFile.getFullPath();
    String oldFilename = macroClients.get(clientId);

    MacroStorage storage = null;
    if (oldFilename != null)
    {
      storage = allMacros.get(oldFilename);
    }

		if (storage == null)
		{
			storage = new MacroStorage(macroFile);
		}
    else
    {
      if (isNonDefaultMacroFile(oldFilename))
      {
        allMacros.remove(oldFilename);
      }
      storage.loadNewFile(macroFile);
    }

    LogMgr.logDebug("MacroManager.loadMacros()", "Loaded " + storage.getSize() + " macros from file " + macroFile.getFullPath() + " for clientId:  " + clientId);
    allMacros.put(newFilename, storage);
		macroClients.put(clientId, newFilename);
    dumpMacroInfo();
	}

  private boolean isNonDefaultMacroFile(String filename)
  {
    WbFile f = new WbFile(filename);
    return f.equals(getDefaultMacroFile()) == false;
  }
  
	private MacroStorage getStorage(int macroClientId)
	{
		String fname = getClientfilename(macroClientId);
		MacroStorage storage = allMacros.get(fname);
		if (storage == null)
		{
			storage = allMacros.get(getDefaultMacroFile().getFullPath());
			LogMgr.logTrace("MacroManager.getStorage()", "No macros registered for clientId=" + macroClientId + ". Using default macros!", new Exception("Client not initialized"));
      LogMgr.logTrace("MacroManager.getStorage()", "Current macro clients: " + macroClients + ", current storages: " + allMacros);
		}
		return storage;
	}

	public synchronized String getMacroText(int macroClientId, String key)
	{
		if (key == null) return null;

    MacroStorage storage = getStorage(macroClientId);
    if (storage == null) return null;

		MacroDefinition macro = storage.getMacro(key);
		if (macro == null) return null;
		return macro.getText();
	}

  public void addChangeListener(MacroChangeListener listener, int clientId)
  {
    MacroStorage storage = getMacros(clientId);
    if (storage != null)
    {
      storage.addChangeListener(listener);
    }
  }

  public void removeChangeListener(MacroChangeListener listener, int clientId)
  {
    MacroStorage storage = getMacros(clientId);
    if (storage != null)
    {
      storage.removeChangeListener(listener);
    }
  }

	public synchronized MacroStorage getMacros(int clientId)
	{
		return getStorage(clientId);
	}

	/**
	 * Checks if the given KeyStroke is assigned to any of the currently loaded macro files.
	 * @param key the key to test
	 *
	 * @return true if the keystroke is currently used
	 */
	public synchronized MacroDefinition getMacroForKeyStroke(KeyStroke key)
	{
		if (key == null) return null;
		for (MacroStorage storage : allMacros.values())
		{
			StoreableKeyStroke sk = new StoreableKeyStroke(key);
			List<MacroGroup> groups = storage.getGroups();
			for (MacroGroup group : groups)
			{
				for (MacroDefinition def : group.getMacros())
				{
					StoreableKeyStroke macroKey = def.getShortcut();
					if (macroKey != null && macroKey.equals(sk))
					{
						return def;
					}
				}
			}
		}
		return null;
	}

	public synchronized Map<String, MacroDefinition> getExpandableMacros(int clientId)
	{
		MacroStorage storage = getStorage(clientId);
		Map<String, MacroDefinition> result = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
		List<MacroGroup> groups = storage.getGroups();
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

  private void dumpMacroInfo()
  {
    if (LogMgr.isTraceEnabled())
    {
      LogMgr.logTrace("MacroManager.dumpMacroInfo()", "Current macro clients: " + macroClients);
      LogMgr.logTrace("MacroManager.dumpMacroInfo()", "Current storages: " + allMacros);
    }
  }
}
