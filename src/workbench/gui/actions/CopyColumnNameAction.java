/*
 * CopyColumnNameAction.java
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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import workbench.gui.components.WbTable;

import workbench.util.StringUtil;

/**
 * Action to copy the contents of an entry field into the clipboard
 *
 * @author Thomas Kellerer
 */
public class CopyColumnNameAction
  extends WbAction
{
  private WbTable client;

  public CopyColumnNameAction(WbTable aClient)
  {
    super();
    this.client = aClient;
    isConfigurable = false;
    initMenuDefinition("MnuTxtCopyColName");
    removeIcon();
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    int col = client.getPopupViewColumnIndex();
    if (col > -1)
    {
      String name = client.getColumnName(col);
      if (StringUtil.isNonBlank(name))
      {
        Clipboard clipboard = client.getToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(name), null);
      }
    }
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
