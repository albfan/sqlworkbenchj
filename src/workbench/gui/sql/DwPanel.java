package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;

import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CopyRowAction;
import workbench.gui.actions.DeleteRowAction;
import workbench.gui.actions.InsertRowAction;
import workbench.gui.actions.StartEditAction;
import workbench.gui.actions.UpdateDatabaseAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.OneLineTableModel;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.interfaces.DbData;
import workbench.interfaces.DbUpdater;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunner;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.interfaces.JobErrorHandler;

/**
 *	A Panel which displays the result of a SELECT statement.
 */
public class DwPanel
	extends JPanel
	implements TableModelListener, ListSelectionListener, ChangeListener,
						RowActionMonitor, DbData, DbUpdater, Interruptable, JobErrorHandler
{
	private WbTable infoTable;
	private DwStatusBar statusBar;

	private String sql;
	private String lastMessage;
	private WbConnection dbConnection;
	private OneLineTableModel errorModel;
	private TableModel resultEmptyMsgModel;
	private boolean hasResultSet = false;

	private WbScrollPane scrollPane;
	private DefaultCellEditor defaultEditor;
	private DefaultCellEditor defaultNumberEditor;
	private int maxRows = 0;

	private boolean success = false;
	private boolean showLoadProgress = false;

	private UpdateDatabaseAction updateAction = null;
	private InsertRowAction insertRow = null;
	private CopyRowAction duplicateRow = null;
	private DeleteRowAction deleteRow = null;
	private StartEditAction startEdit = null;

	private boolean editingStarted = false;
	private boolean batchUpdate = false;
	private boolean manageUpdateAction = false;
	private boolean showErrorMessages = false;
	private boolean readOnly = false;

	private long rowsAffectedByScript = -1;
	private boolean scriptRunning = false;

	private StatementRunner stmtRunner;

	public DwPanel()
	{
		this(null);
	}

	public DwPanel(DwStatusBar aStatusBar)
	{
		JTextField stringField = new JTextField();
		stringField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		stringField.addMouseListener(new TextComponentMouseListener());
		this.defaultEditor = new DefaultCellEditor(stringField);

		JTextField numberField = new JTextField();
		numberField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		numberField.setHorizontalAlignment(SwingConstants.RIGHT);
		numberField.addMouseListener(new TextComponentMouseListener());

		this.defaultNumberEditor = new DefaultCellEditor(numberField);
		this.initLayout(aStatusBar);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(infoTable);
		pol.addComponent(infoTable);
		pol.addComponent(this.statusBar.tfMaxRows);
		this.setFocusTraversalPolicy(pol);
		this.setDoubleBuffered(true);
		this.stmtRunner = new StatementRunner();
		this.stmtRunner.setRowMonitor(this);
		this.infoTable.addTableModelListener(this);

		this.updateAction = new UpdateDatabaseAction(this);
		this.insertRow = new InsertRowAction(this);
		this.deleteRow = new DeleteRowAction(this);
		this.startEdit = new StartEditAction(this);
		this.duplicateRow = new CopyRowAction(this);

		//infoTable.addPopupAction(this.startEdit, true);
		infoTable.addPopupAction(this.updateAction, true);
		infoTable.addPopupAction(this.insertRow, true);
		infoTable.addPopupAction(this.deleteRow, false);
		this.infoTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.infoTable.setRowSelectionAllowed(true);
		this.infoTable.getSelectionModel().addListSelectionListener(this);
	}

	public void setManageActions(boolean aFlag)
	{
		this.manageUpdateAction = aFlag;
	}

	public void setDefaultStatusMessage(String aMessage)
	{
		this.statusBar.setReadyMsg(aMessage);
	}

	public void disconnect()
	{
		try
		{
			this.setConnection(null);
		}
		catch (Exception e)
		{
		}
	}

	public void setShowLoadProcess(boolean aFlag)
	{
		this.showLoadProgress = aFlag;
	}

	private String updateMsg;
	private int monitorType = -1;

	private void clearRowMonitorSettings()
	{
		this.updateMsg = null;
		this.monitorType = -1;
	}

	public void setMonitorType(int aType)
	{
		this.monitorType = aType;
		try
		{
			switch (aType)
			{
				case RowActionMonitor.MONITOR_INSERT:
					this.updateMsg = ResourceMgr.getString("MsgImportingRow") + " ";
					break;
				case RowActionMonitor.MONITOR_UPDATE:
					this.updateMsg = ResourceMgr.getString("MsgUpdatingRow") + " ";
					break;
				case RowActionMonitor.MONITOR_LOAD:
					this.updateMsg = ResourceMgr.getString("MsgLoadingRow") + " ";
					break;
				case RowActionMonitor.MONITOR_EXPORT:
					this.updateMsg = ResourceMgr.getString("MsgWritingRow") + " ";
					break;
				case RowActionMonitor.MONITOR_COPY:
					this.updateMsg = ResourceMgr.getString("MsgCopyingRow") + " ";
					break;
				default:
					clearRowMonitorSettings();
			}
		}
		catch (Exception e)
		{
			clearRowMonitorSettings();
		}
	}

	/**
	 *	Callback method from the {@link workbench.interfaces.RowActionMonitor}
	 */
	public void setCurrentRow(int currentRow, int totalRows)
	{
		if (this.monitorType < 0) return;
		StringBuffer msg = new StringBuffer(40);
		msg.append(this.updateMsg);
		msg.append(currentRow);
		if (totalRows > 0)
		{
			msg.append('/');
			msg.append(totalRows);
		}
		this.statusBar.setStatusMessage(msg.toString());
	}

	public void setPrintHeader(String header)
	{
		this.infoTable.setPrintHeader(header);
	}

	/**
	 *	Enables or disables the display of error messages.
	 */
	public void setShowErrorMessages(boolean aFlag)
	{
		this.showErrorMessages = aFlag;
	}

	/**
	 *	Defines the connection for this DwPanel.
	 */
	public void setConnection(WbConnection aConn)
		throws SQLException
	{
		this.clearContent();
		this.sql = null;
		//this.lastStatement = null;
		this.lastMessage = null;
		this.dbConnection = aConn;
		this.hasResultSet = false;
		this.clearStatusMessage();
		this.stmtRunner.setConnection(aConn);
	}

	/**
	 *	Sets a delegate which performs the DbUpdates.
	 *  This delegate is passed to the UpdateDatabaseAction. The action will in turn
	 *  call the delegate's saveChangesToDatabase() method instead of ours.
	 *  @see workbench.interfaces.DbUpdater#saveChangesToDatabase()
	 *  @see saveChangesToDatabase()
	 */
	public void setUpdateDelegate(DbUpdater aDelegate)
	{
		this.updateAction.setClient(aDelegate);
	}

	private boolean saveChangesInBackground = false;
	
	public void setSaveChangesInBackground(boolean flag) 
	{ 
		this.saveChangesInBackground = flag; 
	}
	
	public synchronized void saveChangesToDatabase()
	{
		if (this.dbConnection == null) return;
		if (!this.shouldSaveChanges(this.dbConnection)) return;
		
		if (this.saveChangesInBackground) 
		{
			this.startBackgroundSave();
			return;
		}
		doSave();
	}

	private void doSave()
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			this.saveChanges(dbConnection, this);
		}
		catch (Exception e)
		{
			String msg = ResourceMgr.getString("ErrorUpdatingDb");
			WbManager.getInstance().showErrorMessage(this, msg + "\n" + e.getMessage());
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
		
	}
	private void startBackgroundSave()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				doSave();
			}
		};
		t.setName("DwPanel update thread");
		t.setDaemon(true);
		t.start();
	}
	
	public boolean shouldSaveChanges(WbConnection aConnection)
	{
		if (!WbManager.getSettings().getDbDebugMode()) return true;

		this.infoTable.stopEditing();
		DataStore ds = this.infoTable.getDataStore();

		boolean doSave = true;

		WbSwingUtilities.showWaitCursor(this);
		Window win = SwingUtilities.getWindowAncestor(this);
		try
		{
			Dimension max = new Dimension(800,600);
			Dimension pref = new Dimension(400, 300);
			EditorPanel preview = EditorPanel.createSqlEditor();
			preview.setEditable(false);
			preview.showFindOnPopupMenu();
			preview.setBorder(WbSwingUtilities.EMPTY_BORDER);
			preview.setPreferredSize(pref);
			preview.setMaximumSize(max);
			JScrollPane scroll = new JScrollPane(preview);
			scroll.setMaximumSize(max);
			List stmts = ds.getUpdateStatements(aConnection);
			StringBuffer text = new StringBuffer(stmts.size() * 80);
			for (int i=0; i < stmts.size(); i++)
			{
				DmlStatement dml = (DmlStatement)stmts.get(i);
				text.append(dml.getExecutableStatement(aConnection.getDatabaseProductName()));
				text.append(";\n");
			}
			preview.setText(text.toString());
			preview.setCaretPosition(0);
			WbSwingUtilities.showDefaultCursor(this);
			int choice = JOptionPane.showConfirmDialog(win, scroll, ResourceMgr.getString("MsgConfirmUpdates"), JOptionPane.OK_CANCEL_OPTION);
			if (choice == JOptionPane.CANCEL_OPTION) doSave = false;
		}
		catch (SQLException e)
		{
			this.lastMessage = ExceptionUtil.getDisplay(e);
			int choice = JOptionPane.showConfirmDialog(win, this.lastMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.NO_OPTION) doSave = false;
		}
		catch (OutOfMemoryError mem)
		{
			String msg = ResourceMgr.getString("MsgOutOfMemorySQLPreview");
			int choice = JOptionPane.showConfirmDialog(win, msg, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.NO_OPTION) doSave = false;
		}
		finally
		{
			WbSwingUtilities.showDefaultCursorOnWindow(this);
		}
		return doSave;
	}

	public synchronized int saveChanges(WbConnection aConnection, JobErrorHandler errorHandler)
		throws SQLException
	{
		int rows = 0;
		JobErrorHandler activeErrorHandler = this;
		if (errorHandler != null) activeErrorHandler = errorHandler;

		this.infoTable.stopEditing();

		try
		{
			DataStore ds = this.infoTable.getDataStore();
			long start, end;
      WbSwingUtilities.showWaitCursor(this);
			ds.setProgressMonitor(this);
			start = System.currentTimeMillis();
			rows = ds.updateDb(aConnection, errorHandler);
			end = System.currentTimeMillis();
			ds.setProgressMonitor(null);
			long sqlTime = (end - start);
			this.lastMessage = ResourceMgr.getString("MsgUpdateSuccessfull");
			this.lastMessage = this.lastMessage + "\n" + rows + " " + ResourceMgr.getString(ResourceMgr.MSG_ROWS_AFFECTED);
			this.lastMessage = this.lastMessage + ResourceMgr.getString("MsgExecTime") + " " + (((double)sqlTime) / 1000.0) + "s";
			this.endEdit();
		}
		catch (SQLException e)
		{
			this.lastMessage = ExceptionUtil.getDisplay(e);
			throw e;
		}
		finally
		{
    	WbSwingUtilities.showDefaultCursor(this);
			this.clearStatusMessage();
    }
		this.repaint();

		return rows;
	}

	public String getCurrentSql()
	{
		return this.sql;
	}

	public void setUpdateTable(String aTable)
	{
		this.infoTable.getDataStore().setUpdateTable(aTable);
	}

	public void setReadOnly(boolean aFlag)
	{
		this.readOnly = aFlag;
		if (this.readOnly && this.editingStarted)
		{
			this.endEdit();
			this.insertRow.setEnabled(false);
			this.deleteRow.setEnabled(false);
			this.updateAction.setEnabled(false);
			this.restoreOriginalValues();
		}
		else
		{
			this.insertRow.setEnabled(true);
		}
	}
	public boolean isReadOnly() { return this.readOnly; }

	public boolean checkUpdateTable()
	{
		if (this.readOnly) return false;
		DataStore ds = this.infoTable.getDataStore();
		if (ds == null) return false;
		if (this.dbConnection == null) return false;
		if (this.sql == null) return false;
		return ds.checkUpdateTable(this.sql, this.dbConnection);
	}

	public boolean isUpdateable()
	{
		if (this.infoTable.getDataStore() == null) return false;
		return this.infoTable.getDataStore().isUpdateable();
	}

	public boolean hasUpdateableColumns()
	{
		if (this.infoTable.getDataStore() == null) return false;
		return this.infoTable.getDataStore().hasUpdateableColumns();
	}

	public int getMaxRows()
	{
		return this.statusBar.getMaxRows();
	}

	public void setMaxRows(int aMax)
	{
		this.statusBar.setMaxRows(aMax);
	}

	public long getRowsAffectedByScript()
	{
		return rowsAffectedByScript;
	}

	/**
	 *	Execute the given SQL statement.
	 */
	public void runStatement(String aSql)
		throws SQLException, Exception
	{
		if (aSql == null)
		{
			LogMgr.logError(this, "No sql statement given!", null);
			return;
		}

		this.success = false;
		StatementRunnerResult result = null;

		try
		{
			long end, sqlTime = 0;
			long execTime = 0;

			this.clearContent();

			boolean repeatLast = aSql.equals(this.sql);
			this.sql = aSql;

			final long start = System.currentTimeMillis();

			this.stmtRunner.runStatement(aSql, this.statusBar.getMaxRows());
			end = System.currentTimeMillis();
			sqlTime = (end - start);

			result = this.stmtRunner.getResult();
			this.hasResultSet = false;
			DataStore newData = null;
			this.success = result.isSuccess();

			if (this.success && result.hasData())
			{
				this.hasResultSet = true;

				if (result.hasDataStores())
				{
					newData = result.getDataStores()[0];
				}
				else
				{
					// the resultset will be closed when stmtRunner.done() is called
					// in scriptFinished()
					ResultSet rs = result.getResultSets()[0];

					// passing the maxrows to the datastore is a workaround for JDBC drivers
					// which do not support the setMaxRows() method.
					// The datastore will make sure that no more rows are read then really requested
					if (this.showLoadProgress)
					{
						newData = new DataStore(rs, true, this, this.getMaxRows());
					}
					else
					{
						newData = new DataStore(rs, true, this.getMaxRows());
					}
				}
				this.stmtRunner.statementDone();

				newData.setOriginalStatement(aSql);
				newData.setSourceConnection(this.dbConnection);
				newData.setProgressMonitor(null);
				this.clearStatusMessage();

				end = System.currentTimeMillis();

				if (repeatLast)
				{
					this.infoTable.saveColumnSizes();
				}

				this.infoTable.reset();
				this.infoTable.setAutoCreateColumnsFromModel(true);
				this.infoTable.setModel(new DataStoreTableModel(newData), true);

				if (repeatLast)
				{
					this.infoTable.restoreColumnSizes();
				}
				else
				{
					this.infoTable.adjustColumns();
				}

				this.setStatusMessage(ResourceMgr.getString("MsgCheckingUpdateTable"));
				this.checkUpdateTable();
				this.clearStatusMessage();

				if (this.manageUpdateAction)
				{
					boolean update = this.isUpdateable();
					this.insertRow.setEnabled(true);
				}
        this.rowCountChanged();
			}
			else if (result.isSuccess())
			{
				end = System.currentTimeMillis();
				this.hasResultSet = false;
				this.setMessageDisplayModel(this.getEmptyMsgTableModel());
			}
			else
			{
				end = System.currentTimeMillis();
				this.hasResultSet = false;
				if (!this.showErrorMessages)
				{
					this.setMessageDisplayModel(this.getErrorTableModel());
				}
				this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\n";
			}
			execTime = (end - start);
			this.rowsAffectedByScript += result.getTotalUpdateCount();

			String[] messages = result.getMessages();
			StringBuffer msg = null;
			if (messages != null)
			{
				msg = new StringBuffer(messages.length * 80);
				for (int i=0; i < messages.length; i++)
				{
					msg.append(messages[i]);
					msg.append(" \n");
				}
				this.lastMessage = msg.toString();
			}

			if (!result.isSuccess() && this.showErrorMessages)
			{
				this.setMessageDisplayModel(this.getErrorTableModel(this.lastMessage));
			}

			if (LogMgr.isDebug())
			{
				this.lastMessage = this.lastMessage + "\n" + ResourceMgr.getString("MsgSqlVerbTime") + " " + (((double)sqlTime) / 1000.0) + "s";
			}
			this.lastMessage = this.lastMessage + "\n" + ResourceMgr.getString("MsgExecTime") + " " + (((double)execTime) / 1000.0) + "s";
		}
		catch (SQLException sqle)
		{
			LogMgr.logError(this, "SQL error when executing statement: \r\n" + this.sql, sqle);
			this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\r\n";
			this.lastMessage = this.lastMessage + ExceptionUtil.getDisplay(sqle);;
			if (this.showErrorMessages)
			{
				//this.setMessageDisplayModel(this.getEmptyMsgTableModel());
				this.setMessageDisplayModel(this.getErrorTableModel(sqle.getMessage()));
				WbManager.getInstance().showErrorMessage(SwingUtilities.getWindowAncestor(this), this.lastMessage);
			}
			else
			{
				this.setMessageDisplayModel(this.getErrorTableModel());
			}
			throw sqle;
		}
		catch (Throwable e)
		{
			if (e instanceof OutOfMemoryError)
			{
				WbManager.getInstance().showErrorMessage(SwingUtilities.getWindowAncestor(this), ResourceMgr.getString("MsgOutOfMemoryError"));
			}
			LogMgr.logError(this, "Error executing statement: \r\n" + this.sql, e);
			this.setMessageDisplayModel(this.getErrorTableModel(e.getMessage()));
			this.lastMessage = ResourceMgr.getString("MsgExecuteError") + "\r\n";
			String s = ExceptionUtil.getDisplay(e);
			this.lastMessage = this.lastMessage + s;
			throw new Exception(s);
		}
		finally
		{
			result = null;
		}
  }

	/**
	 *	Callback method to tell this component that a script is running.
	 *  It resets the total number of rows which have been affected by the script to zero
	 */
	public void scriptStarting()
	{
		this.rowsAffectedByScript = 0;
		this.scriptRunning = true;
	}

	/**
	 *	Callback method to tell this component that the script execution has finished.
	 *  This method will cleanup the StatementRunner
	 *  @see workbench.sql.StatementRunner#done()
	 */
	public void scriptFinished()
	{
		this.scriptRunning = false;
		this.stmtRunner.done();
	}

	public boolean wasSuccessful()
	{
		return this.success;
	}

	/**
	 *  This method will update the row info display on the statusbar.
	 */
  public void rowCountChanged()
  {
		int startRow = 0;
		int endRow = 0;
		int count = 0;

		TableModel model = this.infoTable.getModel();
	  if (model != resultEmptyMsgModel && model != this.errorModel)
	  {
			startRow = this.infoTable.getFirstVisibleRow();
			endRow = this.infoTable.getLastVisibleRow(startRow);
			count = this.infoTable.getRowCount();
		}

		// start row and end row are 0 based
		this.statusBar.setRowcount(startRow + 1, endRow + 1, count);
  }

	public int duplicateRow()
	{
		if (this.readOnly) return -1;
		if (!this.startEdit()) return -1;
		int newRow = this.infoTable.duplicateRow();
		if (newRow >= 0) this.infoTable.getSelectionModel().setSelectionInterval(newRow, newRow);
		this.rowCountChanged();
		return newRow;
	}

	public void deleteRow()
	{
		if (this.readOnly) return;
		if (!this.startEdit()) return;
		this.infoTable.deleteRow();
    this.rowCountChanged();
	}

	public long addRow()
	{
		if (this.readOnly) return -1;
		if (!this.startEdit()) return -1;
		long newRow = this.infoTable.addRow();
		if (newRow > -1) this.rowCountChanged();
		return newRow;
	}

	public boolean confirmCancel() { return true; }
	public void cancelExecution()
	{
		if (this.stmtRunner != null)
		{
			LogMgr.logDebug("DwPanel.cancelExecution()", "Cancelling DwPanel retrieval...");
			this.stmtRunner.cancel();
		}
	}

	/**
	 *	Replaces the current values with the values initially retrieved from the
	 *  database.
	 *	@see workbench.storage.DataStore#restoreOriginalValues()
	 */

	public void restoreOriginalValues()
	{
		DataStore ds = this.infoTable.getDataStore();
		if (ds == null) return;
		ds.restoreOriginalValues();
		this.repaint();
	}

	public String getLastMessage() { return this.lastMessage; }
	public boolean hasResultSet() { return this.hasResultSet; }

	/**
	 *	Returns true if the DataStore of the Table has been modified.
	 *	@see workbench.storage.DataStore#isModified()
	 */
	public boolean isModified()
	{
		DataStore ds = this.infoTable.getDataStore();
		if (ds == null) return false;
		else return ds.isModified();
	}

	private void initLayout(DwStatusBar status)
	{
		this.setLayout(new BorderLayout());
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.infoTable = new WbTable();
		this.infoTable.setRowResizeAllowed(true);
		if (status != null)
		{
			this.statusBar = status; //new DwStatusBar();
		}
		else
		{
			this.statusBar = new DwStatusBar();
			Border b = BorderFactory.createCompoundBorder(new EmptyBorder(2, 0, 0, 0), new EtchedBorder());
			this.statusBar.setBorder(b);
			this.add(this.statusBar, BorderLayout.SOUTH);
		}

		this.statusBar.setFocusable(false);
		this.setFocusable(false);
		this.scrollPane = new WbScrollPane(this.infoTable);
		this.scrollPane.getViewport().addChangeListener(this);

		this.add(this.scrollPane, BorderLayout.CENTER);
		//this.infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.infoTable.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.infoTable.setDoubleBuffered(true);
		this.infoTable.setAdjustToColumnLabel(true);
	}

	/**
	 *	Show a message in the status panel.
	 *	This method might be called from within a background thread, so we
	 *  need to make sure the actual setText() stuff is called on the AWT
	 *  thread in order to update the GUI correctly.
	 *  @see DwStatusBar#setStatusMessage(String)
	 */
	public void setStatusMessage(final String aMsg)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			this.statusBar.setStatusMessage(aMsg);
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						statusBar.setStatusMessage(aMsg);
					}
				});
			}
			catch (Exception e)
			{
				LogMgr.logError("DwPanel.setStatusMessage()", "Error showing status message on AWT thread", e);
			}
		}
	}

	/**
	 *	Clears the display on the status bar.
	 *  It is ensured that the call to the status bar methods is always called
	 *  on the AWT thread.
	 *  @see DwStatusBar#clearStatusMessage()
	 */
	public void clearStatusMessage()
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			this.statusBar.clearStatusMessage();
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						statusBar.clearStatusMessage();
					}
				});
			}
			catch (Exception e)
			{
				LogMgr.logError("DwPanel.setStatusMessage()", "Error clearing status message on AWT thread", e);
			}
		}
	}

	private void setMessageDisplayModel(TableModel aModel)
	{
		this.infoTable.setModel(aModel);
		TableColumnModel colMod = this.infoTable.getColumnModel();
		TableColumn col = colMod.getColumn(0);
		col.setPreferredWidth(this.getWidth() - 10);
		this.statusBar.setRowcount(0,0,0);
	}

	/**
	 *	Clears everything.
	 *	<ul>
	 *	<li>Ends the edit mode</li>
	 *	<li>removes all rows from the table</li>
	 *	<li>sets the hasResultSet flag to false</li>
	 *  <li>the lastMessage is set to an empty string</li>
	 *  <li>the last SQL is set to null</li>
	 *  <li>the statusbar display is cleared</li>
	 *  </ul>
	 */
	public void clearContent()
	{
		this.infoTable.reset();
		this.endEdit();
		this.hasResultSet = false;
		this.lastMessage = StringUtil.EMPTY_STRING;
		this.sql = null;
		this.statusBar.clearRowcount();
	}

	/**
	 *	Returns a table model which displays a message that the last
	 *  statement did not produce a result set.
	 */
	private TableModel getEmptyMsgTableModel()
	{
		if (this.resultEmptyMsgModel == null)
		{
			String msg = ResourceMgr.getString(ResourceMgr.MSG_WARN_NO_RESULT);
			String title = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_TITLE);
			this.resultEmptyMsgModel = new OneLineTableModel(title, msg);
		}
		return this.resultEmptyMsgModel;
	}

	public int getActionOnError(int errorRow, int errorColumn, String data, String errorMessage)
	{
		String msg = ResourceMgr.getString("ErrorUpdateSqlError");
		msg = msg.replaceAll("%statement%", (data == null ? "" : data.substring(0, 50) + "..."));
		msg = msg.replaceAll("%message%", errorMessage);

		String r = "";
		if (errorRow > -1)
		{
			r = ResourceMgr.getString("TxtErrorRow").replaceAll("%row%", Integer.toString(errorRow));
		}
		msg = msg.replaceAll("%row%", r);

		Window w = SwingUtilities.getWindowAncestor(this);
		int choice = WbSwingUtilities.getYesNoIgnoreAll(w, msg);

		int result = JobErrorHandler.JOB_ABORT;

		if (choice == JOptionPane.YES_OPTION)
		{
			result = JobErrorHandler.JOB_CONTINUE;
		}
		else if (choice == WbSwingUtilities.IGNORE_ALL)
		{
			result = JobErrorHandler.JOB_IGNORE_ALL;
		}
		return result;
	}

	private TableModel getErrorTableModel()
	{
		return this.getErrorTableModel(null);
	}

	/**
	 *	Returns a TableModel which displays an error text.
	 *	This is used to show a hint in the table panel that an error
	 *  occurred and the actual error message is displayed in the log
	 *  panel
	 */
	private TableModel getErrorTableModel(String aMsg)
	{
		if (this.errorModel == null)
		{
			String msg = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_DATA);
			String title = ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_TITLE);
			this.errorModel = new OneLineTableModel(title, msg);
		}
		if (aMsg == null)
		{
			this.errorModel.setMessage(ResourceMgr.getString(ResourceMgr.TXT_ERROR_MSG_DATA));
		}
		else
		{
			this.errorModel.setMessage(aMsg);
		}
		return this.errorModel;
	}

	/**
	 *	Set the focus to the max rows field.
	 *	@see DwStatusBar#selectMaxRowsField()
	 */
  public void selectMaxRowsField()
  {
    this.statusBar.selectMaxRowsField();
  }

	public WbTable getTable() { return this.infoTable; }


	/**
	 *	Stops the editing mode of the displayed WbTable:
	 *	<ul>
	 *	<li>the status column is turned off</li>
	 *  <li>the edit actions are enabled/disabled correctly </li>
	 *  <li>the originalValues for the DataStore are restored </li>
	 *  </ul>
	 */
	public void endEdit()
	{
		this.editingStarted = false;
		this.infoTable.setShowStatusColumn(false);
		this.updateAction.setEnabled(false);
		int rows = this.infoTable.getSelectedRowCount();
		this.insertRow.setEnabled(this.isUpdateable());
		this.deleteRow.setEnabled(this.isUpdateable() && rows > 0);
		this.duplicateRow.setEnabled(this.isUpdateable() && rows == 1);
		this.startEdit.setSwitchedOn(false);
		this.restoreOriginalValues();
	}

	/**
	 *	Starts the "edit" mode of the table. It will not start the edit
	 *  mode, if the table is "read only" meaning if no update table (=database
	 *  table) is defined.
	 *  The following actions are carried out:
	 *	<ul>
	 *	<li>if the updateable flag is not yet set, try to find out which table to update</li>
	 *  <li>the status column is displayec</li>
	 *  <li>the corresponding actions (insert row, delete row) are enabled</li>
	 *  <li>the startEdit action is turned to "switched on"</li>
	 *  </ul>
	 */
	public boolean startEdit()
	{
		if (this.readOnly) return false;
		this.editingStarted = false;

		// if the result is not yet updateable (automagically)
		// then try to find the table. If the table cannot be
		// determined, then ask the user
		if (!this.isUpdateable())
		{
			if (!this.checkUpdateTable())
			{
				String sql = this.getCurrentSql();
				List tables = SqlUtil.getTables(sql);
				String table = null;

				if (tables.size() > 1)
				{
					table = (String)JOptionPane.showInputDialog(SwingUtilities.getWindowAncestor(this),
							null, ResourceMgr.getString("MsgEnterUpdateTable"),
							JOptionPane.QUESTION_MESSAGE,
							null,tables.toArray(),null);
				}

				if (table != null)
				{
					this.setUpdateTable(table);
				}
			}
		}
		boolean update = this.isUpdateable();
		if (update)
		{
			this.infoTable.setShowStatusColumn(true);
			if (this.updateAction != null)
			{
				if (this.infoTable.getDataStore().isModified())
				{
					this.updateAction.setEnabled(true);
				}
			}

			this.editingStarted = true;
			this.startEdit.setSwitchedOn(true);
		}
		else
		{
			this.startEdit.setSwitchedOn(false);
		}
		int rows = this.infoTable.getSelectedRowCount();
		if (this.insertRow != null) this.insertRow.setEnabled(update);
		if (this.deleteRow != null) this.deleteRow.setEnabled(update && rows > 0);
		if (this.duplicateRow != null) this.duplicateRow.setEnabled(update && rows == 1);

		return update;
	}

	public InsertRowAction getInsertRowAction() { return this.insertRow; }
	public CopyRowAction getCopyRowAction() { return this.duplicateRow; }
	public DeleteRowAction getDeleteRowAction() { return this.deleteRow; }
	public UpdateDatabaseAction getUpdateDatabaseAction() { return this.updateAction; }
	public StartEditAction getStartEditAction() { return this.startEdit; }

	/**
	 *	Turns on the batchUpdate mode.
	 *  In this mode, the automatic switch to edit mode is disabled. This
	 *  is used when populating the table from the database otherwise, the
	 *  first row, which is retrieved will start the edit mode
	 */
	public void setBatchUpdate(boolean aFlag)
	{
		this.batchUpdate = aFlag;
	}

	/**
	 *	If the user changes something in the database (which is possible, as
	 *  the table defaults to beeing editable) the edit mode (with status column
	 *  and the different actions enabled) is switched on automatically.
	 */
	public void tableChanged(TableModelEvent e)
	{
		if (this.batchUpdate) return;
		if (this.readOnly) return;

		if (e.getFirstRow() != TableModelEvent.ALL_COLUMNS &&
		    e.getFirstRow() != TableModelEvent.HEADER_ROW &&
				this.isModified() )
		{
			if (!this.editingStarted)
			{
				this.startEdit();
			}
			if (this.updateAction != null) this.updateAction.setEnabled(true);
		}
	}

	/**
	 *	This is called when the selection in the table changes.
	 *  The copy row action will be enabled when exactly one row
	 *  is selected
	 */
	public void valueChanged(javax.swing.event.ListSelectionEvent e)
	{
		if (this.readOnly) return;
		long rows = this.infoTable.getSelectedRowCount();
		boolean update = this.isUpdateable();
		this.deleteRow.setEnabled( (rows > 0) && update);
		this.duplicateRow.setEnabled(rows == 1 && update);
	}

	/**
	 *	Called from the viewport, when the display has been scrolled
	 *  We need to update the row display then.
	 */
	public void stateChanged(javax.swing.event.ChangeEvent e)
	{
		this.rowCountChanged();
	}

	public void jobFinished()
	{
	}

}