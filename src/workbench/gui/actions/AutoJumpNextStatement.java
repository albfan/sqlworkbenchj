/*
 * AutoJumpNextStatement.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;



import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *	Action to toggle the automatic jump to the next statement 
 *  if "Execute current" statement is used
 * 
 *	@author  Thomas Kellerer
 */
public class AutoJumpNextStatement 
	extends CheckBoxAction
{
	public AutoJumpNextStatement()
	{
		super("MnuTxtJumpToNext", Settings.PROPERTY_AUTO_JUMP_STATEMENT);
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		this.setCreateMenuSeparator(true);
	}
	
}
