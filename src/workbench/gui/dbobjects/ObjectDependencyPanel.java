/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import workbench.interfaces.Reloadable;
import workbench.resource.ResourceMgr;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.db.dependency.DependencyReader;
import workbench.db.dependency.DependencyReaderFactory;

import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.StopAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;

import workbench.storage.DataStore;

import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectDependencyPanel
  extends JPanel
  implements Reloadable
{
  private DbObject currentObject;
  private JMenuItem selectTableItem;
  private ReloadAction reload;
  private StopAction cancelAction;

	private WbConnection dbConnection;
  private DependencyReader reader;

  private JScrollPane usedScroll;
  private WbTable objectsUsed;
  private WbTable usedByObjects;

	private boolean isRetrieving;
  private int labelHeight;
  private WbSplitPane split;

  public ObjectDependencyPanel()
  {
    super(new BorderLayout());
    objectsUsed = new WbTable(false, false, false);
    usedByObjects = new WbTable(false, false, false);

    split = new WbSplitPane(JSplitPane.VERTICAL_SPLIT);

    JPanel usesPanel = new JPanel(new BorderLayout());
    usesPanel.add(createTitleLabel("TxtDepsUses"), BorderLayout.PAGE_START);
    usedScroll = new JScrollPane(objectsUsed);
    usesPanel.add(usedScroll, BorderLayout.CENTER);
    split.setTopComponent(usesPanel);

    JPanel usingPanel = new JPanel(new BorderLayout());
    JLabel lbl = createTitleLabel("TxtDepsUsedBy");
    labelHeight = lbl.getPreferredSize().height;

    usingPanel.add(lbl, BorderLayout.PAGE_START);
    JScrollPane scroll2 = new JScrollPane(usedByObjects);
    usingPanel.add(scroll2, BorderLayout.CENTER);
    split.setBottomComponent(usingPanel);
    split.setDividerLocation(150);
    split.setDividerBorder(new EmptyBorder(0, 0, 0, 0));

    add(split, BorderLayout.CENTER);

    reload = new ReloadAction(this);
    WbToolbar toolbar = new WbToolbar();
    toolbar.add(reload);
//    toolbar.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    add(toolbar, BorderLayout.PAGE_START);
  }

  private JLabel createTitleLabel(String key)
  {
    JLabel title = new JLabel(ResourceMgr.getString(key));
    title.setOpaque(true);
    title.setBackground(Color.WHITE);
    Font f = title.getFont();
    Font f2 = f.deriveFont(Font.BOLD);
    //title.setBorder();
    title.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(5, 2, 5, 6)));
    title.setFont(f2);
    return title;
  }

  public void dispose()
  {
    if (objectsUsed != null) objectsUsed.dispose();
    if (usedByObjects != null) usedByObjects.dispose();
  }

  public void setCurrentObject(DbObject object)
  {
    currentObject = object;
    reset();
  }

  public void setConnection(WbConnection conn)
  {
    reset();
    dbConnection = conn;
    reader = DependencyReaderFactory.getReader(dbConnection);
  }

  public void reset()
  {
    objectsUsed.reset();
    usedByObjects.reset();
  }

  public void cancel()
  {
  }

  @Override
  public void reload()
  {
    reset();
    WbThread loader = new WbThread(new Runnable()
    {
      @Override
      public void run()
      {
        doLoad();
      }
    }, "DependencyLoader Thread");
    loader.start();
  }

  public void doLoad()
  {
    if (reader == null) return;
    if (isRetrieving) return;

    try
    {
      isRetrieving = true;
      final List<DbObject> using = reader.getUsedBy(dbConnection, currentObject);
      EventQueue.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          showResult(using, objectsUsed);
        }
      });

      List<DbObject> used = reader.getUsedObjects(dbConnection, currentObject);

      EventQueue.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          showResult(used, usedByObjects);
          invalidate();
        }
      });

      EventQueue.invokeLater(this::calculateSplit);
    }
    finally
    {
      isRetrieving = false;
    }
  }

  private void calculateSplit()
  {
    invalidate();
    int rows = Math.max(objectsUsed.getRowCount() + 2, 5);
    int height = (int)((objectsUsed.getRowHeight() * rows) * 1.10);
    int minHeight = (int)getHeight() / 5;
    split.setDividerLocation(Math.max(minHeight, height));
    doLayout();
  }

  private void showResult(List<DbObject> objects, WbTable display)
  {
    DataStore ds = dbConnection.getMetadata().createTableListDataStore();
    for (DbObject dbo : objects)
    {
      int row = ds.addRow();
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, dbo.getCatalog());
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, dbo.getSchema());
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, dbo.getObjectName());
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, dbo.getObjectType());
      ds.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, dbo.getComment());
    }
    ds.resetStatus();
    DataStoreTableModel model = new DataStoreTableModel(ds);
    display.setModel(model, true);
  }

}
