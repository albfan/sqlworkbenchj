package workbench.db.oracle;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import workbench.log.LogMgr;


public class DbmsOutput
{
/*
 * our instance variables. It is always best to
 * use callable or prepared statements and prepare (parse)
 * them once per program execution, rather then one per
 * execution in the program.  The cost of reparsing is
 * very high.  Also -- make sure to use BIND VARIABLES!
 *
 * we use three statments in this class. One to enable
 * dbms_output - equivalent to SET SERVEROUTPUT on in SQL*PLUS.
 * another to disable it -- like SET SERVEROUTPUT OFF.
 * the last is to "dump" or display the results from dbms_output
 * using system.out
 *
 */
	private CallableStatement enable_stmt;
	private CallableStatement disable_stmt;
	private CallableStatement show_stmt;

	private boolean enabled;
	private long lastSize;

/*
 * our constructor simply prepares the three
 * statements we plan on executing.
 *
 * the statement we prepare for SHOW is a block of
 * code to return a String of dbms_output output.  Normally,
 * you might bind to a PLSQL table type but the jdbc drivers
 * don't support PLSQL table types -- hence we get the output
 * and concatenate it into a string.  We will retrieve at least
 * one line of output -- so we may exceed your MAXBYTES parameter
 * below. If you set MAXBYTES to 10 and the first line is 100
 * bytes long, you will get the 100 bytes.  MAXBYTES will stop us
 * from getting yet another line but it will not chunk up a line.
 *
 */
	public DbmsOutput( Connection conn )
		throws SQLException
	{
		enable_stmt  = conn.prepareCall( "begin dbms_output.enable(:1); end;" );
		disable_stmt = conn.prepareCall( "begin dbms_output.disable; end;" );

		show_stmt = conn.prepareCall(
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
	}

	/*
	 * enable simply sets your size and executes
	 * the dbms_output.enable call
	 *
	 */
	public void enable(long size) throws SQLException
	{
		if (this.enabled && size == this.lastSize) return;

		//this.disable();
		enable_stmt.setLong( 1, size );
		enable_stmt.executeUpdate();
		this.enabled = true;
		this.lastSize = size;
		LogMgr.logDebug("DbmsOutput.enable()", "Support for DBMS_OUTPUT package enabled");
	}

	public void enable()
		throws SQLException
	{
		this.enable(-1);
	}

	/*
	 * disable only has to execute the dbms_output.disable call
	 */
	public void disable() throws SQLException
	{
		disable_stmt.executeUpdate();
		this.enabled = false;
		LogMgr.logDebug("DbmsOutput.disable()", "Support for DBMS_OUTPUT package disabled");
	}

	/*
	 * getResult() does most of the work.  It loops over
	 * all of the dbms_output data, fetching it in this
	 * case 32,000 bytes at a time (give or take 255 bytes).
	 * It will print this output on stdout by default (just
	 * reset what System.out is to change or redirect this
	 * output).
	 */
	public String getResult()
		throws SQLException
	{
		int done = 0;

		show_stmt.registerOutParameter( 2, Types.INTEGER );
		show_stmt.registerOutParameter( 3, Types.VARCHAR );
		StringBuffer result = new StringBuffer(1024);
		for(;;)
		{
			show_stmt.setInt( 1, 32000 );
			show_stmt.executeUpdate();
			result.append(show_stmt.getString(3).trim());
			if ( (done = show_stmt.getInt(2)) == 1 ) break;
		}

		return result.toString().trim();
	}

/*
 * close closes the callable statements associated with
 * the DbmsOutput class. Call this if you allocate a DbmsOutput
 * statement on the stack and it is going to go out of scope --
 * just as you would with any callable statement, result set
 * and so on.
 */
	public void close()
	{
		try { this.disable(); } catch (Throwable th) {}
		try { enable_stmt.close(); } catch (Throwable th) {}
		try { disable_stmt.close(); } catch (Throwable th) {}
		try { show_stmt.close(); } catch (Throwable th) {}
	}

	public void finalize()
	{
		this.close();
	}
}
