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

import java.awt.Window;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import workbench.interfaces.WbSelectionModel;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;

import workbench.gui.MainWindow;
import workbench.gui.actions.CompileDbObjectAction;
import workbench.gui.actions.CountTableRowsAction;
import workbench.gui.actions.CreateDropScriptAction;
import workbench.gui.actions.CreateDummySqlAction;
import workbench.gui.actions.CreateIndexAction;
import workbench.gui.actions.DeleteTablesAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.ScriptDbObjectAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.components.WbPopupMenu;
import workbench.gui.dbobjects.EditorTabSelectMenu;
import workbench.gui.sql.PasteType;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
class ContextMenuFactory
{
  static JPopupMenu createContextMenu(DbTreePanel dbTree, WbSelectionModel selection)
  {
    final JPopupMenu menu = new WbPopupMenu();

    menu.addPopupMenuListener(new PopupMenuListener()
    {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e)
      {
      }
      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
      {
        menu.removeAll();
      }
      @Override
      public void popupMenuCanceled(PopupMenuEvent e)
      {
      }
    });

    ReloadNodeAction reload = new ReloadNodeAction(dbTree);
    menu.add(reload);

    List<ObjectTreeNode> selectedNodes = dbTree.getSelectedNodes();
    if (selectedNodes.size() == 1)
    {
      ObjectTreeNode selectedNode = selectedNodes.get(0);
      if (selectedNode.getType().equals(TreeLoader.TYPE_COLUMN_LIST))
      {
        SortColumnsAction nameSort = new SortColumnsAction(dbTree.getTree(), selectedNode, true);
        SortColumnsAction positionSort = new SortColumnsAction(dbTree.getTree(), selectedNode, false);
        menu.add(nameSort);
        menu.add(positionSort);
      }
    }

    // this returns the number of selected DbObjects, not the selected nodes in general
    int dboCount = dbTree.getSelectionCount();
    if (dboCount == 1)
    {
      ObjectTreeNode selectedNode = dbTree.getSelectedNode();

      boolean showFind = selectedNode.isFKTable() || dbTree.getLoader().isDependencyNode(selectedNode.getParent());

      if (showFind)
      {
        FindObjectAction find = new FindObjectAction(null);
        find.setFinder(dbTree);
        find.setTargetTable(selectedNode.getDbObject());
        menu.add(find);
      }
    }

    menu.addSeparator();

    SpoolDataAction export = new SpoolDataAction(dbTree);
    menu.add(export);

    if (DbTreeSettings.showCountRowsAction())
    {
      // this is the action that displays the row counts in a new window as the DbExplorer does
      CountTableRowsAction countAction = new CountTableRowsAction(dbTree, selection);
      countAction.setConnection(dbTree.getConnection());
      if (countAction.isEnabled())
      {
        menu.add(countAction);
      }
    }

    ShowRowCountAction showCount = new ShowRowCountAction(dbTree, dbTree, dbTree.getStatusBar());
    menu.add(showCount);

    menu.addSeparator();

    Window window = SwingUtilities.getWindowAncestor(dbTree);
    if (window instanceof MainWindow)
    {
      EditorTabSelectMenu showSelect = new EditorTabSelectMenu(ResourceMgr.getString("MnuTxtShowTableData"), "LblShowDataInNewTab", "LblDbTreePutSelectInto", (MainWindow)window, true);
      showSelect.setPasteType(PasteType.insert);
      showSelect.setObjectList(dbTree);
      menu.add(showSelect);
		}

    menu.add(CreateDummySqlAction.createDummyInsertAction(dbTree, selection));
		menu.add(CreateDummySqlAction.createDummyUpdateAction(dbTree, selection));
		menu.add(CreateDummySqlAction.createDummySelectAction(dbTree, selection));
    menu.addSeparator();

    if (window instanceof MainWindow)
    {
      EditorTabSelectMenu editMenu = new EditorTabSelectMenu(ResourceMgr.getString("LblEditScriptSource"), "LblEditInNewTab", "LblEditInTab", (MainWindow)window, false);
      EditAction edit = new EditAction(dbTree);
      List<DbObject> selectedObjects = dbTree.getSelectedObjects();
      if (selectedObjects.size() == 1)
      {
        editMenu.setEnabled(selectedObjects.get(0).supportsGetSource());
      }
      else
      {
        editMenu.setEnabled(false);
      }
      editMenu.setActionListener(edit);
      menu.add(editMenu);
    }

    ScriptDbObjectAction script = new ScriptDbObjectAction(dbTree, selection, "MnuTxtShowSource");
    script.setEnabled(dboCount > 0);
    script.setShowSinglePackageProcedure(true);
    menu.add(script);

    if (dbTree.getConnection().getMetadata().isOracle())
    {
      CompileDbObjectAction compile = new CompileDbObjectAction(dbTree, selection);
      menu.add(compile);
    }

    menu.addSeparator();

    if (onlyColumnsSelected(selectedNodes))
    {
      CreateIndexAction createIndex = new CreateIndexAction(dbTree, null);
      createIndex.setConnection(dbTree.getConnection());
      menu.add(createIndex);
      menu.addSeparator();
    }

    DropDbObjectAction drop = new DropDbObjectAction(dbTree, selection);
    drop.setEnabled(dboCount > 0);
    drop.addDropListener(dbTree);
    menu.add(drop);

    boolean onlyTables = onlyTablesSelected(selectedNodes);
    CreateDropScriptAction dropScript = new CreateDropScriptAction(dbTree, selection);
    dropScript.setEnabled(onlyTables);
    menu.add(dropScript);

    DeleteTablesAction deleteData = new DeleteTablesAction(dbTree, selection, null);
    deleteData.setEnabled(onlyTables);
    menu.add(deleteData);

    return menu;
  }

  private static boolean onlyColumnsSelected(List<ObjectTreeNode> selectedNodes)
  {
    if (CollectionUtil.isEmpty(selectedNodes)) return false;
    for (ObjectTreeNode node : selectedNodes)
    {
      if (! (node.getDbObject() instanceof ColumnIdentifier)) return false;
    }
    return true;
  }

  private static boolean onlyTablesSelected(List<ObjectTreeNode> selectedNodes)
  {
    if (CollectionUtil.isEmpty(selectedNodes)) return false;
    for (ObjectTreeNode node : selectedNodes)
    {
      if (! (node.getDbObject() instanceof TableIdentifier)) return false;
    }
    return true;
  }

}
