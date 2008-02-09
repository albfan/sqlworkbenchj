/*
 * ControlFileFormat.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public enum ControlFileFormat
{
	none,
	oracle,
	sqlserver;
	
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
			case oracle:
				return new OracleControlFileWriter();
			case sqlserver:
				return new SqlServerFormatFileWriter();
		}
		return null;
	}
}
