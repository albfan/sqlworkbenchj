/*
 * ShowObjectInfoAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.KeyStroke;
import workbench.db.WbConnection;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.TextSelectionListener;
import workbench.log.LogMgr;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.ObjectInfo;
import workbench.storage.DataStore;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
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
		initMenuDefinition("MnuTxtShowObjectDef", KeyStroke.getKeyStroke(KeyEvent.VK_I, PlatformShortcuts.getDefaultModifier()));
		setEnabled(false);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		if (display.isBusy()) return;
		final boolean includeDependencies;
		if (invokedByMouse(e))
		{
			includeDependencies = isCtrlPressed(e);
		}
		else
		{
			includeDependencies = false;
		}
		WbThread t = new WbThread(new Runnable()
		{
			public void run()
			{
				showInfo(includeDependencies);
			}
		}, "ObjectInfoThread");
		t.start();
	}

	protected void showInfo(boolean includeDependencies)
	{
		if (display.isBusy()) return;
		try
		{
			display.setBusy(true);
			display.fireDbExecStart();
			setEnabled(false);
			
			ObjectInfo info = new ObjectInfo();
			WbConnection conn = display.getConnection();
			String text = display.getSelectedText();
			if (conn != null && StringUtil.isNonBlank(text))
			{
				display.showStatusMessage(ResourceMgr.getString("TxtRetrieveTableDef") + " " + text);
				StatementRunnerResult result = info.getObjectInfo(conn, text, includeDependencies);
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
			display.fireDbExecEnd();
			display.setBusy(false);
			display.clearStatusMessage();
			checkEnabled();
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
