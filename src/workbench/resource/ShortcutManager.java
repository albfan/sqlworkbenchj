/*
 * ShortcutManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Action;
import javax.swing.KeyStroke;

import workbench.gui.actions.WbAction;
import workbench.util.WbPersistence;

/**
 * @author info@sql-workbench.net
 *
 */
public class ShortcutManager
{

	private String filename;

	// key to the map is the action's class name,
	// the actual object in it will be a ShortcutDefinition
	private HashMap keyMap;

	// we need the list of registered actions, in order to be able to
	// display the label for the action for the customization dialog
	private HashMap actionNames;

	private ArrayList allActions;

	ShortcutManager(String aFilename)
	{
		this.filename = aFilename;
		try
		{
			this.keyMap = (HashMap)WbPersistence.readObject(this.filename);
		}
		catch (Exception e)
		{
			this.keyMap = null;
		}

		if (this.keyMap == null)
		{
			this.keyMap = new HashMap(30);
			this.actionNames = new HashMap(30);
		}
		else
		{
			this.actionNames = new HashMap(this.keyMap.size());
		}
		this.allActions = new ArrayList(150);
	}

	public void removeShortcut(String clazz)
	{
		this.assignKey(clazz, null);
	}

	public void registerAction(WbAction anAction)
	{
		String clazz = anAction.getClass().getName();
		ShortcutDefinition def = this.getDefinition(clazz);
		if (def == null)
		{
			def = new ShortcutDefinition(clazz);
			this.keyMap.put(clazz, def);
		}

		if (!actionNames.containsKey(clazz))
		{
			String label = anAction.getMenuLabel();

			if (label == null) label = anAction.getActionName();
			if (label != null)
			{
				this.actionNames.put(clazz, label);
			}
		}

		if (!def.hasDefault())
		{
			KeyStroke defaultkey = anAction.getDefaultAccelerator();
			def.assignDefaultKey(defaultkey);
		}

		// a list of all instances is needed when updating
		// the shortcuts at runtime
		this.allActions.add(anAction);
	}

	/**
	 * Return the class name of the action to which the passed key is mapped.
	 * @param key
	 * @return the action's class name or null if the key is not mapped
	 */
	public String getActionClassForKey(KeyStroke key)
	{
		String clazz = null;
		String name = null;
		Iterator itr = this.keyMap.values().iterator();

		while (itr.hasNext())
		{
			ShortcutDefinition def = (ShortcutDefinition)itr.next();
			if (def.isMappedTo(key))
			{
				clazz = def.getActionClass();
				break;
			}
		}

		return clazz;
	}

	public String getActionNameForKey(KeyStroke key)
	{
		String clazz = this.getActionClassForKey(key);
		String name = null;

		if (clazz != null)
		{
			name = (String)this.actionNames.get(clazz);
			if (name == null) name = clazz;
		}
		return name;
	}

	public void resetToDefault(String actionClass)
	{
		ShortcutDefinition def = this.getDefinition(actionClass);
		if (def == null) return;
		def.resetToDefault();
	}

	public void assignKey(String actionClass, KeyStroke key)
	{
		ShortcutDefinition def = this.getDefinition(actionClass);
		if (def == null) return;
		if (key == null)
		{
			def.setShortcutRemoved(true);
		}
		else
		{
			def.assignKey(key);
		}
	}

	public void updateActions()
	{
		int count = this.allActions.size();
		for (int i=0; i < count; i++)
		{
			WbAction action = (WbAction)this.allActions.get(i);
			String actionClass = action.getClass().getName();
			ShortcutDefinition def = this.getDefinition(actionClass);
			KeyStroke key = def.getActiveKeyStroke();
			action.setAccelerator(key);
		}
	}

	public String getActionNameForClass(String className)
	{
		return (String)this.actionNames.get(className);
	}
	/**
	 * Saves the current key assignments into an external file.
	 * Only mappings which are different from the default are saved.
	 * If a definition file exists, and the current map contains only
	 * default mappings, then the existing file is deleted.
	 *
	 */
	public void saveSettings()
	{
		// we only want to save those definitions where a different mapping is defined
		HashMap toSave = new HashMap(this.keyMap);
		Iterator itr = this.keyMap.entrySet().iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			ShortcutDefinition def = (ShortcutDefinition)entry.getValue();
			if (!def.isCustomized())
			{
				toSave.remove(entry.getKey());
			}
		}
		// if no mapping at all is defined, then don't save it
		if (toSave.size() > 0)
		{
			WbPersistence.writeObject(toSave, this.filename);
		}
		else
		{
			// but we need to make sure that, an existing definition
			// is removed, otherwise we'll load it at the next startup
			File f = new File(this.filename);
			if (f.exists())
			{
				f.delete();
			}
		}
	}

	public ShortcutDefinition[] getDefinitions()
	{
		ShortcutDefinition[] list = new ShortcutDefinition[this.keyMap.size()];
		Iterator itr = this.keyMap.values().iterator();
		int i = 0;
		while (itr.hasNext())
		{
			list[i] = (ShortcutDefinition)itr.next();
			i++;
		}
		return list;
	}

	public KeyStroke getCustomizedKeyStroke(Action anAction)
	{
		ShortcutDefinition def = this.getDefinition(anAction);
		if (def == null) return null;
		return def.getActiveKeyStroke();
	}

	public void setCustomizedKeyStroke(String aClassName, KeyStroke aKey)
	{
		String oldclass = this.getActionClassForKey(aKey);
		if (oldclass != null)
		{
			ShortcutDefinition old = (ShortcutDefinition)this.keyMap.get(oldclass);
			if (old != null) old.clearKeyStroke();
		}
		ShortcutDefinition def = (ShortcutDefinition)this.keyMap.get(aClassName);
		if (def == null) return;
		def.assignKey(aKey);
	}

	private ShortcutDefinition getDefinition(Action anAction)
	{
		return this.getDefinition(anAction.getClass().getName());
	}

	private ShortcutDefinition getDefinition(String aClassname)
	{
		ShortcutDefinition def = (ShortcutDefinition)this.keyMap.get(aClassname);
		return def;
	}


}
