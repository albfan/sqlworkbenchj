/*
 * DbmsOutput.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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

	public void enable(long size) throws SQLException
	{
		if (this.enabled && size == this.lastSize) return;
		CallableStatement enableStatement = conn.prepareCall( "begin dbms_output.enable(:1); end;" );
		enableStatement.setLong( 1, size );
		enableStatement.executeUpdate();
		enableStatement.close();
		this.enabled = true;
		this.lastSize = size;
		LogMgr.logDebug("DbmsOutput.enable()", "Support for DBMS_OUTPUT package enabled (max size=" + size + ")");
	}

	public void enable()
		throws SQLException
	{
		this.enable(-1);
	}

	/*
	 * disable simply executes the dbms_output.disable
	 */
	public void disable() throws SQLException
	{
		CallableStatement disableStatement = conn.prepareCall( "begin dbms_output.disable; end;" );
		disableStatement.executeUpdate();
		disableStatement.close();
		this.enabled = false;
	}

	/*
	 * getResult() does most of the work.  It loops over
	 * all of the dbms_output data.
	 */
	public String getResult()
		throws SQLException
	{
		int done = 0;
    if (!this.enabled) return "";
		CallableStatement showOutputStatement = conn.prepareCall(
		"declare " +
		"    l_line varchar2(255); " +
		"    l_done number; " +
		"    l_buffer long; " +
		"begin " +
		"  loop " +
		"    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; " +
		"    dbms_output.get_line( l_line, l_done ); " +
		"    l_buffer := l_buffer || l_line || chr(10); " +
		"  end loop; " +
		" :done := l_done; " +
		" :buffer := l_buffer; " +
		"end;" );

		showOutputStatement.registerOutParameter( 2, Types.INTEGER );
		showOutputStatement.registerOutParameter( 3, Types.VARCHAR );
		StringBuffer result = new StringBuffer(1024);
		for(;;)
		{
			showOutputStatement.setInt( 1, 32000 );
			showOutputStatement.executeUpdate();
			result.append(showOutputStatement.getString(3).trim());
			if ( (done = showOutputStatement.getInt(2)) == 1 ) break;
		}
		try { showOutputStatement.close(); } catch (Throwable th) {}
		return result.toString().trim();
	}

	public void close()
	{
		try { this.disable(); } catch (Throwable th) {}
	}

	protected void finalize()
	{
		this.close();
	}
}
