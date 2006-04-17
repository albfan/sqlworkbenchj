/*
 * CopySelectedAsTextAction.java
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
      this.setIcon(null);
  }
  
  public CopySelectedAsTextAction(WbTable aClient, String labelKey)
	{
		super();
		this.client = aClient;

		this.setMenuItemName(ResourceMgr.MNU_TXT_COPY_SELECTED);
		String desc = ResourceMgr.getDescription(labelKey, true);
		this.initMenuDefinition(ResourceMgr.getString(labelKey), desc, null);
		this.setIcon(ResourceMgr.getBlankImage());
		this.setEnabled(false);
	}

	public void executeAction(ActionEvent e)
	{
		boolean shiftPressed = ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK);
		boolean ctrlPressed = ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
		ctrlPressed = ctrlPressed && ((e.getModifiers() & ActionEvent.MOUSE_EVENT_MASK) == ActionEvent.MOUSE_EVENT_MASK);
		ClipBoardCopier copier = new ClipBoardCopier(this.client);
		copier.copyDataToClipboard(!shiftPressed, true, ctrlPressed);
	}
}
