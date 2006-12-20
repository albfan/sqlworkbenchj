/*
 * ShortcutDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import workbench.gui.renderer.*;

/**
 * @author support@sql-workbench.net
 */
public class ActionDisplay
	implements Comparable
{
	
	public String text;
	public String tooltip;
	
	public ActionDisplay(String txt, String tip)
	{
		text = txt;
		tooltip = tip;
	}
	
	public int compareTo(Object other)
	{
		ActionDisplay a = (ActionDisplay)other;
		return text.compareToIgnoreCase(a.text);
	}
	
	public String toString()
	{
		return text;
	}
}
