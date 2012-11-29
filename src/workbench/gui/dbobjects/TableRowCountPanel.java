/*
 * TableRowCountPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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

import workbench.interfaces.Interruptable;
import workbench.interfaces.Reloadable;
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
	implements WindowListener, Reloadable, Interruptable
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
	public TableRowCountPanel(List<TableIdentifier> toCount, WbConnection connection)
	{
		super(new BorderLayout(0,0));
		tables = toCount;
		instanceCount ++;
		sourceConnection = connection;

		statusBar = new JLabel();
		data = new WbTable(false, false, false);
		data.setReadOnly(true);

		JScrollPane scroll = new JScrollPane(data);
		JPanel statusPanel = new JPanel(new BorderLayout(0,0));

		Border etched = new EtchedBorder(EtchedBorder.LOWERED);
		Border current = scroll.getBorder();
		Border frame = new CompoundBorder(new EmptyBorder(3,3,0,3), current);
		scroll.setBorder(frame);

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
		add(scroll, BorderLayout.CENTER);
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

			WbSwingUtilities.showWaitCursor(data);

			this.window.setTitle(RunningJobIndicator.TITLE_PREFIX + ResourceMgr.getString("TxtWindowTitleRowCount"));
			boolean useSavepoint = dbConnection.getDbSettings().useSavePointForDML();

			Savepoint sp = null;

			if (useSavepoint)
			{
				sp = dbConnection.setSavepoint();
			}

			for (TableIdentifier table : tables)
			{
				if (cancel) break;

				showTable(table);
				String sql = builder.getSelectForCount(table);

				rs = currentStatement.executeQuery(sql);
				if (cancel) break;

				long count = 0;
				if (rs.next())
				{
					count = rs.getLong(1);
				}
				SqlUtil.closeResult(rs);
				addRowCount(table, count);
			}

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
			WbSwingUtilities.showDefaultCursorOnWindow(data);
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

	private void showTable(final TableIdentifier table)
	{
		String msg = ResourceMgr.getFormattedString("MsgCalculatingRowCount", table.getTableExpression());
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
		}
		this.window.setVisible(true);
		this.retrieveRowCounts();
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		cancelExecution();
		Settings.getInstance().storeWindowPosition(this.window, TableRowCountPanel.class.getName());
		Settings.getInstance().storeWindowSize(this.window, TableRowCountPanel.class.getName());
		this.window.setVisible(false);
		this.window.dispose();
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		if (useSeparateConnection && dbConnection != null)
		{
			dbConnection.disconnect();
		}
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


}
