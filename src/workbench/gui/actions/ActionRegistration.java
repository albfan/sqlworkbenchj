/*
 * ActionRegistration.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
 */
public class ActionRegistration
{
	public static void registerActions()
	{
		ToggleTableSourceAction action = new ToggleTableSourceAction(null);
	}
}
