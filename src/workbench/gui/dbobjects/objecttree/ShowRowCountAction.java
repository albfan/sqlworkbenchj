/*
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
package workbench.gui.dbobjects.objecttree;

import java.awt.event.ActionEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.dbobjects.DbObjectList;
import workbench.interfaces.StatusBar;


import workbench.util.SqlUtil;
import workbench.util.WbThread;

/**
 * @author Thomas Kellerer
 */
public class ShowRowCountAction
	extends WbAction
  implements Interruptable
{
	private DbObjectList source;
  private StatusBar statusBar;
  private boolean cancelCount;
  private Statement currentStatement;
  private RowCountDisplay display;

	public ShowRowCountAction(DbObjectList client, RowCountDisplay countDisplay, StatusBar status)
	{
		super();
		initMenuDefinition("MnuTxtShowRowCounts");
		source = client;
    display = countDisplay;
    statusBar = status;
    setEnabled(getSelectedObjects().size() > 0);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		countRows();
	}

	private void countRows()
	{
		if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection()))
		{
			return;
		}

		List<TableIdentifier> objects = getSelectedObjects();
		if (objects == null || objects.isEmpty())
		{
			return;
		}

    WbThread counter = new WbThread("RowCount Thread")
    {

      @Override
      public void run()
      {
        doCount();
      }
    };
    counter.start();
	}

	private List<TableIdentifier> getSelectedObjects()
	{
		List<? extends DbObject> selected = this.source.getSelectedObjects();
		if (selected == null || selected.isEmpty())
		{
			return Collections.emptyList();
		}

		DbMetadata meta = source.getConnection().getMetadata();
		Set<String> typesWithData = meta.getObjectsWithData();
		List<TableIdentifier> objects = new ArrayList<>();

		for (DbObject dbo : selected)
		{
			String type = dbo.getObjectType();
			if (typesWithData.contains(type) && dbo instanceof TableIdentifier)
			{
				objects.add((TableIdentifier)dbo);
			}
		}
		return objects;
	}

  private void doCount()
  {
    List<TableIdentifier> tables = getSelectedObjects();
    if (tables.isEmpty()) return;
    WbConnection conn = source.getConnection();

    TableSelectBuilder builder = new TableSelectBuilder(conn, TableSelectBuilder.TABLEDATA_TEMPLATE_NAME);
  	boolean useSavepoint = conn.getDbSettings().useSavePointForDML();

    ResultSet rs = null;

    try
    {
      conn.setBusy(true);
      WbSwingUtilities.showWaitCursor(source.getComponent());
			currentStatement = conn.createStatementForQuery();

      int count = tables.size();
      for (int i=0; i < count; i++)
      {
        TableIdentifier table = tables.get(i);

				if (statusBar != null)
				{
          String msg = ResourceMgr.getFormattedString("MsgProcessing", table.getRawTableName(), i + 1, count);
					statusBar.setStatusMessage(msg);
				}

				String sql = builder.getSelectForCount(table);

				rs = JdbcUtils.runStatement(conn, currentStatement, sql, true, useSavepoint);

				if (rs != null && rs.next())
				{
					long rowCount = rs.getLong(1);
          display.showRowCount(table, rowCount);
				}

				SqlUtil.closeResult(rs);
				if (cancelCount) break;
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("ShowRowCountAction.doCount()", "Error counting rows: ", ex);
    }
		finally
		{
			SqlUtil.closeAll(rs, currentStatement);
			currentStatement = null;
			conn.setBusy(false);
      if (statusBar != null)
      {
        statusBar.clearStatusMessage();
      }
      WbSwingUtilities.showDefaultCursor(source.getComponent());
		}

  }
  @Override
  public void cancelExecution()
  {
    cancelCount = true;
		if (currentStatement != null)
		{
			LogMgr.logDebug("ShowRowCountAction.cancel()", "Trying to cancel the current statement");
			try
			{
				currentStatement.cancel();
			}
			catch (SQLException sql)
			{
				LogMgr.logWarning("ShowRowCountAction.cancel()", "Could not cancel statement", sql);
			}
		}

  }

  @Override
  public boolean confirmCancel()
  {
    return true;
  }

}
