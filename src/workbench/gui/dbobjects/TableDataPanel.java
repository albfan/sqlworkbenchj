/*
 * TableDataPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
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
import workbench.util.WbThread;
import workbench.interfaces.JobErrorHandler;
import java.awt.Cursor;
import javax.swing.JComponent;

/**
 *
 * @author  info@sql-workbench.net
 *
 */
public class TableDataPanel
  extends JPanel
	implements Reloadable, ActionListener, Interruptable, TableDeleteListener
{
	private WbConnection dbConnection;
	private DwPanel dataDisplay;

	private ReloadAction reloadAction;

	private JButton config;
	private JLabel rowCountLabel;
	private JCheckBox autoRetrieve;

	private long warningThreshold = -1;

	private boolean shiftDown = false;
	private boolean retrieveRunning = false;
	private boolean updateRunning = false;

	private TableIdentifier table;
	private ImageIcon loadingIcon;
	private Image loadingImage;

	private StopAction cancelRetrieve;

	public TableDataPanel() throws Exception
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

		this.dataDisplay.setManageActions(true);
		this.dataDisplay.setShowLoadProcess(true);
		this.dataDisplay.setDefaultStatusMessage("");
		this.dataDisplay.setShowErrorMessages(true);
		this.dataDisplay.getTable().setMaxColWidth(Settings.getInstance().getMaxColumnWidth());
		this.dataDisplay.getTable().setMinColWidth(Settings.getInstance().getMinColumnWidth());
		this.dataDisplay.setSaveChangesInBackground(true);

    JPanel topPanel = new JPanel();
		topPanel.setMaximumSize(new Dimension(32768, 32768));
		//topPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		BoxLayout box = new BoxLayout(topPanel, BoxLayout.X_AXIS);
		topPanel.setLayout(box);

		this.reloadAction = new ReloadAction(this);
		this.reloadAction.setTooltip(ResourceMgr.getDescription("TxtLoadTableData"));

		WbToolbar toolbar = new WbToolbar();
		toolbar.addDefaultBorder();
		topPanel.add(toolbar);
		toolbar.add(this.reloadAction);
		toolbar.addSeparator();

		this.cancelRetrieve = new StopAction(this);
		this.cancelRetrieve.setEnabled(false);
		toolbar.add(this.cancelRetrieve);
		toolbar.addSeparator();

		//JButton b = a.getToolbarButton();
		//b.setBorder(new EtchedBorder());
		//b.setToolTipText(ResourceMgr.getDescription("TxtLoadTableData"));
		//topPanel.add(b);

		topPanel.add(Box.createHorizontalStrut(15));

		autoRetrieve = new JCheckBox(ResourceMgr.getString("LabelAutoRetrieveTableData"));
		autoRetrieve.setToolTipText(ResourceMgr.getDescription("LabelAutoRetrieveTableData"));
		autoRetrieve.setHorizontalTextPosition(SwingConstants.LEFT);
		topPanel.add(autoRetrieve);

		topPanel.add(Box.createHorizontalStrut(10));

		rowCountLabel = new JLabel(ResourceMgr.getString("LabelTableDataRowCount"));
		rowCountLabel.setToolTipText(ResourceMgr.getDescription("LabelTableDataRowCount"));
		Font std = Settings.getInstance().getStandardFont();
		Font bold = std.deriveFont(Font.BOLD);
		rowCountLabel.setFont(bold);
		rowCountLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		topPanel.add(rowCountLabel);

		topPanel.add(Box.createHorizontalStrut(10));

		topPanel.add(Box.createHorizontalGlue());
		this.config = new WbButton(ResourceMgr.getString("LabelConfigureWarningThreshold"));
		this.config.addActionListener(this);
		Border border = new CompoundBorder(new EtchedBorder(), new EmptyBorder(1,6,1,6));
		this.config.setBorder(border);
		topPanel.add(this.config);

		//maxRowsLabel = new JLabel(ResourceMgr.getString("LabelTableDataMaxRows"));
		//topPanel.add(maxRowsLabel);

		//this.maxRowField = new JTextField(4);
		//topPanel.add(this.maxRowField);

		this.add(topPanel, BorderLayout.NORTH);

		toolbar.add(this.dataDisplay.getUpdateDatabaseAction());
		toolbar.addSeparator();
		toolbar.add(this.dataDisplay.getInsertRowAction());
		toolbar.add(this.dataDisplay.getCopyRowAction());
		toolbar.add(this.dataDisplay.getDeleteRowAction());

		//this.dataTable = this.dataDisplay.getTable();
		//JScrollPane scroll = new JScrollPane(this.dataTable);
		this.add(dataDisplay, BorderLayout.CENTER);
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

	private int getMaxRows()
	{
		return this.dataDisplay.getMaxRows();
	}

	public void reset()
	{
		if (this.isRetrieving()) return;
		this.dataDisplay.clearContent();
		this.rowCountLabel.setText(ResourceMgr.getString("LabelTableDataRowCount"));
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

	public long showRowCount()
	{
		if (this.dbConnection == null) return -1;
		if (this.isRetrieving()) return -1;

		this.rowCountLabel.setText(ResourceMgr.getString("LabelTableDataRowCount"));
		this.rowCountLabel.setIcon(this.getLoadingIndicator());

		this.reloadAction.setEnabled(false);
		this.dataDisplay.setStatusMessage(ResourceMgr.getString("MsgCalculatingRowCount"));
		this.repaint();
		this.rowCountLabel.repaint();
		String sql = this.buildSqlForTable(true);
		if (sql == null) return -1;

		long rowCount = 0;
		Statement stmt = null;
		ResultSet rs = null;

		try
		{
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				rowCount = rs.getLong(1);
			}
			this.rowCountLabel.setText(ResourceMgr.getString("LabelTableDataRowCount") + " " + rowCount);

		}
		catch (SQLException e)
		{
			this.rowCountLabel.setText(ResourceMgr.getString("LabelTableDataRowCount") + " " + ResourceMgr.getString("TxtError"));
			LogMgr.logError("TableDataPanel.showRowCount()", "Error retrieving rowcount for " + this.table.getTableExpression() + ": " + ExceptionUtil.getDisplay(e), null);
		}
		catch (Exception e)
		{
			this.rowCountLabel.setText(ResourceMgr.getString("LabelTableDataRowCount") + " " + ResourceMgr.getString("TxtError"));
			LogMgr.logError("TableDataPanel.showRowCount()", "Error retrieving rowcount for " + this.table.getTableExpression(), e);
		}
		finally
		{
			this.dataDisplay.setStatusMessage("");
			this.clearLoadingImage();
			try { if (rs != null) rs.close(); } catch (Throwable th) {}
			try { if (stmt != null) stmt.close(); } catch (Throwable th) {}
			this.reloadAction.setEnabled(true);
		}
		return rowCount;
	}

	public void setTable(TableIdentifier aTable)
	{
		if (!this.isRetrieving()) reset();
		this.table = aTable;
	}

	private String buildSqlForTable(boolean forRowCount)
	{
		//if (this.currentTable == null) return null;

		if (this.table == null) return null;

		StringBuffer sql = new StringBuffer(100);
		if (forRowCount)
			sql.append("SELECT COUNT(*) FROM ");
		else
			sql.append("SELECT * FROM ");

		sql.append(this.table.getTableExpression());

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

	private synchronized void retrieveStart()
	{
		this.retrieveRunning = true;
	}

	private synchronized void retrieveEnd()
	{
		this.retrieveRunning = false;
	}

	public boolean isRetrieving()
	{
		return this.retrieveRunning || this.updateRunning;
	}

	private void doRetrieve()
	{
		if (this.isRetrieving()) return;

    String sql = this.buildSqlForTable(false);
    if (sql == null) return;

		this.retrieveStart();

		this.cancelRetrieve.setEnabled(true);
		this.reloadAction.setEnabled(false);
		try
		{
			//WbSwingUtilities.showWaitCursor(this);
			dataDisplay.setShowErrorMessages(true);
			dataDisplay.setAutomaticUpdateTableCheck(false);
			dataDisplay.scriptStarting();
			dataDisplay.setMaxRows(this.getMaxRows());
			dataDisplay.runStatement(sql);
			dataDisplay.setUpdateTable(this.table.getTableExpression());
			String header = ResourceMgr.getString("TxtTableDataPrintHeader") + " " + table;
			dataDisplay.setPrintHeader(header);
			dataDisplay.setStatusMessage("");
		}
		catch (OutOfMemoryError mem)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					WbSwingUtilities.showErrorMessage(TableDataPanel.this, ResourceMgr.getString("MsgOutOfMemoryError"));
				}
			});
		}
		catch (Throwable e)
		{
			LogMgr.logError("TableDataPanel.doRetrieve()", "Error retrieving table data", e);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
			dataDisplay.scriptFinished();
			cancelRetrieve.setEnabled(false);
			reloadAction.setEnabled(true);
			this.retrieveEnd();
			final JComponent th = this;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					WbSwingUtilities.showDefaultCursor(th);
				}
			});
		}
	}

	public void setCursor(Cursor newCursor)
	{
		super.setCursor(newCursor);
		this.dataDisplay.setCursor(null);
	}

	public void retrieve()
	{
		if (this.isRetrieving()) return;

		Thread t = new WbThread("TableDataPanel retrieve thread")
		{
			public void run()
			{
				doRetrieve();
			}
		};
		t.start();
	}

	public void saveSettings()
	{
		Settings.getInstance().setProperty(TableDataPanel.class.getName(), "maxrows", this.getMaxRows());
		String auto = Boolean.toString(this.autoRetrieve.isSelected());
		Settings.getInstance().setProperty(TableDataPanel.class.getName(), "autoretrieve", auto);
		Settings.getInstance().setProperty(TableDataPanel.class.getName(), "warningthreshold", Long.toString(this.warningThreshold));
	}

	public void restoreSettings()
	{
		int max = Settings.getInstance().getIntProperty(TableDataPanel.class.getName(), "maxrows", 500);
		this.dataDisplay.setMaxRows(max);
		boolean auto = "true".equals(Settings.getInstance().getProperty(TableDataPanel.class.getName(), "autoretrieve", "false"));
		this.autoRetrieve.setSelected(auto);

		try
		{
			String v = Settings.getInstance().getProperty(TableDataPanel.class.getName(), "warningthreshold", "1500");
			this.warningThreshold = Long.parseLong(v);
		}
		catch (Exception e)
		{
			this.warningThreshold = -1;
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
		long rows = this.showRowCount();
		if (this.autoRetrieve.isSelected() && includeData)
		{
			int max = this.getMaxRows();
			if ( this.warningThreshold > 0 &&
			     rows > this.warningThreshold &&
			     (max > this.warningThreshold || max == 0)
				 )
			{
				String msg = ResourceMgr.getString("MsgDataDisplayWarningThreshold");
				msg = msg.replaceAll("%rows%", Long.toString(rows));
				int choice = JOptionPane.showConfirmDialog(this, msg, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.NO_OPTION) return;
			}
			this.doRetrieve();
		}
	}

	public void reload()
	{
		this.reset();
		this.showRowCount();
		this.retrieve();
	}

	public Window getParentWindow()
	{
		return SwingUtilities.getWindowAncestor(this);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.config)
		{
			ConfigureWarningThreshold p = new ConfigureWarningThreshold();
			p.setThresholdValue(this.warningThreshold);
			Window parent = SwingUtilities.getWindowAncestor(this);
			int choice = JOptionPane.showConfirmDialog(parent, p, ResourceMgr.getString("LabelConfigureWarningThresholdTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice == JOptionPane.OK_OPTION)
			{
				this.warningThreshold = p.getThresholdValue();
			}
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
		int count = tables.size();
		for (int i=0; i < count; i++)
		{
			TableIdentifier tid = (TableIdentifier)tables.get(i);
			if (this.table.equals(tid))
			{
				this.reset();
				break;
			}
		}
	}

	private void dbUpdateStart()
	{
		this.reloadAction.setEnabled(false);
		synchronized (this)
		{
			this.updateRunning = true;
		}
	}

	private void dbUpdateEnd()
	{
		try
		{
			this.reloadAction.setEnabled(true);
		}
		finally
		{
			synchronized (this)
			{
				this.updateRunning = false;
			}
		}
	}


}