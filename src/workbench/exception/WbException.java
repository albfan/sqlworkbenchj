/*
 * WBException.java
 *
 * Created on November 25, 2001, 1:41 PM
 */

package workbench.exception;

import java.sql.SQLException;

/**
 *	Common Exception for jWorkbench
 * @author  thomas
 * @version
 */
public class WbException extends java.lang.Exception
{
	private SQLException sqlError;
	
	/**
	 * Constructs an <code>WBException</code> with the specified detail message.
	 * @param msg the detail message.
	 */
	public WbException(String msg)
	{
		super(msg);
	}
	
	public WbException(String msg, SQLException aSQLError)
	{
		super(msg);
		this.sqlError = aSQLError;
	}
}


