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
package workbench.gui.editor.actions;

import workbench.resource.PlatformShortcuts;

/**
 *
 * @author support@sql-workbench.net
 */
public class SelectLineEnd
	extends LineEnd
{

	public SelectLineEnd()
	{
		super("TxtEdLineEndSel", PlatformShortcuts.getDefaultEndOfLine(true));
		select = true;
	}
}
