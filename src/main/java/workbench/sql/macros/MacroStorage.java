/*
 * MacroStorage.java
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
package workbench.sql.macros;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.interfaces.MacroChangeListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.ShortcutManager;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.FileUtil;
import workbench.util.FileVersioner;
import workbench.util.WbFile;
import workbench.util.WbPersistence;

/**
 * Manages laoding and saving of macros to an external (XML) file.
 * <br/>
 * It also converts the old (HashMap based) implementation of the Macro storage
 * into the new format.
 *
 * @author Thomas Kellerer
 */
public class MacroStorage
{
	private final Object lock = new Object();
	private final Map<String, MacroDefinition> allMacros = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
	private final List<MacroGroup> groups = new ArrayList<>();

	private boolean modified = false;
	private List<MacroChangeListener> changeListeners = new ArrayList<>(1);
	private WbFile sourceFile;

	public MacroStorage(WbFile toLoad)
	{
    sourceFile = toLoad;
    loadMacros();
	}

  MacroStorage()
  {
  }

	public synchronized MacroDefinition getMacro(String key)
	{
		return allMacros.get(key);
	}

	public File getCurrentFile()
	{
		return sourceFile;
	}

	public String getCurrentMacroFilename()
	{
		if (sourceFile == null) return null;
    return sourceFile.getFullPath();
	}

	public void removeGroup(MacroGroup group)
	{
		groups.remove(group);
		modified = true;
	}

	public synchronized int getSize()
	{
		int size = 0;
		for (MacroGroup group : groups)
		{
			size += group.getSize();
		}
		return size;
	}

	public void addChangeListener(MacroChangeListener aListener)
	{
		this.changeListeners.add(aListener);
	}

	public void removeChangeListener(MacroChangeListener aListener)
	{
		this.changeListeners.remove(aListener);
	}

	public synchronized void copyFrom(MacroStorage source)
	{
		synchronized (lock)
		{
			this.allMacros.clear();
			this.groups.clear();
			groups.addAll(source.groups);
			modified = true;
			updateMap();
		}
		fireMacroListChanged();
	}

	public MacroStorage createCopy()
	{
		MacroStorage copy = new MacroStorage();
		for (MacroGroup group : groups)
		{
			copy.groups.add(group.createCopy());
		}
		copy.updateMap();
		copy.resetModified();
		return copy;
	}

	private void createBackup(WbFile f)
	{
    if (!Settings.getInstance().getCreateMacroBackup()) return;

		int maxVersions = Settings.getInstance().getMaxBackupFiles();
		String dir = Settings.getInstance().getBackupDir();
		String sep = Settings.getInstance().getFileVersionDelimiter();
		FileVersioner version = new FileVersioner(maxVersions, dir, sep);
		try
		{
			version.createBackup(f);
		}
		catch (IOException e)
		{
			LogMgr.logWarning("MacroStorage.createBackup()", "Error when creating backup for: " + f.getAbsolutePath(), e);
		}
	}

  /**
   * Saves the macros to a new file.
   *
   * The contents of the file they were loaded from, will be unaffected
   * (effectively creating a copy of the current file).
   *
   * After saving the macros to a new file, getCurrentFile() will return the new file name.
   *
   * @param file
   * @see #saveMacros()
   */
	public void saveMacros(WbFile file)
  {
    sourceFile = file;
    saveMacros();
  }

  /**
   * Saves the macros to the file they were loaded from.
   *
   * This will also reset the modified flag.
   *
   * @see #isModified()
   */
	public void saveMacros()
	{
		if (sourceFile == null) return;

		synchronized (lock)
		{
			if (this.getSize() == 0)
			{
				if (sourceFile.exists() && isModified())
				{
          createBackup(sourceFile);
					sourceFile.delete();
					LogMgr.logDebug("MacroStorage.saveMacros()", "All macros from " + sourceFile.getFullPath()+ " were removed. Macro file deleted.");
				}
        else
        {
          LogMgr.logDebug("MacroStorage.saveMacros()", "No macros defined, nothing to save");
        }
			}
			else
			{
        createBackup(sourceFile);

				WbPersistence writer = new WbPersistence(sourceFile.getAbsolutePath());
				try
				{
					writer.writeObject(this.groups);
					LogMgr.logDebug("MacroStorage.saveMacros()", "Saved " + allMacros.size() + " macros to " + sourceFile.getFullPath());
				}
				catch (Exception th)
				{
					LogMgr.logError("MacroManager.saveMacros()", "Error saving macros to " + sourceFile.getFullPath(), th);
				}
			}
			resetModified();
		}
	}

