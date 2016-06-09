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
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import workbench.interfaces.MacroChangeListener;
import workbench.resource.ResourceMgr;

import workbench.gui.menu.RecentFileManager;

import workbench.sql.macros.MacroFileSelector;
import workbench.sql.macros.MacroManager;
import workbench.sql.macros.MacroStorage;

import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class SaveMacrosAction
  extends WbAction
  implements MacroChangeListener
{
  private final int macroClientId;

  public SaveMacrosAction(int clientId)
  {
    super();
    this.macroClientId = clientId;
    this.initMenuDefinition("MnuTxtSaveMacros");
    this.setMenuItemName(ResourceMgr.MNU_TXT_MACRO);
    this.setIcon(null);
    MacroStorage macros = MacroManager.getInstance().getMacros(macroClientId);
    macros.addChangeListener(this);
    String fname = macros.getCurrentMacroFilename();
    setTooltip(fname);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    MacroFileSelector selector = new MacroFileSelector();
    WbFile f = selector.selectStorageForSave(macroClientId);
    if (f == null) return;
    MacroManager.getInstance().saveAs(macroClientId, f);
    RecentFileManager.getInstance().macrosLoaded(f);
    setTooltip(f.getFullPath());
  }

  @Override
  public void macroListChanged()
  {
    EventQueue.invokeLater(() ->
    {
      String fname = MacroManager.getInstance().getMacros(macroClientId).getCurrentMacroFilename();
      setTooltip(fname);
    });
  }

  @Override
  public void dispose()
  {
    super.dispose();
    MacroManager.getInstance().getMacros(macroClientId).removeChangeListener(this);
  }

}
