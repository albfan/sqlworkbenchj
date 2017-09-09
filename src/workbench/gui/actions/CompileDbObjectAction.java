/*
 * CompileDbObjectAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import workbench.interfaces.WbSelectionListener;
import workbench.interfaces.WbSelectionModel;
import workbench.log.LogMgr;

import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleObjectCompiler;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectCompilerUI;

import workbench.util.CollectionUtil;

/**
 * @author Thomas Kellerer
 */
public class CompileDbObjectAction
  extends WbAction
  implements WbSelectionListener
{
  private JMenuItem menuItem;
  private DbObjectList source;
  private WbSelectionModel selection;

  public CompileDbObjectAction(DbObjectList client, WbSelectionModel list)
  {
    super();
    this.initMenuDefinition("MnuTxtRecompile");
    this.source = client;
    this.selection = list;
    setVisible(false);
    checkState();
  }

  public void setVisible(boolean flag)
  {
    if (this.menuItem == null)
    {
      menuItem = getMenuItem();
    }
    menuItem.setVisible(flag);
  }

  public void setConnection(WbConnection conn)
  {
    if (conn != null && conn.getMetadata().isOracle())
    {
      setVisible(true);
      selection.addSelectionListener(this);
      checkState();
    }
    else
    {
      selection.removeSelectionListener(this);
      setVisible(false);
      setEnabled(false);
    }
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    compileObjects();
  }

  private void compileObjects()
  {
    if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection())) return;

    List<DbObject> objects = getSelectedObjects();
    if (CollectionUtil.isEmpty(objects)) return;

    try
    {
      ObjectCompilerUI compilerUI = new ObjectCompilerUI(objects, this.source.getConnection());
      compilerUI.show(SwingUtilities.getWindowAncestor(source.getComponent()));
    }
    catch (SQLException e)
    {
      LogMgr.logError("ProcedureListPanel.compileObjects()", "Error initializing ObjectCompilerUI", e);
    }
  }

  private List<DbObject> getSelectedObjects()
  {
    List<? extends DbObject> selected = this.source.getSelectedObjects();
    if (CollectionUtil.isEmpty(selected)) return null;

    Set<String> packageNames = CollectionUtil.caseInsensitiveSet();
    List<DbObject> objects = new ArrayList<>();

    for (DbObject dbo : selected)
    {
      if (!OracleObjectCompiler.canCompile(dbo)) continue;

      if (dbo instanceof ProcedureDefinition)
      {
        ProcedureDefinition pd = (ProcedureDefinition)dbo;
        if (pd.isPackageProcedure())
        {
          boolean added = packageNames.add(pd.getPackageName());
          if (!added)
          {
            // the package was already processed and at least one
            // procedure is therefor part of the list of objects
            continue;
          }
        }
      }
      objects.add(dbo);
    }

    return objects;
  }

  private void checkState()
  {
    List<DbObject> selected = getSelectedObjects();
    this.setEnabled(CollectionUtil.isNonEmpty(selected));
  }

  @Override
  public void selectionChanged(WbSelectionModel source)
  {
    EventQueue.invokeLater(this::checkState);
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
