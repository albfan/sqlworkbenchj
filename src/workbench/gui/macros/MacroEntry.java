/*
 * MacroEntry.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

public class MacroEntry
{
	private String text;
	private String name;
	
	public MacroEntry(String aName, String aText)
	{
		this.text = aText;
		this.name = aName;
	}
	
	public String toString() { return this.name; }
	public final String getName() { return this.name; }
	public final void setName(String aName) { this.name = aName; }
	public final String getText() { return this.text; }
	public final void setText(String aText) { this.text = aText; }
}
