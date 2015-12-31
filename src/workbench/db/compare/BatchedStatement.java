/*
 * BatchedStatement.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.compare;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.WbConnection;

import workbench.util.FileUtil;
import workbench.util.SqlUtil;

/**
 * A wrapper to treat batched statements just like a "normal" statement.
 * <br/>
 * In order to have proper housekeeping for Streams used for LOB columns
 * the wrapped setXXX() methods from this class should be used instead of
 * calling them on the PreparedStatement directly.
 * <br/>
 * The BatchedStatement will remember all streams used for LOB
 * columns and will close them automatically once the statement
 * has been executed, i.e. for a batch size == 1 this will be
 * done immediately when calling executeUpdate(). For a batch size > 1
 * this will be done whenever the batch is sent to the database.
 * <br/>
 * @author Thomas Kellerer
 */
public class BatchedStatement
{
	private PreparedStatement statement;
	private int batchSize;
	private int currentSize;
	private boolean commitBatch;
	private final List<Closeable> streams = new ArrayList<>();

	/**
	 * Creates a BatchedStatement with a batch size of 10.
	 *
	 * @param pstmt the PreparedStatement to be batched
	 * @param conn the statement that was used to create the statement
	 * @param size the batch size to use
	 */
	public BatchedStatement(PreparedStatement pstmt, WbConnection conn, int size)
	{
		this.statement = pstmt;
		try
		{
			if (size > 1 && conn.getSqlConnection().getMetaData().supportsBatchUpdates())
			{
				this.batchSize = size;
			}
			else
			{
				this.batchSize = 1;
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("BatchedStatement.<init>", "Error initializing statement", e);
			this.batchSize = 1;
		}
		this.currentSize = 0;
	}

	public void cancel()
		throws SQLException
	{
		if (statement != null)
		{
			statement.cancel();
		}
	}

	/**
	 * Defines if each batch should be committed.
	 *
	 * @param flag
	 */
	public void setCommitBatch(boolean flag)
	{
		this.commitBatch = flag;
	}

	/**
	 * Wrapped PreparedStatement method
	 */
	public void setObject(int index, Object value)
		throws SQLException
	{
		this.statement.setObject(index, value);
	}

	/**
	 * Wrapped PreparedStatement method
	 */
	public void setObject(int index, Object value, int type)
		throws SQLException
	{
		this.statement.setObject(index, value, type);
	}

	/**
	 * Wrapped PreparedStatement method
	 */
	public void setDate(int index, java.sql.Date date)
		throws SQLException
	{
		statement.setDate(index, date);
	}

	/**
	 * Wrapped PreparedStatement method
	 */
	public void setTimestamp(int index, java.sql.Timestamp ts)
		throws SQLException
	{
		statement.setTimestamp(index, ts);
	}

	/**
	 * Wrapped PreparedStatement method
	 */
	public void setNull(int index, int type)
		throws SQLException
	{
		statement.setNull(index, type);
	}

	public void setXML(int index, SQLXML xml)
		throws SQLException
	{
		statement.setSQLXML(index, xml);
	}

	/**
	 * Wrapped PreparedStatement method
	 */
	public void setCharacterStream(int index, Reader in, int streamSize)
		throws SQLException
	{
		statement.setCharacterStream(index, in, streamSize);
		synchronized (streams)
		{
			streams.add(in);
		}
	}

	/**
	 * Wrapped PreparedStatement method
	 */
	public void setBinaryStream(int index, InputStream in, int streamSize)
		throws SQLException
	{
		statement.setBinaryStream(index, in, streamSize);
		synchronized (streams)
		{
			streams.add(in);
		}
	}

	/**
	 * Exposing the statement variable in case something else
	 * apart from the wrapped setXXXX() methods is needed
	 *
	 * @return the internally used PreparedStatement
	 */
	public PreparedStatement getStatement()
	{
		return this.statement;
	}

	/**
	 * Adds the current set of value to the JDBC batch.
	 * <br/>
	 * If the batch is full, the batch is executed (using runBatch())
	 * If any streams were set through setBinaryStream() or setCharacterStream()
	 * they will be closed automatically.
	 *
	 * @return the update counts for the batch or null if the batch was not yet full
	 * @throws java.sql.SQLException
	 */
	protected long addBatch()
		throws SQLException
	{
		long result = 0;
		if (batchSize == 1)
		{
			result = statement.executeUpdate();
			closeStreams();
		}
		else
		{
			statement.addBatch();
			currentSize ++;
			if (currentSize == batchSize)
			{
				result = runBatch();
			}
		}
		return result;
	}

	/**
	 * Calls executeBatch() on the prepared statement.
	 * <br/>
	 * The row number returned is obtained from the array
	 * returned by executeBatch().
	 * <br>
	 * Entries in the array with <tt>Statement.SUCCESS_NO_INFO</tt> are assumed
	 * to be successful as well.
	 * <br/>
	 * If any streams were set through setBinaryStream() or setCharacterStream()
	 * they will be closed automatically.
	 *
	 * @return the number of rows affected
	 */
	private long runBatch()
		throws SQLException
	{
		int[] result = statement.executeBatch();
		currentSize = 0;
		if (result == null) return 0;

		long rows = 0;
		for (int i : result)
		{
			if (i == Statement.SUCCESS_NO_INFO) rows ++;
			else rows += i;
		}

		if (commitBatch)
		{
			statement.getConnection().commit();
		}

		statement.clearBatch();
		closeStreams();
		return rows;
	}

	private void closeStreams()
	{
		synchronized (streams)
		{
			FileUtil.closeStreams(streams);
			streams.clear();
		}
	}

	/**
	 * Executes the statement. If a batch is defined, this will simply
	 * add the current values to the batch, otherwise it will be executed
	 * directly.
	 *
	 * @return the number of rows affected
	 *
	 * @throws java.sql.SQLException
	 */
	public long executeUpdate()
		throws SQLException
	{
		return addBatch();
	}

	/**
	 * Executes any pending rows in the batch queue.
	 *
	 * @return the number of rows affected
	 * @see #runBatch()
	 *
	 * @throws java.sql.SQLException
	 */
	public long flush()
		throws SQLException
	{
		if (this.currentSize > 0)
		{
			return this.runBatch();
		}
		return 0;
	}

	/**
	 * Closes the underlying statement, clears the batch
	 * and closes any stream that was used by the batch.
	 */
	public void close()
	{
		try { statement.clearBatch(); } catch (Throwable th) {}
		SqlUtil.closeStatement(this.statement);
		closeStreams();
	}

  @Override
  public String toString()
  {
    if (statement != null) return statement.toString();
    return "";
  }

}
