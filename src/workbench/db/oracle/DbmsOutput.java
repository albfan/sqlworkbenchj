/*
 * DbmsOutput.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.oracle;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import workbench.log.LogMgr;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

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
			enableStatement = conn.prepareCall( "{call dbms_output.enable(?) }" );
			if (size <= 0)
			{
				enableStatement.setNull(1, Types.BIGINT);
			}
			else
			{
				enableStatement.setLong(1, size);
			}
			enableStatement.executeUpdate();
			this.enabled = true;
			this.lastSize = size;
			LogMgr.logDebug("DbmsOutput.enable()", "Support for DBMS_OUTPUT package enabled (max size=" + (size > 0 ? Long.toString(size) : "unlimited") + ")");
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

  public boolean isEnabled()
  {
    return enabled;
  }

	/**
	 * Retrieve all server messages written with dbms_output.
   *
   * Nothing will be retrieved if enable() was never called or if disable() was called since then.
	 *
	 * @return all messages written with dbms_output.put_line()
	 */
	public String getResult()
		throws SQLException
	{
    if (!this.enabled) return "";
    return retrieveOutput();
  }

	/**
	 * Retrieve all server messages written with dbms_output regardless if enable() has been called before.
   *
	 * @return all messages written with dbms_output.put_line()
	 */
  public String retrieveOutput()
    throws SQLException
  {
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
				if (line == null) line = "";
				status = stmt.getInt(2);
				if (status == 0)
				{
					result.append(StringUtil.rtrim(line));
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
