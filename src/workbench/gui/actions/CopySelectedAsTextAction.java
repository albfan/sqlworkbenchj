/*
 * CopySelectedAsTextAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.components.ClipBoardCopier;
import workbench.gui.components.WbTable;

import workbench.resource.ResourceMgr;

/**
 *	Action to copy the contents of a entry field into the clipboard
 *	@author  support@sql-workbench.net
 */
public class CopySelectedAsTextAction extends WbAction
{
	private WbTable client;


  public CopySelectedAsTextAction(WbTable aClient)
  {
      this(aClient, "MnuTxtCopySelectedAsText");
  }
  
  public CopySelectedAsTextAction(WbTable aClient, String labelKey)
	{
		super();
		this.client = aClient;
		this.setMenuItemName(ResourceMgr.MNU_TXT_COPY_SELECTED);
		this.initMenuDefinition(labelKey, null);
		this.setEnabled(false);
	}

	public boolean hasCtrlModifier() { return true; }
	public boolean hasShiftModifier() { return true; }
	
	public void executeAction(ActionEvent e)
	{
		ClipBoardCopier copier = new ClipBoardCopier(this.client);
		copier.copyDataToClipboard(!isShiftPressed(e), true, isCtrlPressed(e));
	}
}
