/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.db.importer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
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
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.db.compare.BatchedStatement;
import workbench.interfaces.ImportFileParser;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.MemoryWatcher;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class ImportWorker
	implements Runnable
{
	private int threadId;
	private BlockingQueue<ImportRow> dataQueue;
	
	private WbConnection dbConn;
	
	private BatchedStatement insertStatement;
	private BatchedStatement updateStatement;
	private MultiThreadedDataImporter controller;

	private boolean useSavepoint;
	private boolean continueOnError;
	
	private long updatedRows;
	private long insertedRows;
	private long totalRows;
	private long errorCount;

	private boolean hasErrors;
	private boolean useSetNull;
	
	private boolean cancel;
	private boolean regularStop;

	private Savepoint insertSavepoint;
	private Savepoint updateSavepoint;
	private long currentImportRow;
	private boolean checkRealClobLength;
	private boolean isOracle;

	// to be initialized
	private int commitEvery;
	private int[] columnMap;
	private int mode;
	private List<ColumnIdentifier> targetColumns;
	
	public ImportWorker(WbConnection base, int id)
		throws SQLException
	{
		threadId = id;
		try
		{
			if (id > 0)
			{
				ConnectionProfile prof = base.getProfile();

				dbConn = ConnectionMgr.getInstance().getConnection(prof, "ImportThread-" + threadId);
			}
			else
			{
				dbConn = base;
			}
			isOracle = dbConn.getMetadata().isOracle();
			checkRealClobLength = this.dbConn.getDbSettings().needsExactClobLength();

			useSavepoint = this.dbConn.getDbSettings().useSavepointForImport();
			useSavepoint = this.useSavepoint && !this.dbConn.getAutoCommit();
			if (useSavepoint && !this.dbConn.supportsSavepoints())
			{
				LogMgr.logWarning("ImportWorker.setConnection", "A savepoint should be used for each statement but the driver does not support savepoints!");
				this.useSavepoint = false;
			}

		}
		catch (ClassNotFoundException cnf)
		{
			// should not happen
		}
	}

	public synchronized void setController(MultiThreadedDataImporter imp)
	{
		controller = imp;
		mode = imp.getMode();
		commitEvery = imp.getCommitEvery();
		targetColumns = imp.getTargetColumns();
		columnMap = imp.getColumnMap();
		dataQueue = imp.getImportQueue();
		insertedRows = 0;
		updatedRows = 0;
		totalRows = 0;
	}
	
	public void setInsertSql(String sql)
		throws SQLException
	{
		PreparedStatement pstmt = this.dbConn.getSqlConnection().prepareStatement(sql);
		insertStatement = new BatchedStatement(pstmt, dbConn, controller.getBatchSize());
		insertStatement.setCommitBatch(controller.getCommitBatch());
	}

	public synchronized void setUpdateSql(String sql)
		throws SQLException
	{
		PreparedStatement pstmt = this.dbConn.getSqlConnection().prepareStatement(sql);
		updateStatement = new BatchedStatement(pstmt, dbConn, controller.getBatchSize());
		updateStatement.setCommitBatch(controller.getCommitBatch());
	}

	public String toString()
	{
		return "ImportWorker Thread " + threadId;
	}
	
	public synchronized void dispose()
	{
		if (threadId > 1)
		{
			dbConn.disconnect();
		}
	}
	
	public synchronized void flush()
		throws SQLException
	{
//		if (this.hasErrors || cancel) return;
		
		if (insertStatement != null)
		{
			insertedRows += insertStatement.flush();
		}
		if (updateStatement != null)
		{
			updatedRows += updateStatement.flush();
		}
	}

	public synchronized long getTotalRows()
	{
		return totalRows;
	}

	public synchronized long getInsertedRows()
	{
		return insertedRows;
	}

	public synchronized long getUpdatedRows()
	{
		return updatedRows;
	}
	
	public void cancel()
	{
		this.cancel = true;
	}
	
	public void run()
	{
		cancel = false;
		while (!cancel)
		{
			try
			{
				// take() will wait until a row becomes available
				ImportRow row = dataQueue.take();

				if (row != null && row.data != null)
				{
					// The DataImporter will put a special
					// ImportRow into the queue to signal the end of the import run.
					if (row.rowNumber == -42)
					{
						LogMgr.logDebug("ImportWorker.run()", "Thread " + threadId + ": received termination signal!");
						break;
					}

					try
					{
						currentImportRow = row.rowNumber;
						processRow(row.data);
						if (cancel) break;
					}
					catch (Exception e)
					{
						hasErrors = true;
						if (!continueOnError) 
						{
							LogMgr.logWarning("ImportWorker.run()", "Terminating thread " + threadId + " due to exception", e);
							controller.abort(row, e);
							break;
						}
					}
				}
			}
			catch (Exception ie)
			{
				if (!regularStop) LogMgr.logError("ImportWorker.run()", "Interrupted thread " + threadId + " while taking rows out of the queue", ie);
				break;
			}
		}
	}

	/**
	 * Used as a signal from the WorkerPool to flag
	 * the interrupt() call as "valid"
	 */
	public void stop()
	{
		regularStop = true;
	}
	public boolean isModeInsert()
	{
		return (this.mode == MultiThreadedDataImporter.MODE_INSERT);
	}

	public boolean isModeUpdate()
	{
		return (this.mode == MultiThreadedDataImporter.MODE_UPDATE);
	}

	public boolean isModeInsertUpdate()
	{
		return (this.mode == MultiThreadedDataImporter.MODE_INSERT_UPDATE);
	}

	public boolean isModeUpdateInsert()
	{
		return (this.mode == MultiThreadedDataImporter.MODE_UPDATE_INSERT);
	}

	public boolean hasErrors()
	{
		return hasErrors;
	}
	
	public void processRow(Object[] row)
		throws SQLException
	{
		long rows = 0;
		try
		{
			switch (this.mode)
			{
				case MultiThreadedDataImporter.MODE_INSERT:
					rows = this.insertRow(row, useSavepoint && continueOnError);
					break;

				case MultiThreadedDataImporter.MODE_INSERT_UPDATE:
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
					catch (Exception e)
					{
						inserted = false;
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

				case MultiThreadedDataImporter.MODE_UPDATE_INSERT:
					// an exception is not expected when updating the row
					// if the row does not exist, the update counter should be
					// zero. If the update violates any constraints, then the
					// INSERT will fail as well, so any exception thrown, indicates
					// an error with this row, so we will not proceed with the insert

					if (this.updateStatement == null)
					{
						try
						{
							rows = this.insertRow(row, useSavepoint && continueOnError);
						}
						catch (SQLException ignore)
						{
							// if UPDATE/INSERT was requested but the update statement
							// has been set to null, an update is not possible
							// so a failed insert should not be considered an error
						}
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

				case MultiThreadedDataImporter.MODE_UPDATE:
					rows = this.updateRow(row, useSavepoint && continueOnError);
					break;
			}
			totalRows += rows;
		}
		catch (OutOfMemoryError oome)
		{
			this.hasErrors = true;
			closeStatements();
			System.gc();
			controller.addMessage(ResourceMgr.getString("MsgOutOfMemoryGeneric"), true);
			if (controller.getBatchSize() > 0)
			{
				LogMgr.logError("ImportWorker.processRow()", "Not enough memory to hold statement batch! Use the -batchSize parameter to reduce the batch size!", null);
				controller.addMessage(ResourceMgr.getString("MsgOutOfMemoryJdbcBatch"), true);
			}
			else
			{
				LogMgr.logError("ImportWorker.processRow()", "Not enough memory to run this import!", null);
			}
			throw new SQLException("Not enough memory!");
		}
		catch (SQLException e)
		{
			this.hasErrors = true;
			LogMgr.logError("ImportWorker.processRow()", "Error importing row " + currentImportRow + ": " + ExceptionUtil.getDisplay(e), null);
			errorCount ++;
			if (!this.continueOnError) throw e;
			ValueDisplay display = new ValueDisplay(row);
			String rec = display.toString();
			controller.recordRejected(rec, currentImportRow, e);
		}

		if (MemoryWatcher.isMemoryLow())
		{
			this.hasErrors = true;
			closeStatements();
			controller.clearMessages();
			controller.addMessage(ResourceMgr.getString("MsgLowMemoryError"), true);
			throw new SQLException("Not enough memory!");
		}

		if (this.commitEvery > 0 && ((this.totalRows % this.commitEvery) == 0) && !this.dbConn.getAutoCommit())
		{
			try
			{
				this.dbConn.commit();
			}
			catch (SQLException e)
			{
				String error = ExceptionUtil.getDisplay(e);
				controller.addError(error);
				this.hasErrors = true;
				if (!continueOnError)	throw e;
			}
		}
	}

	private void setUpdateSavepoint()
	{
		try
		{
			this.updateSavepoint = this.dbConn.getSqlConnection().setSavepoint();
		}
		catch (Exception e)
		{
			LogMgr.logError("ImportWorker", "Could not create pre-update Savepoint", e);
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
			LogMgr.logError("ImportWorker", "Could not set pre-insert Savepoint", e);
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
			LogMgr.logError("ImportWorker.rollbackToSavePoint()", "Error when performing rollback to savepoint", e);
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
			LogMgr.logError("ImportWorker.processrow()", "Error when releasing savepoint", th);
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

	private long processRowData(BatchedStatement stmt, Object[] row, boolean useColMap)
		throws SQLException
	{
		List<Closeable> streams = new LinkedList<Closeable>();
		
		for (int i=0; i < row.length; i++)
		{
			int colIndex = i  + 1;
			if (useColMap)
			{
				// The colIndex points to the correct location in the PreparedStatement
				// when using UPDATE with different column names
				colIndex = this.columnMap[i] + 1;
			}

			int targetSqlType = this.targetColumns.get(i).getDataType();
			String targetDbmsType = this.targetColumns.get(i).getDbmsType();

			if (row[i] == null)
			{
				if (useSetNull)
				{
					stmt.setNull(colIndex, targetSqlType);
				}
				else
				{
					stmt.setObject(colIndex, null);
				}
			}
			else if ( SqlUtil.isClobType(targetSqlType) || "LONG".equals(targetDbmsType) ||
				       "CLOB".equals(targetDbmsType) )
			{
				Reader in = null;
				int size = -1;

				if (row[i] instanceof Clob)
				{
					Clob clob = (Clob) row[i];
					in = clob.getCharacterStream();
					streams.add(in);
				}
				else if (row[i] instanceof File)
				{
					ImportFileParser parser = controller.getParser();
					
					ImportFileHandler handler = (parser != null ? parser.getFileHandler() : null);
					String encoding = (handler != null ? handler.getEncoding() : null);
					if (encoding == null)
					{
						encoding = (parser != null ? parser.getEncoding() : Settings.getInstance().getDefaultDataEncoding());
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
							streams.add(in);
						}
						else
						{
							if (!f.isAbsolute())
							{
								File sourcefile = new File(parser.getSourceFilename());
								f = new File(sourcefile.getParentFile(), f.getName());
							}
							in = EncodingUtil.createBufferedReader(f, encoding);
							streams.add(in);

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
						controller.addError(msg);
						throw new SQLException(ex.getMessage());
					}
				}
				else
				{
					// this assumes that the JDBC driver will actually
					// implement the toString() for whatever object
					// it created when reading that column!
					String value = row[i].toString();
					in = null;
					stmt.setObject(colIndex, value);
				}

				if (in != null)
				{
          // For Oracle, this will only work with Oracle 10g drivers.
          // Oracle 9i drivers do not implement the setCharacterStream()
          // and associated methods properly
					stmt.setCharacterStream(colIndex, in, size);
				}
			}
			else if (SqlUtil.isBlobType(targetSqlType) || "BLOB".equals(targetDbmsType))
			{
				InputStream in = null;
				int len = -1;
				if (row[i] instanceof File)
				{
					// When importing files created by SQL Workbench/J
					// blobs will be "passed" as File objects pointing to the external file
					ImportFileParser parser = controller.getParser();
					ImportFileHandler handler = (parser != null ? parser.getFileHandler() : null);
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
								File sourcefile = new File(parser.getSourceFilename());
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
						controller.addError(msg);
						throw new SQLException(ex.getMessage());
					}
					streams.add(in);
				}
				else if (row[i] instanceof Blob)
				{
					Blob b = (Blob)row[i];
					in = b.getBinaryStream();
					streams.add(in);
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
					stmt.setBinaryStream(colIndex, in, len);
				}
				else
				{
					stmt.setNull(colIndex, Types.BLOB);
					controller.addMessage(ResourceMgr.getFormattedString("MsgBlobNotRead", Integer.valueOf(i+1)), true);
				}
			}
			else
			{
				if (isOracle &&	targetSqlType == java.sql.Types.DATE && row[i] instanceof java.sql.Date)
				{
					java.sql.Timestamp ts = new java.sql.Timestamp(((java.sql.Date)row[i]).getTime());
					stmt.setTimestamp(colIndex, ts);
				}
				else
				{
					stmt.setObject(colIndex, row[i]);
				}
			}
		}

		if (this.controller.getColumnConstants() != null && stmt == this.insertStatement)
		{
			ConstantColumnValues values = controller.getColumnConstants();
			int count = values.getColumnCount();
			int colIndex = row.length + 1;
			for (int i=0; i < count; i++)
			{
				if (!values.isFunctionCall(i))
				{
					values.setParameter(stmt.getStatement(), colIndex, i);
					colIndex ++;
				}
			}
		}

		long rows = stmt.executeUpdate();
		
		return rows;
	}

	public void rollback()
		throws SQLException
	{
		this.dbConn.rollback();
	}

	public void commit()
		throws SQLException
	{
		this.dbConn.commit();
	}
	
	public synchronized void done()
		throws SQLException
	{
		flush();
		closeStatements();
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
}
