/*
 * ManageDriversAction.java
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
import java.awt.event.ActionEvent;
import workbench.WbManager;
import workbench.resource.ResourceMgr;

/**
 *	@author  Thomas Kellerer
 */
public class ManageDriversAction extends WbAction
{
	public ManageDriversAction()
	{
		super();
		this.initMenuDefinition("MnuTxtEditDrivers");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
	}

	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().showDialog("workbench.gui.profiles.DriverEditorDialog");
	}
	
}
