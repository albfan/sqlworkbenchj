/*
 * NoConnectionException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
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

