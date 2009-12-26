/*
 * ControlFileFormat.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import workbench.db.ibm.Db2FormatFileWriter;
import workbench.util.StringUtil;

/**
 * Type definitions for various control file formats.
 * Currently Oracle, SQL Server, Postgres (COPY command) are covered
 * 
 * @author support@sql-workbench.net
 * @see OracleControlFileWriter
 * @see SqlServerFormatFileWriter
 * @see PostgresCopyStatementWriter
 */
public enum ControlFileFormat
{
	none,
	oracle,
	sqlserver,
	postgres,
	db2;
	
	public static Set<ControlFileFormat> parseCommandLine(String args)
	{
		if (StringUtil.isEmptyString(args)) return Collections.emptySet();
		Set<ControlFileFormat> result = new  HashSet<ControlFileFormat>();
		List<String> formats = StringUtil.stringToList(args);
		for (String fs : formats)
		{
			try 
			{
				ControlFileFormat f = ControlFileFormat.valueOf(fs);
				result.add(f);
			}
			catch (Exception e)
			{
			}
		}
		return result;
	}
	
	public static FormatFileWriter createFormatWriter(ControlFileFormat format)
	{
		switch (format)
		{
			case postgres:
				return new PostgresCopyStatementWriter();
			case oracle:
				return new OracleControlFileWriter();
			case sqlserver:
				return new SqlServerFormatFileWriter();
			case db2:
				return new Db2FormatFileWriter();
			default:
				return null;
		}
	}
}
