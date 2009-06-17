/*
 * ShowObjectInfoAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import workbench.db.WbConnection;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.ObjectInfo;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class ShowObjectInfoAction
	extends WbAction
	implements TextSelectionListener

{
	private SqlPanel display;

	public ShowObjectInfoAction(SqlPanel panel)
	{
		display = panel;
		display.getEditor().addSelectionListener(this);
		setIcon(null);
		setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		initMenuDefinition("MnuTxtShowObjectDef");
		setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		try
		{
			ObjectInfo info = new ObjectInfo();
			WbConnection conn = display.getConnection();
			String text = display.getSelectedText();
			if (conn != null && StringUtil.isNonBlank(text))
			{
				display.showStatusMessage(ResourceMgr.getString("TxtRetrieveTableDef") + " " + text);
				StatementRunnerResult result = info.getObjectInfo(conn, text, false);
				if (result != null)
				{
					int count = display.getResultTabCount();

					// Retrieving the messages will reset the hasMessages() flag...
					boolean hasMessages = result.hasMessages();
					
					if (hasMessages)
					{
						display.appendToLog("\n");
						display.appendToLog(result.getMessageBuffer().toString());
					}

					if (result.hasDataStores())
					{
						List<DataStore> data = result.getDataStores();
						for (int i=0; i < data.size(); i++)
						{
							data.get(i).resetStatus();
						}
						display.addResult(result);
						display.setSelectedResultTab(count - 1);
					}
					else if (hasMessages)
					{
						display.showLogPanel();
					}
				}
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("ShowObjectInfoAction.executeAcion()", "Error retrieving objec tinfo", ex);
		}
		finally
		{
			display.clearStatusMessage();
		}
	}

	public void checkEnabled()
	{
		setEnabled(display != null && display.isConnected() && StringUtil.isNonBlank(display.getSelectedText()));
	}
	
	public void selectionChanged(int newStart, int newEnd)
	{
		if (!display.isConnected()) return;
		boolean selected = (newStart > -1 && newEnd > newStart);
		this.setEnabled(selected);
	}

}
