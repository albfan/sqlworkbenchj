/*
 * InvalidStatementException.java
 *
 * Created on November 25, 2001, 2:41 PM
 */

package workbench.exception;

/**
 *
 * @author  thomas
 * @version
 */
public class NoConnectionException extends Exception
{

	/**
	 * Constructs an <code>InvalidStatementException</code> with the specified detail message.
	 * @param msg the detail message.
	 */
	public NoConnectionException(String msg)
	{
		super(msg);
	}
}

