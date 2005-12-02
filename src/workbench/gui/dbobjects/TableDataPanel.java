/*
 * TableDataPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

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
import workbench.gui.actions.SelectKeyColumnsAction;
import workbench.interfaces.PropertyStorage;
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
import workbench.util.StringUtil;
import workbench.util.WbThread;
import workbench.interfaces.JobErrorHandler;
import java.awt.Cursor;
import java.awt.EventQueue;
import workbench.util.WbWorkspace;


/**
 *
 * @author  support@sql-workbench.net
 *
 */
public class TableDataPanel
  extends JPanel
	implements Reloadable, ActionListener, Interruptable, TableDeleteListener, MouseListener
{
	private WbConnection dbConnection;
	private DwPanel dataDisplay;

	private ReloadAction reloadAction;

	private JButton config;
	private JLabel tableNameLabel;
	private JLabel rowCountLabel;
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

    topPanel = new JPanel();
		topPanel.setMaximumSize(new Dimension(32768, 32768));
		BoxLayout box = new BoxLayout(topPanel, BoxLayout.X_AXIS);
		topPanel.setLayout(box);

		this.reloadAction = new ReloadAction(this);
		this.reloadAction.setTooltip(ResourceMgr.getDescription("TxtLoadTableData"));

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
		topPanel.add(new JLabel(ResourceMgr.getString("LabelTable") + ":"));
		Font std = Settings.getInstance().getStandardLabelFont();
		Font bold = std.deriveFont(Font.BOLD);
		tableNameLabel = new JLabel();
		tableNameLabel.setFont(bold);
		topPanel.add(Box.createHorizontalStrut(5));
		topPanel.add(tableNameLabel);

		topPanel.add(Box.createHorizontalStrut(10));
		JLabel l = new JLabel(ResourceMgr.getString("LabelTableDataRowCount"));
		l.setToolTipText(ResourceMgr.getDescription("LabelTableDataRowCount"));
		topPanel.add(l);
		topPanel.add(Box.createHorizontalStrut(5));
		rowCountLabel = new JLabel();
		rowCountLabel.setToolTipText(ResourceMgr.getDescription("LabelTableDataRowCount"));
		rowCountLabel.setFont(bold);
		rowCountLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		rowCountLabel.addMouseListener(this);
		topPanel.add(rowCountLabel);
		topPanel.add(Box.createHorizontalStrut(10));

		autoRetrieve = new JCheckBox(ResourceMgr.getString("LabelAutoLoadTableData"));
		autoRetrieve.setToolTipText(ResourceMgr.getDescription("LabelAutoLoadTableData"));
		autoRetrieve.setHorizontalTextPosition(SwingConstants.LEFT);
		topPanel.add(autoRetrieve);

		topPanel.add(Box.createHorizontalGlue());
		this.config = new WbButton(ResourceMgr.getString("LabelConfigureWarningThreshold"));
		this.config.addActionListener(this);
		this.config.setBorder(WbSwingUtilities.FLAT_BUTTON_BORDER);
		topPanel.add(this.config);

		this.add(topPanel, BorderLayout.NORTH);

		mytoolbar.add(this.dataDisplay.getUpdateDatabaseAction());
		mytoolbar.add(this.dataDisplay.getSelectKeysAction());
		mytoolbar.addSeparator();
		mytoolbar.add(this.dataDisplay.getInsertRowAction());
		mytoolbar.add(this.dataDisplay.getCopyRowAction());
		mytoolbar.add(this.dataDisplay.getDeleteRowAction());
		mytoolbar.addSeparator();
		mytoolbar.add(this.dataDisplay.getTable().getFilterAction());
		mytoolbar.add(this.dataDisplay.getTable().getResetFilterAction());

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
		this.rowCountLabel.setText("");
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
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next())
			{
				rowCount = rs.getLong(1);
			}
			this.rowCountLabel.setText(Long.toString(rowCount));
		}
		catch (SQLException e)
		{
			this.rowCountLabel.setText(ResourceMgr.getString("TxtError"));
			LogMgr.logError("TableDataPanel.showRowCount()", "Error retrieving rowcount for " + this.table.getTableExpression() + ": " + ExceptionUtil.getDisplay(e), null);
		}
		catch (Exception e)
		{
			this.rowCountLabel.setText(ResourceMgr.getString("TxtError"));
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
		this.tableNameLabel.setText(this.table.getTableName());
		this.topPanel.doLayout();
		this.topPanel.repaint();
	}

	private String buildSqlForTable(boolean forRowCount)
	{
		if (this.table == null) return null;

		StringBuffer sql = new StringBuffer(100);
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

	private synchronized void retrieveStart()
	{
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
	}

	private void dbUpdateStart()
	{
		this.reloadAction.setEnabled(false);
		synchronized (this.retrieveLock)
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
			synchronized (this.retrieveLock)
			{
				this.updateRunning = false;
			}
		}
	}

	public boolean isRetrieving()
	{
		synchronized (this.retrieveLock)
		{
			return this.retrieveRunning || this.updateRunning;
		}
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
			dataDisplay.setShowErrorMessages(true);
			dataDisplay.setAutomaticUpdateTableCheck(false);
			dataDisplay.scriptStarting();
			dataDisplay.setMaxRows(this.getMaxRows());
			dataDisplay.runStatement(sql);
			dataDisplay.setUpdateTable(this.table);
			dataDisplay.getSelectKeysAction().setEnabled(true);
			String header = ResourceMgr.getString("TxtTableDataPrintHeader") + " " + table;
			dataDisplay.setPrintHeader(header);
			dataDisplay.setStatusMessage("");
			dataDisplay.showlastExecutionTime();
		}
		catch (OutOfMemoryError mem)
		{
			EventQueue.invokeLater(new Runnable()
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
			dataDisplay.scriptFinished();
			cancelRetrieve.setEnabled(false);
			reloadAction.setEnabled(true);
			this.retrieveEnd();
			WbSwingUtilities.showDefaultCursor(this);
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
		props.setProperty(prefix + "maxrows", this.getMaxRows());
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
			int max = this.getMaxRows();
			if ( this.warningThreshold > 0
				   && rows > this.warningThreshold
				   && max == 0)
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
			TableDataSettings p = new TableDataSettings();
			p.setThresholdValue(this.warningThreshold);
			p.setAutoloadData(this.autoRetrieve.isSelected());
			p.setAutoloadRowCount(this.autoloadRowCount);
			Window parent = SwingUtilities.getWindowAncestor(this);
			int choice = JOptionPane.showConfirmDialog(parent, p, ResourceMgr.getString("LabelConfigureWarningThresholdTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice == JOptionPane.OK_OPTION)
			{
				this.warningThreshold = p.getThresholdValue();
				this.autoRetrieve.setSelected(p.getAutoloadData());
				this.autoloadRowCount = p.getAutoloadRowCount();
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
		if (tables.contains(this.table))
		{
			this.reset();
		}
	}

	public void mouseClicked(java.awt.event.MouseEvent e)
	{
		if (e.getSource() == this.rowCountLabel && e.getClickCount() == 2)
		{
			WbThread t = new WbThread("RowCount Thread")
			{
				public void run()
				{
					synchronized (retrieveLock)
					{
						showRowCount();
					}
				}
			};
			t.start();
		}
	}

	public void mouseEntered(java.awt.event.MouseEvent e)
	{
	}

	public void mouseExited(java.awt.event.MouseEvent e)
	{
	}

	public void mousePressed(java.awt.event.MouseEvent e)
	{
	}

	public void mouseReleased(java.awt.event.MouseEvent e)
	{
	}



}