/*
 * DbmsOutput.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * A class to control the dbms_output package in Oracle through JDBC
 *
 * @author Thomas Kellerer
 */
public class DbmsOutput
{
	private Connection conn;
	private boolean enabled = false;
	private long lastSize;

	public DbmsOutput(Connection aConn)
		throws SQLException
	{
		this.conn = aConn;
	}

	/**
	 * Enable Oracle's dbms_output with the specified buffer size
	 * This essentially calls dbms_output.enable().
	 *
	 * @param size the buffer size, if &lt; 0 no limit will be passed, otherwise the specified number
	 * @throws SQLException
	 */
	public void enable(long size)
		throws SQLException
	{
		if (this.enabled && size == this.lastSize) return;

		CallableStatement enableStatement = null;
		try
		{
			if (size < 0)
			{
				enableStatement = conn.prepareCall( "{call dbms_output.enable}" );
			}
			else
			{
				enableStatement = conn.prepareCall( "{call dbms_output.enable(?) }" );
				enableStatement.setLong(1, size);
			}
			enableStatement.executeUpdate();
			this.enabled = true;
			this.lastSize = size;
			LogMgr.logDebug("DbmsOutput.enable()", "Support for DBMS_OUTPUT package enabled (max size=" + size + ")");
		}
		finally
		{
			SqlUtil.closeStatement(enableStatement);
		}
	}

	/**
	 * Disable dbms_output.
	 * This simply calls dbms_output.disable();
	 */
	public void disable()
		throws SQLException
	{
		CallableStatement disableStatement = null;
		try
		{
			disableStatement = conn.prepareCall( "{call dbms_output.disable}" );
			disableStatement.executeUpdate();
			this.enabled = false;
		}
		finally
		{
			SqlUtil.closeStatement(disableStatement);
		}
	}

	/**
	 * Retrieve all server messages written with dbms_output.
	 *
	 * @return all messages written with dbms_output.put_line()
	 */
	public String getResult()
		throws SQLException
	{
    if (!this.enabled) return "";

		CallableStatement stmt = null;
		StringBuilder result = new StringBuilder(1024);
		try
		{
			stmt = conn.prepareCall("{call dbms_output.get_line(?,?)}");
			stmt.registerOutParameter(1,java.sql.Types.VARCHAR);
			stmt.registerOutParameter(2,java.sql.Types.NUMERIC);

			int status = 0;
			while (status == 0)
			{
				stmt.execute();
				String line = stmt.getString(1);
				status = stmt.getInt(2);
				if (line != null && status == 0)
				{
					result.append(line.trim());
					result.append('\n');
				}
			}
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
		return result.toString();
	}

	public void close()
	{
		try
		{
			this.disable();
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("DbmsOutput", "Error when disabling dbms_output", th);
		}
	}

}
