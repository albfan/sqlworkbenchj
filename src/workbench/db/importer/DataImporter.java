/*
 * DataImporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.importer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import workbench.interfaces.BatchCommitter;
import workbench.interfaces.Committer;
import workbench.interfaces.ImportFileParser;
import workbench.interfaces.Interruptable;
import workbench.interfaces.ProgressReporter;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DmlExpressionBuilder;
import workbench.db.SequenceAdjuster;
import workbench.db.TableCreator;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.compare.BatchedStatement;

import workbench.storage.RowActionMonitor;

import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.MemoryWatcher;
import workbench.util.MessageBuffer;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * Import data that is provided from a {@link RowDataProducer} into
 * a table in the database.
 *
 * @see workbench.sql.wbcommands.WbImport
 * @see workbench.sql.wbcommands.WbCopy
 * @see workbench.db.datacopy.DataCopier
 *
 * @author  Thomas Kellerer
 */
public class DataImporter
	implements Interruptable, DataReceiver, ProgressReporter, BatchCommitter
{
	public static final int MODE_INSERT = 0;
	public static final int MODE_UPDATE = 1;
	public static final int MODE_INSERT_UPDATE = 2;
	public static final int MODE_UPDATE_INSERT = 3;

	private WbConnection dbConn;

	private RowDataProducer source;
	private BatchedStatement insertStatement;
	private BatchedStatement updateStatement;

	private TableIdentifier targetTable;

	private int commitEvery = 0;

	private DeleteType deleteTarget = DeleteType.none;
	private boolean createTarget;
	private String createType;
	private boolean continueOnError;

	private boolean ignoreIdentityColumns;

	private long totalRows = 0;
	private long updatedRows = 0;
	private long insertedRows = 0;
	private long currentImportRow = 0;
	private int mode = MODE_INSERT;
	private boolean useBatch;
	private int batchSize = -1;
	private boolean commitBatch;

	private boolean hasErrors;
	private boolean hasWarnings;
	private int reportInterval = 10;
	private MessageBuffer messages;

	private int colCount;
	private int totalTables = -1;
	private int currentTable = -1;
	private boolean transactionControl = true;
	private boolean useSetNull;

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
	private boolean isRunning;
	private ImportFileParser parser;

	// Use for partial imports
	private long startRow = 0;
	private long endRow = Long.MAX_VALUE;
	private boolean partialImportEnded;

	// Additional WHERE clause for UPDATE statements
	private String whereClauseForUpdate;
	private BadfileWriter badWriter;
	private String badfileName;

	private boolean useSavepoint;
	private Savepoint insertSavepoint;
	private Savepoint updateSavepoint;

	private boolean checkRealClobLength;
	private boolean isOracle;
	private boolean useSetObjectWithType;
	private int maxErrorCount = 1000;

	private boolean verifyTargetTable = true;
	private boolean adjustSequences;
	private String insertSqlStart;

	/**
	 * Indicates multiple imports run with this instance oft DataImporter.
	 * Set via {@link #beginMultiTable() }
	 */
	private boolean multiTable;

	private TableStatements tableStatements;

	private int errorCount;
	private boolean errorLimitAdded;
	private DmlExpressionBuilder builder;

	public DataImporter()
	{
		this.messages = new MessageBuffer();
		maxErrorCount = Settings.getInstance().getIntProperty("workbench.import.maxerrors", 1000);
	}

	public void setConnection(WbConnection aConn)
	{
		this.dbConn = aConn;
		if (dbConn == null) return;

		this.checkRealClobLength = this.dbConn.getDbSettings().needsExactClobLength();
		this.isOracle = this.dbConn.getMetadata().isOracle();
		this.useSetNull = this.dbConn.getDbSettings().useSetNull();

		// apparently the JDBC/ODBC bridge does not support setObject(x, null) so always use setNull()
		// regardless of the DBMS
		if (dbConn.getProfile().getDriverclass().equals("sun.jdbc.odbc.JdbcOdbcDriver"))
		{
			this.useSetNull = true;
		}

		this.useSetObjectWithType = this.dbConn.getDbSettings().getUseTypeWithSetObject();
		this.builder = DmlExpressionBuilder.Factory.getBuilder(dbConn);
	}

	public void setUseSavepoint(boolean flag)
	{

		this.useSavepoint = flag;
		if (dbConn != null)
		{
			// no need to use a savepoint with autocommit
			if (this.dbConn.getAutoCommit())
			{
				useSavepoint = false;
			}
			if (useSavepoint && !this.dbConn.supportsSavepoints())
			{
				LogMgr.logWarning("DataImporter.setUseSavepoint()", "A savepoint should be used for each statement but the driver does not support savepoints!");
				this.useSavepoint = false;
			}
		}
		LogMgr.logInfo("DataImporter.setUseSavepoint()", "Using savepoints for DML: " + useSavepoint);
	}

	public void setAdjustSequences(boolean flag)
	{
		this.adjustSequences = flag;
	}

	public void setIgnoreIdentityColumns(boolean flag)
	{
		this.ignoreIdentityColumns = flag;
	}

	public void setInsertStart(String sql)
	{
		if (StringUtil.isBlank(sql))
		{
			insertSqlStart = null;
		}
		else
		{
			insertSqlStart = sql;
		}
	}

	private boolean supportsBatch()
	{
		if (this.dbConn == null) return true;
		return dbConn.getMetadata().supportsBatchUpdates();
	}

	@Override
	public boolean isTransactionControlEnabled()
	{
		return this.transactionControl;
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

	/**
	 * Indicates that this DataImporter is used for multiple import files into one or more target tables.
	 * If setDeleteTarget() has been set to anything other than none, then all tables that have been defined
	 * so far are deleted.
	 *
	 * @throws SQLException
	 * @see #deleteTargetTables()
	 * @see #setDeleteTarget(workbench.db.importer.DeleteType)
	 * @see #setTableList(java.util.List)
	 * @see #setTargetTable(workbench.db.TableIdentifier, java.util.List)
	 */
	@Override
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

	@Override
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

	@Override
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
	@Override
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
	@Override
	public void setCommitEvery(int aCount)
	{
		if (aCount > 0 || aCount == Committer.NO_COMMIT_FLAG)
		{
			this.commitBatch = false;
		}
		this.commitEvery = aCount;
	}

	public boolean getContinueOnError()
	{
		return this.continueOnError;
	}

	public void setContinueOnError(boolean flag)
	{
		this.continueOnError = flag;
	}

	@Override
	public int getBatchSize()
	{
		return batchSize;
	}

	@Override
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

	@Override
	public void setTableList(List<TableIdentifier> targetTables)
	{
		this.tablesToBeProcessed = targetTables;
	}

	@Override
	public void deleteTargetTables()
		throws SQLException
	{
		if (!this.isModeInsert())
		{
			LogMgr.logWarning("DataImporter.deleteTargetTables()", "Target tables will not be deleted because import mode is not set to 'insert'");
			this.messages.appendMessageKey("ErrImpNoDeleteUpd");
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
	 * @see #setTargetTable(workbench.db.TableIdentifier, java.util.List)
	 */
	public void setCreateTarget(boolean flag)
	{
		this.createTarget = flag;
	}

	@Override
	public boolean getCreateTarget()
	{
		return createTarget;
	}

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
	@Override
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
			return 10;
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
	 *	Return the numeric mode value based on keywords.
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
		this.keyColumns = new LinkedList<>();
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier col = new ColumnIdentifier((String)cols.get(i));
			keyColumns.add(col);
		}
	}

	public List<ColumnIdentifier> getKeyColumns()
	{
		if (this.keyColumns == null) return Collections.emptyList();
		return Collections.unmodifiableList(keyColumns);
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

	private boolean ignoreColumn(ColumnIdentifier col)
	{
		if (col == null) return true;
		if (!ignoreIdentityColumns) return false;
		return col.isAutoGenerated();
	}

	/**
	 *	Start the import
	 */
	public void startImport()
		throws IOException, SQLException, Exception
	{
		if (this.source == null) return;
		this.isRunning = true;

		if (this.useBatch)
		{
			if (!supportsBatch())
			{
				LogMgr.logWarning("DataImporter.setUseBatch()", "JDBC driver does not support batch updates. Ignoring request to use batch updates");
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
				this.messages.appendMessageKey("ErrImportNoBatchMode");
			}
		}

		try
		{
			this.source.start();
		}
		catch (CycleErrorException e)
		{
			this.hasErrors = true;
			messages.appendMessageKey("ErrImpCycle");
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

		Statement stmt = null;
		try
		{
			stmt = this.dbConn.createStatement();
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
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}

	private void createTarget()
		throws SQLException
	{
		TableCreator creator = new TableCreator(this.dbConn, createType, this.targetTable, this.targetColumns);
		creator.useDbmsDataType(true);
		creator.createTable();
		String table = creator.getTable().getTableName();
		String msg = StringUtil.replace(ResourceMgr.getString("MsgImporterTableCreated"), "%table%", table);
		targetTable = dbConn.getMetadata().findTable(targetTable); // make sure we use the correct table name
		this.messages.append(msg);
		this.messages.appendNewLine();
	}

	public boolean isRunning()
	{
		return this.isRunning;
	}

	public boolean isSuccess()
	{
		return !hasErrors;
	}

	public boolean hasWarnings()
	{
		return this.hasWarnings;
	}

	public long getAffectedRows()
	{
		return this.totalRows;
	}

	public long getInsertedRows()
	{
		return this.insertedRows;
	}

	public long getUpdatedRows()
	{
		return this.updatedRows;
	}

	/**
	 *	This method is called if cancelExecution() is called
	 *	to check if the user should confirm the cancelling of the import
	 */
	@Override
	public boolean confirmCancel()
	{
		return true;
	}

	@Override
	public void cancelExecution()
	{
		if (this.tableDeleter != null)
		{
			this.tableDeleter.cancel();
		}

		if (this.source != null) this.source.cancel();
		this.messages.append(ResourceMgr.getString("MsgImportCancelled") + "\n");
	}

	private void cancelStatement(BatchedStatement stmt)
	{
		if (stmt == null) return;
		try
		{
			stmt.cancel();
		}
		catch (Exception e)
		{
			LogMgr.logDebug("DataImporter.cancelStatement()", "Error when cancelling statement", e);
		}
	}

	@Override
	public void setTableCount(int total)
	{
		this.totalTables = total;
	}

	@Override
	public void setCurrentTable(int current)
	{
		this.currentTable = current;
	}

	private void addError(String msg)
	{
		errorCount ++;
		if (errorCount < maxErrorCount)
		{
			this.messages.append(msg);
		}
		else if (!errorLimitAdded)
		{
			messages.appendNewLine();
			messages.append(ResourceMgr.getString("MsgImpTooManyError"));
			messages.appendNewLine();
			errorLimitAdded = true;
		}
	}

	@Override
	public void recordRejected(String record, long importRow, Throwable error)
	{
		if (record == null) return;

		if (badWriter != null && record != null)
		{
			badWriter.recordRejected(record);
		}
		else
		{
			this.addError(ResourceMgr.getString("ErrImportingRow") + " " + importRow + "\n");
			this.addError(ResourceMgr.getString("ErrImportErrorMsg") + " " + ExceptionUtil.getAllExceptions(error) + "\n");
			this.addError(ResourceMgr.getString("ErrImportValues") + " " + record + "\n");
			if (errorLimitAdded)
			{
				LogMgr.logError("DataImporter.recordRejected()", "Values: " + record, error);
			}
		}
	}

	@Override
	public boolean shouldProcessNextRow()
	{
		return currentImportRow + 1 >= startRow && currentImportRow + 1 <= endRow;
	}

	@Override
	public void nextRowSkipped()
	{
		this.currentImportRow ++;
	}

	@Override
	public void processFile(StreamImporter stream)
		throws SQLException, IOException
	{
		if (stream != null)
		{
			try
			{
				this.insertedRows += stream.processStreamData();
			}
			catch (SQLException sql)
			{
				this.hasErrors = true;
				LogMgr.logError("DataImporter.processFile()", "Error importing file: " + ExceptionUtil.getDisplay(sql), null);
				this.addError(sql.getLocalizedMessage()+ "\n");
			}
		}
	}

	/**
	 *	Callback function for RowDataProducer. The order in the data array
	 * 	has to be the same as initially passed in the setTargetTable() method.
	 */
	@Override
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

		long rows = 0;
		try
		{
			switch (this.mode)
			{
				case MODE_INSERT:
					rows = this.insertRow(row, useSavepoint && continueOnError);
					break;

				case MODE_INSERT_UPDATE:
					boolean inserted = false;
					// in case of an Exception we are retrying the row
					// with an update. Theoretically the only expected
					// exception should indicate a primary key violation,
					// but as we don't analyze the exception, we will
					// try the update, for any exception. If the exception
					// was not a key violation, the update will most probably
					// fail as well.
					try
					{
						rows = this.insertRow(row, useSavepoint);
						inserted = true;
					}
					catch (SQLException sql)
					{
						if (ignoreInsertError(sql))
						{
							inserted = false;
						}
						else
						{
							throw sql;
						}
					}
					catch (Exception e)
					{
						LogMgr.logDebug("DataImporter.processRow()", "Unexpected error when inserting row in insert/update mode", e);
						throw new SQLException("Error during insert", e);
					}

					// The update statement might have been set to null
					// because an update is not possible (when only key columns
					// are present in the table). In this case we silently skip
					// the failed insert
					if (!inserted && this.updateStatement != null)
					{
						rows = this.updateRow(row, useSavepoint && continueOnError);
					}
					break;

				case MODE_UPDATE_INSERT:
					// an exception is not expected when updating the row
					// if the row does not exist, the update counter should be
					// zero. If the update violates any constraints, then the
					// INSERT will fail as well, so any exception thrown, indicates
					// an error with this row, so we will not proceed with the insert

					if (this.updateStatement == null)
					{
						rows = this.insertRow(row, useSavepoint && continueOnError);
					}
					else
					{
						rows = this.updateRow(row, useSavepoint && continueOnError);
						if (rows <= 0)
						{
							rows = this.insertRow(row, useSavepoint && continueOnError);
						}
					}
					break;

				case MODE_UPDATE:
					rows = this.updateRow(row, useSavepoint && continueOnError);
					break;
			}
			this.totalRows += rows;
		}
		catch (OutOfMemoryError oome)
		{
			this.hasErrors = true;
			closeStatements();
			this.messages.clear();
			System.gc();
			this.messages.append(ResourceMgr.getString("MsgOutOfMemoryGeneric"));
			this.messages.appendNewLine();
			if (this.batchSize > 0)
			{
				LogMgr.logError("DataImporter.processRow()", "Not enough memory to hold statement batch! Use the -batchSize parameter to reduce the batch size!", null);
				this.messages.append(ResourceMgr.getString("MsgOutOfMemoryJdbcBatch"));
				this.messages.appendNewLine();
				this.messages.appendNewLine();
			}
			else
			{
				LogMgr.logError("DataImporter.processRow()", "Not enough memory to run this import!", null);
			}
			throw new SQLException("Not enough memory!");
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.processRow()", "Error importing row " + currentImportRow + ": " + ExceptionUtil.getDisplay(e), null);
			String rec = this.source.getLastRecord();
			if (rec == null)
			{
				ValueDisplay display = new ValueDisplay(row);
				rec = display.toString();
			}
			recordRejected(rec, currentImportRow, e);
			if (this.continueOnError)
			{
				this.hasWarnings = true;
			}
			else
			{
				this.hasErrors = true;
				throw e;
			}
		}

		if (MemoryWatcher.isMemoryLow(false))
		{
			this.hasErrors = true;
			closeStatements();
			this.messages.clear();
			this.messages.append(ResourceMgr.getString("MsgLowMemoryError"));
			this.messages.appendNewLine();
			throw new SQLException("Not enough memory!");
		}

	}

	/**
	 * Check if the given SQL Exception identifies a primary (unique) key violation.
	 * If an error code or SQLState is configured for the current DBMS this is checked.
	 * Otherwise this returns true.
	 *
	 * @param sql the exception
	 * @return true if the error should be ignored in insert/update mode
	 * @see DbSettings#getUniqueKeyViolationErrorCode()
	 * @see DbSettings#getUniqueKeyViolationErrorState()
	 */
	private boolean ignoreInsertError(SQLException sql)
	{
		String state = dbConn.getDbSettings().getUniqueKeyViolationErrorState();
		int error = dbConn.getDbSettings().getUniqueKeyViolationErrorCode();
		if (state != null)
		{
			return state.equals(sql.getSQLState());
		}

		if (error > 0)
		{
			return sql.getErrorCode() == error;
		}

		// Nothing configured to detect a primary key violation --> ignore it
		return true;
	}

	private void setUpdateSavepoint()
	{
		try
		{
			this.updateSavepoint = this.dbConn.getSqlConnection().setSavepoint();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter", "Could not create pre-update Savepoint", e);
		}
	}

	private void setInsertSavepoint()
	{
		try
		{
			this.insertSavepoint = this.dbConn.getSqlConnection().setSavepoint();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter", "Could not set pre-insert Savepoint", e);
		}
	}

	private void rollbackUpdate()
	{
		rollbackToSavepoint(updateSavepoint);
		updateSavepoint = null;
	}

	private void rollbackInsert()
	{
		rollbackToSavepoint(insertSavepoint);
		insertSavepoint = null;
	}

	private void rollbackToSavepoint(Savepoint savepoint)
	{
		if (savepoint == null) return;
		try
		{
			this.dbConn.getSqlConnection().rollback(savepoint);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.rollbackToSavePoint()", "Error when performing rollback to savepoint", e);
		}
	}

	private void releaseInsertSavepoint()
	{
		releaseSavepoint(insertSavepoint);
		insertSavepoint = null;
	}

	private void releaseUpdateSavepoint()
	{
		releaseSavepoint(updateSavepoint);
		updateSavepoint = null;
	}

	private void releaseSavepoint(Savepoint savepoint)
	{
		if (savepoint == null) return;
		try
		{
			this.dbConn.getSqlConnection().releaseSavepoint(savepoint);
		}
		catch (Throwable th)
		{
			LogMgr.logError("DataImporter.processrow()", "Error when releasing savepoint", th);
		}
	}

	/**
	 *	Insert a row of data into the target table.
	 *	This method relies on insertStatement correctly initialized with
	 *	all parameters at the correct location.
	 */
	private long insertRow(Object[] row, boolean useSP)
		throws SQLException
	{
		try
		{
			if (useSP) setInsertSavepoint();
			long rows = processRowData(this.insertStatement, row, false);
			this.insertedRows += rows;
			releaseInsertSavepoint();
			return rows;
		}
		catch (SQLException e)
		{
			if (useSP)
			{
				rollbackInsert();
			}
			throw e;
		}
	}

	/**
	 *	Update the data in the target table using the PreparedStatement
	 *	available in updateStatement
	 */
	private long updateRow(Object[] row, boolean useSP)
		throws SQLException
	{
		try
		{
			if (useSP) setUpdateSavepoint();
			long rows = processRowData(this.updateStatement, row, true);
			this.updatedRows += rows;
			releaseUpdateSavepoint();
			return rows;
		}
		catch (SQLException e)
		{
			if (useSP)
			{
				rollbackUpdate();
			}
			throw e;
		}
	}

	private long processRowData(BatchedStatement pstmt, Object[] row, boolean useColMap)
		throws SQLException
	{
		int colIndex = 0;

		for (int i=0; i < row.length; i++)
		{
			ColumnIdentifier column = targetColumns.get(i);
			if (ignoreColumn(column)) continue;
			colIndex ++;

			if (useColMap)
			{
				// The colIndex points to the correct location in the PreparedStatement
				// when using UPDATE with different column names
				colIndex = this.columnMap[i] + 1;
			}

			int jdbcType = column.getDataType();
			String dbmsType = column.getDbmsType();

			if (row[i] == null)
			{
				if (useSetNull)
				{
					pstmt.setNull(colIndex, jdbcType);
				}
				else
				{
					pstmt.setObject(colIndex, null);
				}
			}
			else if (SqlUtil.isClobType(jdbcType, dbmsType, dbConn.getDbSettings()) || SqlUtil.isXMLType(jdbcType))
			{
				Reader in = null;
				int size = -1;

				if (row[i] instanceof Clob)
				{
					Clob clob = (Clob) row[i];
					in = clob.getCharacterStream();
				}
				else if (row[i] instanceof File)
				{
					ImportFileHandler handler = (this.parser != null ? parser.getFileHandler() : null);
					String encoding = (handler != null ? handler.getEncoding() : null);
					if (encoding == null)
					{
						encoding = (this.parser != null ? parser.getEncoding() : Settings.getInstance().getDefaultDataEncoding());
					}

					File f = (File)row[i];
					try
					{
						if (handler != null)
						{
							in = EncodingUtil.createReader(handler.getAttachedFileStream(f), encoding);

							// Apache Derby needs the exact length in characters
							// which might not be the file size if a multi-byte encoding is used
							if (checkRealClobLength)
							{
								size = (int) handler.getCharacterLength(f);
							}
							else
							{
								size = (int) handler.getLength(f);
							}
						}
						else
						{
							if (!f.isAbsolute())
							{
								File sourcefile = new File(this.parser.getSourceFilename());
								f = new File(sourcefile.getParentFile(), f.getName());
							}
							in = EncodingUtil.createBufferedReader(f, encoding);

							// Apache Derby needs the exact length in characters
							// which might not be the file size if a multi-byte encoding is used
							if (checkRealClobLength)
							{
								size = (int) FileUtil.getCharacterLength(f, encoding);
							}
							else
							{
								size = (int) f.length();
							}
						}
					}
					catch (IOException ex)
					{
						hasErrors = true;
						String msg = ResourceMgr.getFormattedString("ErrFileNotAccessible", f.getAbsolutePath(), ex.getMessage());
						messages.append(msg);
						throw new SQLException(ex.getMessage());
					}
				}
				else
				{
					// this assumes that the JDBC driver will actually
					// implement the toString() for whatever object
					// it created when reading that column!
					in = null;
					pstmt.setObject(colIndex, row[i]);
				}

				if (in != null)
				{
					// For Oracle, this will only work with Oracle 10g drivers (and later)
					// Oracle 9i drivers do not implement the setCharacterStream()
					// and associated methods properly
					pstmt.setCharacterStream(colIndex, in, size);
				}
			}
			else if (SqlUtil.isBlobType(jdbcType) || "BLOB".equals(dbmsType))
			{
				InputStream in = null;
				int len = -1;
				if (row[i] instanceof File)
				{
					// When importing files created by SQL Workbench/J
					// blobs will be "passed" as File objects pointing to the external file
					ImportFileHandler handler = (this.parser != null ? parser.getFileHandler() : null);
					File f = (File)row[i];
					try
					{
						if (handler != null)
						{
							in = new BufferedInputStream(handler.getAttachedFileStream(f));
							len = (int)handler.getLength(f);
						}
						else
						{
							if (!f.isAbsolute())
							{
								File sourcefile = new File(this.parser.getSourceFilename());
								f = new File(sourcefile.getParentFile(), f.getName());
							}
							in = new BufferedInputStream(new FileInputStream(f), 64*1024);
							len = (int)f.length();
						}
					}
					catch (IOException ex)
					{
						hasErrors = true;
						String msg = ResourceMgr.getFormattedString("ErrFileNotAccessible", f.getAbsolutePath(), ex.getMessage());
						messages.append(msg);
						throw new SQLException(ex.getMessage());
					}
				}
				else if (row[i] instanceof Blob)
				{
					Blob b = (Blob)row[i];
					in = b.getBinaryStream();
					len = (int)b.length();
				}
				else if (row[i] instanceof byte[])
				{
					byte[] buffer = (byte[])row[i];
					in = new ByteArrayInputStream(buffer);
					len = buffer.length;
				}

				if (in != null && len > -1)
				{
					pstmt.setBinaryStream(colIndex, in, len);
				}
				else
				{
					pstmt.setNull(colIndex, Types.BLOB);
					this.messages.append(ResourceMgr.getFormattedString("MsgBlobNotRead", Integer.valueOf(i+1)));
					this.messages.appendNewLine();
				}
			}
			else
			{
				if (isOracle &&	jdbcType == java.sql.Types.DATE && row[i] instanceof java.sql.Date)
				{
					java.sql.Timestamp ts = new java.sql.Timestamp(((java.sql.Date)row[i]).getTime());
					pstmt.setTimestamp(colIndex, ts);
				}
				else if (useSetObjectWithType)
				{
					pstmt.setObject(colIndex, row[i], jdbcType);
				}
				else
				{
					pstmt.setObject(colIndex, row[i]);
				}
			}
		}

		if (columnConstants != null && pstmt == this.insertStatement)
		{
			int count = this.columnConstants.getColumnCount();
			int constIndex = row.length + 1;
			for (int i=0; i < count; i++)
			{
				if (columnConstants.isSelectStatement(i))
				{
					ValueStatement stmt = columnConstants.getStatement(i);

					Map<Integer, Object> values = source.getInputColumnValues(stmt.getInputColumnIndexes());
					Object data = stmt.getDatabaseValue(dbConn, values);
					pstmt.getStatement().setObject(constIndex, data);
					constIndex ++;
				}
				else if (!columnConstants.isFunctionCall(i))
				{
					columnConstants.setParameter(pstmt.getStatement(), constIndex, i);
					constIndex ++;
				}
			}
		}

		long rows = pstmt.executeUpdate();

		return rows;
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

	@Override
	public void tableImportFinished()
		throws SQLException
	{
		// be prepared to import more then one table...
		if (this.targetTable != null)
		{
			try
			{
				finishTable();
				targetTable = null;
			}
			catch (SQLException e)
			{
				if (!this.continueOnError)
				{
					targetTable = null;
					hasErrors = true;
					throw e;
				}
			}
		}
	}

	public void skipTargetCheck(boolean flag)
	{
		verifyTargetTable = !flag;
	}

	/**
	 *	Callback function from the RowDataProducer
	 */
	@Override
	public void setTargetTable(TableIdentifier table, List<ColumnIdentifier> columns)
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
		this.updatedRows = 0;
		this.insertedRows = 0;

		this.errorCount = 0;
		this.errorLimitAdded = false;


		try
		{
			this.targetTable = table.createCopy();
			this.targetColumns = new ArrayList<>(columns);

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
					String msg = ResourceMgr.getFormattedString("ErrImportTableNotCreated", this.targetTable.getTableExpression(this.dbConn), ExceptionUtil.getDisplay(e));
					this.messages.append(msg);
					this.messages.appendNewLine();
					LogMgr.logError("DataImporter.setTargetTable()", "Could not create target: " + this.targetTable, e);
					this.hasErrors = true;
					throw e;
				}
			}

			if (verifyTargetTable)
			{
				try
				{
					this.checkTable();
				}
				catch (SQLException e)
				{
					this.messages.append(ResourceMgr.getFormattedString("ErrTargetTableNotFound", this.targetTable.getTableExpression()));
					if (parser != null)
					{
						this.messages.appendNewLine();
						this.messages.append(ResourceMgr.getFormattedString("ErrImportFileNotProcessed", this.parser.getSourceFilename()));
					}
					this.hasErrors = true;
					this.messages.appendNewLine();
					this.targetTable = null;
					throw e;
				}
			}

			checkConstantValues();

			this.currentImportRow = 0;
			this.totalRows = 0;

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
			runPreTableStatement();
		}
		catch (Exception th)
		{
			String tname = targetTable == null ? "null" : this.targetTable.getTableExpression();
			String msg = "Error initializing import for table " + tname;
			if (this.continueOnError)
			{
				this.hasWarnings = true;
			}
			else
			{
				this.hasErrors = true;
			}
			LogMgr.logError("DataImporter.setTargetTable()", msg, th);
			throw th;
		}
	}

	private void runPreTableStatement()
		throws SQLException
	{
		try
		{
			if (this.tableStatements != null)
			{
				this.tableStatements.runPreTableStatement(dbConn, targetTable);
			}
		}
		catch (TableStatementError tse)
		{
			String err = ResourceMgr.getFormattedString("ErrTableStmt", tse.getTable(), tse.getMessage());
			messages.append(err);
			messages.appendNewLine();
			throw tse;
		}
	}

	private void runPostTableStatement()
		throws SQLException
	{
		try
		{
			if (this.tableStatements != null && this.tableStatements.wasSuccess())
			{
				this.tableStatements.runPostTableStatement(dbConn, targetTable);
			}
		}
		catch (TableStatementError tse)
		{
			String err = ResourceMgr.getFormattedString("ErrTableStmt", tse.getTable(), tse.getMessage());
			messages.append(err);
			messages.appendNewLine();
			throw tse;
		}
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

		String sql = (insertSqlStart != null ? insertSqlStart : dbConn.getDbSettings().getInsertForImport());
		if (StringUtil.isNonBlank(sql))
		{
			text.append(sql);
			text.append(' ');
		}
		else
		{
			text.append("INSERT INTO ");
		}
		text.append(targetTable.getFullyQualifiedName(this.dbConn));
		text.append(" (");

		DbMetadata meta = dbConn.getMetadata();

		int colIndex = 0;
		for (int i=0; i < this.colCount; i++)
		{
			ColumnIdentifier col = this.targetColumns.get(i);
			if (ignoreColumn(col)) continue;
			if (colIndex > 0)
			{
				text.append(',');
				parms.append(',');
			}

			String colname = col.getDisplayName();
			if (!verifyTargetTable)
			{
				// if the target table was not verified, we need to make
				// sure the default case for column names is used
				colname = meta.adjustObjectnameCase(meta.removeQuotes(colname));
			}
			colname = meta.quoteObjectname(colname);
			text.append(colname);

			parms.append(getDmlExpression(col));
			colIndex ++;
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
			PreparedStatement stmt = this.dbConn.getSqlConnection().prepareStatement(insertSql);
			int batch = (this.isModeInsert() ? batchSize : 1);
			this.insertStatement = new BatchedStatement(stmt, dbConn, batch);
			this.insertStatement.setCommitBatch(this.commitBatch);
			LogMgr.logInfo("DataImporter.prepareInsertStatement()", "Statement for insert: " + insertSql);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.prepareInsertStatement()", "Error when preparing INSERT statement: " + insertSql, e);
			this.messages.append(ResourceMgr.getString("ErrImportInitTargetFailed"));
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.insertStatement = null;
			this.hasErrors = true;
			throw e;
		}
	}

	private String getDmlExpression(ColumnIdentifier column)
	{
		return builder.getDmlExpression(column);
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
				if (messages.getLength() > 0) this.messages.appendNewLine();
				this.messages.append(ResourceMgr.getFormattedString("ErrImportNoKeyForUpdate", this.targetTable.getTableExpression()));
				throw new SQLException("No key columns defined for update mode");
			}
		}

		DbMetadata meta = dbConn.getMetadata();

		this.columnMap = new int[this.colCount];
		int pkIndex = this.colCount - this.keyColumns.size();
		int pkCount = 0;
		int colIndex = 0;
		StringBuilder sql = new StringBuilder(this.colCount * 20 + 80);
		StringBuilder where = new StringBuilder(this.keyColumns.size() * 10);
		sql.append("UPDATE ");
		sql.append(this.targetTable.getFullyQualifiedName(this.dbConn));
		sql.append(" SET ");
		where.append(" WHERE ");
		boolean pkAdded = false;
		for (int i=0; i < this.colCount; i++)
		{
			ColumnIdentifier col = this.targetColumns.get(i);
			if (ignoreColumn(col)) continue;
			String colname = col.getDisplayName();
			if (!verifyTargetTable)
			{
				// if the target table was not verified, we need to make
				// sure the default case for column names is used
				colname = meta.adjustObjectnameCase(meta.removeQuotes(colname));
			}
			colname = meta.quoteObjectname(colname);
			if (keyColumns.contains(col))
			{
				this.columnMap[i] = pkIndex;
				if (pkAdded) where.append(" AND ");
				else pkAdded = true;
				where.append(colname);
				where.append(" = ");
				where.append(getDmlExpression(col));
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
				sql.append(colname);
				sql.append(" = ");
				sql.append(getDmlExpression(col));
				colIndex ++;
			}
		}

		if (!pkAdded)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "No primary key columns defined! Update mode not available\n", null);
			this.messages.append(ResourceMgr.getString("ErrImportNoKeyForUpdate"));
			this.messages.appendNewLine();
			this.updateStatement = null;
			this.hasErrors = true;
			throw new SQLException("No key columns defined for update mode");
		}

		if (pkCount != this.keyColumns.size())
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "At least one of the supplied primary key columns was not found in the target table!", null);
			this.messages.append(ResourceMgr.getString("ErrImportUpdateKeyColumnNotFound") + "\n");
			this.updateStatement = null;
			this.hasErrors = true;
			throw new SQLException("Not enough key columns defined for update mode");
		}

		if (colIndex == 0)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "Only PK columns defined! Update mode is not available!", null);
			this.messages.append(ResourceMgr.getString("ErrImportOnlyKeyColumnsForUpdate"));
			this.messages.appendNewLine();
			this.updateStatement = null;
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

		if (StringUtil.isNonEmpty(this.whereClauseForUpdate))
		{
			boolean addBracket = false;
			String whereClause = this.whereClauseForUpdate.trim().toUpperCase();
			if (!whereClause.startsWith("AND") && !whereClause.startsWith("OR"))
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
			PreparedStatement stmt = this.dbConn.getSqlConnection().prepareStatement(updateSql);
			int batch = (this.isModeUpdate() ? batchSize : 1);
			this.updateStatement = new BatchedStatement(stmt, dbConn, batch);
			this.updateStatement.setCommitBatch(this.commitBatch);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "Error when preparing UPDATE statement", e);
			this.messages.append(ResourceMgr.getString("ErrImportInitTargetFailed"));
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.updateStatement = null;
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
			this.keyColumns = new LinkedList<>();
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

		try
		{
			if (this.useBatch)
			{
				if (this.insertStatement != null)
				{
					this.insertedRows += insertStatement.flush();
				}

				if (this.updateStatement != null)
				{
					this.updatedRows += updateStatement.flush();
				}

				// If the batch is executed and committed, there is no
				// need to send another commit. In fact some DBMS don't like
				// a commit or rollback if no transaction was started.
				if (commitBatch) commitNeeded = false;
			}

			closeStatements();

			runPostTableStatement();

			String msg = this.targetTable.getTableName() + ": " + this.getInsertedRows() + " row(s) inserted. " + this.getUpdatedRows() + " row(s) updated.";
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
				 this.dbConn.commit();
			}

			if (adjustSequences)
			{
				SequenceAdjuster adjuster = SequenceAdjuster.Factory.getSequenceAdjuster(dbConn);
				if (adjuster != null)
				{
					int numSequences = adjuster.adjustTableSequences(dbConn, targetTable, transactionControl);
					LogMgr.logInfo("DataImporter.finishTable()", "Adjusted " + numSequences + " sequence(s) for table " + targetTable.getTableExpression(dbConn));
				}
			}

			LogMgr.logInfo("DataImporter.finishTable()", msg);

			MessageBuffer msgBuffer = this.source.getMessages();
			if (msgBuffer != null)
			{
				messages.append(msgBuffer);
				msgBuffer.clear();
			}

			if (this.insertedRows > -1)
			{
				this.messages.append(this.insertedRows + " " + ResourceMgr.getString("MsgCopyNumRowsInserted"));
				this.messages.appendNewLine();
			}
			if (this.updatedRows > -1)
			{
				this.messages.append(this.updatedRows + " " + ResourceMgr.getString("MsgCopyNumRowsUpdated"));
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
				this.dbConn.rollbackSilently();
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

	public void clearMessages()
	{
		this.messages.clear();
	}

	/**
	 *	Callback from the RowDataProducer
	 */
	@Override
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

		this.hasErrors = this.hasErrors || this.source.hasErrors();
		this.hasWarnings = this.hasWarnings || this.source.hasWarnings();
	}

	private void cleanupRollback()
	{
		try
		{
			cancelStatement(insertStatement);
			cancelStatement(updateStatement);
			closeStatements();

			if (this.transactionControl && !this.dbConn.getAutoCommit())
			{
				LogMgr.logInfo("DataImporter.cleanupRollback()", "Rollback changes");
				this.dbConn.rollback();
				this.updatedRows = 0;
				this.insertedRows = 0;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.cleanupRollback()", "Error on rollback", e);
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.hasErrors = true;
		}
		this.isRunning = false;
		if (this.progressMonitor != null) this.progressMonitor.jobFinished();
	}

	@Override
	public void tableImportError()
	{
		cleanupRollback();
		if (this.tableStatements != null && this.tableStatements.getRunPostStatementAfterError() && targetTable != null)
		{
			try
			{
				this.tableStatements.runPostTableStatement(dbConn, targetTable);
			}
			catch (TableStatementError tse)
			{
				LogMgr.logWarning("DataImporter.tableImportError()", "Error running post-table statement", tse);
				String err = ResourceMgr.getFormattedString("ErrTableStmt", tse.getTable(), tse.getMessage());
				messages.append(err);
				messages.appendNewLine();
			}
			catch (Exception e)
			{
				LogMgr.logWarning("DataImporter.tableImportError()", "Error running post-table statement", e);
			}
		}
	}

	@Override
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

	private void closeStatements()
	{
		if (this.insertStatement != null)
		{
			try { this.insertStatement.close();	} catch (Throwable th) {}
		}
		if (this.updateStatement != null)
		{
			try { this.updateStatement.close();	} catch (Throwable th) {}
		}
		if (columnConstants != null)
		{
			columnConstants.done();
		}
	}

	public int getReportInterval()
	{
		return this.reportInterval;
	}

	@Override
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

}
