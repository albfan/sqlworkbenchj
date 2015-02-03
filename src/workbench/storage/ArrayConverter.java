/*
 * ArrayConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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
	public static String getArrayDisplay(Object value, String dbmsType, boolean showType)
		throws SQLException
	{
		if (value == null) return null;

		Object[] elements = null;
		String prefix = "";
		if (value instanceof Array)
		{
			Array ar = (Array)value;
			elements = (Object[])ar.getArray();
			prefix = dbmsType;
		}
		else if (value instanceof Object[])
		{
			// this is for H2
			elements = (Object[])value;
		}

		if (elements != null)
		{
			int len = elements.length;
			StringBuilder sb = new StringBuilder(len * 10);
			if (showType)
			{
				sb.append(prefix);
			}
			sb.append('[');
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
			sb.append(']');
			return sb.toString();
		}
		return value.toString();
	}
}
