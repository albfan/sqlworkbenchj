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
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.db.TableIdentifier;
import workbench.db.TableSelectBuilder;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.RunningJobIndicator;
import workbench.gui.components.WbTable;

import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class TableRowCountPanel
	extends JPanel
	implements WindowListener
{
	private WbTable data;
	private JLabel statusBar;
	private List<TableIdentifier> tables;
	private Statement currentStatement;
	private boolean cancel;
	private JFrame window;

	public TableRowCountPanel(List<TableIdentifier> toCount)
	{
		super(new BorderLayout(0,0));
		tables = toCount;
		statusBar = new JLabel();
		data = new WbTable(false, false, false);
		JScrollPane scroll = new JScrollPane(data);
		JPanel statusPanel = new JPanel(new BorderLayout(0,0));
		Border b = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(1,5,1,1));
		statusPanel.setBorder(b);
		statusPanel.add(statusBar);

		add(scroll, BorderLayout.CENTER);
		add(statusPanel, BorderLayout.PAGE_END);
	}

	public void cancel()
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

	public void retrieveRowCounts(final WbConnection conn)
	{
		if (CollectionUtil.isEmpty(tables)) return;
		if (conn.isBusy()) return;
		WbThread retrieveThread = new WbThread("RowCounter")
		{
			@Override
			public void run()
			{
				doRetrieveRowCounts(conn);
			}
		};
		retrieveThread.start();
	}

	private void doRetrieveRowCounts(WbConnection conn)
	{
		String[] tableListColumns = conn.getMetadata().getTableListColumns();
		String[] columns = new String[tableListColumns.length];

		columns[0] = "ROWS";
		columns[1] = tableListColumns[0];
		columns[2] = tableListColumns[1];
		columns[3] = tableListColumns[2];
		columns[4] = tableListColumns[3];

		DataStore ds = new DataStore(columns, new int[] { Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR} );
		DataStoreTableModel model = new DataStoreTableModel(ds);
		setModel(model);

		ResultSet rs = null;

		try
		{
			conn.setBusy(true);
			TableSelectBuilder builder = new TableSelectBuilder(conn, "tabledata");
			currentStatement = conn.createStatementForQuery();

			WbSwingUtilities.showWaitCursorOnWindow(data);

			this.window.setTitle(RunningJobIndicator.TITLE_PREFIX + ResourceMgr.getString("TxtWindowTitleRowCount"));
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
		}
		catch (SQLException sql)
		{
			LogMgr.logError("TableRowCountPanel.retrieveRowCounts()", "Error retrieving table count", sql);
		}
		finally
		{
			SqlUtil.closeAll(rs, currentStatement);
			conn.setBusy(false);
			showStatusMessage("   ");
			WbSwingUtilities.showDefaultCursorOnWindow(data);
			this.window.setTitle(ResourceMgr.getString("TxtWindowTitleRowCount"));
			data.checkCopyActions();
		}
	}

	private void showTable(final TableIdentifier table)
	{
		showStatusMessage(table.getTableExpression());
	}

	private void showStatusMessage(final String message)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				statusBar.setText(message);
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

	public void showWindow(Window aParent, WbConnection conn)
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
		this.retrieveRowCounts(conn);
	}

	@Override
	public void windowOpened(WindowEvent e)
	{

	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		cancel();
		Settings.getInstance().storeWindowPosition(this.window, TableRowCountPanel.class.getName());
		Settings.getInstance().storeWindowSize(this.window, TableRowCountPanel.class.getName());
		this.window.setVisible(false);
		this.window.dispose();
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
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
