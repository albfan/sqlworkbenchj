/*
 * WrongFormatFileException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

/**
 *
 * @author Thomas Kellerer
 */
public class WrongFormatFileException
	extends Exception
{
	private String format;

	public WrongFormatFileException(String inputFormat)
	{
		super("Illegal format " + inputFormat + " specified");
		format = inputFormat;
	}

	public String getFormat()
	{
		return format;
	}
}
