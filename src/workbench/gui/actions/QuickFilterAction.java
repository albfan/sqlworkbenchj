/*
 * QuickFilterAction.java
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

import java.awt.event.ActionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.gui.components.WbTable;
import workbench.gui.filter.DefineFilterExpressionPanel;
import workbench.interfaces.QuickFilter;
import workbench.resource.ResourceMgr;

/**
 *	Filter data from a WbTable 
 *	@author  support@sql-workbench.net
 */
public class QuickFilterAction 
		extends WbAction
{
	private QuickFilter client;
	
	public QuickFilterAction(QuickFilter filterGui)
	{
		super();
		this.client = filterGui;
		this.initMenuDefinition("MnuTxtQuickFilter");
		this.setIcon(ResourceMgr.getImage("filter"));
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setCreateToolbarSeparator(false);
	}

	public void executeAction(ActionEvent e)
	{
		client.applyQuickFilter();
	}

}
