/*
 * TableRowCountPanel.java
 *
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
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import workbench.WbManager;
import workbench.interfaces.Interruptable;
import workbench.interfaces.Reloadable;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.actions.StopAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.RunningJobIndicator;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class TableRowCountPanel
	extends JPanel
	implements WindowListener, Reloadable, Interruptable, ToolWindow
{
	private static int instanceCount;
	private WbTable data;
	private JLabel statusBar;
	private List<TableIdentifier> tables;
	private Statement currentStatement;
	private boolean cancel;
	private JFrame window;
	private WbConnection dbConnection;
	private WbConnection sourceConnection;
	private boolean useSeparateConnection;
	private StopAction cancelAction;
	private JScrollPane scrollPane;

	public TableRowCountPanel(List<TableIdentifier> toCount, WbConnection connection)
	{
		super(new BorderLayout(0,0));
		tables = toCount;
		instanceCount ++;
		sourceConnection = connection;

		statusBar = new JLabel();
		data = new WbTable(false, false, false);
		data.setReadOnly(true);

		scrollPane = new JScrollPane(data);
		JPanel statusPanel = new JPanel(new BorderLayout(0,0));

		Border etched = new EtchedBorder(EtchedBorder.LOWERED);
		Border current = scrollPane.getBorder();
		Border frame = new CompoundBorder(new EmptyBorder(3,3,0,3), current);
		scrollPane.setBorder(frame);

		Border b = new CompoundBorder(new EmptyBorder(3, 2, 2, 3), etched);
		statusPanel.setBorder(b);
		statusPanel.add(statusBar);

		WbToolbar toolbar = new WbToolbar();
		toolbar.setBorder(new CompoundBorder(new EmptyBorder(3,3,3,3), etched));
		ReloadAction reload = new ReloadAction(this);
		toolbar.add(reload);
		toolbar.addSeparator();
		cancelAction = new StopAction(this);
		cancelAction.setEnabled(false);
		toolbar.add(cancelAction);

		add(toolbar, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
		add(statusPanel, BorderLayout.PAGE_END);
	}

	private void checkConnection()
	{
		if (dbConnection != null) return;

		if (sourceConnection.getProfile().getUseSeparateConnectionPerTab())
		{
			try
			{
				showStatusMessage(ResourceMgr.getString("MsgConnecting"));
				dbConnection = ConnectionMgr.getInstance().getConnection(sourceConnection.getProfile(), "TableRowCount-" + Integer.toString(instanceCount));
			}
			catch (Exception cne)
			{
				LogMgr.logError("TableRowCountPanel.checkConnection()", "Could not get connection", cne);
			}
			finally
			{
				showStatusMessage("");
			}
			useSeparateConnection = true;
		}
		else
		{
			dbConnection = sourceConnection;
			useSeparateConnection = false;
		}
	}

	@Override
	public void reload()
	{
		retrieveRowCounts();
	}

	@Override
	public boolean confirmCancel()
	{
		return true;
	}

	@Override
	public void cancelExecution()
	{
		showStatusMessage(ResourceMgr.getString("MsgCancelling"));
		cancel = true;
		if (currentStatement != null)
		{
			LogMgr.logDebug("TableRowCountPanel.cancel()", "Trying to cancel the current statement");
			try
			{
				currentStatement.cancel();
			}
			catch (SQLException sql)
			{
				LogMgr.logWarning("TableRowCountPanel.cancel()", "Could not cancel statement", sql);
			}
		}
	}

	private void connectAndRetrieve()
	{
		WbThread conn = new WbThread("RowCountConnect")
		{
			@Override
			public void run()
			{
				checkConnection();
				doRetrieveRowCounts();
			}
		};
		conn.start();

	}
	public void retrieveRowCounts()
	{
		if (CollectionUtil.isEmpty(tables)) return;

		if (dbConnection == null)
		{
			connectAndRetrieve();
			return;
		}

		WbThread retrieveThread = new WbThread("RowCounter")
		{
			@Override
			public void run()
			{
				doRetrieveRowCounts();
			}
		};
		retrieveThread.start();
	}

	private void doRetrieveRowCounts()
	{
		if (!WbSwingUtilities.isConnectionIdle(this, dbConnection))
		{
			return;
		}

		cancelAction.setEnabled(true);
		cancel = false;

		String[] tableListColumns = dbConnection.getMetadata().getTableListColumns();
		String[] columns = new String[tableListColumns.length];

		columns[0] = ResourceMgr.getString("TxtRowCnt").toUpperCase();
		columns[1] = tableListColumns[0];
		columns[2] = tableListColumns[1];
		columns[3] = tableListColumns[2];
		columns[4] = tableListColumns[3];

		DataStore ds = new DataStore(columns, new int[] { Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR} );
		DataStoreTableModel model = new DataStoreTableModel(ds);
		model.setAllowEditing(false);
		setModel(model);
		ResultSet rs = null;

		try
		{
			dbConnection.setBusy(true);
			TableSelectBuilder builder = new TableSelectBuilder(dbConnection, "tabledata");
			currentStatement = dbConnection.createStatementForQuery();

			WbSwingUtilities.showWaitCursor(scrollPane);

			this.window.setTitle(RunningJobIndicator.TITLE_PREFIX + ResourceMgr.getString("TxtWindowTitleRowCount"));
			boolean useSavepoint = dbConnection.getDbSettings().useSavePointForDML();

			int tblCount = tables.size();
			for (int tableNum=0; tableNum < tblCount; tableNum++)
			{
				if (cancel) break;

				TableIdentifier table = tables.get(tableNum);
				showTable(table, tableNum, tblCount);
				String sql = builder.getSelectForCount(table);

				rs = runStatement(sql, useSavepoint);
				if (cancel) break;

				long rowCount = 0;
				if (rs == null)
				{
					rowCount = -1;
				}
				else if (rs.next())
				{
					rowCount = rs.getLong(1);
				}
				SqlUtil.closeResult(rs);
				addRowCount(table, rowCount);
			}
		}
		catch (SQLException sql)
		{
			LogMgr.logError("TableRowCountPanel.retrieveRowCounts()", "Error retrieving table count", sql);
		}
		finally
		{
			SqlUtil.closeAll(rs, currentStatement);
			currentStatement = null;
			dbConnection.setBusy(false);
			showStatusMessage("");
			WbSwingUtilities.showDefaultCursor(scrollPane);
			window.setTitle(ResourceMgr.getString("TxtWindowTitleRowCount"));
			data.checkCopyActions();
			cancelAction.setEnabled(false);
		}

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				data.requestFocusInWindow();
			}
		});
	}

	private ResultSet runStatement(String sql, boolean useSavepoint)
	{
		ResultSet rs = null;
		Savepoint sp = null;

		try
		{
			if (useSavepoint)
			{
				sp = dbConnection.setSavepoint();
			}

			rs = currentStatement.executeQuery(sql);
			if (useSeparateConnection)
			{
				if (this.dbConnection.selectStartsTransaction())
				{
					dbConnection.rollback();
				}
			}
			else
			{
				dbConnection.rollback(sp);
			}
		}
		catch (SQLException ex)
		{
			dbConnection.rollback(sp);
			LogMgr.logError("TableRowCountPanel.retrieveRowCounts()", "Error retrieving table count", ex);
			rs = null;
		}
		return rs;
	}

	private void showTable(final TableIdentifier table, int current, int total)
	{
		String msg = ResourceMgr.getFormattedString("MsgCalculatingRowCount", table.getTableExpression(), current, total);
		showStatusMessage(msg);
	}

	private void showStatusMessage(final String message)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (StringUtil.isBlank(message))
				{
					statusBar.setText(" " + Integer.toString(data.getRowCount()) + " " + ResourceMgr.getString("TxtTableListObjects"));
				}
				else
				{
					statusBar.setText(" " + message);
				}
			}
		});
	}

	private void addRowCount(final TableIdentifier table, final long count)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				DataStoreTableModel model = data.getDataStoreTableModel();
				int row = model.addRow();
				model.setValueAt(Long.valueOf(count), row, 0);
				model.setValueAt(table.getTableName(), row, 1);
				model.setValueAt(table.getObjectType(), row, 2);
				model.setValueAt(table.getCatalog(), row, 3);
				model.setValueAt(table.getSchema(), row, 4);
				data.adjustRowsAndColumns();
			}
		});
	}
	private void setModel(final DataStoreTableModel model)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				data.setModel(model, true);
			}
		});
	}

	public void showWindow(Window aParent)
	{
		if (this.window == null)
		{
			this.window = new JFrame(ResourceMgr.getString("TxtWindowTitleRowCount"));
			this.window.getContentPane().setLayout(new BorderLayout());
			this.window.getContentPane().add(this, BorderLayout.CENTER);

			ResourceMgr.setWindowIcons(window, "rowcounts");

			if (!Settings.getInstance().restoreWindowSize(this.window, TableRowCountPanel.class.getName()))
			{
				this.window.setSize(500, 400);
			}

			if (!Settings.getInstance().restoreWindowPosition(this.window, TableRowCountPanel.class.getName()))
			{
				WbSwingUtilities.center(this.window, aParent);
			}
			this.window.addWindowListener(this);
			WbManager.getInstance().registerToolWindow(this);
		}
		this.window.setVisible(true);
		this.retrieveRowCounts();
	}

	private void doClose()
	{
		cancelExecution();
		Settings.getInstance().storeWindowPosition(this.window, TableRowCountPanel.class.getName());
		Settings.getInstance().storeWindowSize(this.window, TableRowCountPanel.class.getName());
		this.window.setVisible(false);
		this.window.dispose();
		this.window = null;
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		WbManager.getInstance().unregisterToolWindow(this);
		doClose();
	}


	@Override
	public void windowClosed(WindowEvent e)
	{
		disconnect();
	}

	@Override
	public void windowIconified(WindowEvent e)
	{

	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{

	}

	@Override
	public void windowActivated(WindowEvent e)
	{

	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public void closeWindow()
	{
		doClose();
	}

	@Override
	public void disconnect()
	{
		if (useSeparateConnection && dbConnection != null)
		{
			dbConnection.disconnect();
			dbConnection = null;
		}
	}

	@Override
	public void activate()
	{
		if (window != null)
		{
			window.requestFocus();
		}
	}

	@Override
	public WbConnection getConnection()
	{
		return dbConnection;
	}

	@Override
	public JFrame getWindow()
	{
		return window;
	}

}
