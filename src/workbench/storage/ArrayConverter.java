/*
 * ArrayConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Struct;

/**
 * A class for a generic toString() method on java.sql.Array values.
 *
 * @author Thomas Kellerer
 */
public class ArrayConverter
{
	public static String getArrayDisplay(Object value, String dbmsType)
		throws SQLException
	{
		if (value == null) return null;

		if (value instanceof Array)
		{
			Array ar = (Array)value;
			Object[] elements = (Object[])ar.getArray();
			int len = elements.length;
			StringBuilder sb = new StringBuilder(len * 10);
			sb.append(dbmsType);
			sb.append('(');
			StructConverter conv = StructConverter.getInstance();

			for (int x=0; x < len; x++)
			{
				if (x > 0) sb.append(',');
				if (elements[x] == null)
				{
					sb.append("NULL");
				}
				else if (elements[x] instanceof Struct)
				{
					sb.append(conv.getStructDisplay((Struct)elements[x]));
				}
				else
				{
					conv.appendValue(sb, elements[x]);
				}
			}
			sb.append(')');
			return sb.toString();
		}
		return value.toString();
	}
}
