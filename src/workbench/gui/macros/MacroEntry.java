/*
 * MacroEntry.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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
	public String getName() { return this.name; }
	public void setName(String aName) { this.name = aName; }
	public String getText() { return this.text; }
	public void setText(String aText) { this.text = aText; }
}
