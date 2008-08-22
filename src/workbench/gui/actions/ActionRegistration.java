/*
 * ActionRegistration.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

/**
 * 	Register actions with the ShortcutManager that are not created upon startup.
 * 	For this, a dummy action is created (with no client) which will
 * 	kick off the registration with the ShortcutManager
 * 	 
 * @author support@sql-workbench.net  
 */
public class ActionRegistration
{
	public static void registerActions()
	{
		new ToggleTableSourceAction(null);
		new MoveSqlTabLeft(null);
		new MoveSqlTabRight(null);
		new ReloadAction(null);
	}
}
