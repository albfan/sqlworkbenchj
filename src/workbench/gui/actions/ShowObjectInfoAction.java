/*
 * ShowObjectInfoAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.log.LogMgr;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.gui.sql.SqlPanel;

import workbench.storage.DataStore;

import workbench.sql.StatementRunnerResult;
import workbench.sql.wbcommands.ObjectInfo;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ShowObjectInfoAction
	extends WbAction
{
	private SqlPanel display;

	public ShowObjectInfoAction(SqlPanel panel)
	{
		display = panel;
		setIcon(null);
		setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		initMenuDefinition("MnuTxtShowObjectDef", KeyStroke.getKeyStroke(KeyEvent.VK_I, PlatformShortcuts.getDefaultModifier()));
		checkEnabled();
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
		WbThread t = new WbThread("ObjectInfoThread")
		{
			@Override
			public void run()
			{
				showInfo(includeDependencies);
			}
		};
		t.start();
	}

	protected void showInfo(boolean includeDependencies)
	{
		if (display.isBusy()) return;
		WbConnection conn = display.getConnection();
		if (conn == null)	return;

		try
		{
			display.setBusy(true);
			display.fireDbExecStart();
			setEnabled(false);

			ObjectInfo info = new ObjectInfo();

			boolean deps = conn.getDbSettings().objectInfoWithDependencies();
      String text = SqlUtil.getIdentifierAtCursor(display.getEditor(), conn);

			if (StringUtil.isNonBlank(text))
			{
				display.setStatusMessage(ResourceMgr.getString("TxtRetrieveTableDef") + " " + text);
				StatementRunnerResult result = info.getObjectInfo(conn, text, includeDependencies || deps, true);

				if (result != null)
				{
					int count = display.getResultTabCount();

					// if the display is "kept busy" the current "data" will not be recognized
					// when switching panels
					display.setBusy(false);

					// Retrieving the messages will reset the hasMessages() flag...
					boolean hasMessages = result.hasMessages();

					if (hasMessages)
					{
						display.appendToLog("\n");
						display.appendToLog(result.getMessageBuffer().toString());
					}

					if (result.hasDataStores())
					{
						for (DataStore data : result.getDataStores())
						{
							data.resetStatus();
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
			LogMgr.logError("ShowObjectInfoAction.executeAcion()", "Error retrieving object info", ex);
		}
		finally
		{
			display.fireDbExecEnd();
			display.clearStatusMessage();

			// just in case...
			if (display.isBusy())
			{
				display.setBusy(false);
			}

			checkEnabled();
		}
	}

	public void checkEnabled()
	{
		setEnabled(display != null && display.isConnected());
	}

}
