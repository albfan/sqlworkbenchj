/*
 * RestoreDataAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.components.WbCellEditor;
import workbench.interfaces.NullableEditor;

/**
 *
 * @author Thomas Kellerer
 */
public class RestoreDataAction
	extends WbAction
{
	private NullableEditor client;

	public RestoreDataAction(NullableEditor editor)
	{
		initMenuDefinition("MnuTxtRestoreOrgValue");
		client = editor;
		if (client != null && client.getEditor() != null)
		{
			addToInputMap(client.getEditor());
		}
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (client != null)
		{
			client.restoreOriginal();
		}
	}

	@Override
	public boolean allowDuplicate()
	{
		return true;
	}

}
