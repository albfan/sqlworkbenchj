/*
 * ScriptDbObjectAction.java
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

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.SwingUtilities;

import workbench.interfaces.WbSelectionListener;
import workbench.interfaces.WbSelectionModel;
import workbench.resource.DbExplorerSettings;

import workbench.db.DbObject;
import workbench.db.ObjectScripter;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectScripterUI;

/**
 * @author Thomas Kellerer
 */
public class ScriptDbObjectAction
  extends WbAction
  implements WbSelectionListener
{
  private DbObjectList source;
  private final WbSelectionModel selection;
  private boolean showSinglePackageProcedure;

  public ScriptDbObjectAction(DbObjectList client, WbSelectionModel list)
  {
    this(client, list, "MnuTxtCreateScript");
  }

  public ScriptDbObjectAction(DbObjectList client, WbSelectionModel list, String labelKey)
  {
    super();
    this.initMenuDefinition(labelKey);
    this.source = client;
    this.selection = list;
    checkEnabled();
    setIcon("script");
    selection.addSelectionListener(this);
  }

  @Override
  public void dispose()
  {
    super.dispose();
    if (selection != null) selection.removeSelectionListener(this);
  }

  /**
   * Controls if just the source of package procedure should be shown/generated or the complete package.
   *
   * @param showPackageProcOnly
   */
  public void setShowSinglePackageProcedure(boolean showPackageProcOnly)
  {
    this.showSinglePackageProcedure = showPackageProcOnly;
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection())) return;

    List<? extends DbObject> objects = source.getSelectedObjects();
    if (objects == null || objects.isEmpty()) return;

    ObjectScripter s = new ObjectScripter(objects, source.getConnection());
    s.setEndTransaction(true);
    s.setShowPackageProcedureOnly(showSinglePackageProcedure);
    s.setIncludeForeignKeys(source.getConnection().getDbSettings().getGenerateTableFKSource());
    s.setIncludeGrants(DbExplorerSettings.getGenerateTableGrants());
    ObjectScripterUI scripterUI = new ObjectScripterUI(s);
    if (objects.size() == 1)
    {
      scripterUI.setScriptObjectName(objects.get(0).getObjectExpression(source.getConnection()));
    }
    scripterUI.setDbConnection(source.getConnection());
    scripterUI.show(SwingUtilities.getWindowAncestor(source.getComponent()));
  }

  private void checkEnabled()
  {
    if (selection.hasSelection())
    {
      List<? extends DbObject> objects = source.getSelectedObjects();
      for (DbObject dbo : objects)
      {
        if (!dbo.supportsGetSource())
        {
          setEnabled(false);
          return;
        }
      }
      setEnabled(true);
    }
    else
    {
      setEnabled(false);
    }
  }

  @Override
  public void selectionChanged(WbSelectionModel source)
  {
    checkEnabled();
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
