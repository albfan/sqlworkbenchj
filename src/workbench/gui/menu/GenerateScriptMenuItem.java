/*
 * GenerateScriptMenuItem.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.menu;

import workbench.gui.components.WbMenuItem;
import workbench.resource.ResourceMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class GenerateScriptMenuItem
	extends WbMenuItem
{
	public static final String SCRIPT_CMD = "create-scripts";
	
	public GenerateScriptMenuItem()
	{
		super(ResourceMgr.getString("MnuTxtCreateScript"));
		setIcon(ResourceMgr.getImage("script"));
		setActionCommand(SCRIPT_CMD);
		setEnabled(true);
		setToolTipText(ResourceMgr.getDescription("MnuTxtCreateScript"));
	}
	
}
