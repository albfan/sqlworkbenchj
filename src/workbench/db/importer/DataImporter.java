/*
 * DataImporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableCreator;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import java.io.StringReader;
import java.io.Reader;
import java.sql.Clob;
import workbench.interfaces.ImportFileParser;
import workbench.storage.NullValue;
import workbench.util.WbThread;


/**
 * Import data that is provided from {@link RowDataProducer} into
 * a table in the database.
 *
 * @author  support@sql-workbench.net
 */
public class DataImporter
	implements Interruptable, RowDataReceiver
{
	public static final int MODE_INSERT = 0;
	public static final int MODE_UPDATE = 1;
	public static final int MODE_INSERT_UPDATE = 2;
	public static final int MODE_UPDATE_INSERT = 3;

	private WbConnection dbConn;
	private String insertSql;
	private String updateSql;

	private RowDataProducer source;
	private PreparedStatement insertStatement;
	private PreparedStatement updateStatement;

	private TableIdentifier targetTable = null;

	private int commitEvery = 0;

	private boolean deleteTarget = false;
	private boolean createTarget = false;
	private boolean continueOnError = true;
	private boolean useLongTags = true;

	private long totalRows = 0;
	private long updatedRows = 0;
	private long insertedRows = 0;
	private int currentImportRow = 0;
	private int mode = MODE_INSERT;
	private boolean useBatch = false;
	private boolean supportsBatch = false;
	private boolean canCommitInBatch = true;
	private boolean hasErrors = false;
	private boolean hasWarnings = false;
	private int reportInterval = 1;
	private StrBuffer messages;
	private String targetSchema;

	private int colCount;
	private boolean useTruncate = false;
	private int totalTables = -1;
	private int currentTable = -1;

	// this array will map the columns for updating the target table
	// the index into this array will be the index
	// from the row data array supplied by the producer.
	// (which should be the same order as the columns in targetColumns)
	// the value of that index position is the index
	// for the setXXX() method for the prepared statement
	// to update the table
	private int[] columnMap = null;

	private ColumnIdentifier[] targetColumns;
	private List keyColumns;

	private RowActionMonitor progressMonitor;
	private boolean isRunning = false;
	private int batchSize = -1;
	private ImportFileParser parser;

	// Additional WHERE clause for UPDATE statements
	private String whereClauseForUpdate;

	public DataImporter()
	{
		this.messages = new StrBuffer(1000);
	}

	public void setConnection(WbConnection aConn)
	{
		this.dbConn = aConn;
		this.supportsBatch = this.dbConn.getMetadata().supportsBatchUpdates();
		this.useBatch = this.useBatch && supportsBatch;
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

	public void setCommitEvery(int aCount) { this.commitEvery = aCount; }
	public int getCommitEvery() { return this.commitEvery; }

	public boolean getContinueOnError() { return this.continueOnError; }
	public void setContinueOnError(boolean flag) { this.continueOnError = flag; }

	public boolean getDeleteTarget() { return deleteTarget; }
	public void setBatchSize(int size) { this.batchSize = size; }

	
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

	/**
	 * Controls creation of target table for imports where the 
	 * producer can retrieve a full table definition (i.e. XML files
	 * created with SQL Workbench
	 * @see #createTable()
	 * @see #setTargetTable(String, workbench.db.ColumnIdentifier[])
	 */
	public void setCreateTarget(boolean flag) { this.createTarget = flag; }
	
	/**
	 *	Controls deletion of the target table.
	 */
	public void setDeleteTarget(boolean deleteTarget)
	{
		this.deleteTarget = deleteTarget;
	}


	/**
	 * 	Use batch updates if the driver supports this
	 */
	public void setUseBatch(boolean flag)
	{
		if (this.isModeInsertUpdate() || this.isModeUpdateInsert()) return;

		if (flag && !this.supportsBatch)
		{
			LogMgr.logWarning("DataImporter.setUseBatch()", "JDBC driver does not support batch updates. Ignoring request to use batch updates");
			this.messages.append(ResourceMgr.getString("MsgJDBCDriverNoBatch") + "\n");
		}

		if (this.dbConn != null)
		{
			this.useBatch = flag && this.supportsBatch;
		}
		else
		{
			// we cannot yet decide if the driver supports batch updates.
			// this will be checked if the connection is set
			this.useBatch = flag;
		}
	}

	public boolean getUseLongTags()
	{
		return useLongTags;
	}

	public void setUseLongTags(boolean flag)
	{
		this.useLongTags = flag;
	}

	public boolean getUseBatch()
	{
		 return this.useBatch;
	}
	public void setModeInsert() { this.mode = MODE_INSERT; }
	public void setModeUpdate() { this.mode = MODE_UPDATE; }

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

	public static int estimateReportIntervalFromFileSize(String filename)
	{
		try
		{
			long records = FileUtil.estimateRecords(filename, 10);
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
	 *	Define the mode by supplying keywords.
	 *	The following combinations are valid:
	 *	If a valid mode definition is passed, true is returned.
	 *	Valid mode definitions are:
	 *	<ul>
	 *	<li>insert</li>
	 *	<li>update</li>
	 *	<li>insert,update</li>
	 *	<li>update,insert</li>
	 *  </ul>
	 *	The mode string is not case sensitive (INSERT is the same as insert)
	 *	@return true if the passed string is valid, false otherwise
	 */
	public boolean setMode(String mode)
	{
		if (mode == null) return true;
		mode = mode.trim().toLowerCase();
		if (mode.indexOf(',') == -1)
		{
			// only one keyword supplied
			if ("insert".equals(mode))
			{
				this.setModeInsert();
			}
			else if ("update".equals(mode))
			{
				this.setModeUpdate();
			}
			else
			{
				return false;
			}
		}
		else
		{
			List l = StringUtil.stringToList(mode, ",");
			String first = (String)l.get(0);
			String second = (String)l.get(1);
			if ("insert".equals(first) && "update".equals(second))
			{
				this.setModeInsertUpdate();
			}
			else if ("update".equals(first) && "insert".equals(second))
			{
				this.setModeUpdateInsert();
			}
			else
			{
				return false;
			}
		}
		return true;
	}

	/**
	 *	Define the key columns by supplying a comma separated
	 *	list of column names
	 */
	public void setKeyColumns(String aColumnList)
	{
		List cols = StringUtil.stringToList(aColumnList, ",");
		int count = cols.size();
		ArrayList keys = new ArrayList(count);
		for (int i=0; i < count; i++)
		{
			ColumnIdentifier col = new ColumnIdentifier((String)cols.get(i));
			keys.add(col);
		}
		this.setKeyColumns(keys);
	}

	/**
	 * 	Set the key columns for the target table to be used
	 * 	for update mode.
	 * 	The list has to contain objects of type {@link workbench.db.ColumnIdentifier}
	 */
	public void setKeyColumns(List cols)
	{
		this.keyColumns = cols;
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

		if (this.useBatch && (this.isModeInsertUpdate() || this.isModeUpdateInsert()))
		{
			this.useBatch = false;
		}
		this.source.start();
	}

	/**
	 *	Deletes the target table by issuing a DELETE FROM ...
	 */
	private void deleteTarget()
		throws SQLException
	{
		if (this.targetTable == null) return;
		String deleteSql = null;

		if (this.useTruncate)
		{
			deleteSql = "TRUNCATE TABLE " + this.targetTable.getTableExpression(this.dbConn);
		}
		else
		{
			deleteSql = "DELETE FROM " + this.targetTable.getTableExpression(this.dbConn);
		}
		Statement stmt = this.dbConn.createStatement();
		LogMgr.logDebug("DataImporter.deleteTarget()", "Executing: [" + deleteSql + "] to delete target table...");
		int rows = stmt.executeUpdate(deleteSql);
		if (this.useTruncate)
		{
			String msg = ResourceMgr.getString("MsgImportTableTruncated").replaceAll("%table%", this.targetTable.getTableExpression(this.dbConn));
			this.messages.append(msg);
			this.messages.append('\n');
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
		this.messages.append(msg + "\n");
	}

	public void setUseTruncate(boolean flag)
	{
		this.useTruncate = flag;
	}
	public boolean isRunning() { return this.isRunning; }
	public boolean isSuccess() { return !hasErrors; }
	public boolean hasWarnings() { return this.hasWarnings; }
	public long getAffectedRows() { return this.totalRows; }

	public long getInsertedRows() { return this.insertedRows; }
	public long getUpdatedRows() { return this.updatedRows; }

	/**
	 *	This method is called if cancelExecution() is called
	 *	to check if the user should confirm the cancelling of the import
	 */
	public boolean confirmCancel()
	{
		return true;
	}

	public void cancelExecution()
	{
		this.isRunning = false;
		this.source.cancel();
		this.messages.append(ResourceMgr.getString("MsgImportCancelled") + "\n");
		//if (this.progressMonitor != null) this.progressMonitor.jobFinished();
	}

	public void setTableCount(int total)
	{
		this.totalTables = total;
	}

	public void setCurrentTable(int current)
	{
		this.currentTable = current;
	}

	/**
	 *	Callback function for RowDataProducer. The order in the data array
	 * 	has to be the same as initially passed in the setTargetTable() method.
	 */
	public void processRow(Object[] row)
		throws SQLException
	{
		if (row == null) return;
		if (row.length != this.colCount) return;

		currentImportRow++;
		if (this.progressMonitor != null && this.reportInterval > 0 && (currentImportRow == 1 || currentImportRow % reportInterval == 0))
		{
			if (this.totalTables > 0)
			{
				StringBuffer msg = new StringBuffer(this.targetTable.getTableName().length() + 20);
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

		int rows = 0;
		try
		{
			switch (this.mode)
			{
				case MODE_INSERT:
					rows = this.insertRow(row);
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
						rows = this.insertRow(row);
						inserted = true;
					}
					catch (Exception e)
					{
						LogMgr.logDebug("DataImporter.processRow()", "Error inserting row, trying update");
						inserted = false;
					}
					if (!inserted)
					{
						rows = this.updateRow(row);
					}
					break;

				case MODE_UPDATE_INSERT:
					// an exception is not expected when updating the row
					// if the row does not exist, the update counter should be
					// zero. If the update violates any constraints, then the
					// INSERT will fail as well, so any exception thrown, indicates
					// an error with this row, so we will not proceed with the insert
					rows = this.updateRow(row);
					if (rows <= 0)
						rows = this.insertRow(row);
					break;

				case MODE_UPDATE:
					rows = this.updateRow(row);
					break;
			}
			this.totalRows += rows;
		}
		catch (SQLException e)
		{
			this.hasErrors = true;
			LogMgr.logError("DataImporter.processRow()", "Error importing row " + this.totalRows + ": " + e.getMessage(), null);
			this.messages.append(ResourceMgr.getString("ErrImportingRow") + " " + currentImportRow + "\n");
			this.messages.append(ResourceMgr.getString("ErrImportErrorMsg") + " " + e.getMessage() + "\n");
			this.messages.append(ResourceMgr.getString("ErrImportValues") + " " + this.getValueDisplay(row) + "\n");
			this.messages.append("\n");
			if (!this.continueOnError) throw e;
		}

		if (this.useBatch && this.batchSize > 0 && ((this.totalRows % this.batchSize) == 0))
		{
			try
			{
				this.executeBatch();
			}
			catch (SQLException e)
			{
				this.hasErrors = true;
				LogMgr.logError("DataImporter.processRow()", "Error executing batch after " + this.totalRows + " rows", e);
				this.messages.append(ResourceMgr.getString("ErrImportExecuteBatchQueue") + "\n");
				this.messages.append("\n");
				if (!this.continueOnError) throw e;
			}
		}

		if (this.commitEvery > 0 && ((this.totalRows % this.commitEvery) == 0) && !this.dbConn.getAutoCommit())
		{
			try
			{
				if (this.useBatch)
				{
					// Oracle seems to have a problem with adding another SQL statement
					// to the batch of a prepared Statement (works fine with PostgreSQL)
					if (this.canCommitInBatch)
					{
						PreparedStatement stmt = null;
						if (this.isModeInsert())
						{
							stmt = this.insertStatement;
						}
						else if (this.isModeUpdate())
						{
							stmt = this.updateStatement;
						}

						try
						{
							if (stmt != null) stmt.addBatch("COMMIT");
						}
						catch (Exception e)
						{
							LogMgr.logWarning("DataImporter.processRow()", "Error when adding COMMIT to batch. This does not seem to be supported by the server: " + ExceptionUtil.getDisplay(e));
							String msg = ResourceMgr.getString("ErrCommitInBatch").replaceAll("%error%", e.getMessage()) + "\n";
							this.messages.append(msg);
							this.canCommitInBatch = false;
						}
					}
				}
				else
				{
					this.dbConn.commit();
				}
			}
			catch (SQLException e)
			{
				String error = ExceptionUtil.getDisplay(e);
				this.messages.append(error + "\n");
				this.hasErrors = true;
				if (!continueOnError) throw e;
			}
		}
	}

	private String getValueDisplay(Object[] row)
	{
		int count = row.length;
		StringBuffer values = new StringBuffer(count * 20);
		values.append("[");

		for (int i=0; i < count; i++)
		{
			if (i > 0) values.append(",");
			if (row[i] == null)
			{
				values.append("NULL");
			}
			else
			{
				values.append(row[i].toString());
			}
		}
		values.append("]");
		return values.toString();
	}

	/**
	 *	Insert a row of data into the target table.
	 *	This method relies on insertStatement correctly initialized with
	 *	all parameters at the correct location.
	 */
	private int insertRow(Object[] row)
		throws SQLException
	{
		int rows = 0;
		if (this.useBatch)
		{
			rows = processRowData(this.insertStatement, row, true, false);
		}
		else
		{
			rows = processRowData(this.insertStatement, row, false, false);
			this.insertedRows += rows;
		}
		return rows;
	}

	/**
	 *	Update the data in the target table using the PreparedStatement
	 *	available in updateStatement
	 */
	private int updateRow(Object[] row)
		throws SQLException
	{
		int rows = 0;
		if (this.useBatch && this.isModeUpdate())
		{
			rows = processRowData(this.updateStatement, row, true, true);
		}
		else
		{
			rows = processRowData(this.updateStatement, row, false, true);
			this.updatedRows += rows;
		}
		return rows;
	}

	private int processRowData(PreparedStatement pstmt, Object[] row, boolean addBatch, boolean useColMap)
		throws SQLException
	{
		for (int i=0; i < row.length; i++)
		{
			int colIndex = i  + 1;
			if (useColMap)
			{
				colIndex = this.columnMap[i] + 1;
			}
			
			if (row[i] == null || row[i] instanceof NullValue)
			{
				pstmt.setNull(colIndex, this.targetColumns[i].getDataType());
			}
			// This will only work with Oracle 10g drivers.
			// Oracle 9i drivers do not implement the setCharacterStream() 
			// and associated methods properly
			else if ( SqlUtil.isClobType(this.targetColumns[i].getDataType()) || 
				       "LONG".equals(this.targetColumns[i].getDbmsType()) ||
				       "CLOB".equals(this.targetColumns[i].getDbmsType()) )
			{
				String value = null;
				if (row[i] instanceof Clob)
				{
					try
					{
						Clob clob = (Clob)row[i];
						int len = (int)clob.length();
						value = clob.getSubString(1, len);
					}
					catch (Throwable e)
					{
						value = null;
					}
				}
				else
				{
					// this assumes that the JDBC driver will actually
					// implement the toString() for whatever object 
					// it created when reading that column!
					value = row[i].toString();
				}
				int size = value.length();
				Reader in = new StringReader(value);
				pstmt.setCharacterStream(colIndex, in, size);
			}
			else if (SqlUtil.isBlobType(this.targetColumns[i].getDataType()) || 
				       "BLOB".equals(this.targetColumns[i].getDbmsType()))
			{
				InputStream in = null;
				int len = -1;
				if (row[i] instanceof File)
				{
					// When importing files created by SQL Workbench/J
					// blobs will be "passed" as File objects pointing to the external file 
					ImportFileHandler handler = (this.parser != null ? parser.getFileHandler() : null);
					InputStream dataStream = null;
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
								File source = new File(this.parser.getSourceFilename());
								f = new File(source.getParentFile(), f.getName());
							}
							else
							{
								in = new BufferedInputStream(new FileInputStream(f), 64*1024);
							}
							len = (int)f.length();
						}
					}
					catch (IOException ex)
					{
						throw new SQLException("Could not read data file " + f.getAbsolutePath() + " not found");
					}
				}
				else if (row[i] instanceof Blob)
				{
					Blob b = (Blob)row[i];
					in = b.getBinaryStream();
					len = (int)b.length();
				}
				
				if (in != null && len > -1)
				{
					pstmt.setBinaryStream(colIndex, in, len);
				}
				else
				{
					this.messages.append(ResourceMgr.getString("MsgBlobNotRead") + " " + (i+1) +"\n");
				}
			}
			else
			{
				if (this.dbConn.getMetadata().isOracle() &&
					  this.targetColumns[i].getDataType() == java.sql.Types.DATE &&
						row[i] instanceof java.sql.Date)
				{
					java.sql.Timestamp ts = new java.sql.Timestamp(((java.sql.Date)row[i]).getTime());
					pstmt.setTimestamp(colIndex, ts);
				}
				else
				{
					pstmt.setObject(colIndex, row[i]);
				}
			}
		}

		int rows = 0;
		if (addBatch)
		{
			pstmt.addBatch();
			// let's assume the batch statement affects at least one row
			// if this is not done, the rowcount will never be increased
			// in batchmode and thus each row will be committed even if 
			// a different commit frequency is selected.
			// Thanks to Pascal for pointing this out!
			rows = 1;
		}
		else
		{
			rows = pstmt.executeUpdate();
		}
		return rows;
	}

	/**
	 *	Callback function from the RowDataProducer
	 */
	public void setTargetTable(String tableName, ColumnIdentifier[] columns)
		throws SQLException
	{
		// be prepared to import more then one table...
		if (this.isRunning && this.targetTable != null)
		{
			try
			{
				this.finishTable();
				this.totalRows = 0;
				this.currentImportRow = 0;
				this.updatedRows = 0;
				this.insertedRows = 0;
			}
			catch (SQLException e)
			{
				this.totalRows = -1;
				this.currentImportRow = -1;
				this.updatedRows = -1;
				this.insertedRows = -1;
				if (!this.continueOnError) throw e;
			}
		}
		if (this.parser != null)
		{
			this.messages.append(ResourceMgr.getString("MsgImportingFile"));
			this.messages.append(' ');
			this.messages.append(this.parser.getSourceFilename());
			this.messages.append('\n');
		}

		try
		{
			this.targetTable = new TableIdentifier(tableName);
			if (this.targetSchema != null)
			{
				targetTable.setSchema(this.targetSchema);
			}
			this.targetColumns = columns;
			this.colCount = this.targetColumns.length;

			if (this.createTarget)
			{
				try
				{
					this.createTarget();
				}
				catch (SQLException e)
				{
					LogMgr.logError("DataImporter.setTargetTable()", "Could not create target: " + this.targetTable, e);
					throw e;
				}
			}
			
			try
			{
				this.checkTable();
			}
			catch (SQLException e)
			{
				String msg = ResourceMgr.getString("ErrImportTableNotFound").replaceAll("%table%", this.targetTable.getTableExpression(this.dbConn));
				if (parser != null)
				{
					String s = ResourceMgr.getString("ErrImportFileNotProcessed");
					msg = msg + " " + StringUtil.replace(s, "%filename%", this.parser.getSourceFilename());
				}
				this.messages.append(msg + "\n");
				this.targetTable = null;
				throw e;
			}

			if (this.mode != MODE_UPDATE)
			{
				this.prepareInsertStatement();
			}
			if (this.mode != MODE_INSERT)
			{
				this.prepareUpdateStatement();
			}
			
			if (this.deleteTarget)
			{
				try
				{
					this.deleteTarget();
				}
				catch (Exception e)
				{
					LogMgr.logError("DataImporter.setTargetTable()", "Could not delete contents of table " + this.targetTable, e);
				}
			}
				
			this.currentImportRow = 0;
			this.totalRows = 0;

			if (this.reportInterval == 0 && this.progressMonitor != null)
			{
				this.progressMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
				this.progressMonitor.setCurrentObject(ResourceMgr.getString("MsgImportingTableData") + " " + this.targetTable + " (" + this.getModeString() + ")",-1,-1);
			}
			if (LogMgr.isInfoEnabled())
			{
				LogMgr.logInfo("DataImporter.setTargetTable()", "Starting import for table " + tableName);
			}
		}
		catch (RuntimeException th)
		{
			LogMgr.logError("DataImporter.setTargetTable()", "Error when setting target table " + tableName, th);
			throw th;
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
		StringBuffer text = new StringBuffer(this.targetColumns.length * 50);
		StringBuffer parms = new StringBuffer(targetColumns.length * 20);

		text.append("INSERT INTO ");
		text.append(targetTable.getTableExpression(this.dbConn));
		text.append(" (");
		for (int i=0; i < this.colCount; i++)
		{
			if (i > 0)
			{
				text.append(",");
				parms.append(",");
			}
			text.append(this.targetColumns[i].getColumnName());
			parms.append('?');
		}
		text.append(") VALUES (");
		text.append(parms);
		text.append(")");

		try
		{
			this.insertSql = text.toString();
			this.insertStatement = this.dbConn.getSqlConnection().prepareStatement(this.insertSql);
			if (LogMgr.isDebugEnabled()) LogMgr.logDebug("DataImporter.prepareInsertStatement()", "Statement for insert: " + this.insertSql);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.prepareInsertStatement()", "Error when preparing INSERT statement: " + this.insertSql, e);
			this.messages.append(ResourceMgr.getString("ErrImportInitTargetFailed"));
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.insertStatement = null;
			throw e;
		}
	}

	/**
	 * 	Prepare the statement to be used for updates
	 * 	targetTable and targetColumns have to be initialized before calling this!
	 */
	private void prepareUpdateStatement()
		throws SQLException
	{
		if (this.keyColumns == null)
		{
			this.retrieveKeyColumns();
			if (this.keyColumns == null)
			{
				this.messages.append(ResourceMgr.getString("ErrImportNoKeyForUpdate"));
				throw new SQLException("No key columns defined for update mode");
			}
		}

		this.columnMap = new int[this.colCount];
		int pkIndex = this.colCount - this.keyColumns.size();
		int pkCount = 0;
		int colIndex = 0;
		StringBuffer sql = new StringBuffer(this.colCount * 20 + 80);
		StringBuffer where = new StringBuffer(this.keyColumns.size() * 10);
		sql.append("UPDATE ");
		sql.append(this.targetTable.getTableExpression(this.dbConn));
		sql.append(" SET ");
		where.append(" WHERE ");
		boolean pkAdded = false;
		for (int i=0; i < this.colCount; i++)
		{
			ColumnIdentifier col = this.targetColumns[i];
			int index = this.keyColumns.indexOf(col);
			if (index < 0)
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
			else
			{
				this.columnMap[i] = pkIndex;
				if (pkAdded) where.append(" AND ");
				else pkAdded = true;
				where.append(col.getColumnName());
				where.append(" = ?");
				pkIndex ++;
				pkCount ++;
			}
		}
		if (!pkAdded)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "No primary key columns defined! Update mode not available\n", null);
			this.messages.append(ResourceMgr.getString("ErrImportNoKeyForUpdate") + "\n");
			this.updateSql = null;
			this.updateStatement = null;
			throw new SQLException("No key columns defined for update mode");
		}
		if (pkCount != this.keyColumns.size())
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "At least one of the supplied primary key columns was not found in the target table!\n", null);
			this.messages.append(ResourceMgr.getString("ErrImportUpdateKeyColumnNotFound") + "\n");
			this.updateSql = null;
			this.updateStatement = null;
			throw new SQLException("Not enough key columns defined for update mode");
		}
		
		if (colIndex == 0)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "Only PK columns defined! Update mode is not available!", null);
			this.messages.append(ResourceMgr.getString("ErrImportOnlyKeyColumnsForUpdate"));
			this.updateSql = null;
			this.updateStatement = null;
			throw new SQLException("Only key columns defined for update mode");
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

		try
		{
			this.updateSql = sql.toString();
			this.updateStatement = this.dbConn.getSqlConnection().prepareStatement(this.updateSql);
			if (LogMgr.isDebugEnabled()) LogMgr.logDebug("DataImporter.prepareUpdateStatement()", "Statement for update: " + this.updateSql);
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.prepareUpdateStatement()", "Error when preparing UPDATE statement", e);
			this.messages.append(ResourceMgr.getString("ErrImportInitTargetFailed"));
			this.messages.append(ExceptionUtil.getDisplay(e));
			this.updateStatement = null;
			throw e;
		}
		return;
	}

	/**
	 *	If the key columns have not been defined externally through {@link #setKeyColumns(List)}
	 *	this method  is used to retrieve the key columns for the target table
	 */
	private void retrieveKeyColumns()
	{
		try
		{
			ColumnIdentifier cols[] = this.dbConn.getMetadata().getColumnIdentifiers(this.targetTable);
			int count = cols.length;
			this.keyColumns = new ArrayList();
			for (int i=0; i < count; i++)
			{
				if (cols[i].isPkColumn())
				{
					this.keyColumns.add(cols[i]);
				}
			}
		}
		catch (SQLException e)
		{
			this.columnMap = null;
			this.keyColumns = null;
		}
	}

	private void executeBatch()
		throws SQLException
	{
		if (!this.useBatch) return;

		if (this.isModeInsert())
		{
			int rows[] = this.insertStatement.executeBatch();
			if (rows != null)
			{
				for (int i=0; i < rows.length; i++)
				{
					// Oracle does not seem to report the correct number
					// so, if we get a SUCCESS_NO_INFO status, we'll simply
					// assume that one row has been inserted
					if (rows[i] == Statement.SUCCESS_NO_INFO)	this.insertedRows ++;
					else if (rows[i] >= 0) this.insertedRows += rows[i];
				}
			}
			this.insertStatement.clearBatch();
		}
		else if (this.isModeUpdate())
		{
			int rows[] = this.updateStatement.executeBatch();
			if (rows != null)
			{
				for (int i=0; i < rows.length; i++)
				{
					if (rows[i] == Statement.SUCCESS_NO_INFO) this.updatedRows ++;
					else if (rows[i] >= 0) this.updatedRows += rows[i];
				}
			}
			this.updateStatement.clearBatch();
		}
	}

	private void finishTable()
		throws SQLException
	{
		try
		{
			if (this.useBatch)
			{
				this.executeBatch();
			}
			this.closeStatements();
			if (!this.dbConn.getAutoCommit())
			{
				LogMgr.logDebug("DataImporter.finishTable()", this.getAffectedRows() + " row(s) imported. Committing changes");
				this.dbConn.commit();
			}
			this.messages.append(this.source.getMessages());
			if (this.insertedRows > -1)
			{
				this.messages.append(this.insertedRows + " " + ResourceMgr.getString("MsgCopyNumRowsInserted"));
				this.messages.append('\n');
			}
			if (this.updatedRows > -1)
			{
				this.messages.append(this.updatedRows + " " + ResourceMgr.getString("MsgCopyNumRowsUpdated"));
				this.messages.append("\n");
			}
			this.messages.append('\n');
		}
		catch (SQLException e)
		{
			if (!this.dbConn.getAutoCommit())
			{
				try { this.dbConn.rollback(); } catch (Throwable ignore) {}
			}
			LogMgr.logError("DataImporter.finishTable()", "Error commiting changes", e);
			this.messages.append(ExceptionUtil.getDisplay(e) + "\n");
			throw e;
		}
	}

	public String getMessages()
	{
		return this.messages.toString();
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
			LogMgr.logError("DataImporter.importFinished()", "Error commiting changes", e);
			this.messages.append(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			this.isRunning = false;
			if (this.progressMonitor != null) this.progressMonitor.jobFinished();
		}
	}

	public void tableImportError()
	{
		try
		{
			this.closeStatements();
			if (!this.dbConn.getAutoCommit())
			{
				LogMgr.logDebug("DataImporter.importCancelled()", "Rollback changes");
				this.dbConn.rollback();
				this.updatedRows = 0;
				this.insertedRows = 0;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importCancelled()", "Error on rollback", e);
			this.messages.append(ExceptionUtil.getDisplay(e));
		}
		this.messages.append(this.source.getMessages());
		if (this.progressMonitor != null) this.progressMonitor.jobFinished();
	}

	public void importCancelled()
	{
		if (!isRunning) return;
		
		try
		{
			LogMgr.logDebug("DataImporter.importCancelled()", "Ending import...");
			this.closeStatements();
			if (!this.dbConn.getAutoCommit())
			{
				LogMgr.logDebug("DataImporter.importCancelled()", "Rollback changes");
				this.dbConn.rollback();
				this.updatedRows = 0;
				this.insertedRows = 0;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importCancelled()", "Error on rollback", e);
			this.messages.append(ExceptionUtil.getDisplay(e));
		}
		finally
		{
			this.isRunning = false;
		}
		this.messages.append(this.source.getMessages());
		if (this.progressMonitor != null) this.progressMonitor.jobFinished();
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
