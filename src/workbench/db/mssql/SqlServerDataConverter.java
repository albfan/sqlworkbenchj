/*
 * SqlServerDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mssql;

import java.sql.Types;
import workbench.log.LogMgr;
import workbench.storage.DataConverter;
import workbench.util.NumberStringCache;

/**
 *
 * @author support@sql-workbench.net
 */
public class SqlServerDataConverter
	implements DataConverter
{

	protected static class LazyInstanceHolder
	{
		protected static final SqlServerDataConverter instance = new SqlServerDataConverter();
	}

	public static final SqlServerDataConverter getInstance()
	{
		return LazyInstanceHolder.instance;
	}
	
	private SqlServerDataConverter()
	{

	}
	public boolean convertsType(int jdbcType, String dbmsType)
	{
		return (jdbcType == Types.BINARY && dbmsType.equals("timestamp"));
	}

	public Object convertValue(int jdbcType, String dbmsType, Object originalValue)
	{
		if (!convertsType(jdbcType, dbmsType)) return originalValue;
		Object newValue = null;
		try
		{
			byte[] b = (byte[])originalValue;
			StringBuilder buffer = new StringBuilder(18);
			buffer.append("0x");
			for (byte v : b)
			{
				int c = (v < 0 ? 256 + v : v);
				buffer.append(NumberStringCache.getHexString(c));
			}
			newValue = buffer.toString();
		}
		catch (Throwable th)
		{
			LogMgr.logWarning("SqlServerDataConverter.convertValue()", "Error converting value " + originalValue, th);
			newValue = originalValue;
		}
		return newValue;
	}

}
