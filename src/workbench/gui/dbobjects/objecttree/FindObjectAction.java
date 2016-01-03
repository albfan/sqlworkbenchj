/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dbobjects.objecttree;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import workbench.interfaces.TextContainer;
import workbench.log.LogMgr;

import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.actions.WbAction;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FindObjectAction
  extends WbAction
{
  private ObjectFinder finder;
  private TextContainer textContainer;
  private WbConnection editorConnection;
  private DbObject toShow;

  public FindObjectAction(TextContainer container)
  {
    super();
    initMenuDefinition("MnuTxtFindObjInTree");
    textContainer = container;
    setEnabled(false);
  }

  public void setFinder(ObjectFinder objFinder)
  {
    finder = objFinder;
    setEnabled(finder != null);
  }

  public void setEditorConnection(WbConnection conn)
  {
    editorConnection = conn;
    setEnabled(editorConnection != null);
  }

  public void setTargetTable(DbObject tbl)
  {
    toShow = tbl;
    setEnabled(toShow != null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (toShow != null)
    {
      showTable(toShow);
      return;
    }

    if (textContainer == null) return;
    if (finder == null) return;

    String text = SqlUtil.getIdentifierAtCursor(textContainer, editorConnection);
    if (StringUtil.isBlank(text)) return;

    // make sure we have a fully qualified table name based on the search path of the current connection
    // the DBTree uses its own connection and therefore the search can't be done there
    TableIdentifier tbl = null;
    if (editorConnection.isBusy())
    {
      LogMgr.logWarning("FindObjectAction.executAction()", "Can not create full qualified table because the connection is busy");
      tbl = new TableIdentifier(text, editorConnection);
    }
    else
    {
      tbl = editorConnection.getMetadata().searchObjectOnPath(new TableIdentifier(text, editorConnection), null);
    }
    showTable(tbl);
  }

  private void showTable(final DbObject tbl)
  {
    EventQueue.invokeLater(() ->
    {
      finder.selectObject(tbl);
    });
  }

}
