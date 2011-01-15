/*
 * ConverterException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 * @author Thomas Kellerer
 */
public class ConverterException extends java.lang.Exception
{

	public ConverterException(String msg)
	{
		super(msg);
	}

	public ConverterException(Object input, int type,  Exception cause)
	{
		super("Could not convert [" + input + "] for datatype " + SqlUtil.getTypeName(type), cause);
	}

	public String getLocalizedMessage()
	{
		if (this.getCause() == null)
		{
			return getMessage();
		}
		return getCause().getMessage();
	}
}
