/*
 * ExecutionTimeLogger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.log;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import workbench.util.CharacterRange;
import workbench.util.DurationFormatter;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ExecutionTimeLogger
{
	private File logfile;
	private PrintWriter logfileWriter;
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat(StringUtil.ISO_TIMESTAMP_FORMAT);
	private DurationFormatter durationFormatter = new DurationFormatter();

	public ExecutionTimeLogger()
	{
	}

	public void logExecutionTime(long millis, String sql)
	{
		StringBuilder line = new StringBuilder(250);
		synchronized (dateFormatter)
		{
			line.append(dateFormatter.format(new java.util.Date()));
		}
		line.append('\t');
		line.append(millis);
		line.append('\t');
		String cleanSql = SqlUtil.makeCleanSql(sql, false, false);
		cleanSql = StringUtil.escapeUnicode(cleanSql, CharacterRange.RANGE_CONTROL);
		line.append(cleanSql);
	}

	
}
