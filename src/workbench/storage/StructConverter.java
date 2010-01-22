/*
 * StructConverter
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Thomas Kellerer
 */
public class StructConverter
{

	public static final StructConverter getInstance()
	{
		return InstanceHolder.INSTANCE;
	}

	protected static class InstanceHolder
	{
		protected static final StructConverter INSTANCE = new StructConverter();
	}

	private final SimpleDateFormat timestampFormatter;
	private final SimpleDateFormat dateFormatter;
	private final SimpleDateFormat timeFormatter;

	private StructConverter()
	{
		// The ANSI literals should be OK, as all databases that support structs
		// also support ANSI compliant date literals.
		timestampFormatter = new SimpleDateFormat("'TIMESTAMP '''yyyy-MM-dd HH:mm:ss''");
		timeFormatter = new SimpleDateFormat("'TIME '''HH:mm:ss''");
		dateFormatter = new SimpleDateFormat("'DATE '''yyyy-MM-dd''");
	}

	public CharSequence getStructDisplay(Struct data)
		throws SQLException
	{
		if (data == null) return null;

		Object[] attr = data.getAttributes();
		if (attr == null) return null;

		StringBuilder buffer = new StringBuilder(attr.length * 20);

		String name = data.getSQLTypeName();
		if (name != null) buffer.append(name);

		buffer.append('(');
		boolean first = true;
		for (Object a : attr)
		{
			if (!first) buffer.append(", ");
			else first = false;
			if (a == null)
			{
				buffer.append("NULL");
			}
			else
			{
				if (a instanceof Struct)
				{
					buffer.append(getStructDisplay((Struct)a));
				}
				else
				{
					if (a instanceof CharSequence)
					{
						buffer.append('\'');
						buffer.append(a.toString());
						buffer.append('\'');
					}
					else if (a instanceof Timestamp)
					{
						synchronized (timestampFormatter)
						{
							buffer.append(timestampFormatter.format((Timestamp)a));
						}
					}
					else if (a instanceof Time)
					{
						synchronized (timeFormatter)
						{
							buffer.append(timeFormatter.format((Time)a));
						}
					}
					else if (a instanceof Date)
					{
						synchronized (dateFormatter)
						{
							buffer.append(dateFormatter.format((Date)a));
						}
					}
					else
					{
						buffer.append(a.toString());
					}
				}
			}
		}
		buffer.append(')');
		return buffer.toString();
	}
}
