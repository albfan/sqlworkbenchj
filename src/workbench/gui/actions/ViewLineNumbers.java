/*
 * ViewLineNumbers.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import workbench.resource.Settings;

/**
 * Toggle the display of line numbers in the editor
 * @author  Thomas Kellerer
 */
public class ViewLineNumbers 
	extends CheckBoxAction
{
	public ViewLineNumbers()
	{
		super("MnuTxtShowLineNumbers", Settings.PROPERTY_SHOW_LINE_NUMBERS);
	}

}
