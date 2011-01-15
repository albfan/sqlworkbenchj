/*
 * HighlightCurrentStatement.java
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



import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *	Toggle highlighting of the currently executed statement.
 *	@author  Thomas Kellerer
 */
public class HighlightCurrentStatement 
	extends CheckBoxAction
	implements PropertyChangeListener
{
	
	public HighlightCurrentStatement()
	{
		super("MnuTxtHighlightCurrent", Settings.PROPERTY_HIGHLIGHT_CURRENT_STATEMENT);
		this.setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_HIGHLIGHT_CURRENT_STATEMENT);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		this.setSwitchedOn(Settings.getInstance().getBoolProperty(Settings.PROPERTY_HIGHLIGHT_CURRENT_STATEMENT, false));
	}

}
