/*
 * DbmsOutput.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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
	private CallableStatement enableStatement;
	private CallableStatement disableStatement;
	private CallableStatement showOutputStatement;

	private boolean enabled = false;
	private long lastSize;
	private boolean initDone = false;

	public DbmsOutput(Connection aConn)
		throws SQLException
	{
		this.conn = aConn;
	}

/*
 * the statement we prepare for SHOW is a block of
 * code to return a String via dbms_output.get_line().  
 * At least one line of output will be retrieved even if that exceeds 
 * the given size limit
 */
	private void init()
		throws SQLException
	{
		enableStatement  = conn.prepareCall( "begin dbms_output.enable(:1); end;" );
		disableStatement = conn.prepareCall( "begin dbms_output.disable; end;" );

		showOutputStatement = conn.prepareCall(
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
		this.initDone = true;
	}
	
	public void enable(long size) throws SQLException
	{
		if (this.enabled && size == this.lastSize) return;
		if (!this.initDone) this.init();
		enableStatement.setLong( 1, size );
		enableStatement.executeUpdate();
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
		if (!this.initDone) this.init();
		disableStatement.executeUpdate();
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
		if (!this.initDone) this.init();

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

		return result.toString().trim();
	}

/*
 * close closes all prepared statements that are used internally
 */
	public void close()
	{
		if (!this.initDone) return;
		try { this.disable(); } catch (Throwable th) {}
		try { enableStatement.close(); } catch (Throwable th) {}
		try { disableStatement.close(); } catch (Throwable th) {}
		try { showOutputStatement.close(); } catch (Throwable th) {}
	}

	protected void finalize()
	{
		this.close();
	}
}
