/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.gui.settings;

/**
 *
 * @author support@sql-workbench.net
 */
public enum ExternalFileHandling
{
	none,
	content,
	link;

	public static ExternalFileHandling getValue(String input)
	{
		if (input == null) return none;
		if (input.equalsIgnoreCase(content.toString())) return content;
		if (input.equalsIgnoreCase(link.toString())) return link;
		return none;
	}
	
}
