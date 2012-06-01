/*
 * CopyAsSqlInsertAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.db.exporter.ExportType;
import workbench.gui.components.ClipBoardCopier;

import workbench.gui.components.WbTable;
import workbench.resource.ResourceMgr;

/**
 * Action to copy the contents of a table to the clipboard as SQL INSERT statements
 * @see workbench.gui.components.ClipBoardCopier
 * @author  Thomas Kellerer
 */
public class CopyAsSqlMergeAction extends WbAction
{
	private WbTable client;

	public CopyAsSqlMergeAction(WbTable aClient)
	{
		super();
		this.client = aClient;
		this.initMenuDefinition("MnuTxtCopyAsSqlMerge");
		this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
		this.setEnabled(false);
	}

	@Override
	public boolean hasCtrlModifier()
	{
		return true;
	}

	@Override
	public boolean hasShiftModifier()
	{
		return true;
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		ClipBoardCopier copier = new ClipBoardCopier(this.client);
		boolean selectColumns = false;
		if (invokedByMouse(e))
		{
			selectColumns = isCtrlPressed(e) ;
		}
		copier.copyAsSql(ExportType.SQL_MERGE, false, selectColumns);
	}

}
