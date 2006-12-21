/*
 * TableDataPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.actions.SelectionFilterAction;
import workbench.gui.components.FlatButton;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.Resettable;
import workbench.util.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.StopAction;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbToolbar;
import workbench.gui.sql.DwPanel;
import workbench.interfaces.Interruptable;
import workbench.interfaces.Reloadable;
import workbench.interfaces.TableDeleteListener;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.WbThread;
import workbench.interfaces.JobErrorHandler;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import workbench.gui.MainWindow;
import workbench.gui.actions.FilterPickerAction;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.DbExecutionNotifier;
import workbench.util.WbWorkspace;


/**
 *
 * @author  support@sql-workbench.net
 *
 */
public class TableDataPanel
  extends JPanel
	implements Reloadable, ActionListener, Interruptable, TableDeleteListener, Resettable, DbExecutionNotifier
{
	private WbConnection dbConnection;
	protected DwPanel dataDisplay;

	private ReloadAction reloadAction;

	private JButton config;
	private JLabel tableNameLabel;
	private JLabel rowCountLabel;
	private WbButton rowCountButton;
	private JCheckBox autoRetrieve;
	private JPanel topPanel;

	private int warningThreshold = -1;
	private boolean retrieveRunning = false;
	private boolean updateRunning = false;
	private boolean autoloadRowCount = true;
	private TableIdentifier table;
	private ImageIcon loadingIcon;
	private Image loadingImage;
	private Object retrieveLock = new Object();
	protected StopAction cancelRetrieve;
	private List execListener;

	public TableDataPanel() 
		throws Exception
	{
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setLayout(new BorderLayout());

		this.dataDisplay = new DwPanel()
		{
			public synchronized int saveChanges(WbConnection aConnection, JobErrorHandler errorHandler)
				throws SQLException
			{
				int result = -1;
				try
				{
					dbUpdateStart();
					result = super.saveChanges(aConnection, errorHandler);
				}
				finally
				{
					dbUpdateEnd();
				}
				return result;
			}
		};

		this.dataDisplay.setManageUpdateAction(true);
		this.dataDisplay.setShowLoadProcess(true);
		this.dataDisplay.setDefaultStatusMessage("");
		this.dataDisplay.setShowErrorMessages(true);

		topPanel = new JPanel();
		topPanel.setMaximumSize(new Dimension(32768, 32768));
		BoxLayout box = new BoxLayout(topPanel, BoxLayout.X_AXIS);
		topPanel.setLayout(box);
		
		this.reloadAction = new ReloadAction(this);
		this.reloadAction.setTooltip(ResourceMgr.getDescription("TxtLoadTableData", true));
		
		WbToolbar mytoolbar = new WbToolbar();
		mytoolbar.addDefaultBorder();
		topPanel.add(mytoolbar);
		mytoolbar.add(this.reloadAction);
		mytoolbar.addSeparator();
		
		this.cancelRetrieve = new StopAction(this);
		this.cancelRetrieve.setEnabled(false);
		mytoolbar.add(this.cancelRetrieve);
		mytoolbar.addSeparator();

		topPanel.add(Box.createHorizontalStrut(15));
		topPanel.add(new JLabel(ResourceMgr.getString("LblTable") + ":"));
		Font std = Settings.getInstance().getStandardLabelFont();
		Font bold = std.deriveFont(Font.BOLD);
		tableNameLabel = new JLabel();
		tableNameLabel.setFont(bold);
		topPanel.add(Box.createHorizontalStrut(5));
		topPanel.add(tableNameLabel);

		topPanel.add(Box.createHorizontalStrut(10));
		rowCountButton = new WbButton(ResourceMgr.getString("LblTableDataRowCount"));
		rowCountButton.setRollover(true);
		rowCountButton.addActionListener(this);
		rowCountButton.setToolTipText(ResourceMgr.getDescription("LblTableDataRowCountButton"));
		rowCountButton.setFocusable(false);
		
		topPanel.add(rowCountButton);
		topPanel.add(Box.createHorizontalStrut(5));
		rowCountLabel = new JLabel();
		rowCountLabel.setFont(bold);
		rowCountLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		topPanel.add(rowCountLabel);
		topPanel.add(Box.createHorizontalStrut(10));

		autoRetrieve = new JCheckBox(ResourceMgr.getString("LblAutoLoad"));
		autoRetrieve.setToolTipText(ResourceMgr.getDescription("LblAutoLoadTableData"));
		autoRetrieve.setHorizontalTextPosition(SwingConstants.LEFT);
		topPanel.add(autoRetrieve);

		topPanel.add(Box.createHorizontalGlue());
		this.config = new FlatButton(ResourceMgr.getString("LblConfigureWarningThreshold"));
		this.config.setToolTipText(ResourceMgr.getDescription("LblConfigureWarningThreshold"));
		this.config.addActionListener(this);
		topPanel.add(this.config);

		this.add(topPanel, BorderLayout.NORTH);

		mytoolbar.add(this.dataDisplay.getUpdateDatabaseAction());
		mytoolbar.add(this.dataDisplay.getSelectKeysAction());
		mytoolbar.addSeparator();
		mytoolbar.add(this.dataDisplay.getInsertRowAction());
		mytoolbar.add(this.dataDisplay.getCopyRowAction());
		mytoolbar.add(this.dataDisplay.getDeleteRowAction());
		mytoolbar.addSeparator();
		SelectionFilterAction a = new SelectionFilterAction();
		a.setClient(this.dataDisplay.getTable());
		mytoolbar.add(a);
		mytoolbar.addSeparator();
		mytoolbar.add(this.dataDisplay.getTable().getFilterAction());
		
		FilterPickerAction p = new FilterPickerAction(dataDisplay.getTable());
		mytoolbar.add(p);
		mytoolbar.addSeparator();
		mytoolbar.add(this.dataDisplay.getTable().getResetFilterAction());

		this.add(dataDisplay, BorderLayout.CENTER);
	}

	public void setResultContainer(MainWindow container)
	{
		if (this.dataDisplay != null && container != null)
		{
			this.dataDisplay.initTableNavigation(container);
		}
	}
	
	private ImageIcon getLoadingIndicator()
	{
		if (this.loadingIcon == null)
		{
			this.loadingImage = ResourceMgr.getPicture("wait").getImage();
			this.loadingIcon = new ImageIcon(this.loadingImage);
		}
		return this.loadingIcon;
	}

	public void disconnect()
	{
		this.dbConnection = null;
		this.reset();
	}

	public void reset()
	{
		if (this.isRetrieving()) return;
		this.dataDisplay.clearContent();
		this.rowCountLabel.setText(ResourceMgr.getString("LblNotAvailable"));
		this.clearLoadingImage();
	}

	public void setConnection(WbConnection aConnection)
	{
		this.dbConnection = aConnection;
		try
		{
			this.dataDisplay.setConnection(aConnection);
		}
		catch (Throwable th)
		{
			LogMgr.logError("TableDataPanel.setConnection()", "Error when setting connection", th);
		}
	}

	private boolean rowCountCancel = false;
	
	private void startRetrieveRowCount()
	{
		Thread t = null;
		if (rowCountRetrieveStmt != null)
		{
			t = new WbThread("RowCount cancel")
			{
				public void run()
				{
					cancelRowCountRetrieve();
				}
			};
		}
		else
		{
			t = new WbThread("RowCount Retrieve")
			{
				public void run()
				{
					showRowCount();
				}
			};
		}
		t.start();
	}
	
	private Statement rowCountRetrieveStmt = null;
	
	public long showRowCount()
	{
		if (this.dbConnection == null) return -1;
		if (this.isRetrieving()) return -1;

		this.retrieveRunning = true;
		
		this.rowCountLabel.setText("");
		this.rowCountLabel.setIcon(this.getLoadingIndicator());

		this.reloadAction.setEnabled(false);
		this.dataDisplay.setStatusMessage(ResourceMgr.getString("MsgCalculatingRowCount"));
		//this.topPanel.doLayout();
		this.topPanel.validate();
		String sql = this.buildSqlForTable(true);
		if (sql == null) return -1;

		long rowCount = 0;
		Statement stmt = null;
		ResultSet rs = null;

		try
		{
			fireDbExecStart();
			rowCountButton.setToolTipText(ResourceMgr.getDescription("LblTableDataRowCountCancel"));
			
			rowCountRetrieveStmt = this.dbConnection.createStatement();
			rs = rowCountRetrieveStmt.executeQuery(sql);
			if (rs.next())
			{
				rowCount = rs.getLong(1);
			}
			this.rowCountLabel.setText(Long.toString(rowCount));
			this.rowCountLabel.setToolTipText(null);
			commitRetrieveIfNeeded();
		}
		catch (Exception e)
		{
			rollbackIfNeeded();
			LogMgr.logError("TableDataPanel.showRowCount()", "Error retrieving rowcount for " + this.table.getTableExpression() + ": " + ExceptionUtil.getDisplay(e), null);
			if (rowCountCancel)
			{
				this.rowCountLabel.setText(ResourceMgr.getString("LblNotAvailable"));
				this.rowCountLabel.setToolTipText(null);
			}
			else
			{
				this.rowCountLabel.setText(ResourceMgr.getString("TxtError"));
				this.rowCountLabel.setToolTipText(ExceptionUtil.getDisplay(e));
			}
			String title = ResourceMgr.getString("TxtErrorRowCount");
			WbSwingUtilities.showErrorMessage(SwingUtilities.getWindowAncestor(this), title, ExceptionUtil.getDisplay(e));
		}
		finally
		{
			this.rowCountCancel = false;
			this.retrieveRunning = false;
			this.dataDisplay.setStatusMessage("");
			this.clearLoadingImage();
			SqlUtil.closeAll(rs, rowCountRetrieveStmt);
			rowCountRetrieveStmt = null;
			this.reloadAction.setEnabled(true);
			fireDbExecEnd();
			rowCountButton.setToolTipText(ResourceMgr.getDescription("LblTableDataRowCountButton"));
		}
		return rowCount;
	}

	protected void cancelRowCountRetrieve()
	{
		if (this.rowCountRetrieveStmt != null)
		{
			try 
			{ 
				this.dataDisplay.setStatusMessage(ResourceMgr.getString("MsgCancelRowCount"));
				this.rowCountCancel = true;
				this.rowCountRetrieveStmt.cancel();
			} 
			catch (Throwable th) 
			{
				LogMgr.logError("TableDataPanel.cancelRowCountRetrieve()", "Error when cancelling row count retrieve", th);
			}
		}
	}
	
	/**
	 * Define the table for which the data should be displayed
	 */
	public void setTable(TableIdentifier aTable)
	{
		if (!this.isRetrieving()) reset();
		this.table = aTable;
		this.dataDisplay.getTable().clearLastFilter(true);
		this.dataDisplay.getTable().resetFilter();
		this.tableNameLabel.setText(this.table.getTableName());
		this.topPanel.doLayout();
		this.topPanel.repaint();
	}

	private String buildSqlForTable(boolean forRowCount)
	{
		if (this.table == null) return null;

		StringBuilder sql = new StringBuilder(100);
		if (forRowCount)
			sql.append("SELECT COUNT(*) FROM ");
		else
			sql.append("SELECT * FROM ");

		sql.append(this.table.getTableExpression(this.dbConnection));

		return sql.toString();
	}

	private void clearLoadingImage()
	{
		if (this.loadingImage != null) this.loadingImage.flush();
		this.rowCountLabel.setIcon(null);
	}

	public boolean confirmCancel() { return true; }

	/**
	 * 	Directly cancel the retrieval (in the same thread)
	 */
	public void cancelRetrieve()
	{
		dataDisplay.cancelExecution();
	}

	/**
	 * 	Implementation of the Interruptable interface.
	 * 	This will kick off a Thread that cancels the retrieval.
	 */
	public void cancelExecution()
	{
		Thread t = new WbThread("Cancel thread")
		{
			public void run()
			{
				try
				{
					dataDisplay.cancelExecution();
				}
				finally
				{
					cancelRetrieve.setEnabled(false);
					WbSwingUtilities.showDefaultCursor(dataDisplay);
				}
			}
		};
		t.start();
	}

	protected void retrieveStart()
	{
		fireDbExecStart();
		synchronized (this.retrieveLock)
		{
			this.retrieveRunning = true;
		}
	}

	private void retrieveEnd()
	{
		synchronized (this.retrieveLock)
		{
			this.retrieveRunning = false;
		}
		fireDbExecEnd();
	}

	protected void dbUpdateStart()
	{
		this.reloadAction.setEnabled(false);
		fireDbExecStart();
		synchronized (this.retrieveLock)
		{
			this.updateRunning = true;
		}
	}

	protected void dbUpdateEnd()
	{
		try
		{
			this.reloadAction.setEnabled(true);
		}
		finally
		{
			synchronized (this.retrieveLock)
			{
				this.updateRunning = false;
			}
			fireDbExecEnd();
		}
	}

	public boolean isRetrieving()
	{
		synchronized (this.retrieveLock)
		{
			return this.retrieveRunning || this.updateRunning;
		}
	}

	private void rollbackIfNeeded()
	{
		if (!this.dbConnection.getAutoCommit() && this.dbConnection.getProfile().getUseSeparateConnectionPerTab())
		{
			try { this.dbConnection.commit(); } catch (Throwable th) {}
		}
	}
	
	private void commitRetrieveIfNeeded()
	{
		if (!this.dbConnection.getAutoCommit() && this.dbConnection.getProfile().getUseSeparateConnectionPerTab())
		{
			if (this.dbConnection.selectStartsTransaction())
			{
				try { this.dbConnection.commit(); } catch (Throwable th) {}
			}
		}
	}
		
	protected void doRetrieve(boolean respectMaxRows)
	{
		if (this.isRetrieving()) return;

    String sql = this.buildSqlForTable(false);
    if (sql == null) return;

		this.retrieveStart();

		this.cancelRetrieve.setEnabled(true);
		this.reloadAction.setEnabled(false);
		boolean error = false;
		
		try
		{
			dataDisplay.setShowErrorMessages(true);
			dataDisplay.setStatusMessage(ResourceMgr.getString("LblLoadingProgress"));
			dataDisplay.runQuery(sql, respectMaxRows);
			dataDisplay.getTable().adjustOrOptimizeColumns();
			dataDisplay.getTable().getDataStore().setUpdateTableToBeUsed(this.table);
			dataDisplay.getSelectKeysAction().setEnabled(true);
			String header = ResourceMgr.getString("TxtTableDataPrintHeader") + " " + table;
			dataDisplay.setPrintHeader(header);
			dataDisplay.showlastExecutionTime();
			commitRetrieveIfNeeded();
		}
		catch (Throwable e)
		{
			
			error = true;
			final String msg;
			
			if (e instanceof OutOfMemoryError)
			{
				try { dataDisplay.getTable().reset(); } catch (Throwable th) {}
				System.gc();
				msg = ResourceMgr.getString("MsgOutOfMemoryError");
			}				
			else
			{
				msg = ExceptionUtil.getDisplay(e);
			}

			rollbackIfNeeded();
			
			LogMgr.logError("TableDataPanel.doRetrieve()", "Error retrieving table data", e);
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					WbSwingUtilities.showErrorMessage(TableDataPanel.this, msg);
				}
			});
		}
		finally
		{
			dataDisplay.clearStatusMessage();
			cancelRetrieve.setEnabled(false);
			reloadAction.setEnabled(true);
			this.retrieveEnd();
			WbSwingUtilities.showDefaultCursor(this);
		}
		if (!error)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					dataDisplay.getTable().requestFocus();
				}
			});
		}
	}

	public void setCursor(Cursor newCursor)
	{
		super.setCursor(newCursor);
		this.dataDisplay.setCursor(null);
	}

	public void retrieve(final boolean respectMaxRows)
	{
		if (this.isRetrieving()) return;

		Thread t = new WbThread("TableDataPanel retrieve thread")
		{
			public void run()
			{
				doRetrieve(respectMaxRows);
			}
		};
		t.start();
	}

	private String getWorkspacePrefix(int index)
	{
		return "dbexplorer" + index + ".tabledata.";
	}

	/**
	 * Save the settings to a Workspace
	 */
	public void saveToWorkspace(WbWorkspace wb, int index)
	{
		String prefix = getWorkspacePrefix(index);
		saveSettings(prefix, wb.getSettings(), false);
	}

	/**
	 * Restore the settings from a Workspace
	 */
	public void readFromWorkspace(WbWorkspace wb, int index)
	{
		this.restoreSettings(); // load "global" settings first;
		String prefix = getWorkspacePrefix(index);
		this.readSettings(prefix, wb.getSettings(), false);
	}

	/**
	 *	Store global settings for this DbExplorer
	 */
	public void saveSettings()
	{
		String prefix = TableDataPanel.class.getName() + ".";
		saveSettings(prefix, Settings.getInstance(), true);
	}

	private void saveSettings(String prefix, PropertyStorage props, boolean includeGlobal)
	{
		props.setProperty(prefix + "maxrows", this.dataDisplay.getMaxRows());
		props.setProperty(prefix + "autoretrieve", this.autoRetrieve.isSelected());
		props.setProperty(prefix + "autoloadrowcount", this.autoloadRowCount);
		if (includeGlobal)
		{
			props.setProperty(prefix + "warningthreshold", this.warningThreshold);
		}
	}
	/**
	 *	Restore global settings for this DbExplorer
	 */
	public void restoreSettings()
	{
		String prefix = TableDataPanel.class.getName() + ".";
		readSettings(prefix, Settings.getInstance(), true);
	}

	private void readSettings(String prefix, PropertyStorage props, boolean includeGlobal)
	{
		int max = props.getIntProperty(prefix + "maxrows", -1);
		if (max == -1 && includeGlobal)
		{
			max = 500;
		}
		if (max != -1) this.dataDisplay.setMaxRows(max);
		String v = props.getProperty(prefix + "autoretrieve", null);
		if (v == null && includeGlobal)
		{
			v = "true";
		}
		boolean auto = "true".equals(v);
		this.autoRetrieve.setSelected(auto);
		this.autoloadRowCount = props.getBoolProperty(prefix + "autoloadrowcount", true);
		if (includeGlobal)
		{
			this.warningThreshold = props.getIntProperty(prefix + "warningthreshold", 1500);
		}
	}

	public void showData()
	{
		this.showData(true);
	}

	public void showData(boolean includeData)
	{
		if (this.isRetrieving()) return;

		this.reset();
		long rows = -1;
		if (this.autoloadRowCount) rows = this.showRowCount();

		if (this.autoRetrieve.isSelected() && includeData)
		{
			int max = this.dataDisplay.getMaxRows();
			if ( this.warningThreshold > 0
				   && rows > this.warningThreshold
				   && max == 0)
			{
				String msg = ResourceMgr.getString("MsgDataDisplayWarningThreshold");
				msg = msg.replaceAll("%rows%", Long.toString(rows));
				int choice = JOptionPane.showConfirmDialog(this, msg, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.NO_OPTION) return;
			}
			this.doRetrieve(true);
		}
	}

	public void reload()
	{
		this.reset();
		if (this.autoloadRowCount) this.showRowCount();
		boolean ctrlPressed = this.reloadAction.ctrlPressed();
		this.retrieve(!ctrlPressed);
	}

	public Window getParentWindow()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.config)
		{
			TableDataSettings p = new TableDataSettings();
			p.setThresholdValue(this.warningThreshold);
			p.setAutoloadData(this.autoRetrieve.isSelected());
			p.setAutoloadRowCount(this.autoloadRowCount);
			Window parent = SwingUtilities.getWindowAncestor(this);
			int choice = JOptionPane.showConfirmDialog(parent, p, ResourceMgr.getString("LblConfigureWarningThresholdTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice == JOptionPane.OK_OPTION)
			{
				this.warningThreshold = p.getThresholdValue();
				this.autoRetrieve.setSelected(p.getAutoloadData());
				this.autoloadRowCount = p.getAutoloadRowCount();
			}
		}
		else if (e.getSource() == this.rowCountButton)
		{
			this.startRetrieveRowCount();
		}
	}

	public void setReadOnly(boolean aFlag)
	{
		this.dataDisplay.setReadOnly(aFlag);
	}

	public void tableDataDeleted(List tables)
	{
		if (tables == null) return;
		if (this.table == null) return;
		if (tables.contains(this.table))
		{
			this.reset();
		}
	}

	public void addDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) this.execListener = Collections.synchronizedList(new ArrayList());
		this.execListener.add(l);
	}

	public void removeDbExecutionListener(DbExecutionListener l)
	{
		if (this.execListener == null) return;
		this.execListener.remove(l);
	}

	protected synchronized void fireDbExecStart()
	{
		this.dbConnection.executionStart(null, this);		
		if (this.execListener == null) return;
		int count = this.execListener.size();
		for (int i=0; i < count; i++)
		{
			DbExecutionListener l = (DbExecutionListener)this.execListener.get(i);
			if (l != null) l.executionStart(this.dbConnection, this);
		}
	}
	
	protected synchronized void fireDbExecEnd()
	{
		this.dbConnection.executionEnd(null, this);
		if (this.execListener == null) return;
		int count = this.execListener.size();
		for (int i=0; i < count; i++)
		{
			DbExecutionListener l = (DbExecutionListener)this.execListener.get(i);
			if (l != null) l.executionEnd(this.dbConnection, this);
		}
	}
	
}
