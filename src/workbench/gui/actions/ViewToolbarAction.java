/*
 * ViewToolbarAction.java
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

import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class ViewToolbarAction 
	extends CheckBoxAction
{
	public ViewToolbarAction()
	{
		super("MnuTxtShowToolbar", Settings.PROPERTY_SHOW_TOOLBAR);
	}

}
