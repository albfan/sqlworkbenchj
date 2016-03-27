/*
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

import java.awt.event.ActionEvent;

import workbench.gui.MainWindow;
import workbench.gui.dbobjects.objecttree.DbTreePanel;

/**
 * @author Thomas Kellerer
 */
public class ShowDbTreeAction
  extends WbAction
{
  private MainWindow mainWin;

  public ShowDbTreeAction(MainWindow aWindow)
  {
    super();
    mainWin = aWindow;
    initMenuDefinition("MnuTxtNewDbTreeWindow");
    setIcon("dbtree");
    setEnabled(false);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (mainWin.isDbTreeVisible())
    {
      DbTreePanel dbTree = mainWin.getDbTree();
      dbTree.requestFocusInWindow();
    }
    else
    {
      mainWin.showDbTree();
    }
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
