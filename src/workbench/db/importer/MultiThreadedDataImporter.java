/*
 * DataImporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableCreator;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.interfaces.Committer;
import workbench.interfaces.ProgressReporter;
import workbench.util.ExceptionUtil;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import workbench.interfaces.BatchCommitter;
import workbench.interfaces.ImportFileParser;
import workbench.resource.Settings;
import workbench.util.MessageBuffer;
import workbench.util.WbThread;


/**
 * Import data that is provided from a {@link RowDataProducer} into
 * a table in the database.
 * 
 * @see workbench.sql.wbcommands.WbImport
 * @see workbench.sql.wbcommands.WbCopy
 * @see workbench.db.datacopy.DataCopier
 * 
 * @author  support@sql-workbench.net
 */
public class MultiThreadedDataImporter
	implements Interruptable, RowDataReceiver, ProgressReporter, BatchCommitter
{
	public static final int MODE_INSERT = 0;
	public static final int MODE_UPDATE = 1;
	public static final int MODE_INSERT_UPDATE = 2;
	public static final int MODE_UPDATE_INSERT = 3;

	private WbConnection dbConn;

	private RowDataProducer source;

	private TableIdentifier targetTable = null;

	private int commitEvery = 0;

	private DeleteType deleteTarget = DeleteType.none;
	private boolean createTarget = false;
	private boolean continueOnError = true;

	private long currentImportRow = 0;
	private int mode = MODE_INSERT;
	private boolean useBatch = false;
	private int batchSize = -1;
	private boolean canCommitInBatch = true;
	private boolean commitBatch = false;
	
	private boolean hasErrors = false;
	private boolean errorAbort = false;
	private boolean hasWarnings = false;
	private int reportInterval = 10;
	private MessageBuffer messages;
	private String targetSchema;

	private int colCount;
	private int totalTables = -1;
	private int currentTable = -1;
	private boolean transactionControl = true;
	
	private List<TableIdentifier> tablesToBeProcessed;
	private TableDeleter tableDeleter;

	// this array will map the columns for updating the target table
	// the index into this array will be the index
	// from the row data array supplied by the producer.
	// (which should be the same order as the columns in targetColumns)
	// the value of that index position is the index
	// for the setXXX() method for the prepared statement
	// to update the table
	private int[] columnMap = null;

	private List<ColumnIdentifier> targetColumns;
	private List<ColumnIdentifier> keyColumns;
	
	// A map that stores constant values for the import. 
	// e.g. for columns not part of the input file.
	private ConstantColumnValues columnConstants;
	
	private RowActionMonitor progressMonitor;
	private boolean isRunning = false;
	private ImportFileParser parser;

	// Use for partial imports
	private long startRow = 0;
	private long endRow = Long.MAX_VALUE;
	private boolean partialImportEnded = false;
	
	// Additional WHERE clause for UPDATE statements
	private String whereClauseForUpdate;
	private BadfileWriter badWriter;
	private String badfileName;

	private BlockingQueue<ImportRow> importQueue;
	
	private int errorCount = 0;
	private boolean errorLimitAdded = false;
	private int threadCount = 1;
	
	/**
	 * Indicates multiple imports run with this instance oft DataImporter.
	 * Set via {@link #beginMultiTable() }
	 */ 
	private boolean multiTable = false;
	
	private TableStatements tableStatements;
	private WorkerPool worker;


	public MultiThreadedDataImporter()
	{
		this(1);
	}

	public MultiThreadedDataImporter(int numThreads)
	{
		threadCount = numThreads;
		this.messages = new MessageBuffer();
		int defaultSize = Settings.getInstance().getIntProperty("workbench.import.queue.size", 100);

		// When using a LinkedBlockingQueue the synchronisation between the
		// producer and the Worker threads does not work properly. It seems that
		// the queue gets empty even if the producer is still sending rows.
		importQueue = new ArrayBlockingQueue<ImportRow>(defaultSize, true);
	}

	public void setConnection(WbConnection aConn)
		throws SQLException
	{
		this.dbConn = aConn;
		if (dbConn == null) return;
		worker = new WorkerPool(aConn, threadCount);
		worker.init(this);
	}

	public BlockingQueue getImportQueue()
	{
		return this.importQueue;
	}
	
	private boolean supportsBatch()
	{
		if (this.dbConn == null) return true;
		return dbConn.getMetadata().supportsBatchUpdates();
	}
	
	public void setTransactionControl(boolean flag)
	{
		this.transactionControl = flag;
	}
	
	public RowActionMonitor getRowActionMonitor()
	{
		return this.progressMonitor;
	}
	
	public void setRowActionMonitor(RowActionMonitor rowMonitor)
	{
		this.progressMonitor = rowMonitor;
		if (this.progressMonitor != null)
		{
			this.progressMonitor.setMonitorType(RowActionMonitor.MONITOR_INSERT);
		}
	}

	public void setProducer(RowDataProducer producer)
	{
		this.source = producer;
		this.source.setReceiver(this);
		this.source.setAbortOnError(!this.continueOnError);
		if (producer instanceof ImportFileParser)
		{
			this.parser = (ImportFileParser)producer;
		}
	}

	/**
	 * Define statements that should be executed before an import
	 * for a table starts and after the last record has been inserted.
	 * 
	 * @param stmt the statement definitions. May be null
	 */
	public void setPerTableStatements(TableStatements stmt)
	{
		if (stmt != null && stmt.hasStatements())
		{
			this.tableStatements = stmt;
		}
		else
		{
			this.tableStatements = null;
		}
	}

	public void beginMultiTable()
		throws SQLException
	{
		this.multiTable = true;
		// If more than one table is imported and those tables need to 
		// be deleted before the import starts (due to FK constraints) the producer
		// has sent a list of tables that need to be deleted.
		if (this.deleteTarget != DeleteType.none && this.tablesToBeProcessed != null)
		{
			this.deleteTargetTables();
		}
	}
	
	public void endMultiTable()
	{
		this.multiTable = false;
		if (this.progressMonitor != null) this.progressMonitor.jobFinished();
	}
	
	public void setStartRow(long row) 
	{ 
		if (row >= 0) this.startRow = row; 
		else this.startRow = 0;
	}
	
	public void setEndRow(long row) 
	{ 
		if (row >= 0) this.endRow = row; 
		else this.endRow = Long.MAX_VALUE;
	}

	public void setCommitBatch(boolean flag)
	{
		this.commitBatch = flag;
		if (flag)
		{
			this.commitEvery = 0;
		}
	}
	
	/**
	 * Do not commit any changes after finishing the import
	 */
	public void commitNothing()
	{
		this.commitBatch = false;
		this.commitEvery = Committer.NO_COMMIT_FLAG; 
	}
	
	public RowDataProducer getProducer()
	{
		return this.source;
	}
	
	/**
	 * Set the commit interval.
	 * When this parameter is set, commitBatch is set to false.
	 * 
	 * @param aCount the interval in which commits should be sent
	 */
	public void setCommitEvery(int aCount) 
	{ 
		if (aCount > 0 || aCount == Committer.NO_COMMIT_FLAG)
		{
			this.commitBatch = false;
		}
		this.commitEvery = aCount; 
	}

	public List<ColumnIdentifier> getTargetColumns()
	{
		return targetColumns;
	}

	public int[] getColumnMap()
	{
		return columnMap;
	}

	public ImportFileParser getParser()
	{
		return parser;
	}
	
	public boolean getCommitBatch()
	{
		return commitBatch;
	}
	
	public int getCommitEvery()
	{
		return this.commitEvery;
	}

	public boolean getContinueOnError() { return this.continueOnError; }
	public void setContinueOnError(boolean flag) 
	{ 
		this.continueOnError = flag; 
	}

	public DeleteType getDeleteTarget() { return deleteTarget; }
	
	public int getBatchSize()
	{
		return batchSize;
	}
	
	public void setBatchSize(int size) 
	{ 
		this.batchSize = size; 
	}

	public void setBadfileName(String fname)
	{
		this.badfileName = fname;
	}
	
	public void setWhereClauseForUpdate(String clause)
	{
		if (StringUtil.isEmptyString(clause))
		{
			this.whereClauseForUpdate = null;
		}
		else
		{
			this.whereClauseForUpdate = clause;
		}
	}

	public void setTableList(List<TableIdentifier> targetTables)
	{
		this.tablesToBeProcessed = targetTables;
	}
	
	public void deleteTargetTables()
		throws SQLException
	{
		if (!this.isModeInsert())
		{
			LogMgr.logWarning("DataImporter.deleteTargetTables()", "Target tables will not be deleted because import mode is not set to 'insert'");
			this.messages.append(ResourceMgr.getString("ErrImpNoDeleteUpd"));
			this.messages.appendNewLine();
			return;
		}
		
		try
		{
			// The instance of the tableDeleter is stored in an instance
			// variable in order to allow for cancel() during the initial 
			// delete as well
			tableDeleter = new TableDeleter(this.dbConn, true);
			tableDeleter.setRowMonitor(this.progressMonitor);
			tableDeleter.deleteRows(this.tablesToBeProcessed, true);
			this.messages.append(tableDeleter.getMessages());
		}
		finally
		{
			this.tableDeleter = null;
		}
	}
	
	/**
	 * Controls creation of target table for imports where the 
	 * producer can retrieve a full table definition (i.e. XML files
	 * created with SQL Workbench)
	 * 
	 * @see #createTarget()
	 * @see #setTargetTable(workbench.db.TableIdentifier, workbench.db.ColumnIdentifier[])
	 */
	public void setCreateTarget(boolean flag) { this.createTarget = flag; }
	public boolean getCreateTarget() { return createTarget; }
	
	/**
	 *	Controls deletion of the target table.
	 */
	public void setDeleteTarget(DeleteType deleteTarget)
	{
		this.deleteTarget = deleteTarget;
	}

	/**
	 * 	Use batch updates if the driver supports this
	 */
	public void setUseBatch(boolean flag)
	{
		if (this.isModeInsertUpdate() || this.isModeUpdateInsert()) return;
		this.useBatch = flag;
	}

	public void setModeInsert() 
	{ 
		this.mode = MODE_INSERT; 
	}
	
	public void setModeUpdate() 
	{ 
		this.mode = MODE_UPDATE; 
	}

	public void setModeInsertUpdate()
	{
		this.mode = MODE_INSERT_UPDATE;
		this.useBatch = false;
	}
	
	public void setModeUpdateInsert()
	{
		this.mode = MODE_UPDATE_INSERT;
		this.useBatch = false;
	}

	public boolean isModeInsert() { return (this.mode == MODE_INSERT); }
	public boolean isModeUpdate() { return (this.mode == MODE_UPDATE); }
	public boolean isModeInsertUpdate() { return (this.mode == MODE_INSERT_UPDATE); }
	public boolean isModeUpdateInsert() { return (this.mode == MODE_UPDATE_INSERT); }

	public static int estimateReportIntervalFromFileSize(File file)
	{
		try
		{
			long records = FileUtil.estimateRecords(file, 10);
			if (records < 100)
			{
				return 1;
			}
			else if (records < 10000)
			{
				return 10;
			}
			else if (records < 250000)
			{
				return 100;
			}
			else
			{
				return 1000;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.estimateReportIntervalFromFileSize()", "Error when checking input file", e);
			return 0;
		}
	}

	public void setMode(int mode)
	{
		if (mode == MODE_INSERT)
			this.setModeInsert();
		else if (mode == MODE_UPDATE)
			this.setModeUpdate();
		else if (mode == MODE_INSERT_UPDATE)
			this.setModeInsertUpdate();
		else if (mode == MODE_UPDATE_INSERT)
			this.setModeUpdateInsert();
	}

	/**
	 *	Return the numer mode value based on keywords.
	 * 
	 *	Valid mode definitions are:
	 *	<ul>
	 *	<li>insert</li>
	 *	<li>update</li>
	 *	<li>insert,update</li>
	 *	<li>update,insert</li>
	 *  </ul>
	 * The mode string is not case sensitive (INSERT is the same as insert)
	 * @return -1 if the value is not valid
	 * 
	 * @see #getModeValue(String)
	 * @see #MODE_INSERT
	 * @see #MODE_UPDATE
	 * @see #MODE_INSERT_UPDATE
	 * @see #MODE_UPDATE_INSERT
	 */
	public static int getModeValue(String mode)
	{
		if (mode == null) return -1;
		mode = mode.trim().toLowerCase();
		if (mode.indexOf(',') == -1)
		{
			// only one keyword supplied
			if ("insert".equals(mode))
			{
				return MODE_INSERT;
			}
			else if ("update".equals(mode))
			{
				return MODE_UPDATE;
			}
			else
			{
				return -1;
			}
		}
		else
		{
			List l = StringUtil.stringToList(mode, ",");
			String first = (String)l.get(0);
			String second = (String)l.get(1);
			if ("insert".equals(first) && "update".equals(second))
			{
				return MODE_INSERT_UPDATE;
			}
			else if ("update".equals(first) && "insert".equals(second))
			{
				return MODE_UPDATE_INSERT;
			}
			else
			{
				return -1;
			}
		}
		
	}
	
	/**
	 * Define the mode by supplying keywords.
	 * A null value means "keep the current (default)" and is a valid mode
	 * @return true if the passed string is valid, false otherwise
	 * @see #getModeValue(String)
	 */
	public boolean setMode(String mode)
	{
		if (mode == null) return true;
		int modevalue = getModeValue(mode);
		if (modevalue == -1) return false;
		setMode(modevalue);
		return true;
	}

	/**
	 * Define column constants for the import. 
	 * It is expected that the value object is already converted to the correct
	 * class. DataImporter will not convert the passed values in any way.
	 */
	public void setConstantColumnValues(ConstantColumnValues constantValues)
	{
		this.columnConstants = null;
		if (constantValues != null && constantValues.getColumnCount() > 0)
		{
			this.columnConstants = constantValues;
		}
	}
	
	/**
	 *	Define the key columns by supplying a comma separated
	 *	list of column names
	 */
	public void setKeyColumns(String aColumnList)
	{
		List cols = StringUtil.stringToList(aColumnList, ",");
		int count = cols.size();
		this.keyColumns = new LinkedList<ColumnIdentifier>();
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier col = new ColumnIdentifier((String)cols.get(i));
			keyColumns.add(col);
		}
	}

	/**
	 * 	Set the key columns for the target table to be used
	 * 	for update mode.
	 * 	The list has to contain objects of type {@link workbench.db.ColumnIdentifier}
	 */
	public void setKeyColumns(List<ColumnIdentifier> cols)
	{
		this.keyColumns = cols;
	}

	private boolean hasKeyColumns()
	{
		return (this.keyColumns != null && keyColumns.size() > 0);
	}
	
	public void startBackgroundImport()
	{
		if (this.source == null) return;
		Thread t = new WbThread("WbImport Thread")
		{
			public void run()
			{
				try { startImport(); } catch (Throwable th) {}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	/**
	 *	Start the import
	 */
	public void startImport()
		throws IOException, SQLException, Exception
	{
		if (this.source == null) return;
		this.isRunning = true;
		this.canCommitInBatch = true;

		// make sure the importQueue is empty before starting another import
		this.importQueue.clear();

		if (!worker.isActive())
		{
			worker.start();
		}
		
		if (this.useBatch)
		{
			if (!supportsBatch())
			{
				LogMgr.logWarning("DataImporter.startImport()", "JDBC driver does not support batch updates. Ignoring request to use batch updates");
				this.messages.append(ResourceMgr.getString("MsgJDBCDriverNoBatch") + "\n");
				useBatch = false;
			}
			else if (this.isModeInsertUpdate() || this.isModeUpdateInsert())
			{
				// When using UPDATE/INSERT or INSERT/UPDATE
				// we cannot use batch mode as we immediately need
				// the result of the first statement to decide
				// whether we have to send another one
				this.useBatch = false;
				this.messages.append(ResourceMgr.getString("ErrImportNoBatchMode"));
			}
		}
		
		try
		{
			this.source.start();
		}
		catch (CycleErrorException e)
		{
			this.hasErrors = true;
			messages.append(ResourceMgr.getString("ErrImpCycle"));
			messages.append(" (" + e.getRootTable() + ")");
			this.messages.append(this.source.getMessages());
			throw e;
		}
		catch (Exception e)
		{
			this.hasErrors = true;
			this.messages.append(this.source.getMessages());
			throw e;
		}
	}

	public static boolean isDeleteTableAllowed(int mode)
	{
		return mode == MODE_INSERT;
	}
	
	/**
	 *	Deletes the target table by issuing a DELETE FROM ...
	 */
	private void deleteTarget()
		throws SQLException
	{
		if (this.deleteTarget == DeleteType.none) return;
		if (this.targetTable == null) return;
		String deleteSql = null;

		if (!this.isModeInsert())
		{
			LogMgr.logWarning("DataImporter.deleteTarget()", "Target table will not be deleted because import mode is not set to 'insert'");
			this.messages.append(ResourceMgr.getString("ErrImpNoDeleteUpd"));
			this.messages.appendNewLine();
			return;
		}
		
		if (this.deleteTarget == DeleteType.truncate)
		{
			deleteSql = "TRUNCATE TABLE " + this.targetTable.getTableExpression(this.dbConn);
		}
		else
		{
			deleteSql = "DELETE FROM " + this.targetTable.getTableExpression(this.dbConn);
		}
		Statement stmt = this.dbConn.createStatement();
		LogMgr.logInfo("DataImporter.deleteTarget()", "Executing: [" + deleteSql + "] to delete target table...");
		int rows = stmt.executeUpdate(deleteSql);
		if (this.deleteTarget == DeleteType.truncate)
		{
			String msg = ResourceMgr.getString("MsgImportTableTruncated").replace("%table%", this.targetTable.getTableExpression(this.dbConn));
			this.messages.append(msg);
			this.messages.appendNewLine();
		}
		else
		{
			this.messages.append(rows + " " + ResourceMgr.getString("MsgImporterRowsDeleted") + " " + this.targetTable.getTableExpression(this.dbConn) + "\n");
		}
	}
	
	private void createTarget()
		throws SQLException
	{
		TableCreator creator = new TableCreator(this.dbConn, this.targetTable, this.targetColumns);
		creator.useDbmsDataType(true);
		creator.createTable();
		String table = creator.getTable().getTableName();
		String msg = StringUtil.replace(ResourceMgr.getString("MsgImporterTableCreated"), "%table%", table);
		this.messages.append(msg);
		this.messages.appendNewLine();
	}

	public boolean isRunning() { return this.isRunning; }
	public boolean isSuccess() { return !hasErrors; }
	public boolean hasWarnings() { return this.hasWarnings; }

	/**
	 *	This method is called if cancelExecution() is called
	 *	to check if the user should confirm the cancelling of the import
	 */
	public boolean confirmCancel()
	{
		return true;
	}

	protected void sendAbortMessage()
	{
		ImportRow terminator = new ImportRow();
		terminator.rowNumber = -42;
		terminator.data = null;
		for (int i=0; i < threadCount; i++)
		{
			importQueue.offer(terminator);
		}
	}

	public void abort(ImportRow row, Exception e)
	{
		hasErrors = true;
		errorAbort = true;

		addError(ResourceMgr.getString("ErrImportingRow") + " " + row.rowNumber + "\n");
		ValueDisplay display = new ValueDisplay(row.data);
		addError(ResourceMgr.getString("ErrImportErrorMsg") + " " + e.getMessage() + "\n");
		addError(ResourceMgr.getString("ErrImportValues") + " " + display.toString() + "\n\n");
		cancelExecution();
	}

	public void cancelExecution()
	{
		this.isRunning = false;
		if (this.tableDeleter != null)
		{
			this.tableDeleter.cancel();
		}
		System.out.println("cancelling producer...");
		source.cancel();
//		worker.cancel();
		if (!errorAbort) this.messages.append(ResourceMgr.getString("MsgImportCancelled") + "\n");
	}

	public void setTableCount(int total)
	{
		this.totalTables = total;
	}

	public void setCurrentTable(int current)
	{
		this.currentTable = current;
	}

	public ConstantColumnValues getColumnConstants()
	{
		return columnConstants;
	}
	
	protected synchronized void clearMessages()
	{
		messages.clear();
	}
	
	protected synchronized void addMessage(String msg, boolean withNewLine)
	{
		this.messages.append(msg);
		if (withNewLine) messages.appendNewLine();
	}

	protected void addError(String msg)
	{
		hasErrors = true;
		errorCount ++;
		if (errorCount < 5000)
		{
			this.messages.append(msg);
		}
		else
		{
			if (!errorLimitAdded)
			{
				messages.appendNewLine();
				messages.append(ResourceMgr.getString("MsgImpTooManyError"));
				messages.appendNewLine();
				errorLimitAdded = true;
			}
		}
	}
	
	public void recordRejected(String record, long importRow, Throwable error)
	{
		if (badWriter != null && record != null)
		{
			badWriter.recordRejected(record);
		}
		else
		{
			addError(ResourceMgr.getString("ErrImportingRow") + " " + importRow + "\n");
			addError(ResourceMgr.getString("ErrImportErrorMsg") + " " + error.getMessage() + "\n");
			addError(ResourceMgr.getString("ErrImportValues") + " " + record + "\n\n");
			if (errorLimitAdded)
			{
				LogMgr.logError("DataImporter.processRow()", "Values: " + record, null);
			}
		}
	}

	public boolean shouldProcessNextRow()
	{
		if (currentImportRow + 1 < startRow) return false;
		if (currentImportRow + 1 > endRow) return false;
		return true;
	}

	public void nextRowSkipped()
	{
		this.currentImportRow ++;
	}
	
	/**
	 *	Callback function for RowDataProducer. The order in the data array
	 * 	has to be the same as initially passed in the setTargetTable() method.
	 */
	public void processRow(Object[] row)
		throws SQLException
	{
		if (row == null) return;
		if (row.length != this.colCount) 
		{
			throw new SQLException("Invalid row data received. Size of row array does not match column count");
		}

		currentImportRow++;
		if (currentImportRow < startRow) return;
		if (currentImportRow > endRow) 
		{
			LogMgr.logInfo("DataImporter.processRow()", "Import limit (" + this.endRow + ") reached. Stopping import");
			String msg = ResourceMgr.getString("MsgPartialImportEnded");
			msg = StringUtil.replace(msg, "%rowlimit%", Long.toString(endRow));
			this.messages.append(msg);
			this.messages.appendNewLine();
			this.source.stop();
			return;
		}

		ImportRow irow = new ImportRow();
		irow.data = row;
		irow.rowNumber = currentImportRow;

		try
		{
			importQueue.put(irow);
		}
		catch (Exception ie)
		{
			LogMgr.logError("DataImporter.processRow()", "Got interrupted while putting a row into the queue. Please increase the queue size", ie);
		}

		if (this.progressMonitor != null && this.reportInterval > 0 && (currentImportRow == 1 || currentImportRow % reportInterval == 0))
		{
			if (this.totalTables > 0)
			{
				StringBuilder msg = new StringBuilder(this.targetTable.getTableName().length() + 20);
				msg.append(this.targetTable.getTableName());
				msg.append(" [");
				msg.append(this.currentTable);
				msg.append('/');
				msg.append(this.totalTables);
				msg.append(']');
				progressMonitor.setCurrentObject(msg.toString(), currentImportRow, -1);
			}
			else
			{
				progressMonitor.setCurrentObject(this.targetTable.getTableName(), currentImportRow, -1);
			}
		}

	}
	
	private void checkConstantValues()
		throws SQLException
	{
		if (this.columnConstants == null) return;
		for (ColumnIdentifier col : this.targetColumns)
		{
			if (this.columnConstants.removeColumn(col))
			{
				String msg = ResourceMgr.getFormattedString("MsgImporterConstIgnored", col.getColumnName());
				this.messages.append(msg);
				this.messages.appendNewLine();
				if (this.continueOnError)
				{
					LogMgr.logWarning("DataImporter.checkConstanValues()", msg);
				}
				else
				{
					throw new SQLException(msg);
				}
			}
		}
	}
	
	/**
	 *	Callback function from the RowDataProducer
	 */
	public void setTargetTable(TableIdentifier table, ColumnIdentifier[] columns)
		throws SQLException
	{
		// be prepared to import more then one table...
		if (this.isRunning && this.targetTable != null)
		{
			try
			{
				this.finishTable();
			}
			catch (SQLException e)
			{
				if (!this.continueOnError) 
				{
					this.hasErrors = true;
					throw e;
				}
			}
		}
		
		this.currentImportRow = 0;
		this.errorCount = 0;
		this.errorLimitAdded = false;
		
		try
		{
			this.targetTable = table.createCopy();
			this.targetColumns = Arrays.asList(columns);
			
			// Key columns might have been externally defined if
			// a single table import is run which is not possible
			// when using a multi-table import. So the keyColumns 
			// should only be reset if a multi-table import is running!
			if (this.multiTable) 
			{
				this.keyColumns = null;
			}
			
			this.colCount = this.targetColumns.size();

			if (this.parser != null)
			{
				String msg = ResourceMgr.getFormattedString("MsgImportingFile", this.parser.getSourceFilename(), this.targetTable.getTableName());
				this.messages.append(msg);
				this.messages.appendNewLine();
			}
			
			if (this.createTarget)
			{
				try
				{
					this.createTarget();
				}
				catch (SQLException e)
				{
					String msg = ResourceMgr.getString("ErrImportTableNotCreated");
					msg = StringUtil.replace(msg, "%table%", this.targetTable.getTableExpression(this.dbConn));
					msg = StringUtil.replace(msg, "%error%", ExceptionUtil.getDisplay(e));
					this.messages.append(msg);
					this.messages.appendNewLine();
					LogMgr.logError("DataImporter.setTargetTable()", "Could not create target: " + this.targetTable, e);
					this.hasErrors = true;
					throw e;
				}
			}
			
			try
			{
				this.checkTable();
			}
			catch (SQLException e)
			{
				String msg = ResourceMgr.getFormattedString("ErrImportTableNotFound", this.targetTable.getTableExpression());
				if (parser != null)
				{
					String s = ResourceMgr.getString("ErrImportFileNotProcessed");
					msg = msg + " " + StringUtil.replace(s, "%filename%", this.parser.getSourceFilename());
				}
				this.hasErrors = true;
				this.messages.append(msg);
				this.messages.appendNewLine();
				this.targetTable = null;
				throw e;
			}

			checkConstantValues();
			
			if (this.mode != MODE_UPDATE)
			{
				this.prepareInsertStatement();
			}
			
			if (this.mode != MODE_INSERT)
			{
				this.prepareUpdateStatement();
			}

			if (this.deleteTarget != DeleteType.none && this.tablesToBeProcessed == null)
			{
				if (this.progressMonitor != null)
				{
					this.progressMonitor.saveCurrentType("importDelete");
					this.progressMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
					String msg = ResourceMgr.getFormattedString("TxtDeletingTable", this.targetTable.getObjectName());
					this.progressMonitor.setCurrentObject(msg,-1,-1);
				}

				try
				{
					this.deleteTarget();
				}
				catch (SQLException e)
				{
					this.hasErrors = true;
					String msg = ResourceMgr.getString("ErrDeleteTableData");
					msg = msg.replace("%table%",table.toString());
					msg = msg.replace("%error%", ExceptionUtil.getDisplay(e));
					this.messages.append(msg);
					this.messages.appendNewLine();
					
					LogMgr.logError("DataImporter.setTargetTable()", "Could not delete contents of table " + this.targetTable, e);
					if (!this.continueOnError)
					{
						throw e;
					}
				}
			}
				
			this.currentImportRow = 0;

			if (progressMonitor != null) this.progressMonitor.restoreType("importDelete");

			if (this.reportInterval == 0 && this.progressMonitor != null)
			{
				this.progressMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
				this.progressMonitor.setCurrentObject(ResourceMgr.getString("MsgImportingTableData") + " " + this.targetTable + " (" + this.getModeString() + ")",-1,-1);
			}

			if (LogMgr.isInfoEnabled())
			{
				LogMgr.logInfo("DataImporter.setTargetTable()", "Starting import for table " + this.targetTable.getTableExpression());
			}
			
			if (this.badfileName != null)
			{
				this.badWriter = new BadfileWriter(this.badfileName, this.targetTable, "UTF8");
			}
			else
			{
				this.badWriter = null;
			}
			
			if (this.tableStatements != null)
			{
				this.tableStatements.runPreTableStatement(dbConn, targetTable);
			}
			worker.init(this);
		}
		catch (RuntimeException th)
		{
			this.hasErrors = true;
			LogMgr.logError("DataImporter.setTargetTable()", "Error when setting target table " + this.targetTable.getTableExpression(), th);
			throw th;
		}
	}

	public int getMode()
	{
		return mode;
	}
	
	private String getModeString()
	{
		if (this.isModeInsert()) return "insert";
		if (this.isModeUpdate()) return "update";
		if (this.isModeInsertUpdate()) return "insert/update";
		if (this.isModeUpdateInsert()) return "update/insert";
		return "";
	}

	private void checkTable()
		throws SQLException
	{
		if (this.dbConn == null) return;
		if (this.targetTable == null) return;

		DbMetadata meta = this.dbConn.getMetadata();
		boolean exists = meta.tableExists(this.targetTable);
		if (!exists)
		{
			throw new SQLException("Table " + this.targetTable.getTableExpression(this.dbConn) + " not found!");
		}
	}

	/**
	 * 	Prepare the statement to be used for inserts.
	 * 	targetTable and targetColumns have to be initialized before calling this!
	 */
	private void prepareInsertStatement()
		throws SQLException
	{
		StringBuilder text = new StringBuilder(this.targetColumns.size() * 50);
		StringBuilder parms = new StringBuilder(targetColumns.size() * 20);

		String sql = dbConn.getDbSettings().getInsertForImport();
		if (!StringUtil.isEmptyString(sql))
		{
			text.append(sql);
			text.append(' ');
		}
		else
		{
			text.append("INSERT INTO ");
		}
		text.append(targetTable.getTableExpression(this.dbConn));
		text.append(" (");
		for (int i=0; i < this.colCount; i++)
		{
			if (i > 0)
			{
				text.append(',');
				parms.append(',');
			}
			text.append(this.targetColumns.get(i).getColumnName());
			parms.append('?');
		}
		if (this.columnConstants != null)
		{
			int cols = columnConstants.getColumnCount();
			for (int i=0; i < cols; i++)
			{
				text.append(',');
				text.append(columnConstants.getColumn(i).getColumnName());
				parms.append(',');
				if (columnConstants.isFunctionCall(i))
				{
					parms.append(columnConstants.getFunctionLiteral(i));
				}
				else
				{
					parms.append('?');
				}
			}
		}
		text.append(") VALUES (");
		text.append(parms);
		text.append(')');

		String insertSql = text.toString();
		try
		{
			worker.setInsertSql(insertSql);
			LogMgr.logInfo("DataImporter.prepareInsertStatement()", "Statement for insert: " + insertSql);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.prepareInsertStatement()", "Error when preparing INSERT statement: " + insertSql, e);
			this.messages.append(ResourceMgr.getString("ErrImportInitTargetFailed"));
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.hasErrors = true;
			throw e;
		}
	}

	/**
	 * 	Prepare the statement to be used for updates
	 * 	targetTable and targetColumns have to be initialized before calling this!
	 */
	private void prepareUpdateStatement()
		throws SQLException, ModeNotPossibleException
	{
		if (!this.hasKeyColumns())
		{
			this.retrieveKeyColumns();
			if (!this.hasKeyColumns())
			{
				this.messages.append(ResourceMgr.getString("ErrImportNoKeyForUpdate"));
				this.messages.appendNewLine();
				throw new SQLException("No key columns defined for update mode");
			}
		}

		this.columnMap = new int[this.colCount];
		int pkIndex = this.colCount - this.keyColumns.size();
		int pkCount = 0;
		int colIndex = 0;
		StringBuilder sql = new StringBuilder(this.colCount * 20 + 80);
		StringBuilder where = new StringBuilder(this.keyColumns.size() * 10);
		sql.append("UPDATE ");
		sql.append(this.targetTable.getTableExpression(this.dbConn));
		sql.append(" SET ");
		where.append(" WHERE ");
		boolean pkAdded = false;
		for (int i=0; i < this.colCount; i++)
		{
			ColumnIdentifier col = this.targetColumns.get(i);
			if (keyColumns.contains(col))
			{
				this.columnMap[i] = pkIndex;
				if (pkAdded) where.append(" AND ");
				else pkAdded = true;
				where.append(col.getColumnName());
				where.append(" = ?");
				pkIndex ++;
				pkCount ++;
			}
			else
			{
				this.columnMap[i] = colIndex;
				if (colIndex > 0)
				{
					sql.append(", ");
				}
				sql.append(col.getColumnName());
				sql.append(" = ?");
				colIndex ++;
			}
		}
		
		if (!pkAdded)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "No primary key columns defined! Update mode not available\n", null);
			this.messages.append(ResourceMgr.getString("ErrImportNoKeyForUpdate"));
			this.messages.appendNewLine();
			this.hasErrors = true;
			throw new SQLException("No key columns defined for update mode");
		}
		
		if (pkCount != this.keyColumns.size())
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "At least one of the supplied primary key columns was not found in the target table!", null);
			this.messages.append(ResourceMgr.getString("ErrImportUpdateKeyColumnNotFound") + "\n");
			this.hasErrors = true;
			throw new SQLException("Not enough key columns defined for update mode");
		}
		
		if (colIndex == 0)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "Only PK columns defined! Update mode is not available!", null);
			this.messages.append(ResourceMgr.getString("ErrImportOnlyKeyColumnsForUpdate"));
			this.messages.appendNewLine();
			if (this.isModeUpdate())
			{
				// if only update mode was specified this is an error!
				this.hasErrors = true;
				throw new ModeNotPossibleException("Only key columns available. No update mode possible");
			}
			else
			{
				this.hasWarnings = true;
			}
			return;
		}
		
		sql.append(where);
		if (!StringUtil.isEmptyString(this.whereClauseForUpdate))
		{
			boolean addBracket = false;
			if (!this.whereClauseForUpdate.trim().toUpperCase().startsWith("AND") &&
				  !this.whereClauseForUpdate.trim().toUpperCase().startsWith("OR")
				)
			{
				sql.append(" AND (");
				addBracket = true;
			}
			else
			{
				sql.append(' ');
			}
			sql.append(this.whereClauseForUpdate.trim());
			if (addBracket) sql.append(")");
		}

		String updateSql = sql.toString();
		try
		{
			LogMgr.logInfo("DataImporter.prepareUpdateStatement()", "Statement for update: " + updateSql);
			worker.setUpdateSql(updateSql);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "Error when preparing UPDATE statement", e);
			this.messages.append(ResourceMgr.getString("ErrImportInitTargetFailed"));
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.hasErrors = true;
			throw e;
		}
	}

	/**
	 *	If the key columns have not been defined externally through {@link #setKeyColumns(List)}
	 *	this method  is used to retrieve the key columns for the target table
	 */
	private void retrieveKeyColumns()
	{
		try
		{
			List<ColumnIdentifier> cols = this.dbConn.getMetadata().getTableColumns(this.targetTable);
			this.keyColumns = new LinkedList<ColumnIdentifier>();
			for (ColumnIdentifier col : cols)
			{
				if (col.isPkColumn())
				{
					this.keyColumns.add(col);
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.retrieveKeyColumns()", "Error when retrieving key columns", e);
			this.columnMap = null;
			this.keyColumns = null;
		}
	}

	private void finishTable()
		throws SQLException
	{
		if (this.targetTable == null) return;
		
		boolean commitNeeded = this.transactionControl && !dbConn.getAutoCommit() && (this.commitEvery != Committer.NO_COMMIT_FLAG);

		// make sure all pending rows are processed before starting the next import
		if (!errorAbort && !hasErrors)
		{
			while (importQueue.peek() != null)
			{
				try { Thread.sleep(100); } catch (Throwable th) {}
			}
		}
		sendAbortMessage();
		
//		if (importQueue.peek() != null)
//		{
//			System.out.println("finishTable called but still rows to process!");
//		}
		
		try
		{
			worker.finishTable();
			
			if (this.useBatch && commitBatch)
			{
				// If the batch is executed and committed, there is no 
				// need to send another commit. In fact some DBMS don't like
				// a commit or rollback if no transaction was started.
				commitNeeded = false;
			}

			long inserted = worker.getInsertedRows();
			long updated = worker.getUpdatedRows();

			// done() will reset all counters, so the number of rows must be retrieved before that
			worker.done();

			if (this.tableStatements != null)
			{
				this.tableStatements.runPostTableStatement(dbConn, targetTable);
			}
			
			String msg = this.targetTable.getTableName() + ": " + inserted + " row(s) inserted. " + updated + " row(s) updated.";
			if (!transactionControl)
			{
				msg += " Transaction control disabled. No commit sent to server.";
			}
			else
			{
				msg += " Committing changes.";
			}
			
			if (commitNeeded)
			{
				 worker.commit();
			}
			
			LogMgr.logInfo("DataImporter.finishTable()", msg);

			this.messages.append(this.source.getMessages());
			if (inserted > -1)
			{
				this.messages.append(inserted + " " + ResourceMgr.getString("MsgCopyNumRowsInserted"));
				this.messages.appendNewLine();
			}
			if (updated > -1)
			{
				this.messages.append(updated + " " + ResourceMgr.getString("MsgCopyNumRowsUpdated"));
			}
			if (this.badWriter != null && badWriter.getRows() > 0)
			{
				this.messages.appendNewLine();
				this.messages.append(this.badWriter.getMessage());
			}
			this.messages.appendNewLine();
			this.hasErrors = hasErrors || this.source.hasErrors();
			this.hasWarnings = hasWarnings || this.source.hasWarnings();
		}
		catch (SQLException e)
		{
			if (commitNeeded)
			{
				try { this.dbConn.rollback(); } catch (Throwable ignore) {}
			}
			LogMgr.logError("DataImporter.finishTable()", "Error commiting changes", e);
			this.hasErrors = true;
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.messages.appendNewLine();
			throw e;
		}
	}

	/** 
	 * Return the messages generated during import.
	 * Calling this, clears the message buffer
	 * @return the message buffer.
	 * @see workbench.util.MessageBuffer#getBuffer()
	 */
	public CharSequence getMessages()
	{
		return messages.getBuffer();
	}

	public void copyMessages(MessageBuffer target)
	{
		target.append(this.messages);
		clearMessages();
	}
	

	/**
	 *	Callback from the RowDataProducer
	 */
	public void importFinished()
	{
		if (!isRunning) return;
		try
		{
			this.finishTable();
		}
		catch (SQLException sql)
		{
			// already logged in finishTable()
		}
		catch (Exception e)
		{
			// log all others...
			LogMgr.logError("DataImporter.importFinished()", "Error when commiting changes", e);
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.hasErrors = true;
		}
		
		finally
		{
			this.isRunning = false;
			if (!multiTable)
			{
				if (this.progressMonitor != null) this.progressMonitor.jobFinished();
			}
		}
		sendAbortMessage();
		
		this.hasErrors = this.hasErrors || this.source.hasErrors();
		this.hasWarnings = this.hasWarnings || this.source.hasWarnings();

		worker.dispose();
	}

	private void cleanupRollback()
	{
		try
		{
			worker.done();
			if (this.transactionControl && !this.dbConn.getAutoCommit())
			{
				LogMgr.logInfo("DataImporter.cleanupRollback()", "Rollback changes");
				this.dbConn.rollback();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.cleanupRollback()", "Error on rollback", e);
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.hasErrors = true;
		}
		this.isRunning = false;
		//this.messages.append(this.source.getMessages());
		if (this.progressMonitor != null) this.progressMonitor.jobFinished();
	}
	
	public void tableImportError()
	{
		cleanupRollback();
	}

	
	public void importCancelled()
	{
		if (!isRunning) return;
		
		if (this.partialImportEnded)
		{
			this.importFinished();
			return;
		}
		
		cleanupRollback();
		this.hasErrors = this.hasErrors || this.source.hasErrors();
		this.hasWarnings = this.hasWarnings || this.source.hasWarnings();
		
	}

	public int getReportInterval()
	{
		return this.reportInterval;
	}
	
	public void setReportInterval(int interval)
	{
		if (interval > 0)
		{
			this.reportInterval = interval;
		}
		else
		{
			this.reportInterval = 0;
		}
	}

	public String getTargetSchema()
	{
		return targetSchema;
	}

	public void setTargetSchema(String targetSchema)
	{
		this.targetSchema = targetSchema;
	}

}
