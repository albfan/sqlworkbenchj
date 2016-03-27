/*
 * TransposeRowAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.log.LogMgr;

import workbench.gui.components.WbTable;
import workbench.gui.sql.SqlPanel;

import workbench.storage.DataStore;
import workbench.storage.DatastoreTransposer;

/**
 * An action to transpose the selected row in a WbTable.
 *
 * @author Thomas Kellerer
 */
public class TransposeRowAction
  extends WbAction
  implements ListSelectionListener
{
  private WbTable client;

  public TransposeRowAction(WbTable aClient)
  {
    super();
    this.client = aClient;
    if (client != null)
    {
      client.getSelectionModel().addListSelectionListener(this);
    }
    this.initMenuDefinition("MnuTxtTransposeRow");
    this.setEnabled(false);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    DatastoreTransposer transpose = new DatastoreTransposer(client.getDataStore());
    int[] rows = client.getSelectedRows();
    DataStore ds = transpose.transposeRows(rows);
    showDatastore(ds);
  }

  private void showDatastore(DataStore ds)
  {
    SqlPanel p = findPanel();
    if (p != null)
    {
      try
      {
        p.showData(ds);
      }
      catch (SQLException sql)
      {
        LogMgr.logError("TransposeRowAction.showDataStore()", "Could not display datastore", sql);
      }
    }
  }

  private SqlPanel findPanel()
  {
    if (client == null) return null;
    Component c = client.getParent();
    while (c != null)
    {
      if (c instanceof SqlPanel)
      {
        return (SqlPanel)c;
      }
      c = c.getParent();
    }
    return null;
  }

  @Override
  public void valueChanged(ListSelectionEvent e)
  {
    boolean selected = false;
    if (client != null)
    {
      selected = client.getSelectedRowCount() > 0;
    }
    setEnabled(selected);
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
