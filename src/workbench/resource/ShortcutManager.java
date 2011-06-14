/*
 * ShortcutManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Set;
import javax.swing.Action;
import javax.swing.KeyStroke;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.gui.actions.WbAction;
import workbench.log.LogMgr;
import workbench.util.WbPersistence;

/**
 * A class to manage, store and retrieve customized shortcuts for actions.
 *
 * @author Thomas Kellerer
 */
public class ShortcutManager
{
	private Set<ChangeListener> changeListener = new HashSet<ChangeListener>(5);

	private String filename;

	// key to the map is the action's class name,
	private Map<String, ShortcutDefinition> keyMap;

	private Map<String, String> actionNames;

	// we need the list of registered actions, in order to be able to
	// display the label for the action in the customization dialog
	private List<WbAction> allActions = new LinkedList<WbAction>();

	private Map<KeyStroke, WbAction> keyDebugMap;

	private boolean modified;

  private static class LazyInstanceHolder
	{
    static final ShortcutManager instane = new ShortcutManager(Settings.getInstance().getShortcutFilename());
  }

	@SuppressWarnings("unchecked")
	protected ShortcutManager(String aFilename)
	{
		this.filename = aFilename;
//		LogMgr.logDebug("ShortcutManager", "Using file: " + filename);
		try
		{
			WbPersistence reader = new WbPersistence(this.filename);
			this.keyMap = (HashMap<String, ShortcutDefinition>)reader.readObject();
		}
		catch (Exception e)
		{
			this.keyMap = null;
		}

		if (this.keyMap == null)
		{
			this.keyMap = new HashMap<String, ShortcutDefinition>(30);
			this.actionNames = new HashMap<String, String>(30);
		}
		else
		{
			this.actionNames = new HashMap<String, String>(this.keyMap.size());
		}
	}

	public synchronized static ShortcutManager getInstance()
	{
		return LazyInstanceHolder.instane;
	}

	public void removeShortcut(String clazz)
	{
		this.assignKey(clazz, null);
	}

	public void addChangeListener(ChangeListener l)
	{
		this.changeListener.add(l);
	}

	public void removeChangeListener(ChangeListener l)
	{
		this.changeListener.remove(l);
	}

	public void fireShortcutsChanged()
	{
		if (this.changeListener.isEmpty()) return;

		ChangeEvent event = new ChangeEvent(this);
		for (ChangeListener l : changeListener)
		{
			if (l != null)
			{
				l.stateChanged(event);
			}
		}
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
			if (defaultkey != null)
			{
				def.assignDefaultKey(defaultkey);
			}
		}

		if (LogMgr.isDebugEnabled())
		{
			KeyStroke key = anAction.getDefaultAccelerator();
			if (key != null)
			{
				if (this.keyDebugMap == null) this.keyDebugMap = new HashMap<KeyStroke, WbAction>(100);
				WbAction a = this.keyDebugMap.get(key);
				if (a != null && !a.equals(anAction))
				{
					Exception e = new Exception("Duplicate key mapping");
					LogMgr.logError("ShortcutManager.registerAction", "Duplicate key assignment for keyStroke " + key + " from " + clazz + ", already registered for "+ a.getClass().getName(), e);
				}
				else
				{
					this.keyDebugMap.put(key, anAction);
				}
			}
		}

		// a list of all instances is needed when updating
		// the shortcuts at runtime
		this.allActions.add(anAction);
	}

	public String getTooltip(String clazz)
	{
		Iterator itr = allActions.iterator();
		while (itr.hasNext())
		{
			WbAction action = (WbAction)itr.next();
			String actionClass = action.getClass().getName();
			if (actionClass.equals(clazz))
			{
				return action.getToolTipText();
			}
		}
		return null;
	}

	public WbAction getActionForClass(String clazz)
	{
		for (WbAction a : allActions)
		{
			if (a.getClass().getName().equals(clazz)) return a;
		}
		return null;
	}

	/**
	 * Return the class name of the action to which the passed key is mapped.
	 * @param key
	 * @return the action's class name or null if the key is not mapped
	 */
	public String getActionClassForKey(KeyStroke key)
	{
		String clazz = null;

		for (ShortcutDefinition def : keyMap.values())
		{
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
			name = this.actionNames.get(clazz);
			if (name == null) name = clazz;
		}
		return name;
	}

	public void resetToDefault(String actionClass)
	{
		ShortcutDefinition def = this.getDefinition(actionClass);
		if (def == null) return;
		def.resetToDefault();
		modified = true;
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
		modified = true;
	}

	public void updateActions()
	{
		for (WbAction action : allActions)
		{
			if (action == null) continue;
			String actionClass = action.getClass().getName();
			ShortcutDefinition def = this.getDefinition(actionClass);
			KeyStroke key = def.getActiveKeyStroke();
			action.setAccelerator(key);
		}
	}

	public String getActionNameForClass(String className)
	{
		return this.actionNames.get(className);
	}
	/**
	 * Saves the current key assignments into an external file.
	 * Only mappings which are different from the default are saved.
	 * If a definition file exists, and the current map contains only
	 * default mappings, then the existing file is deleted.
	 */
	public void saveSettings()
	{
		if (!modified)
		{
			return;
		}

		// we only want to save those definitions where a different mapping is defined
		// so we first create a copy of the current keymap, and then remove any
		// definition that is not customized.
		HashMap<String, ShortcutDefinition> toSave = new HashMap<String, ShortcutDefinition>(this.keyMap);
		for (Entry<String, ShortcutDefinition> entry : keyMap.entrySet())
		{
			ShortcutDefinition def = entry.getValue();
			if (!def.isCustomized())
			{
				toSave.remove(entry.getKey());
			}
		}
		// if no mapping at all is defined, then don't save it
		if (toSave.size() > 0)
		{
			WbPersistence writer = new WbPersistence(this.filename);
			try { writer.writeObject(toSave); } catch (Throwable th) {}
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
		int i = 0;
		for (ShortcutDefinition def : keyMap.values())
		{
			list[i] = def;
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
			ShortcutDefinition old = this.keyMap.get(oldclass);
			if (old != null) old.clearKeyStroke();
		}
		ShortcutDefinition def = this.keyMap.get(aClassName);
		if (def == null) return;
		def.assignKey(aKey);
		modified = true;
	}

	private ShortcutDefinition getDefinition(Action anAction)
	{
		return this.getDefinition(anAction.getClass().getName());
	}

	private ShortcutDefinition getDefinition(String aClassname)
	{
		ShortcutDefinition def = this.keyMap.get(aClassname);
		return def;
	}

}