	private void fireMacroListChanged()
	{
		for (MacroChangeListener listener : this.changeListeners)
		{
			if (listener != null)
			{
				listener.macroListChanged();
			}
		}
	}

  public void sortGroupsByName()
  {
		synchronized (lock)
		{
      Comparator<MacroGroup> comp = new Comparator<MacroGroup>()
      {

        @Override
        public int compare(MacroGroup o1, MacroGroup o2)
        {
          if (o1 == null && o2 == null) return 0;
          if (o1 == null) return -1;
          if (o2 == null) return 1;
          return o1.getName().compareToIgnoreCase(o2.getName());
        }
      };
			Collections.sort(groups, comp);
			modified = true;
		}

  }
	public void applySort()
	{
		synchronized (lock)
		{
			Collections.sort(groups, new Sorter());
			for (int i=0; i < groups.size(); i++)
			{
				groups.get(i).setSortOrder(i);
				groups.get(i).applySort();
			}
			modified = true;
		}
	}

	private void updateMap()
	{
		boolean shortcutChanged = false;
		allMacros.clear();
		for (MacroGroup group : groups)
		{
			Collection<MacroDefinition> macros = group.getMacros();
			for (MacroDefinition macro : macros)
			{
				allMacros.put(macro.getName(), macro);
				shortcutChanged = shortcutChanged || macro.isShortcutChanged();
			}
		}
		if (shortcutChanged)
		{
			ShortcutManager.getInstance().fireShortcutsChanged();
		}
	}

