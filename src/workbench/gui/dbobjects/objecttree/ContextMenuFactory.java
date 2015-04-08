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

import java.awt.Window;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import workbench.interfaces.WbSelectionModel;
import workbench.resource.ResourceMgr;

import workbench.gui.MainWindow;
import workbench.gui.actions.CountTableRowsAction;
import workbench.gui.actions.CreateDropScriptAction;
import workbench.gui.actions.CreateDummySqlAction;
import workbench.gui.actions.DeleteTablesAction;
import workbench.gui.actions.DropDbObjectAction;
import workbench.gui.actions.ScriptDbObjectAction;
import workbench.gui.actions.SpoolDataAction;
import workbench.gui.components.WbPopupMenu;
import workbench.gui.dbobjects.EditorTabSelectMenu;
import workbench.gui.sql.PasteType;

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
    menu.addSeparator();

    SpoolDataAction export = new SpoolDataAction(dbTree);
    menu.add(export);

    if (DbTreeSettings.showCountRowsAction())
    {
      CountTableRowsAction countAction = new CountTableRowsAction(dbTree, selection);
      countAction.setConnection(dbTree.getConnection());
      if (countAction.isEnabled())
      {
        menu.add(countAction);
      }
    }

    ShowRowCountAction showCount = new ShowRowCountAction(dbTree, dbTree, dbTree.getStatusBar());
    menu.add(showCount);

    Window w = SwingUtilities.getWindowAncestor(dbTree);
    if (w instanceof MainWindow)
    {
      MainWindow mw = (MainWindow)w;
      EditorTabSelectMenu showSelect = new EditorTabSelectMenu(ResourceMgr.getString("MnuTxtShowTableData"), "LblShowDataInNewTab", "LblDbTreePutSelectInto", mw, true);
      showSelect.setPasteType(PasteType.insert);
      showSelect.setObjectList(dbTree);
      menu.add(showSelect);
		}

    menu.addSeparator();

    menu.add(CreateDummySqlAction.createDummyInsertAction(dbTree, selection));
		menu.add(CreateDummySqlAction.createDummyUpdateAction(dbTree, selection));
		menu.add(CreateDummySqlAction.createDummySelectAction(dbTree, selection));

    ScriptDbObjectAction script = new ScriptDbObjectAction(dbTree, selection);
    menu.add(script);

    menu.addSeparator();

    DropDbObjectAction drop = new DropDbObjectAction(dbTree, selection, null);
    drop.addDropListener(dbTree);
    menu.add(drop);

    CreateDropScriptAction dropScript = new CreateDropScriptAction(dbTree, selection);
    menu.add(dropScript);

    DeleteTablesAction deleteData = new DeleteTablesAction(dbTree, selection, null);
    menu.add(deleteData);

    return menu;
  }

}
