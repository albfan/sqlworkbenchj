/*
 * VersionCheckAction.java
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
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import javax.swing.JDialog;

import javax.swing.JFrame;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *	@author  support@sql-workbench.net
 */
public class VersionCheckAction extends WbAction
{

	private static VersionCheckAction instance = new VersionCheckAction();
	public static VersionCheckAction getInstance() { return instance; }
	
	private VersionCheckAction()
	{
		super();
		this.initMenuDefinition("MnuTxtVersionCheck");
		this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
		this.removeIcon();
	}

	public void executeAction(ActionEvent e)
	{
		WbManager.getInstance().showDialog("workbench.gui.tools.VersionCheckDialog");
	}
}
