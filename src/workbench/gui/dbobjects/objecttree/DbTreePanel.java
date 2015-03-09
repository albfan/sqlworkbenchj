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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import workbench.interfaces.Reloadable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.actions.CollapseTreeAction;
import workbench.gui.actions.ExpandTreeAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.MultiSelectComboBox;
import workbench.gui.components.WbStatusLabel;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;
import workbench.resource.IconMgr;

import workbench.util.CollectionUtil;
import workbench.util.WbThread;



/**
 *
 * @author  Thomas Kellerer
 */
public class DbTreePanel
	extends JPanel
  implements Reloadable, ActionListener
{
  private static int instanceCount = 0;
	private DbObjectsTree tree;
  private int id;
  private WbConnection connection;
  private WbStatusLabel statusBar;
  private MultiSelectComboBox<String> typeFilter;
  private List<String> selectedTypes;
  private JPanel toolPanel;
  private ReloadAction reload;
  // private WbAction closeAction;
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

    selectedTypes = DbTreeSettings.getSelectedObjectTypes();
	}

  private void createToolbar()
  {
    toolPanel = new JPanel(new GridBagLayout());
    typeFilter = new MultiSelectComboBox<>();
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = 0;
    gc.weightx = 1.0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.LINE_START;
    toolPanel.add(typeFilter, gc);

    reload = new ReloadAction(this);

    WbToolbar bar = new WbToolbar();
    bar.add(reload);
    bar.addSeparator();
    ExpandTreeAction expand = new ExpandTreeAction(tree);
    bar.add(expand);
    bar.add(new CollapseTreeAction(tree));
    bar.addSeparator();
    gc.gridx ++;
    gc.weightx = 1.0;
    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.LINE_END;
    toolPanel.add(bar, gc);

    ImageIcon icon = IconMgr.getInstance().getLabelIcon("close-panel");
    closeButton = new WbToolbarButton(icon);
    closeButton.setActionCommand("close-panel");
    closeButton.addActionListener(this);
    closeButton.setRolloverEnabled(true);

    // calculate the regular size of a toolbarbutton.
    WbToolbarButton button = new WbToolbarButton(IconMgr.getInstance().getToolbarIcon("save"));
    Dimension bs = button.getPreferredSize();

    int iconWidth = icon.getIconWidth()/2;
    int iconHeight = icon.getIconHeight()/2;
    int wmargin = (int)(bs.width/2) - iconWidth - 2;
    int hmargin = (int)(bs.height/2) - iconHeight - 2;
    closeButton.setMargin(new Insets(hmargin, wmargin, hmargin, wmargin));
    bar.add(closeButton);
    typeFilter.addActionListener(this);
  }

  @Override
  public void reload()
  {
    WbThread th = new WbThread(new Runnable()
    {

      @Override
      public void run()
      {
        tree.load();
      }
    }, "DbTree Load Thread");
    th.start();
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
    try
    {
      typeFilter.removeActionListener(this);
      List<String> types = new ArrayList<>(connection.getMetadata().getObjectTypes());
      List<String> toSelect = selectedTypes;
      if (CollectionUtil.isEmpty(toSelect))
      {
        toSelect = types;
      }
      typeFilter.setItems(types, toSelect);
      typeFilter.setMaximumRowCount(Math.min(typeFilter.getItemCount() + 1, 25));
    }
    finally
    {
      typeFilter.addActionListener(this);
    }
  }

  public void dispose()
  {
    tree.clear();
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

    th.start();
    if (wait)
    {
      try
      {
        th.join();
      }
      catch (Exception ex)
      {
        LogMgr.logWarning("DbTreePanel.disconnect()", "Error waiting for disconnect thread", ex);
      }
    }
  }

  @Override
  public boolean requestFocusInWindow()
  {
    return tree.requestFocusInWindow();
  }

  private void closePanel()
  {
    Window frame = SwingUtilities.getWindowAncestor(this);
    if (frame instanceof MainWindow)
    {
      final MainWindow mainWin = (MainWindow) frame;
      EventQueue.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          mainWin.closeDbTree();
        }
      });
    }

  }
  @Override
  public void actionPerformed(ActionEvent evt)
  {
    if (evt.getSource() == closeButton)
    {
      closePanel();
    }
    if (evt.getSource() == typeFilter)
    {
      tree.setTypesToShow(typeFilter.getSelectedItems());
      reload();
    }
  }


}
