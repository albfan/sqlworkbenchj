/*
 * DbExplorerWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.gui.dbobjects.objecttree;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.actions.CollapseTreeAction;
import workbench.gui.actions.ExpandTreeAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.MultiSelectComboBox;
import workbench.gui.components.WbStatusLabel;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;

import workbench.util.CollectionUtil;
import workbench.util.WbThread;



/**
 *
 * @author  Thomas Kellerer
 */
public class DbTreePanel
	extends JPanel
  implements Reloadable
{
  public static final String SETTINGS_PREFIX = "workbench.gui.mainwindow.dbtree.";
  
  private static int instanceCount = 0;
	private DbObjectsTree tree;
  private int id;
  private WbConnection connection;
  private WbStatusLabel statusBar;
  private MultiSelectComboBox typeFilter;
  private List<String> selectedTypes;
  private JPanel toolPanel;
  private ReloadAction reload;
  private WbToolbarButton closeButton;

	public DbTreePanel()
	{
		super(new BorderLayout());
    id = ++instanceCount;

    tree = new DbObjectsTree();
    JScrollPane scroll = new JScrollPane(tree);
    statusBar = new WbStatusLabel();
    createToolbar();

    add(toolPanel, BorderLayout.PAGE_START);
    add(scroll, BorderLayout.CENTER);
    add(statusBar, BorderLayout.PAGE_END);

    selectedTypes = Settings.getInstance().getListProperty(SETTINGS_PREFIX + "selectedtypes", false);
	}

  private void createToolbar()
  {
    toolPanel = new JPanel(new GridBagLayout());
    typeFilter = new MultiSelectComboBox<>();
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.anchor = GridBagConstraints.LINE_START;
    toolPanel.add(typeFilter, gc);

    reload = new ReloadAction(this);

    WbToolbar bar = new WbToolbar();
    bar.add(reload);
    bar.addSeparator();
    bar.add(new ExpandTreeAction(tree));
    bar.add(new CollapseTreeAction(tree));
    bar.addSeparator();
    WbAction closeAction = new WbAction("close-panel");
    closeAction.setIcon("close-panel");
    bar.add(closeAction);
    gc.gridx ++;
    gc.weightx = 1.0;
    gc.anchor = GridBagConstraints.LINE_END;
    toolPanel.add(bar, gc);
  }

  @Override
  public void reload()
  {
  }

  public void connect(final ConnectionProfile profile)
  {
    WbThread th = new WbThread(new Runnable()
    {
      @Override
      public void run()
      {
        doConnect(profile);
      }
    }, "DbTree Connect Thread");
    th.start();
  }

  private void doConnect(ConnectionProfile profile)
  {
    String cid = "DbTree-" + Integer.toString(id);

  	statusBar.setStatusMessage(ResourceMgr.getString("MsgConnectingTo") + " " + profile.getName() + " ...");

		ConnectionMgr mgr = ConnectionMgr.getInstance();
		try
		{
			connection = mgr.getConnection(profile, cid);
      tree.setConnection(connection);
      loadTypes();
      tree.load();
		}
    catch (Throwable th)
    {
      LogMgr.logError("DbTreePanel.connect()", "Could not connect", th);
    }
		finally
		{
			statusBar.clearStatusMessage();
		}
  }

  private void loadTypes()
  {
    List<String> types = new ArrayList<>(connection.getMetadata().getObjectTypes());
    List<String> toSelect = selectedTypes;
    if (CollectionUtil.isEmpty(toSelect))
    {
      toSelect = types;
    }
    typeFilter.setItems(types, toSelect);

  }

  public void dispose()
  {
    tree.clear();
  }

  public void load()
  {
    tree.load();
  }

	public void saveSettings()
	{
	}

  public void restoreSettings()
  {
  }

  public void disconnect(boolean wait)
  {
    if (tree != null)
    {
      tree.setConnection(null);
    }
    WbThread th = new WbThread(new Runnable()
    {
      @Override
      public void run()
      {
        ConnectionMgr.getInstance().disconnect(connection);
      }
    }, "Disconnect");

    if (wait)
    {
      th.run();
    }
    else
    {
      th.start();
    }
  }

  @Override
  public boolean requestFocusInWindow()
  {
    return tree.requestFocusInWindow();
  }

}
