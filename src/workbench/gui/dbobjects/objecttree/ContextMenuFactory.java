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
    JPopupMenu menu = new JPopupMenu();

    SpoolDataAction export = new SpoolDataAction(dbTree);
    menu.add(export);

    CountTableRowsAction countAction = new CountTableRowsAction(dbTree, selection);
    countAction.setConnection(dbTree.getConnection());
    if (countAction.isEnabled())
    {
      menu.add(countAction);
    }

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

//		SchemaReportAction action = new SchemaReportAction(dbTree);
//		menu.add(action);

    ScriptDbObjectAction script = new ScriptDbObjectAction(dbTree, selection);
    menu.add(script);

    menu.addSeparator();

    DropDbObjectAction drop = new DropDbObjectAction(dbTree, selection, dbTree);
    menu.add(drop);

    CreateDropScriptAction dropScript = new CreateDropScriptAction(dbTree, selection);
    menu.add(dropScript);

    DeleteTablesAction deleteData = new DeleteTablesAction(dbTree, selection, null);
    if (deleteData.isEnabled())
    {
      menu.add(deleteData);
    }

    return menu;
  }

}
