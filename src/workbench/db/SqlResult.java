/*
 * SqlResult.java
 *
 * Created on November 30, 2001, 11:11 PM
 */

package workbench.db;

import java.sql.Connection;
import workbench.db.DbReader;

/**
 *
 *	The result of executing a Statement
 *	This can either be a result set (for SELECT, EXEC)
 *	Or simply a message (for DELETE, UPDATE etc)
 *
 *  @author  thomas.kellerer@web.de
 *  @version
 */
public class SqlResult
{
	private DbReader result = null;
	private String message = null;
	private boolean error = false;
	
	/** Creates new SqlResult */
	public SqlResult(String aStatement, Connection aDbConnection)
	{
	}
	
	public boolean hasResult()
	{
		return (result != null);
	}
	
	public String getMessage()
	{
		return message;
	}
	
}
