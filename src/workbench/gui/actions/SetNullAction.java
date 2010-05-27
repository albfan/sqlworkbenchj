/*
 * SetNullAction
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.interfaces.NullableEditor;

/**
 *
 * @author Thomas Kellerer
 */
public class SetNullAction
	extends WbAction
{
	private NullableEditor client;

	public SetNullAction(NullableEditor editor)
	{
		initMenuDefinition("MnuTxtSetNull");
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
			client.setNull(true);
		}
	}

	@Override
	public boolean allowDuplicate()
	{
		return true;
	}

}
