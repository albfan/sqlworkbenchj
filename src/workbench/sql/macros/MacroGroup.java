/*
 * MacroGroup.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.util.StringUtil;

/**
 * A list of macros combined into a group.
 * <br/>
 * A MacroGroup defines a sort order which is essentially maintained
 * through the GUI.
 * <br/>
 * Groups can be hidden from the menu.
 * 
 * @author Thomas Kellerer
 */
public class MacroGroup
	implements Sortable
{
	private String name;
	private List<MacroDefinition> macros = new ArrayList<MacroDefinition>();
	private int sortOrder;
	private boolean modified = false;
	private boolean showInMenu = true;

	public MacroGroup()
	{
	}

	public MacroGroup(String groupName)
	{
		this.name = groupName;
	}

	public boolean isVisibleInMenu()
	{
		return showInMenu;
	}

	public void setVisibleInMenu(boolean flag)
	{
		this.modified = modified || flag != showInMenu;
		this.showInMenu = flag;
	}

	public int getSortOrder()
	{
		return sortOrder;
	}

	public void setSortOrder(int order)
	{
		this.modified = modified || sortOrder != order;
		this.sortOrder = order;
	}

	public String toString()
	{
		return name;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String macroName)
	{
		this.modified = modified || !StringUtil.equalString(name, macroName);
		this.name = macroName;
	}

	public synchronized void addMacro(MacroDefinition macro)
	{
		macros.add(macro);
		applySort();
		modified = true;
	}

	/**
	 * Synchronizes the sort order of each macro with the
	 * order of the List in which the macros are stored.
	 */
	public synchronized void applySort()
	{
		Collections.sort(macros, new Sorter());
		for (int i=0; i < macros.size(); i++)
		{
			macros.get(i).setSortOrder(i);
		}
	}

	/**
	 * Returns those macros that are set to "display in menu" (i.e. where MacroDefinition.isVisibleInMenu()
	 * returns true.
	 * <br/>
	 * This ignores the isVisibleInMenu() setting of this group.
	 * 
	 * @see #getVisibleMacroSize() 
	 */
	public synchronized List<MacroDefinition> getVisibleMacros()
	{
		List<MacroDefinition> result = new ArrayList<MacroDefinition>(macros.size());
		for (MacroDefinition macro : macros)
		{
			if (macro.isVisibleInMenu())
			{
				result.add(macro);
			}
		}
		return result;
	}

	/**
	 * Sets the list of macros for this group.
	 *
	 * This method is only here to make the class serializable for the XMLEncoder and should
	 * not be used directly.
	 * 
	 * @param newMacros
	 */
	public synchronized void setMacros(List<MacroDefinition> newMacros)
	{
		macros.clear();
		macros.addAll(newMacros);
		applySort();
		modified = false;
	}

	public synchronized List<MacroDefinition> getMacros()
	{
		return macros;
	}
	
	public synchronized void removeMacro(MacroDefinition macro)
	{
		macros.remove(macro);
		modified = true;
	}

	/**
	 * Creates a deep copy of this group.
	 * For each macro definition that is part of this group, a copy
	 * is created and added to the list of macros of the copy.
	 *
	 * The copy will be marked as "not modified" (i.e. isModified() will
	 * return false on the copy), even if this group is modified.
	 * 
	 * @return a deep copy of this group
	 * @see MacroDefinition#createCopy()
	 */
	public MacroGroup createCopy()
	{
		MacroGroup copy = new MacroGroup();
		copy.name = this.name;
		copy.sortOrder = this.sortOrder;
		copy.showInMenu = this.showInMenu;
		copy.modified = false;
		for (MacroDefinition def : macros)
		{
			copy.macros.add(def.createCopy());
		}
		return copy;
	}

	/**
	 * Checks if this group has been modified.
	 *
	 * Returns true if any attribute of this group has been changed
	 * or if any MacroDefinition in this group has been changed.
	 *
	 * @return true, if this group or any Macro has been modified
	 * @see MacroDefinition#isModified()
	 */
	public boolean isModified()
	{
		if (modified) return true;
		for (MacroDefinition macro : macros)
		{
			if (macro.isModified()) return true;
		}
		return false;
	}

	/**
	 * Resets the internal modified flag and on all macros that are in this group.
	 *
	 * After a call to this method, isModified() will return false;
	 */
	public void resetModified()
	{
		modified = false;
		for (MacroDefinition macro : macros)
		{
			macro.resetModified();
		}
	}

	/**
	 * Returns the number of macros in this groups that should be displayed in the menu.
	 * 
	 * This returns a non-zero count even if isVisibleInMenu() returns false!
	 * @see #getVisibleMacros() 
	 */
	public int getVisibleMacroSize()
	{
		int size = 0;
		for (MacroDefinition def : macros)
		{
			if (def.isVisibleInMenu()) size ++;
		}
		return size;
	}
	
	public int getSize()
	{
		return macros.size();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final MacroGroup other = (MacroGroup) obj;
		if ((this.name == null) ? (other.name != null) : !this.name.equalsIgnoreCase(other.name))
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 19 * hash + (this.name != null ? this.name.toLowerCase().hashCode() : 0);
		return hash;
	}

}
