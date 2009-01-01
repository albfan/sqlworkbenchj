/*
 * HighlightCurrentStatement.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;



import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *	Toggle highlighting of the currently executed statement.
 *	@author  support@sql-workbench.net
 */
public class HighlightCurrentStatement 
	extends CheckBoxAction
{
	
	public HighlightCurrentStatement()
	{
		super("MnuTxtHighlightCurrent", Settings.PROPERTY_HIGHLIGHT_CURRENT_STATEMENT);
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
	}

}
