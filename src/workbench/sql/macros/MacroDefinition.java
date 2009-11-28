/*
 * MacroDefinition.java
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

import workbench.resource.StoreableKeyStroke;
import workbench.util.StringUtil;

/**
 * A single Macro that maps a name to a SQL text.
 * <br/>
 * Each Macro defines a sort order (that is maintained through the GUI).
 * A macro can have a keyboard shortcut assigned and can be hidden from the
 * menu.
 * 
 * @author Thomas Kellerer
 */
public class MacroDefinition
	implements Sortable
{
	private String name;
	private String text;
	private int sortOrder;
	private boolean modified;
	private StoreableKeyStroke shortcut;
	private boolean showInMenu = true;
	
	public MacroDefinition()
	{
	}

	public MacroDefinition(String macroName, String macroText)
	{
		this.name = macroName;
		this.text = macroText;
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
		modified = modified || (order != sortOrder);
		this.sortOrder = order;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String macroName)
	{
		modified = modified || !StringUtil.equalStringOrEmpty(macroName, name);
		this.name = macroName;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String macroText)
	{
		modified = modified || !StringUtil.equalStringOrEmpty(text, macroText);
		this.text = macroText;
	}

	public MacroDefinition createCopy()
	{
		MacroDefinition def = new MacroDefinition(this.name, this.text);
		def.sortOrder = this.sortOrder;
		def.showInMenu = this.showInMenu;
		def.shortcut = this.shortcut;
		return def;
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
		final MacroDefinition other = (MacroDefinition) obj;
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
		hash = 47 * hash + (this.name != null ? this.name.toLowerCase().hashCode() : 0);
		return hash;
	}

	public boolean isModified()
	{
		return modified;
	}

	public void resetModified()
	{
		modified = false;
	}

	public String toString()
	{
		return name;
	}

	public StoreableKeyStroke getShortcut()
	{
		return shortcut;
	}

	public void setShortcut(StoreableKeyStroke keystroke)
	{
		this.shortcut = keystroke;
	}
}
