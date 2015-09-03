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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import workbench.interfaces.ObjectDropListener;
import workbench.interfaces.QuickFilter;
import workbench.interfaces.Reloadable;
import workbench.interfaces.StatusBar;
import workbench.interfaces.WbSelectionModel;
import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbObject;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.QuickFilterAction;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.MultiSelectComboBox;
import workbench.gui.components.WbLabelField;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbStatusLabel;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.dbobjects.DbObjectList;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbProperties;
import workbench.util.WbThread;

/**
 *
 * @author  Thomas Kellerer
 */
public class DbTreePanel
	extends JPanel
  implements Reloadable, ActionListener, MouseListener, DbObjectList,
             ObjectDropListener, KeyListener, QuickFilter, RowCountDisplay,
             ObjectFinder, TreeSelectionListener
{
  public static final String PROP_DIVIDER = "divider.location";
  public static final String PROP_VISIBLE = "tree.visible";
  public static final String PROP_TYPES = "tree.selectedtypes";

  private static int instanceCount = 0;
	private DbObjectsTree tree;
  private int id;
  private WbConnection connection;
  private WbStatusLabel statusBar;
  private WbLabelField currentSchemaLabel;
  private MultiSelectComboBox<String> typeFilter;
  private List<String> selectedTypes;
  private JPanel toolPanel;
  private ReloadAction reload;
  private WbToolbarButton closeButton;
  private JTextField filterValue;
  private QuickFilterAction filterAction;
  private WbAction resetFilter;
  private List<TreePath> expandedNodes;

	public DbTreePanel()
	{
		super(new BorderLayout());
    id = ++instanceCount;

    currentSchemaLabel = new WbLabelField();
    Border b = new CompoundBorder(new DividerBorder(DividerBorder.TOP), new EmptyBorder(2,2,2,0));
    currentSchemaLabel.setBorder(b);
    statusBar = new WbStatusLabel();
    tree = new DbObjectsTree(statusBar);
    tree.addMouseListener(this);
    tree.addTreeSelectionListener(this);
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
    reload.setUseLabelIconSize(true);

    WbToolbar bar = new WbToolbar();
    bar.add(reload);
    gc.gridx ++;
    gc.weightx = 0.0;
    gc.insets = new Insets(0, IconMgr.getInstance().getSizeForLabel(), 0, 0);
    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.LINE_END;
    toolPanel.add(bar, gc);

    ImageIcon icon = IconMgr.getInstance().getLabelIcon("close-panel");
    closeButton = new WbToolbarButton(icon);
    closeButton.setActionCommand("close-panel");
    closeButton.addActionListener(this);
    closeButton.setRolloverEnabled(true);

    // calculate the regular size of the buttons
    // as the toolbar also display a dropdown, I'm using the label size here
    WbToolbarButton button = new WbToolbarButton(IconMgr.getInstance().getLabelIcon("save"));
    Dimension bs = button.getPreferredSize();

    int iconWidth = icon.getIconWidth()/2;
    int iconHeight = icon.getIconHeight()/2;
    int wmargin = (int)(bs.width/2) - iconWidth - 2;
    int hmargin = (int)(bs.height/2) - iconHeight - 2;
    closeButton.setMargin(new Insets(hmargin, wmargin, hmargin, wmargin));
    bar.add(closeButton);
    typeFilter.addActionListener(this);

    JPanel filterPanel = new JPanel(new BorderLayout(0, 1));
    filterPanel.setBorder(new EmptyBorder(2,0,2,4));
    filterValue = new JTextField();

    filterAction = new QuickFilterAction(this);
    resetFilter = new WbAction()
    {
      @Override
      public void executeAction(ActionEvent e)
      {
        resetFilter();
      }
    };
    resetFilter.setIcon("resetfilter");
    resetFilter.setEnabled(false);

    WbToolbar filterBar = new WbToolbar();
    filterBar.add(filterAction);
    filterBar.add(resetFilter);
		filterBar.setMargin(WbSwingUtilities.EMPTY_INSETS);
		filterBar.setBorderPainted(true);

    filterPanel.add(filterBar, BorderLayout.LINE_START);
    filterPanel.add(filterValue, BorderLayout.CENTER);

    filterValue.addKeyListener(this);

    gc.gridy = 1;
    gc.gridx = 0;
    gc.weightx = 1.0;
    gc.insets = WbSwingUtilities.EMPTY_INSETS;
    gc.gridwidth = GridBagConstraints.REMAINDER;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.LINE_START;
    toolPanel.add(filterPanel, gc);

    gc.gridy ++;
    gc.insets = new Insets(4, 0, 0, 0);
    toolPanel.add(currentSchemaLabel, gc);
  }

  @Override
  public void reload()
  {
    resetExpanded();
    WbThread th = new WbThread("DbTree Load Thread")
    {
      @Override
      public void run()
      {
        tree.reload();
      }
    };
    th.start();
  }

  @Override
  public void selectObject(TableIdentifier tbl)
  {
    tree.selectObject(tbl);
  }

  public void connect(final ConnectionProfile profile)
  {
    if (profile == null) return;

    // do not create a new connection if we are already connected for this profile
    if (connection != null && connection.getProfile().equals(profile)) return;

    // if a new connection profile is specified, we need to disconnect the old connection
    final boolean doDisconnect = connection != null;

    WbThread th = new WbThread("DbTree Connect Thread")
    {
      @Override
      public void run()
      {
        if (doDisconnect)
        {
          disconnect(true);
        }
        doConnect(profile);
      }
    };
    th.start();
  }

  private void doConnect(ConnectionProfile profile)
  {
    String cid = "DbTree-" + Integer.toString(id);

    resetExpanded();
  	statusBar.setStatusMessage(ResourceMgr.getString("MsgConnectingTo") + " " + profile.getName() + " ...");

		ConnectionMgr mgr = ConnectionMgr.getInstance();
		try
		{
			connection = mgr.getConnection(profile, cid);
      JdbcUtils.initDbExplorerConnection(connection);
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
      types.add("PROCEDURE");
      if (DbExplorerSettings.getShowTriggerPanel())
      {
        types.add("TRIGGER");
      }
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

  public void reloadSelectedNodes()
  {
    List<ObjectTreeNode> nodes = getSelectedNodes();
    if (nodes.isEmpty()) return;

    try
    {
      statusBar.setStatusMessage(ResourceMgr.getString("MsgRetrieving"));
      WbSwingUtilities.showWaitCursor(this);
      for (ObjectTreeNode node : nodes)
      {
        try
        {
          tree.reloadNode(node);
        }
        catch (SQLException ex)
        {
          WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(ex));
          LogMgr.logError("DbTreePanel.reloadSelectedNodes()", "Could not load node " + node.getType() + " - " + node.getName(), ex);
        }
      }
    }
    finally
    {
      statusBar.clearStatusMessage();
      WbSwingUtilities.showDefaultCursor(this);
    }
  }

  public void dispose()
  {
    resetExpanded();
    tree.removeMouseListener(this);
    tree.clear();
  }

  public StatusBar getStatusBar()
  {
    return statusBar;
  }

	public void saveSettings(WbProperties props)
	{
    if (this.isVisible())
    {
      props.setProperty(PROP_DIVIDER + "." + getCurrentPosition().name(), getDividerLocation());
    }
    List<String> types = typeFilter.getSelectedItems();
    props.setProperty(PROP_TYPES, StringUtil.listToString(types, ','));
	}

  private TreePosition getCurrentPosition()
  {
    WbSplitPane split = (WbSplitPane)getParent();
    if (split == null) return DbTreeSettings.getDbTreePosition();
    if (split.getLeftComponent() == this) return TreePosition.left;
    return TreePosition.right;
  }

  public void restoreSettings(WbProperties props)
  {
    int location = props.getIntProperty(PROP_DIVIDER + "." + getCurrentPosition().name(), -1);
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

  /**
   * Close the connection used by this panel in a background thread.
   *
   * This method will return immediately, the physical closing of the connection is done in a background thread.
   */
  public void disconnectInBackground()
  {
    disconnect(false);
  }

  /**
   * Close the connection used by this panel.
   *
   * This method will wait until the connection is physically closed.
   */
  public void disconnect()
  {
    disconnect(true);
  }

  private void disconnect(boolean wait)
  {
    if (tree != null)
    {
      tree.clear();
      tree.setConnection(null);
    }

    Runnable runner = () ->
    {
      WbConnection old = connection;
      connection = null;
      ConnectionMgr.getInstance().disconnect(old);
    };

    if (wait)
    {
      runner.run();
    }
    else
    {
      WbThread th = new WbThread(runner, "Disconnect");
      th.start();
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
    List<TreePath> nodes = tree.getExpandedNodes();

    // removeObjects will reset any filter, so we need to re-apply it
    // after the objects have been removed
    boolean isFiltered = tree.getModel().getFilteredNodes().size() > 0;

    tree.getModel().removeObjects(objects);

    if (isFiltered)
    {
      applyQuickFilter();
    }
    tree.expandNodes(nodes);
  }

  @Override
  public int getSelectionCount()
  {
    List<DbObject> selected = getSelectedObjects();
    return selected == null ? 0 : selected.size();
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
    if (tree.getSelectionCount() <= 0) return null;

    if (tree.getSelectionCount() == 1)
    {
      ObjectTreeNode node = getSelectedNode();
      if (node == null) return null;

      DbObject dbo = node.getDbObject();
      if (dbo instanceof TableIdentifier)
      {
        return (TableIdentifier)dbo;
      }
    }

    // Multiple objects are selected. Returning the "object table"
    // only makes sense if all of them are columns of the same table
    List<ObjectTreeNode> nodes = getSelectedNodes();
    ObjectTreeNode firstParent = nodes.get(0).getParent();

    int colCount = 0;
    // if all nodes have the same parent this might be a selection of multiple columns of the same table
    for (ObjectTreeNode node : nodes)
    {
      if (node.getParent() != firstParent) return null;
      if (node.getDbObject() instanceof ColumnIdentifier)
      {
        colCount ++;
      }
    }

    // not all selected objects are columns
    if (colCount != nodes.size()) return null;

    // when we wind up here, all nodes belong to the same parent and are columns
    // so the parent of the first parent must be the table to which these columns belong to
    ObjectTreeNode tableNode = firstParent.getParent();

    // the parent could be an index as well so we need to check the type of the DbObject
    if (tableNode.getDbObject() instanceof TableIdentifier)
    {
      return (TableIdentifier)tableNode.getDbObject();
    }
    return null;
  }

  public List<ObjectTreeNode> getSelectedNodes()
  {
    int count = tree.getSelectionCount();

    List<ObjectTreeNode> result = new ArrayList<>(count);
    if (count == 0) return result;

    TreePath[] paths = tree.getSelectionPaths();
    for (TreePath path : paths)
    {
      if (path != null)
      {
        ObjectTreeNode node = (ObjectTreeNode)path.getLastPathComponent();
        if (node != null)
        {
          result.add(node);
        }
      }
    }
    return result;
  }

  @Override
  public List<DbObject> getSelectedObjects()
  {
    List<ObjectTreeNode> nodes = getSelectedNodes();

    int count = nodes.size();
    List<DbObject> result = new ArrayList<>(count);
    if (count == 0) return result;

    for (ObjectTreeNode node : nodes)
    {
      DbObject dbo = node.getDbObject();
      if  (dbo != null)
      {
        result.add(dbo);
      }
    }
    return result;
  }

  public List<TableIdentifier> getSelectedTables()
  {
    List<ObjectTreeNode> nodes = getSelectedTableNodes();

    int count = nodes.size();
    List<TableIdentifier> result = new ArrayList<>(count);
    if (count == 0) return result;

 		Set<String> typesWithData = getConnection().getMetadata().getObjectsWithData();
    for (ObjectTreeNode node : nodes)
    {
      DbObject dbo = node.getDbObject();
      if (dbo instanceof TableIdentifier && typesWithData.contains(dbo.getObjectType()))
      {
        result.add((TableIdentifier)dbo);
      }
    }
    return result;
  }

  public List<ObjectTreeNode> getSelectedTableNodes()
  {
    int count = tree.getSelectionCount();

    List<ObjectTreeNode> result = new ArrayList<>(count);
    if (count == 0) return result;

    Set<String> nodeTypesWithTables = CollectionUtil.caseInsensitiveSet(TreeLoader.TYPE_TABLE, TreeLoader.TYPE_VIEW);
    TreePath[] paths = tree.getSelectionPaths();

    for (TreePath path : paths)
    {
      if (path != null)
      {
        ObjectTreeNode node = (ObjectTreeNode)path.getLastPathComponent();
        if (node != null)
        {
          if (TreeLoader.TYPE_DBO_TYPE_NODE.equalsIgnoreCase(node.getType()) && nodeTypesWithTables.contains(node.getName()))
          {
            result.addAll(getTableNodes(node));
          }
          else if (TreeLoader.TYPE_SCHEMA.equalsIgnoreCase(node.getType()))
          {
            result.addAll(getSchemaTableNodes(node));
          }
          result.add(node);
        }
      }
    }
    return result;
  }

  private List<ObjectTreeNode> getSchemaTableNodes(ObjectTreeNode schemaNode)
  {
    List<ObjectTreeNode> result = new ArrayList<>();
    Set<String> nodeTypesWithTables = CollectionUtil.caseInsensitiveSet(TreeLoader.TYPE_TABLE, TreeLoader.TYPE_VIEW);
    int count = schemaNode.getChildCount();
    for (int i=0;  i < count; i++)
    {
      ObjectTreeNode child = schemaNode.getChildAt(i);
      if (TreeLoader.TYPE_DBO_TYPE_NODE.equalsIgnoreCase(child.getType()) && nodeTypesWithTables.contains(child.getName()))
      {
        result.addAll(getTableNodes(child));
      }
    }
    return result;
  }

  private List<ObjectTreeNode> getTableNodes(ObjectTreeNode node)
  {
    int count = node.getChildCount();
    List<ObjectTreeNode> tables = new ArrayList<>(count);
    for (int i=0; i < count; i++)
    {
      ObjectTreeNode child = node.getChildAt(i);
      if (child == null) continue;
      if (child.getDbObject() instanceof TableIdentifier)
      {
        tables.add(child);
      }
    }
    return tables;
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
      EventQueue.invokeLater(mainWin::closeDbTree);
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
    if (this.getConnection() == null) return;

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

  private boolean isEditKey(KeyEvent event)
  {
    int key = event.getKeyChar();
    switch (key)
    {
      case KeyEvent.VK_BACK_SPACE:
      case KeyEvent.VK_DELETE:
      case KeyEvent.VK_CUT:
      case KeyEvent.VK_INSERT:
      case KeyEvent.VK_CLEAR:
        return true;
      default:
        return false;
    }
  }

	@Override
	public void keyTyped(final KeyEvent e)
	{
    if (e.isConsumed()) return;
    if (Character.isISOControl(e.getKeyChar()) && isEditKey(e) == false) return;

    if (DbTreeSettings.getFilterWhileTyping())
    {
      EventQueue.invokeLater(this::applyQuickFilter);
    }
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getSource() != this.filterValue || e.getModifiers() != 0) return;

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_UP:
				selectPreviousItem();
				e.consume();
				break;
			case KeyEvent.VK_DOWN:
				selectNextItem();
				e.consume();
				break;
			case KeyEvent.VK_ENTER:
        e.consume();
        applyQuickFilter();
        break;
			case KeyEvent.VK_ESCAPE:
        e.consume();
        resetFilter();
        break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

  @Override
  public void resetFilter()
  {
    List<TreePath> expanded = expandedNodes;
    if (expanded == null)
    {
      expanded = tree.getExpandedNodes();
    }

    tree.getModel().resetFilter();
    tree.expandNodes(expanded);
    resetFilter.setEnabled(false);
    resetExpanded();
  }

  @Override
	public synchronized void applyQuickFilter()
	{
    String text = filterValue.getText();
    if (StringUtil.isEmptyString(text))
    {
      resetFilter();
      return;
    }

    if (expandedNodes == null)
    {
      // first invocation, save the currently expanded nodes
      // so that this can be restored when the filter is cleared
      expandedNodes = tree.getExpandedNodes();
    }

    tree.getModel().applyFilter(text);
    resetFilter.setEnabled(true);
    List<TreePath> expanded = null;
    if (DbTreeSettings.autoExpandFilteredNodes())
    {
      expanded = tree.getFilteredNodes();
    }
    else
    {
      expanded = tree.getExpandedNodes();
    }
    tree.expandNodes(expanded);
	}

  @Override
  public void showRowCount(TableIdentifier table, long rows)
  {
    if (table == null) return;

    // The show row count action can only be invoked if something is selected
    // Therefor it's enough to loop through the selected nodes.
    List<ObjectTreeNode> nodes = getSelectedTableNodes();
    for (final ObjectTreeNode node : nodes)
    {
      if (table.equals(node.getDbObject()))
      {
        node.setRowCount(Long.valueOf(rows));
        EventQueue.invokeLater(() ->
        {
          tree.getModel().nodeChanged(node);
        });
      }
    }
  }

  @Override
  public void valueChanged(TreeSelectionEvent e)
  {
    currentSchemaLabel.setText(tree.getSelectedNamespace());
  }

  private void resetExpanded()
  {
    if (expandedNodes != null)
    {
      expandedNodes.clear();
      expandedNodes = null;
    }
  }

  private int getDividerLocation()
  {
    WbSplitPane split = (WbSplitPane)getParent();
    if (split == null)
    {
      return -1;
    }
    return split.getDividerLocation();
  }

	private int getSelectedRow()
	{
		if (tree.getSelectionCount() != 1) return -1;
		return tree.getSelectionRows()[0];
	}

	private void selectPreviousItem()
	{
		int row = getSelectedRow();
		if (row < 0)
		{
			row = 0;
		}
		else if (row > 0)
		{
			row --;
		}
		tree.setSelectionRow(row);
	}

	private void selectNextItem()
	{
		int row = getSelectedRow();
		int count = tree.getRowCount();
		if (row < 0)
		{
			row = 0;
		}
		else if (row < count - 1)
		{
			row ++;
		}
		tree.setSelectionRow(row);
	}

}
