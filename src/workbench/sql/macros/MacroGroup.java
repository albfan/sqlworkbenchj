/*
 * MacroDefinition.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author support@sql-workbench.net
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
		this.showInMenu = flag;
	}

	public int getSortOrder()
	{
		return sortOrder;
	}

	public void setSortOrder(int order)
	{
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
		this.name = macroName;
	}

	public synchronized void addMacro(MacroDefinition macro)
	{
		macros.add(macro);
		applySort();
		modified = true;
	}

	public synchronized void applySort()
	{
		Collections.sort(macros, new Sorter());
		for (int i=0; i < macros.size(); i++)
		{
			macros.get(i).setSortOrder(i);
		}
	}

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

	public MacroGroup createCopy()
	{
		MacroGroup copy = new MacroGroup();
		copy.name = this.name;
		copy.sortOrder = this.sortOrder;
		copy.showInMenu = this.showInMenu;
		for (MacroDefinition def : macros)
		{
			copy.macros.add(def.createCopy());
		}
		return copy;
	}

	public boolean isModified()
	{
		if (modified) return true;
		for (MacroDefinition macro : macros)
		{
			if (macro.isModified()) return true;
		}
		return false;
	}
	
	public void resetModified()
	{
		modified = false;
		for (MacroDefinition macro : macros)
		{
			macro.resetModified();
		}
	}

	/**
	 * Returns the number of macros in this groups that should be displayed in the menu
	 * 
	 * @return
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
