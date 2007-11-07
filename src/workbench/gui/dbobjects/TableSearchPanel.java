/*
 * TableSearchPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Clob;
import java.sql.ResultSet;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.TableSearcher;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.WbAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.EmptyTableModel;
import workbench.gui.components.FlatButton;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.gui.sql.EditorPanel;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.ShareableDisplay;
import workbench.interfaces.TableSearchDisplay;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.util.Like;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbWorkspace;


/**
 *
 * @author  support@sql-workbench.net
 */
public class TableSearchPanel
	extends JPanel
	implements TableSearchDisplay, ListSelectionListener, KeyListener
{
	private TableModel tableListModel;
	private TableSearcher searcher;
	private WbConnection connection;
	private boolean tableLogged;
	private String fixedStatusText;
	private ShareableDisplay tableListSource;
	private DataStore currentResult;
	private WbTable currentDisplayTable;
	private JScrollPane currentScrollPane;
	private TitledBorder currentBorder;
	private Like searchPattern;
	private WbTable firstTable;
	private EditorPanel sqlDisplay;
	private ResultHighlightingRenderer renderer;
	private FlatButton startButton;
	
	public TableSearchPanel(ShareableDisplay aTableListSource)
	{
		this.tableListModel = EmptyTableModel.EMPTY_MODEL;
		this.tableListSource = aTableListSource;
		initComponents();
		this.columnFunction.addMouseListener(new TextComponentMouseListener());
		this.searchText.addMouseListener(new TextComponentMouseListener());

		sqlDisplay = EditorPanel.createSqlEditor();
		this.resultTabPane.addTab(ResourceMgr.getString("LblTableSearchSqlLog"), sqlDisplay);

		WbTable tables = (WbTable)this.tableNames;
		tables.setAdjustToColumnLabel(false);

		WbToolbar toolbar = new WbToolbar();
		WbAction reload = new ReloadAction(this.tableListSource);
		reload.setTooltip(ResourceMgr.getString("TxtRefreshTableList"));
		toolbar.add(reload);
		buttonPanel.add(toolbar);
		
		startButton = new FlatButton();
    startButton.setText(ResourceMgr.getString("LblStartSearch"));
    startButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        startSearch();
      }
    });		
		buttonPanel.add(startButton);
		this.searcher = new TableSearcher();
		this.searcher.setDisplay(this);
		
		this.tableNames.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.fixedStatusText = ResourceMgr.getString("TxtSearchingTable") + " ";
		tables.getSelectionModel().addListSelectionListener(this);
		this.startButton.setEnabled(false);
		this.searchText.addKeyListener(this);
		Border eb = new EmptyBorder(0,2,0,0);
		CompoundBorder b2 = new CompoundBorder(this.statusInfo.getBorder(), eb);
		this.statusInfo.setBorder(b2);
	}

	private void startSearch()                                            
	{                                                
		if (this.searcher.isRunning())
		{
			setStartButtonEnabled(false);
			this.searcher.cancelSearch();
			//setStartButtonEnabled(true);
		}
		else
		{
			this.searchData();
		}
	} 	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    buttonGroup1 = new javax.swing.ButtonGroup();
    jSplitPane1 = new WbSplitPane();
    resultTabPane = new WbTabbedPane();
    resultScrollPane = new WbScrollPane();
    resultPanel = new javax.swing.JPanel();
    tablePane = new javax.swing.JPanel();
    tableListScrollPane = new WbScrollPane();
    tableNames = new WbTable(true, false, false);
    selectButtonPanel = new javax.swing.JPanel();
    selectAllButton = new javax.swing.JButton();
    jPanel2 = new javax.swing.JPanel();
    selectNoneButton = new javax.swing.JButton();
    statusInfo = new javax.swing.JLabel();
    entryPanel = new javax.swing.JPanel();
    searchText = new javax.swing.JTextField();
    likeLabel = new javax.swing.JLabel();
    columnFunction = new javax.swing.JTextField();
    labelRowCount = new javax.swing.JLabel();
    rowCount = new javax.swing.JTextField();
    buttonPanel = new javax.swing.JPanel();

    setLayout(new java.awt.BorderLayout());

    jSplitPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
    jSplitPane1.setDividerLocation(150);

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

    selectButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 3));

    selectAllButton.setText(ResourceMgr.getString("LblSelectAll"));
    selectAllButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        selectAllButtonActionPerformed(evt);
      }
    });
    selectButtonPanel.add(selectAllButton);

    jPanel2.setMaximumSize(new java.awt.Dimension(5, 0));
    jPanel2.setMinimumSize(new java.awt.Dimension(4, 0));
    jPanel2.setPreferredSize(new java.awt.Dimension(4, 0));
    selectButtonPanel.add(jPanel2);

    selectNoneButton.setText(ResourceMgr.getString("LblSelectNone"));
    selectNoneButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        selectNoneButtonActionPerformed(evt);
      }
    });
    selectButtonPanel.add(selectNoneButton);

    tablePane.add(selectButtonPanel, java.awt.BorderLayout.NORTH);

    jSplitPane1.setLeftComponent(tablePane);

    add(jSplitPane1, java.awt.BorderLayout.CENTER);

    statusInfo.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    statusInfo.setMinimumSize(new java.awt.Dimension(4, 22));
    statusInfo.setPreferredSize(new java.awt.Dimension(4, 22));
    add(statusInfo, java.awt.BorderLayout.SOUTH);

    entryPanel.setLayout(new java.awt.GridBagLayout());

    searchText.setColumns(20);
    searchText.setText("% ... %");
    searchText.setToolTipText(ResourceMgr.getDescription("LblSearchTableCriteria"));
    searchText.setMinimumSize(new java.awt.Dimension(100, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    entryPanel.add(searchText, gridBagConstraints);

    likeLabel.setText("LIKE");
    likeLabel.setToolTipText(ResourceMgr.getDescription("LblSearchTableCriteria"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    entryPanel.add(likeLabel, gridBagConstraints);

    columnFunction.setColumns(8);
    columnFunction.setText("$col$");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    entryPanel.add(columnFunction, gridBagConstraints);

    labelRowCount.setLabelFor(rowCount);
    labelRowCount.setText(ResourceMgr.getString("LblMaxRows"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
    entryPanel.add(labelRowCount, gridBagConstraints);

    rowCount.setColumns(4);
    rowCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    rowCount.setText("0");
    rowCount.setMinimumSize(new java.awt.Dimension(30, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 5);
    entryPanel.add(rowCount, gridBagConstraints);

    buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 2, 0));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    entryPanel.add(buttonPanel, gridBagConstraints);

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

	private void setStartButtonEnabled(final boolean flag)
	{
		try
		{
			EventQueue.invokeAndWait(new Runnable()
			{
				public void run()
				{
					startButton.setEnabled(flag);
				}
			});
		}
		catch (Throwable th)
		{
			startButton.setEnabled(flag);
		}
	}
	private void adjustDataTable()
	{
		if (this.currentDisplayTable != null)
		{
			int rows = this.currentDisplayTable.getRowCount();
			int height = this.currentDisplayTable.getRowHeight();
			int width = this.resultScrollPane.getWidth();
			String label = this.currentBorder.getTitle();
			label = label + " (" + rows + " " + (rows == 1 ? ResourceMgr.getString("TxtFoundRow") : ResourceMgr.getString("TxtFoundRows")) + ")";
			this.currentBorder.setTitle(label);
			Dimension size = this.currentScrollPane.getPreferredSize();
			if (rows > 25) rows = 25;
			size.setSize(width - 20, (rows + 4) * height );
			this.currentScrollPane.setPreferredSize(size);
		}
	}

	private void initRenderer(WbTable table, ResultInfo info)
	{
		TableColumnModel model = table.getColumnModel();
		
		for (int i=0; i < info.getColumnCount(); i++)
		{
			int type = info.getColumnType(i);
			if (SqlUtil.isCharacterType(type))
			{
				model.getColumn(i).setCellRenderer(renderer);
			}
		}
	}
	
	public synchronized void addResultRow(TableIdentifier table, ResultSet aResult)
	{
		try
		{
			if (!this.tableLogged)
			{
				// Adjust the last table (which is now completed), before creating the new one
				// Note: this will not adjust the table if only one database table was searched
				// (or only results from one database table where returned)
				// therefor it's important to call this in searchEnded() as well
				this.adjustDataTable();
				
				this.currentDisplayTable = new WbTable(true, true, false);
				this.currentDisplayTable.getCopyAsInsertAction().setEnabled(true);
				this.currentDisplayTable.getCopyAsUpdateAction().setEnabled(true);
				this.currentDisplayTable.getCopyAsDeleteInsertAction().setEnabled(true);
				
				this.currentDisplayTable.setUseDefaultStringRenderer(false);
				//this.currentDisplayTable.setDefaultRenderer(String.class, renderer);
				if (this.firstTable == null)
				{
					this.firstTable = this.currentDisplayTable;
				}
				//this.currentDisplayTable.setDefaultRenderer(Object.class, rend);
				this.currentResult = new DataStore(aResult);
				this.currentResult.setOriginalConnection(this.connection);
				this.currentResult.setUpdateTableToBeUsed(table);
				DataStoreTableModel model = new DataStoreTableModel(this.currentResult);
				this.currentDisplayTable.setModel(model, true);
				
				initRenderer(currentDisplayTable, currentResult.getResultInfo());
				this.currentDisplayTable.adjustOrOptimizeColumns();

				this.currentScrollPane  = new ParentWidthScrollPane(this.currentDisplayTable);
				TitledBorder b = new TitledBorder(table.getTableExpression());
				this.currentBorder = b;
				Font f = b.getTitleFont();
				f = f.deriveFont(Font.BOLD);
				b.setTitleFont(f);
				b.setBorder(new EtchedBorder());
				this.currentScrollPane.setBorder(b);
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.fill = GridBagConstraints.HORIZONTAL;
				constraints.weightx = 1.0;
				constraints.anchor = GridBagConstraints.WEST;
				this.resultPanel.add(this.currentScrollPane, constraints);
				this.tableLogged = true;
			}
			this.currentResult.addRow(aResult);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public synchronized void error(String msg)
	{
		this.sqlDisplay.appendLine(msg);
		this.sqlDisplay.appendLine("\n\n");
	}
	
	/**
	 *	Call back function from the table searcher...
	 */
	public synchronized void setCurrentTable(String aTablename, String aSql)
	{
		this.tableLogged = false;
		this.currentResult = null;
		this.statusInfo.setText(this.fixedStatusText + aTablename);
		this.sqlDisplay.appendLine(aSql);
		this.sqlDisplay.appendLine(";\n\n");
	}

	public void setStatusText(String aStatustext)
	{
		this.statusInfo.setText(aStatustext);
	}
	/** Getter for property connection.
	 * @return Value of property connection.
	 *
	 */
	public WbConnection getConnection()
	{
		return connection;
	}

	/** Setter for property connection.
	 * @param connection New value of property connection.
	 *
	 */
	public void setConnection(WbConnection connection)
	{
		this.connection = connection;
		this.searcher.setConnection(connection);
		this.tableListSource.addTableListDisplayClient(this.tableNames);
	}

	public void disconnect()
	{
		this.reset();
		this.tableListSource.removeTableListDisplayClient(this.tableNames);
	}

	public void reset()
	{
    // resultPanel.removeAll() does not work
    // the old tables just stay in there
    // so I re-create the actual result panel
		this.resultPanel = new JPanel();
    this.resultPanel.setLayout(new GridBagLayout());
    this.resultScrollPane.setViewportView(resultPanel);
		this.sqlDisplay.setText("");
		this.firstTable = null;
	}

	public void searchData()
	{
		if (!searcher.setColumnFunction(this.columnFunction.getText()))
		{
			WbSwingUtilities.showErrorMessageKey(this, "MsgErrorColFunction");
			return;
		}

		if (this.tableNames.getSelectedRowCount() == 0) return;
		if (this.connection.isBusy())
		{
			WbSwingUtilities.showMessageKey(this, "ErrConnectionBusy");
			return;
		}

		this.reset();

		int[] selectedTables = this.tableNames.getSelectedRows();

		TableIdentifier[] searchTables = new TableIdentifier[this.tableNames.getSelectedRowCount()];
		DataStore tables = ((WbTable)(this.tableNames)).getDataStore();
		for (int i=0; i < selectedTables.length; i++)
		{
			String catalog = tables.getValueAsString(selectedTables[i], DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
			String schema = tables.getValueAsString(selectedTables[i], DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
			String tablename = tables.getValueAsString(selectedTables[i], DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			String type = tables.getValueAsString(selectedTables[i], DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);

			searchTables[i] = new TableIdentifier(catalog, schema, tablename);
			searchTables[i].setNeverAdjustCase(true);
			searchTables[i].setType(type);
		}
		
		int maxRows = StringUtil.getIntValue(this.rowCount.getText(), 0);
		
		String text = this.searchText.getText();
		searcher.setMaxRows(maxRows);
		searcher.setCriteria(text);
		boolean sensitive= this.connection.getDbSettings().isStringComparisonCaseSensitive();
		boolean ignoreCase = !sensitive;
		if (sensitive)
		{
			ignoreCase = searcher.getCriteriaMightBeCaseInsensitive();
		}
		this.searchPattern = new Like(searcher.getCriteria(), ignoreCase);
		this.renderer = new ResultHighlightingRenderer(this.searchPattern);

		searcher.setTableNames(searchTables);
		searcher.search(); // starts the background thread
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
		props.setProperty(prefix + ".divider", this.jSplitPane1.getDividerLocation());
		props.setProperty(prefix + ".criteria", this.searchText.getText());
		props.setProperty(prefix + ".maxrows", this.rowCount.getText());
		props.setProperty(prefix + ".column-function", this.columnFunction.getText());
	}

	public void restoreSettings()
	{
		restoreSettings(this.getClass().getName(), Settings.getInstance());
	}

	private void restoreSettings(String prefix, PropertyStorage props)
	{
		int loc = props.getIntProperty(prefix + ".divider",200);
		this.jSplitPane1.setDividerLocation(loc);
		this.searchText.setText(props.getProperty(prefix + ".criteria", ""));
		this.rowCount.setText(props.getProperty(prefix + ".maxrows", "0"));
		this.columnFunction.setText(props.getProperty(prefix + ".column-function", "$col$"));
	}

	public void searchEnded()
	{
		this.adjustDataTable();
		if (this.firstTable != null)
		{
			int height = this.firstTable.getRowHeight();
			JScrollBar sb = this.resultScrollPane.getVerticalScrollBar();
			sb.setUnitIncrement(height);
			sb.setBlockIncrement(height * 5);
		}
		// insert a dummy panel at the end which will move
		// all tables in the pane to the upper border
		// e.g. when there is only one table
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.weighty = 1.0;
		constraints.anchor = GridBagConstraints.WEST;
		this.resultPanel.add(new JPanel(), constraints);

		this.resultPanel.doLayout();
		this.searchText.setEnabled(true);
		this.columnFunction.setEnabled(true);
		startButton.setText(ResourceMgr.getString("LblStartSearch"));
		this.statusInfo.setText("");
		this.startButton.setEnabled(this.tableNames.getSelectedRowCount() > 0);
	}

	public void searchStarted()
	{
		this.searchText.setEnabled(false);
		this.columnFunction.setEnabled(false);
		startButton.setText(ResourceMgr.getString("LblCancelSearch"));
	}

	public void valueChanged(javax.swing.event.ListSelectionEvent e)
	{
		this.startButton.setEnabled(this.tableNames.getSelectedRowCount() > 0);
	}

	public void keyPressed(java.awt.event.KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					searchData();
				}
			}
			);
		}
	}

	public void keyReleased(java.awt.event.KeyEvent e)
	{
	}

	public void keyTyped(java.awt.event.KeyEvent e)
	{
	}

  // Variables declaration - do not modify//GEN-BEGIN:variables
  protected javax.swing.ButtonGroup buttonGroup1;
  protected javax.swing.JPanel buttonPanel;
  protected javax.swing.JTextField columnFunction;
  protected javax.swing.JPanel entryPanel;
  protected javax.swing.JPanel jPanel2;
  protected javax.swing.JSplitPane jSplitPane1;
  protected javax.swing.JLabel labelRowCount;
  protected javax.swing.JLabel likeLabel;
  protected javax.swing.JPanel resultPanel;
  protected javax.swing.JScrollPane resultScrollPane;
  protected javax.swing.JTabbedPane resultTabPane;
  protected javax.swing.JTextField rowCount;
  protected javax.swing.JTextField searchText;
  protected javax.swing.JButton selectAllButton;
  protected javax.swing.JPanel selectButtonPanel;
  protected javax.swing.JButton selectNoneButton;
  protected javax.swing.JLabel statusInfo;
  protected javax.swing.JScrollPane tableListScrollPane;
  protected javax.swing.JTable tableNames;
  protected javax.swing.JPanel tablePane;
  // End of variables declaration//GEN-END:variables

	static class ParentWidthScrollPane
		extends JScrollPane
	{
		private Dimension preferredSize = new Dimension(0,0);

		public ParentWidthScrollPane(Component view)
		{
			super(view);
		}
		
		public Dimension getPreferredSize()
		{
			Dimension d = super.getPreferredSize();
			Container parent = this.getParent();
			this.preferredSize.setSize( (double)parent.getWidth() - 5, d.getHeight());
			return this.preferredSize;
		}
	}

	static class ResultHighlightingRenderer
		extends DefaultTableCellRenderer
	{
		private Like pattern;
		private Color background = UIManager.getColor("Table.background");
		private Color foreground = UIManager.getColor("Table.foreground");
		private Color selectBack = UIManager.getColor("Table.selectionBackground");
		private Color selectText = UIManager.getColor("Table.selectionForeground");

		public ResultHighlightingRenderer(Like aPattern)
		{
			this.pattern = aPattern;
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			String displayValue = null;
			if (value != null)
			{
				if (value instanceof Clob)
				{
					Clob clob = (Clob)value;
					try
					{
						long len = clob.length();
						displayValue = clob.getSubString(1, (int)len);
					}
					catch (Throwable th)
					{
						displayValue = null;
					}
				}
				else
				{
					displayValue = value.toString();
				}
			}
			JLabel result = (JLabel)super.getTableCellRendererComponent(table, displayValue, isSelected, hasFocus, row, column);

			try
			{
				if (!isSelected && displayValue != null && this.pattern.like(displayValue))
				{
					result.setBackground(Color.YELLOW);
					result.setForeground(Color.BLACK);
				}
				else if (isSelected)
				{
					result.setBackground(selectBack);
					result.setForeground(selectText);
				}
				else
				{
					result.setBackground(background);
					result.setForeground(foreground);
				}
				result.setToolTipText(displayValue);
			}
			catch (Exception e)
			{
				result.setBackground(background);
				result.setForeground(foreground);
			}
			return result;
		}


	}

}