	/**
	 * Loads the macros from an external XML file.
	 *
	 * The XML file is a file generated by using an XMLEncoder to serialize
	 * the list of MacroGroups.
	 *
	 * This method also migrates the old storage format into the new one.
	 * If an old format is loaded the given file is copied to a file with extension
	 * <tt>.old</tt> appended.
	 *
	 * @param source
	 * @see workbench.util.WbPersistence#readObject()
	 */
	@SuppressWarnings("unchecked")
	private void loadMacros()
	{
		if (sourceFile == null)
		{
			LogMgr.logDebug("MacroManager.loadMacros()", "No macro file specified. No Macros loaded");
			return;
		}

		if (!sourceFile .exists())
		{
			LogMgr.logDebug("MacroManager.loadMacros()", "Macro file " + sourceFile + " not found. No Macros loaded");
			return;
		}

		try
		{
			synchronized (lock)
			{
				WbPersistence reader = new WbPersistence(sourceFile.getAbsolutePath());
				Object o = reader.readObject();
				if (o instanceof List)
				{
					List<MacroGroup> g = (List)o;
					groups.clear();
					groups.addAll(g);
				}
				else if (o instanceof HashMap)
				{
					// Upgrade from previous version
					File backup = new File(sourceFile.getParentFile(), sourceFile.getName() + ".old");
					FileUtil.copy(sourceFile, backup);
					Map<String, String> oldMacros = (Map)o;
					MacroGroup group = new MacroGroup(ResourceMgr.getString("LblDefGroup"));

					groups.clear();

					int sortOrder = 0;
					for (Map.Entry<String, String> entry : oldMacros.entrySet())
					{
						MacroDefinition def = new MacroDefinition(entry.getKey(), entry.getValue());
						def.setSortOrder(sortOrder);
						sortOrder++;
						group.addMacro(def);
					}
					groups.add(group);
				}
				applySort();
				updateMap();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MacroManager.loadMacros()", "Error loading macro file", e);
		}
		resetModified();
	}

	public synchronized void moveMacro(MacroDefinition macro, MacroGroup newGroup)
	{
		MacroGroup currentGroup = findMacroGroup(macro.getName());

		if (currentGroup != null && currentGroup.equals(newGroup)) return;
		if (currentGroup != null)
		{
			currentGroup.removeMacro(macro);
		}
		newGroup.addMacro(macro);

		this.modified = true;
		this.fireMacroListChanged();
	}

	public MacroGroup findMacroGroup(String macroName)
	{
		synchronized (lock)
		{
			for (MacroGroup group : groups)
			{
				List<MacroDefinition> macros = group.getMacros();
				for (MacroDefinition macro : macros)
				{
					if (macro.getName().equalsIgnoreCase(macroName))
					{
						return group;
					}
				}
			}
		}
		return null;
	}

	public void addMacro(MacroGroup group, MacroDefinition macro)
	{
		synchronized (lock)
		{
			allMacros.put(macro.getName(), macro);
			if (!containsGroup(group.getName()))
			{
				addGroup(group);
			}
			moveMacro(macro, group);
			macro.setSortOrder(group.getSize() + 1);
			this.modified = true;
		}
		this.fireMacroListChanged();
	}

	public void removeMacro(MacroDefinition toDelete)
	{
		synchronized (lock)
		{
			MacroDefinition macro = allMacros.remove(toDelete.getName());
			for (MacroGroup group : groups)
			{
				group.removeMacro(macro);
			}
			this.modified = true;
		}
		this.fireMacroListChanged();
	}

	public void addMacro(String groupName, String key, String text)
	{
		MacroDefinition def = new MacroDefinition(key, text);
		synchronized (lock)
		{
			boolean added = false;
			if (groupName != null)
			{
				for (MacroGroup group : groups)
				{
					if (group.getName().equalsIgnoreCase(groupName))
					{
						group.addMacro(def);
						added = true;
					}
				}
				if (!added)
				{
					MacroGroup group = new MacroGroup(groupName);
					group.addMacro(def);
					groups.add(group);
				}
			}
			else
			{
				groups.get(0).addMacro(def);
			}
			updateMap();
			this.modified = true;
		}
		this.fireMacroListChanged();
	}

	public boolean containsGroup(String groupName)
	{
		synchronized (lock)
		{
			for (MacroGroup group : groups)
			{
				if (group.getName().equalsIgnoreCase(groupName)) return true;
			}
			return false;
		}
	}

	public void addGroup(MacroGroup group)
	{
		synchronized (lock)
		{
			if (!groups.contains(group))
			{
				int newIndex = 1;
				if (groups.size() > 0)
				{
					newIndex = groups.get(groups.size() - 1).getSortOrder() + 1;
				}
				group.setSortOrder(newIndex);
				groups.add(group);
				applySort();
				modified = true;
			}
		}
	}

	/**
	 * Returns only groups that have isVisibleInMenu() == true and
	 * contain only macros hat have isVisibleInMenu() == true
	 *
	 */
	public List<MacroGroup> getVisibleGroups()
	{
		List<MacroGroup> result = new ArrayList<>(groups.size());
		synchronized (lock)
		{
			for (MacroGroup group : groups)
			{
				if (group.isVisibleInMenu() && group.getVisibleMacroSize() > 0)
				{
					result.add(group);
				}
			}
		}
		return Collections.unmodifiableList(result);
	}

	public List<MacroGroup> getGroups()
	{
		synchronized (lock)
		{
			return Collections.unmodifiableList(groups);
		}
	}

	public void resetModified()
	{
		synchronized (lock)
		{
			this.modified = false;
			for (MacroGroup group : groups)
			{
				group.resetModified();
			}
		}
	}

	public boolean isModified()
	{
		synchronized (lock)
		{
			if (this.modified) return true;
			for (MacroGroup group : groups)
			{
				if (group.isModified()) return true;
			}
		}
		return false;
	}

	public void clearAll()
	{
		synchronized (lock)
		{
			this.allMacros.clear();
			this.groups.clear();
			this.modified = true;
		}
		this.fireMacroListChanged();
	}

  @Override
  public String toString()
  {
    return allMacros.size() + " macros";
  }

}
