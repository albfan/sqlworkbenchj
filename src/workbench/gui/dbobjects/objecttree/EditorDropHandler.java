/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import java.awt.Point;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.completion.BaseAnalyzer;
import workbench.gui.completion.StatementContext;
import workbench.gui.sql.EditorPanel;

import workbench.sql.formatter.WbSqlFormatter;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class EditorDropHandler
{
  private EditorPanel editor;

  public EditorDropHandler(EditorPanel editor)
  {
    this.editor = editor;
  }

  public void handleDrop(ObjectTreeTransferable selection, Point location)
  {
    if (selection == null) return;
    ObjectTreeNode[] nodes = selection.getSelectedNodes();
    if (nodes == null || nodes.length == 0) return;

    String id = selection.getConnectionId();
    WbConnection conn = ConnectionMgr.getInstance().findConnection(id);

    ScriptParser parser = new ScriptParser(ParserType.getTypeFromConnection(conn));
    parser.setScript(editor.getSelectedStatement());

    int editorPos = editor.xyToOffset((int)location.getX(), (int)location.getY());
    int context = BaseAnalyzer.NO_CONTEXT;
    int index = parser.getCommandIndexAtCursorPos(editorPos);

    String sql = null;

    if (index > -1)
    {
      sql = parser.getCommand(index, false);
      StatementContext ctx = new StatementContext(conn, sql, editorPos - parser.getStartPosForCommand(index));

			if (ctx.isStatementSupported())
			{
        context = ctx.getAnalyzer().getContext();
      }
    }

    // handle the case where a single table is dragged
    // into an "empty" area of the editor. In that case
    // generate a select statement for the table instead
    // of just inserting the table name
    if (nodes.length == 1 && StringUtil.isEmptyString(sql))
    {
      DbObject dbo = nodes[0].getDbObject();
      if (dbo instanceof TableIdentifier)
      {
        String text = "select * from " + dbo.getObjectExpression(conn);
        WbSqlFormatter formatter = new WbSqlFormatter(text, conn.getDbId());
        text = formatter.getFormattedSql();
        insertString(text, editorPos);
        return;
      }
    }

    StringBuilder text = new StringBuilder(nodes.length * 20);
    for (int i=0; i < nodes.length; i++)
    {
      if (i > 0) text.append(", ");
      text.append(getDisplayString(conn, nodes[i], context));
    }
    insertString(text.toString(), editorPos);
  }

  private void insertString(String text, int location)
  {
    int start = editor.getSelectionStart();
    int end = editor.getSelectionEnd();

    if (start < end && start <= location && location <= end)
    {
      editor.setSelectedText(text);
    }
    else
    {
      editor.insertText(location, text);
    }
  }

  private String getDisplayString(WbConnection conn, ObjectTreeNode node, int context)
  {
    if (node == null) return "";
    DbObject dbo = node.getDbObject();
    if (dbo == null)
    {
      if (TreeLoader.TYPE_COLUMN_LIST.equals(node.getType()))
      {
        return getColumnList(node);
      }
      return node.getName();
    }

    if (context == BaseAnalyzer.CONTEXT_COLUMN_LIST && dbo instanceof TableIdentifier)
    {
      List<ColumnIdentifier> columns = conn.getObjectCache().getColumns((TableIdentifier)dbo);
      if (CollectionUtil.isNonEmpty(columns))
      {
        int count = columns.size();
        StringBuilder result = new StringBuilder(count * 10);
        for (int i=0; i < count; i++)
        {
          if (i > 0) result.append(", ");
          result.append(columns.get(i).getColumnName());
        }
        return result.toString();
      }
    }

    return dbo.getObjectExpression(conn);
  }

  private String getColumnList(ObjectTreeNode columns)
  {
    int count = columns.getChildCount();
    StringBuilder result = new StringBuilder(count * 10);
    int colCount = 0;
    for (int i=0; i < count; i++)
    {
      ObjectTreeNode col = (ObjectTreeNode)columns.getChildAt(i);
      if (col != null && col.getDbObject() != null)
      {
        DbObject dbo = col.getDbObject();
        if (dbo instanceof ColumnIdentifier)
        {
          if (colCount > 0) result.append(", ");
          result.append(dbo.getObjectName());
          colCount++;
        }
      }
    }
    return result.toString();
  }
}
