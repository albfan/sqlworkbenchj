/*
 * MacroStorage.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.macros;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import workbench.interfaces.MacroChangeListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.FileUtil;
import workbench.util.WbPersistence;

/**
 * Manages laoding and saving of macros to an external (XML) file.
 *
 * It also converts the old (HashMap based) implementation of the Macro storage
 * into the new format.
 *
 * @author support@sql-workbench.net
 */
public class MacroStorage
{
	private final Map<String, MacroDefinition> allMacros;
	private final List<MacroGroup> groups;

	private boolean modified = false;
	private List<MacroChangeListener> changeListeners = null;

	public MacroStorage()
	{
		allMacros = new TreeMap<String, MacroDefinition>(new CaseInsensitiveComparator());
		groups = new ArrayList<MacroGroup>();
	}

	public synchronized MacroDefinition getMacro(String key)
	{
		return allMacros.get(key);
	}


	public void removeGroup(MacroGroup group)
	{
		groups.remove(group);
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

	public synchronized void addChangeListener(MacroChangeListener aListener)
	{
		if (this.changeListeners == null)
		{
			this.changeListeners = new ArrayList<MacroChangeListener>();
		}
		this.changeListeners.add(aListener);
	}

	public synchronized void copyFrom(MacroStorage source)
	{
		this.allMacros.clear();
		this.groups.clear();
		groups.addAll(source.groups);
		modified = true;
		updateMap();
		fireMacroListChange();
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

	public synchronized void saveMacros(File file)
	{
		if (this.getSize() == 0)
		{
			if (file.exists())
			{
				file.delete();
			}
		}
		else
		{
			WbPersistence writer = new WbPersistence(file.getAbsolutePath());
			try
			{
				writer.writeObject(this.groups);
			}
			catch (Exception th)
			{
				LogMgr.logError("MacroManager.saveMacros()", "Error saving macros", th);
			}
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

	public synchronized void applySort()
	{
		Collections.sort(groups, new Sorter());
		for (int i=0; i < groups.size(); i++)
		{
			groups.get(i).setSortOrder(i);
			groups.get(i).applySort();
		}
	}

	private synchronized void updateMap()
	{
		allMacros.clear();
		for (MacroGroup group : groups)
		{
			Collection<MacroDefinition> macros = group.getMacros();
			for (MacroDefinition macro : macros)
			{
				allMacros.put(macro.getName(), macro);
			}
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
	public synchronized void loadMacros(File source)
	{
		try
		{
			WbPersistence reader = new WbPersistence(source.getAbsolutePath());
			Object o = reader.readObject();
			if (o instanceof List)
			{
				List<MacroGroup> g = (List)o;
				groups.clear();
				groups.addAll(g);
			}
			else if (o instanceof HashMap)
			{
				File backup = new File(source.getParentFile(), source.getName() + ".old");
				FileUtil.copy(source, backup);
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
		catch (FileNotFoundException fne)
		{
		}
		catch (Exception e)
		{
			LogMgr.logError("MacroManager.loadMacros()", "Error loading macro file", e);
		}
		this.modified = false;
	}

	public synchronized void moveMacro(MacroDefinition macro, MacroGroup newGroup)
	{
		for (MacroGroup group : groups)
		{
			if (!group.equals(newGroup))
			{
				group.removeMacro(macro);
			}
		}
		newGroup.addMacro(macro);
		this.modified = true;
		this.fireMacroListChange();
	}

	public synchronized void addMacro(MacroGroup group, MacroDefinition macro)
	{
		allMacros.put(macro.getName(), macro);
		group.addMacro(macro);
		macro.setSortOrder(group.getSize() + 1);
		this.modified = true;
		this.fireMacroListChange();
	}

	public synchronized void removeMacro(MacroDefinition toDelete)
	{
		MacroDefinition macro = allMacros.remove(toDelete.getName());
		for (MacroGroup group : groups)
		{
			group.removeMacro(macro);
		}
		this.modified = true;
		this.fireMacroListChange();
	}

	public synchronized void addMacro(String groupName, String key, String text)
	{
		MacroDefinition def = new MacroDefinition(key, text);
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
		this.fireMacroListChange();
	}

	public synchronized boolean containsGroup(String groupName)
	{
		for (MacroGroup group : groups)
		{
			if (group.getName().equalsIgnoreCase(groupName)) return true;
		}
		return false;
	}

	public synchronized void addGroup(MacroGroup group)
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

	/**
	 * Returns only groups that have isVisibleInMenu() == true and
	 * contain only macros hat have isVisibleInMenu() == true
	 *
	 */
	public synchronized List<MacroGroup> getVisibleGroups()
	{
		List<MacroGroup> result = new ArrayList<MacroGroup>(groups.size());
		for (MacroGroup group : groups)
		{
			if (group.isVisibleInMenu() && group.getVisibleMacroSize() > 0)
			{
				result.add(group);
			}
		}
		return Collections.unmodifiableList(result);
	}

	public synchronized List<MacroGroup> getGroups()
	{
		return Collections.unmodifiableList(groups);
	}

	public void resetModified()
	{
		this.modified = false;
		for (MacroGroup group : groups)
		{
			group.resetModified();
		}
	}

	public boolean isModified()
	{
		if (this.modified) return true;
		for (MacroGroup group : groups)
		{
			if (group.isModified()) return true;
		}
		return false;
	}

	public synchronized void clearAll()
	{
		this.allMacros.clear();
		this.groups.clear();
		this.modified = true;
		this.fireMacroListChange();
	}

}
