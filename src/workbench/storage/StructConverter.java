/*
 * StructConverter
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
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
 * A class to create a readable display for java.sql.Struct objects
 * retrieved from the database.
 * <br/>
 * <br/>
 * This is a singleton to avoid excessive object creation during data retrieval.
 * <br/>
 * Currently this will only be used when retrieving data from an Oracle database.
 * The Postgres JDBC driver returns a String representation of "structured" types
 * (and not a Struct).
 * DB2 needs a conversion function that will be called by DB2 during retrieval
 * and will thus return a String object as well.
 * 
 * @author Thomas Kellerer
 * @see RowData#read(java.sql.ResultSet, workbench.storage.ResultInfo) 
 */
public class StructConverter
{

	public static StructConverter getInstance()
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

	/**
	 * Create a display for the given Struct.
	 * <br>
	 * The display closeley duplicates the way SQL*Plus shows object types.
	 * If attributes of the Struct are itself a Struct, this method is called
	 * recursively.
	 * <br/>
	 * The name of the Struct will be followed by all values in paranthesis, e.g.
	 * <tt>MY_TYPE('Hello', 'World', 42)</tt> 
	 * <br/>
	 * Note that Oracle apparently always returns the owner as part of the type name,
	 * so the actual display will be <tt>SCOTT.MY_TYPE('Hello', 'World', 42)</tt>
	 *
	 * @param data the Struct to convert
	 * @return a String representation of the data
	 * @throws SQLException
	 */
	public String getStructDisplay(Struct data)
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
						// String need to be enclosed in single quotes
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
						// for anything else, rely on the driver
						// as the JDBC type of this attribute is not known, we also
						// cannot dispatch this to a DataConverter
						buffer.append(a.toString());
					}
				}
			}
		}
		buffer.append(')');
		return buffer.toString();
	}
}
