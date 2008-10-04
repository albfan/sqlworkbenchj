/*
 * BatchProcessor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.compare;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.db.WbConnection;
import workbench.util.SqlUtil;

/**
 * A wrapper to treat batched statements just like a "normal" statement.
 * 
 * @author support@sql-workbench.net
 */
public class BatchedStatement
{
	private PreparedStatement statement;
	private int batchSize;
	private int currentSize;

	public BatchedStatement(PreparedStatement pstmt, WbConnection conn)
	{
		this(pstmt, conn, 10);
	}

	public BatchedStatement(PreparedStatement pstmt, WbConnection conn, int size)
	{
		this.statement = pstmt;
		try
		{
			if (conn.getSqlConnection().getMetaData().supportsBatchUpdates())
			{
				this.batchSize = (size > 0 ? size : 1);
			}
			else
			{
				this.batchSize = 1;
			}
		}
		catch (SQLException e)
		{
			this.batchSize = 1;
		}
		this.currentSize = 0;
	}

	public void setObject(int index, Object value)
		throws SQLException
	{
		this.statement.setObject(index, value);
	}

	/**
	 * Adds the current set of value to the JDBC batch.
	 * If the batch is full, the batch is executed.
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

	private long runBatch()
		throws SQLException
	{
		int[] result = statement.executeBatch();
		currentSize = 0;
		statement.clearBatch();
		if (result == null) return 0;

		long rows = 0;
		for (int i : result)
		{
			if (i == Statement.SUCCESS_NO_INFO) rows ++;
			else rows += i;
		}
		return rows;
	}

	public long executeUpdate()
		throws SQLException
	{
		return addBatch();
	}

	public long flush()
		throws SQLException
	{
		if (this.currentSize > 0)
		{
			return this.runBatch();
		}
		return 0;
	}

	public void close()
	{
		SqlUtil.closeStatement(this.statement);
	}

}
