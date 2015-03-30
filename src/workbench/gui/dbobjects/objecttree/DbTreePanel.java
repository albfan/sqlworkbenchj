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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import workbench.interfaces.ObjectDropListener;
import workbench.interfaces.Reloadable;
import workbench.interfaces.WbSelectionModel;
import workbench.log.LogMgr;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbObject;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CollapseTreeAction;
import workbench.gui.actions.ExpandTreeAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.MultiSelectComboBox;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbStatusLabel;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.dbobjects.DbObjectList;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;
import workbench.util.WbProperties;
import workbench.util.WbThread;



/**
 *
 * @author  Thomas Kellerer
 */
public class DbTreePanel
	extends JPanel
  implements Reloadable, ActionListener, MouseListener, DbObjectList, ObjectDropListener
{
  public static final String PROP_DIVIDER = "divider.location";
  public static final String PROP_VISIBLE = "tree.visible";
  public static final String PROP_TYPES = "tree.selectedtypes";

  private static int instanceCount = 0;
	private DbObjectsTree tree;
  private int id;
  private WbConnection connection;
  private WbStatusLabel statusBar;
  private MultiSelectComboBox<String> typeFilter;
  private List<String> selectedTypes;
  private JPanel toolPanel;
  private ReloadAction reload;
  private WbToolbarButton closeButton;

	public DbTreePanel()
	{
		super(new BorderLayout());
    id = ++instanceCount;

    statusBar = new WbStatusLabel();
    tree = new DbObjectsTree(statusBar);
    tree.addMouseListener(this);
    JScrollPane scroll = new JScrollPane(tree);
    createToolbar();

    add(toolPanel, BorderLayout.PAGE_START);
    add(scroll, BorderLayout.CENTER);
    add(statusBar, BorderLayout.PAGE_END);
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
        tree.reload();
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
      tree.load(true);
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
    tree.removeMouseListener(this);
    tree.clear();
  }

  private int getDividerLocation()
  {
    WbSplitPane split = (WbSplitPane)getParent();
    if (split == null) return -1;
    return split.getDividerLocation();
  }

	public void saveSettings(WbProperties props)
	{
    if (this.isVisible())
    {
      props.setProperty(PROP_DIVIDER, getDividerLocation());
    }
    List<String> types = typeFilter.getSelectedItems();
    props.setProperty(PROP_TYPES, StringUtil.listToString(types, ','));
	}

  public void restoreSettings(WbProperties props)
  {
    int location = props.getIntProperty(PROP_DIVIDER, -1);
    String typeString = props.getProperty(PROP_TYPES);
    selectedTypes = StringUtil.stringToList(typeString, ",", true, true, false);
    tree.setTypesToShow(selectedTypes);

    if (location > -1)
    {
      WbSplitPane split = (WbSplitPane)getParent();
      if (split != null)
      {
        split.setDividerLocation(location);
      }
    }
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

  protected ObjectTreeNode getSelectedNode()
  {
    TreePath p = tree.getSelectionPath();
    if (p == null) return null;

    ObjectTreeNode node = (ObjectTreeNode)p.getLastPathComponent();
    return node;
  }

  public WbSelectionModel getSelectionModel()
  {
    return WbSelectionModel.Factory.createFacade(tree.getSelectionModel());
  }

  @Override
  public void objectsDropped(List<DbObject> objects)
  {
    final Set<String> schemas = CollectionUtil.caseInsensitiveSet();
    for (DbObject dbo : objects)
    {
      schemas.add(dbo.getSchema());
    }
    statusBar.setStatusMessage(ResourceMgr.getString("MsgRetrieving"));
    WbThread th = new WbThread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          WbSwingUtilities.showWaitCursor(DbTreePanel.this);
          tree.reloadSchemas(schemas);
        }
        finally
        {
          WbSwingUtilities.showDefaultCursor(DbTreePanel.this);
          statusBar.clearStatusMessage();
        }
      }
    }, "Schema Reload");
    th.start();
  }

  @Override
  public int getSelectionCount()
  {
    return tree.getSelectionModel().getSelectionCount();
  }

  @Override
  public TableDefinition getCurrentTableDefinition()
  {
    ObjectTreeNode node = getSelectedNode();
    if (node == null) return null;

    DbObject dbo = node.getDbObject();
    if (!(dbo instanceof TableIdentifier)) return null;
    ObjectTreeNode columnNode = tree.findNodeByType(node, TreeLoader.TYPE_COLUMN_LIST);
    if (columnNode == null) return null;
    int childCount = columnNode.getChildCount();
    List<ColumnIdentifier> columns = new ArrayList<>(childCount);
    for (int i=0; i < childCount; i++)
    {
      ObjectTreeNode column = (ObjectTreeNode)columnNode.getChildAt(i);
      DbObject dboCol = column.getDbObject();
      if (dboCol instanceof ColumnIdentifier)
      {
        columns.add((ColumnIdentifier)dboCol);
      }
    }
    TableDefinition def = new TableDefinition((TableIdentifier)dbo, columns);
    return def;
  }

  @Override
  public TableIdentifier getObjectTable()
  {
    if (tree.getSelectionCount() != 1) return null;
    ObjectTreeNode node = getSelectedNode();
    if (node == null) return null;

    DbObject dbo = node.getDbObject();
    if (dbo instanceof TableIdentifier)
    {
      return (TableIdentifier)dbo;
    }
    return null;
  }

  @Override
  public List<DbObject> getSelectedObjects()
  {
    int count = tree.getSelectionCount();

    List<DbObject> result = new ArrayList<>(count);
    if (count == 0) return result;

    TreePath[] paths = tree.getSelectionPaths();
    for (TreePath path : paths)
    {
      if (path != null)
      {
        ObjectTreeNode node = (ObjectTreeNode)path.getLastPathComponent();
        if (node != null)
        {
          DbObject dbo = node.getDbObject();
          if  (dbo != null)
          {
            result.add(dbo);
          }
        }
      }
    }
    return result;
  }

  @Override
  public WbConnection getConnection()
  {
    return connection;
  }

  @Override
  public Component getComponent()
  {
    return tree;
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

  @Override
  public void mouseClicked(MouseEvent e)
  {
		if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1)
		{
			TreePath p = tree.getClosestPathForLocation(e.getX(), e.getY());
			if (p == null) return;

			if (tree.getSelectionCount() == 1)
			{
				tree.setSelectionPath(p);
			}

      JPopupMenu popup = ContextMenuFactory.createContextMenu(this, getSelectionModel());
      if (popup != null)
      {
        popup.show(tree, e.getX(), e.getY());
      }
		}
  }

  @Override
  public void mousePressed(MouseEvent e)
  {
  }

  @Override
  public void mouseReleased(MouseEvent e)
  {
  }

  @Override
  public void mouseEntered(MouseEvent e)
  {
  }

  @Override
  public void mouseExited(MouseEvent e)
  {
  }

}
