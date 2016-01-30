/*
 * TableSearchPanel.java
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
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.DbExecutionNotifier;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.ShareableDisplay;
import workbench.interfaces.TableSearchConsumer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.JdbcUtils;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.search.TableDataSearcher;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.EmptyTableModel;
import workbench.gui.components.FlatButton;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbStatusLabel;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.renderer.RendererSetup;
import workbench.gui.sql.EditorPanel;

import workbench.storage.DataStore;

import workbench.util.FilteredProperties;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.WbWorkspace;


/**
 * A display for the results of a {@link workbench.db.search.TableDataSearcher}
 *
 * @author Thomas Kellerer
 */
public class TableSearchPanel
	extends JPanel
	implements TableSearchConsumer, ListSelectionListener, KeyListener, DbExecutionNotifier
{
	private TableModel tableListModel;
	private TableDataSearcher searcher;
	private WbConnection connection;
	private String fixedStatusText;
	private ShareableDisplay tableListSource;
	private EditorPanel sqlDisplay;
	private JButton startButton;
	private List<DbExecutionListener> execListener;
	private ClientSideTableSearchPanel clientSearcherCriteria;
	private ServerSideTableSearchPanel serverSearcherCriteria;

	private boolean initialized;
	private FilteredProperties workspaceSettings;

	private int tableCount;

	public TableSearchPanel(ShareableDisplay source)
	{
		super();
		this.tableListSource = source;
	}

	private void initGui()
	{
		if (initialized) return;

		WbSwingUtilities.invoke(this::_initGui);
	}

	private void _initGui()
	{
		if (initialized) return;

		this.tableListModel = EmptyTableModel.EMPTY_MODEL;
		initComponents();

		JScrollBar sb = this.resultScrollPane.getVerticalScrollBar();
		sb.setUnitIncrement(25); // approx. one line
		sb.setBlockIncrement(25 * 5); // approx. 5 lines

		sqlDisplay = EditorPanel.createSqlEditor();
		this.resultTabPane.addTab(ResourceMgr.getString("LblTableSearchSqlLog"), sqlDisplay);

		WbTable tables = (WbTable)this.tableNames;
		tables.setAdjustToColumnLabel(false);

		WbToolbar toolbar = new WbToolbar();

		WbAction reload = new ReloadAction(this.tableListSource);
    reload.setUseLabelIconSize(true);
		reload.setTooltip(ResourceMgr.getString("TxtRefreshTableList"));
		toolbar.add(reload);
		buttonPanel.add(toolbar);

		startButton = new JButton();
		startButton.setText(ResourceMgr.getString("LblStartSearch"));
		startButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				startSearch();
			}
		});
		buttonPanel.add(startButton);

		this.tableNames.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		((WbTable)tableNames).setRendererSetup(RendererSetup.getBaseSetup());
		this.fixedStatusText = ResourceMgr.getString("TxtSearchingTable") + " ";
		tables.getSelectionModel().addListSelectionListener(this);
		this.startButton.setEnabled(false);

		CompoundBorder infoBorder = new CompoundBorder(this.statusInfo.getBorder(), new EmptyBorder(0,2,0,0));
		this.statusInfo.setBorder(infoBorder);
		CompoundBorder entryBorder = new CompoundBorder(new DividerBorder(DividerBorder.BOTTOM), new EmptyBorder(2,0,3,0));
		entryPanel.setBorder(entryBorder);
		initCriteriaPanel();
		initialized = true;

		restoreSettings();

		if (workspaceSettings != null)
		{
			restoreSettings(workspaceSettings.getFilterPrefix(), workspaceSettings);
			workspaceSettings = null;
		}
		setConnection(connection);
	}

	private TableSearchCriteriaGUI getCriteriaPanel()
	{
		return (TableSearchCriteriaGUI)criteriaContainer.getComponent(0);
	}

	private void initCriteriaPanel()
	{
		serverSearcherCriteria = new ServerSideTableSearchPanel();
		serverSearcherCriteria.addKeyListenerForCriteria(this);
		clientSearcherCriteria = new ClientSideTableSearchPanel();
		clientSearcherCriteria.addKeyListenerForCriteria(this);
	}

	private void showTableSearcherCriteria()
	{
		criteriaContainer.removeAll();
		if (serverSideSearch.isSelected())
		{
			clientSearcherCriteria.setVisible(false);
			criteriaContainer.add(serverSearcherCriteria, BorderLayout.CENTER, 0);
			serverSearcherCriteria.setVisible(true);
		}
		else
		{
			serverSearcherCriteria.setVisible(false);
			criteriaContainer.add(clientSearcherCriteria, BorderLayout.CENTER, 0);
			clientSearcherCriteria.setVisible(true);
		}
		WbSwingUtilities.repaintLater(criteriaContainer);
	}

	private void startSearch()
	{
		if (searcher != null && this.searcher.isRunning())
		{
			startButton.setEnabled(false);
			this.searcher.cancelSearch();
		}
		else
		{
			this.searchData();
		}
	}

	@Override
	public void tableSearched(final TableIdentifier table, final DataStore result)
	{
		if (result.getRowCount() == 0)
		{
			return;
		}

		tableCount ++;

		// Make sure everything happens on the EDT thread
		EventQueue.invokeLater(() ->
    {
      try
      {
        WbTable display = new WbTable(true, true, false);
        DataStoreTableModel model = new DataStoreTableModel(result);
        display.setModel(model, true);
        display.applyHighlightExpression(searcher.getSearchExpression());
        display.checkCopyActions();
        JScrollPane pane = new ParentWidthScrollPane(display);
        int rows = display.getRowCount();
        String label = table.getTableExpression() + " " + ResourceMgr.getFormattedString("MsgRows", rows);
        TitledBorder b = new TitledBorder(" " + label);
        pane.setBorder(b);
        Font f = b.getTitleFont();
        if (f == null)
        {
          // With JDK 7, getTitleFont() seems to return null...
          UIDefaults def = UIManager.getDefaults();
          f = def.getFont("Label.font");
        }
        // Check for != null again - just to make sure.
        // Because if a NPE is thrown here, nothing will be shown to the user
        if (f != null)
        {
          f = f.deriveFont(Font.BOLD);
          b.setTitleFont(f);
        }
        // only the last component should have weighty = 1.0
        // so reset the weighty attribute for the component that is currently the last one
        int count = resultPanel.getComponentCount();
        if (count > 0)
        {
          Component comp = resultPanel.getComponent(count - 1);
          GridBagLayout layout = (GridBagLayout)resultPanel.getLayout();
          GridBagConstraints prev = layout.getConstraints(comp);
          prev.weighty = 0;
          layout.setConstraints(comp, prev);
        }
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        resultPanel.add(pane, constraints);
        int height1 = display.getRowHeight();
        int width1 = pane.getWidth();
        Dimension size = pane.getPreferredSize();
        if (rows > 25)
        {
          rows = 25;
        }
        size.setSize(width1 - 20, (rows + 4) * height1);
        pane.setPreferredSize(size);
      }catch (Exception e)
      {
        LogMgr.logError("TableSearchPanel.tableSearched()", "Error adding result.", e);
      }
    });
	}

	@Override
	public void error(final String msg)
	{
		EventQueue.invokeLater(() ->
    {
      sqlDisplay.appendLine(msg);
      sqlDisplay.appendLine("\n\n");
    });
	}

	/**
	 *	Call back function from the table searcher...
	 */
	@Override
	public synchronized void setCurrentTable(final String table, final String sql, final long currentObject, final long totalObjects)
	{
		EventQueue.invokeLater(() ->
    {
      if (sql == null)
      {
        String msg = ResourceMgr.getFormattedString("MsgNoCharCols", table);
        sqlDisplay.appendLine("-- " + msg);
      }
      else
      {
        StringBuilder info = new StringBuilder(fixedStatusText.length() + 25);
        info.append(fixedStatusText); // the text already contains a trailing space
        info.append(table);
        info.append(" (");
        info.append(NumberStringCache.getNumberString(currentObject));
        info.append('/');
        info.append(NumberStringCache.getNumberString(totalObjects));
        info.append(')');
        statusInfo.setText(info.toString());
        sqlDisplay.appendLine(sql + ";");
      }
      sqlDisplay.appendLine("\n\n");
    });
	}

	@Override
	public void setStatusText(String statusText)
	{
		this.statusInfo.setText(statusText);
	}

	public void setConnection(WbConnection connection)
	{
		this.connection = connection;
		if (tableNames != null)
		{
			this.tableListSource.addTableListDisplayClient(this.tableNames);
		}
	}

	public void disconnect()
	{
		this.reset();
		this.tableListSource.removeTableListDisplayClient(this.tableNames);
	}

	@Override
	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag)
		{
			initGui();
		}
	}

	public void reset()
	{
		if (!initialized) return;

    // resultPanel.removeAll() does not work properly for some reason
    // the old tables just stay in there so I re-create the actual result panel
		WbSwingUtilities.invoke(() ->
    {
      resultPanel = new JPanel(new GridBagLayout());
      resultScrollPane.setViewportView(resultPanel);
      sqlDisplay.setText("");
      setStatusText("");
    });
	}

	public void searchData()
	{
		if (searcher != null && searcher.isRunning()) return;

		if (!WbSwingUtilities.isConnectionIdle(this, connection)) return;

		if (this.tableNames.getSelectedRowCount() == 0) return;

		if (Settings.getInstance().getBoolProperty("workbench.searchdata.warn.buffer", true))
		{
			if (!serverSideSearch.isSelected() && JdbcUtils.driverMightBufferResults(connection))
			{
				boolean goOn = WbSwingUtilities.getYesNo(this, ResourceMgr.getString("MsgTableSearchBuffered"));
				if (!goOn) return;
			}
		}

		this.reset();

		int[] selectedTables = this.tableNames.getSelectedRows();

		List<TableIdentifier> searchTables = new ArrayList<>(this.tableNames.getSelectedRowCount());
		DataStore tables = ((WbTable)(this.tableNames)).getDataStore();
		for (int i=0; i < selectedTables.length; i++)
		{
			String catalog = tables.getValueAsString(selectedTables[i], DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
			String schema = tables.getValueAsString(selectedTables[i], DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			String tablename = tables.getValueAsString(selectedTables[i], DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String type = tables.getValueAsString(selectedTables[i], DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);

			TableIdentifier tbl = new TableIdentifier(catalog, schema, tablename);
			tbl.setNeverAdjustCase(true);
			tbl.setType(type);
			searchTables.add(tbl);
		}

		int maxRows = StringUtil.getIntValue(this.rowCount.getText(), 0);

		searcher = getCriteriaPanel().getSearcher();
		searcher.setConnection(this.connection);
		searcher.setConsumer(this);
		searcher.setMaxRows(maxRows);
		searcher.setRetrieveLobColumns(!excludeClobs.isSelected());
		searcher.setTableNames(searchTables);
		fireDbExecStart();
		searcher.startBackgroundSearch();
	}

	private String getWorkspacePrefix(int index)
	{
		return "dbexplorer" + index + ".tablesearcher";
	}

	public void saveToWorkspace(WbWorkspace wb, int index)
	{
		saveSettings(getWorkspacePrefix(index), wb.getSettings());
	}

	public void readFromWorkspace(WbWorkspace wb, int index)
	{
		restoreSettings(getWorkspacePrefix(index), wb.getSettings());
	}

	public void saveSettings()
	{
		saveSettings(this.getClass().getName(), Settings.getInstance());
	}

	private void saveSettings(String prefix, PropertyStorage props)
	{
		if (initialized)
		{
			props.setProperty(prefix + ".serversearch", this.serverSideSearch.isSelected());
			props.setProperty(prefix + ".divider", this.jSplitPane1.getDividerLocation());
			if (clientSearcherCriteria != null)
			{
				clientSearcherCriteria.saveSettings(prefix, props);
			}
			if (serverSearcherCriteria != null)
			{
				serverSearcherCriteria.saveSettings(prefix, props);
			}
			props.setProperty(prefix + ".maxrows", this.rowCount.getText());
			props.setProperty(prefix + ".excludelobs", excludeClobs.isSelected());
		}
		else if (workspaceSettings != null)
		{
			workspaceSettings.copyTo(props, prefix);
		}
	}

	public void restoreSettings()
	{
		restoreSettings(this.getClass().getName(), Settings.getInstance());
	}

	private void restoreSettings(String prefix, PropertyStorage props)
	{
		if (initialized)
		{
			int loc = props.getIntProperty(prefix + ".divider",200);
			this.jSplitPane1.setDividerLocation(loc);
			this.serverSideSearch.setSelected(props.getBoolProperty(prefix + ".serversearch", false));
			if (clientSearcherCriteria != null)
			{
				clientSearcherCriteria.restoreSettings(prefix, props);
			}
			if (serverSearcherCriteria != null)
			{
				serverSearcherCriteria.restoreSettings(prefix, props);
			}

			this.rowCount.setText(props.getProperty(prefix + ".maxrows", "0"));
			this.excludeClobs.setSelected(props.getBoolProperty(prefix + ".excludelobs", true));
			showTableSearcherCriteria();
		}
		else
		{
			workspaceSettings = new FilteredProperties(props, prefix);
		}
	}

	@Override
	public void searchEnded()
	{
		fireDbExecEnd();

		EventQueue.invokeLater(() ->
    {
      resultPanel.doLayout();
      getCriteriaPanel().enableControls();
      serverSideSearch.setEnabled(true);
      startButton.setText(ResourceMgr.getString("LblStartSearch"));
      statusInfo.setText("");
      startButton.setEnabled(tableNames.getSelectedRowCount() > 0);
      statusInfo.setText(ResourceMgr.getFormattedString("MsgTablesFound", tableCount));
    });
	}

	@Override
	public void searchStarted()
	{
		getCriteriaPanel().disableControls();
		serverSideSearch.setEnabled(false);
		startButton.setText(ResourceMgr.getString("LblCancelPlain"));
		tableCount = 0;
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (initialized)
		{
			this.startButton.setEnabled(this.tableNames.getSelectedRowCount() > 0);
		}
	}

	@Override
	public void keyPressed(java.awt.event.KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			EventQueue.invokeLater(this::searchData);
		}
	}

	@Override
	public void keyReleased(java.awt.event.KeyEvent e)
	{
	}

	@Override
	public void keyTyped(java.awt.event.KeyEvent e)
	{
	}

	static class ParentWidthScrollPane
		extends JScrollPane
	{
		private Dimension preferredSize = new Dimension(0,0);

		ParentWidthScrollPane(Component view)
		{
			super(view);
		}

		@Override
		public Dimension getPreferredSize()
		{
			Dimension d = super.getPreferredSize();
			Container parent = this.getParent();
			this.preferredSize.setSize( (double)parent.getWidth() - 5, d.getHeight());
			return this.preferredSize;
		}
	}

	@Override
	public synchronized void addDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) this.execListener = Collections.synchronizedList(new ArrayList<DbExecutionListener>());
		this.execListener.add(l);
	}

	@Override
	public synchronized void removeDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) return;
		this.execListener.remove(l);
	}

	protected synchronized void fireDbExecStart()
	{
		this.connection.executionStart(this.connection, this);
		if (this.execListener == null) return;
		for (DbExecutionListener l : execListener)
		{
			if (l != null) l.executionStart(this.connection, this);
		}
	}

	protected synchronized void fireDbExecEnd()
	{
		this.connection.executionEnd(this.connection, this);
		if (this.execListener == null) return;
		for (DbExecutionListener l : execListener)
		{
			if (l != null) l.executionEnd(this.connection, this);
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    buttonGroup1 = new javax.swing.ButtonGroup();
    jPanel3 = new javax.swing.JPanel();
    jSplitPane1 = new WbSplitPane();
    resultTabPane = new WbTabbedPane();
    resultScrollPane = new WbScrollPane();
    resultPanel = new javax.swing.JPanel();
    tablePane = new javax.swing.JPanel();
    tableListScrollPane = new WbScrollPane();
    tableNames = new WbTable(true, false, false);
    selectButtonPanel = new javax.swing.JPanel();
    selectAllButton = new FlatButton();
    jPanel2 = new javax.swing.JPanel();
    selectNoneButton = new FlatButton();
    statusInfo = new WbStatusLabel();
    entryPanel = new javax.swing.JPanel();
    buttonPanel = new javax.swing.JPanel();
    criteriaContainer = new javax.swing.JPanel();
    serverSideSearch = new javax.swing.JCheckBox();
    excludeClobs = new javax.swing.JCheckBox();
    labelRowCount = new javax.swing.JLabel();
    rowCount = new javax.swing.JTextField();
    jSeparator1 = new javax.swing.JSeparator();
    jSeparator2 = new javax.swing.JSeparator();
    jSeparator3 = new javax.swing.JSeparator();
    jSeparator4 = new javax.swing.JSeparator();

    setLayout(new java.awt.BorderLayout());

    jSplitPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
    jSplitPane1.setDividerLocation(200);

    resultScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
    resultScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    resultPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    resultPanel.setLayout(new java.awt.GridBagLayout());
    resultScrollPane.setViewportView(resultPanel);

    resultTabPane.addTab(ResourceMgr.getString("LblTableSearchResultTab"), resultScrollPane);

    jSplitPane1.setRightComponent(resultTabPane);

    tablePane.setLayout(new java.awt.BorderLayout());

    tableNames.setModel(this.tableListModel);
    tableListScrollPane.setViewportView(tableNames);

    tablePane.add(tableListScrollPane, java.awt.BorderLayout.CENTER);

    selectButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 3));

    selectAllButton.setText(ResourceMgr.getString("LblSelectAll")); // NOI18N
    selectAllButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        selectAllButtonActionPerformed(evt);
      }
    });
    selectButtonPanel.add(selectAllButton);

    jPanel2.setMaximumSize(new java.awt.Dimension(5, 0));
    jPanel2.setMinimumSize(new java.awt.Dimension(4, 0));
    jPanel2.setPreferredSize(new java.awt.Dimension(4, 0));
    selectButtonPanel.add(jPanel2);

    selectNoneButton.setText(ResourceMgr.getString("LblSelectNone"));
    selectNoneButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        selectNoneButtonActionPerformed(evt);
      }
    });
    selectButtonPanel.add(selectNoneButton);

    tablePane.add(selectButtonPanel, java.awt.BorderLayout.NORTH);

    jSplitPane1.setLeftComponent(tablePane);

    add(jSplitPane1, java.awt.BorderLayout.CENTER);
    add(statusInfo, java.awt.BorderLayout.SOUTH);

    entryPanel.setLayout(new java.awt.GridBagLayout());

    buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 2, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    entryPanel.add(buttonPanel, gridBagConstraints);

    criteriaContainer.setLayout(new java.awt.BorderLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    entryPanel.add(criteriaContainer, gridBagConstraints);

    serverSideSearch.setText(ResourceMgr.getString("LblSearchServer")); // NOI18N
    serverSideSearch.setToolTipText(ResourceMgr.getString("d_LblSearchServer")); // NOI18N
    serverSideSearch.setBorder(null);
    serverSideSearch.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        serverSideSearchActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    entryPanel.add(serverSideSearch, gridBagConstraints);

    excludeClobs.setText(ResourceMgr.getString("LblExclLobs")); // NOI18N
    excludeClobs.setToolTipText(ResourceMgr.getString("d_LblExclLobs")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    entryPanel.add(excludeClobs, gridBagConstraints);

    labelRowCount.setLabelFor(rowCount);
    labelRowCount.setText(ResourceMgr.getString("LblMaxRows")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
    entryPanel.add(labelRowCount, gridBagConstraints);

    rowCount.setColumns(4);
    rowCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    rowCount.setText("0");
    rowCount.setMinimumSize(new java.awt.Dimension(30, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 10;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 10);
    entryPanel.add(rowCount, gridBagConstraints);

    jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
    entryPanel.add(jSeparator1, gridBagConstraints);

    jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
    entryPanel.add(jSeparator2, gridBagConstraints);

    jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
    entryPanel.add(jSeparator3, gridBagConstraints);

    jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
    entryPanel.add(jSeparator4, gridBagConstraints);

    add(entryPanel, java.awt.BorderLayout.NORTH);
  }// </editor-fold>//GEN-END:initComponents

	private void selectNoneButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_selectNoneButtonActionPerformed
	{//GEN-HEADEREND:event_selectNoneButtonActionPerformed
		this.tableNames.getSelectionModel().removeSelectionInterval(0, this.tableNames.getRowCount() - 1);
	}//GEN-LAST:event_selectNoneButtonActionPerformed

	private void selectAllButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_selectAllButtonActionPerformed
	{//GEN-HEADEREND:event_selectAllButtonActionPerformed
		this.tableNames.getSelectionModel().setSelectionInterval(0, this.tableNames.getRowCount() - 1);
	}//GEN-LAST:event_selectAllButtonActionPerformed

	private void serverSideSearchActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_serverSideSearchActionPerformed
	{//GEN-HEADEREND:event_serverSideSearchActionPerformed
		showTableSearcherCriteria();
	}//GEN-LAST:event_serverSideSearchActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  protected javax.swing.ButtonGroup buttonGroup1;
  protected javax.swing.JPanel buttonPanel;
  protected javax.swing.JPanel criteriaContainer;
  protected javax.swing.JPanel entryPanel;
  protected javax.swing.JCheckBox excludeClobs;
  protected javax.swing.JPanel jPanel2;
  protected javax.swing.JPanel jPanel3;
  protected javax.swing.JSeparator jSeparator1;
  protected javax.swing.JSeparator jSeparator2;
  protected javax.swing.JSeparator jSeparator3;
  protected javax.swing.JSeparator jSeparator4;
  protected javax.swing.JSplitPane jSplitPane1;
  protected javax.swing.JLabel labelRowCount;
  protected javax.swing.JPanel resultPanel;
  protected javax.swing.JScrollPane resultScrollPane;
  protected javax.swing.JTabbedPane resultTabPane;
  protected javax.swing.JTextField rowCount;
  protected javax.swing.JButton selectAllButton;
  protected javax.swing.JPanel selectButtonPanel;
  protected javax.swing.JButton selectNoneButton;
  protected javax.swing.JCheckBox serverSideSearch;
  protected javax.swing.JLabel statusInfo;
  protected javax.swing.JScrollPane tableListScrollPane;
  protected javax.swing.JTable tableNames;
  protected javax.swing.JPanel tablePane;
  // End of variables declaration//GEN-END:variables

}
