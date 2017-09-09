/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.IndexChangeListener;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;
import workbench.db.IndexColumn;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.RunScriptPanel;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class CreateIndexAction
  extends WbAction
{
  private DbObjectList source;
  private WbConnection dbConnection;
  private IndexChangeListener changeListener;

  public CreateIndexAction(DbObjectList source, IndexChangeListener listener)
  {
    super();
    initMenuDefinition("MnuTxtCreateIndex");
    this.source = source;
    this.changeListener = listener;
    setEnabled(false);
  }

  public void setConnection(WbConnection connection)
  {
    dbConnection = connection;
    setEnabled(this.dbConnection != null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    createIndex();
  }

  protected void createIndex()
  {
    if (source == null) return;

    List<DbObject> objects = source.getSelectedObjects();
    if (CollectionUtil.isEmpty(objects)) return;

    List<IndexColumn> columns = new ArrayList<>();
    String indexName = ResourceMgr.getString("TxtNewIndexName");

    TableIdentifier currentTable = source.getObjectTable();

    for (DbObject object : objects)
    {
      if ((object instanceof ColumnIdentifier))
      {
        columns.add(new IndexColumn(((ColumnIdentifier)object).getColumnName(), null));
      }
    }
    if (columns.isEmpty()) return;

    IndexReader reader = this.dbConnection.getMetadata().getIndexReader();

    String sql = reader.buildCreateIndexSql(currentTable, indexName, false, columns);
    if (!sql.trim().endsWith(";"))
    {
      sql += ";\n";
    }

    String title = ResourceMgr.getString("TxtWindowTitleCreateIndex");

    if (dbConnection.generateCommitForDDL())
    {
      sql += "\nCOMMIT;\n";
    }
    RunScriptPanel panel = new RunScriptPanel(dbConnection, sql);
    panel.openWindow(source.getComponent(), title, indexName);

    if (panel.wasRun() && changeListener != null)
    {
      changeListener.indexChanged(currentTable, indexName);
    }
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }

}
