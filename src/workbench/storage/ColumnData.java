/*
 * ColumnData.java
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

import workbench.db.ColumnIdentifier;

/**
 * A wrapper class do hold the current value of a column and it's definition.
 *
 * The column definition is represented by a {@link workbench.db.ColumnIdentifier}<br/>
 * The column value can be any Java object<br/>
 *
 * This is used by {@link workbench.storage.DmlStatement} to store the values when creating PreparedStatements
 *
 * @author Thomas Kellerer
 */
public class ColumnData
{
	final private Object data;
	final private ColumnIdentifier id;

	/**
	 * Creates a new instance of ColumnData
	 *
	 * @param value The current value of the column
	 * @param colid The definition of the column
	 */
	public ColumnData(Object value, ColumnIdentifier colid)
	{
		data = value;
		id = colid;
	}

	public Object getValue()
	{
		return data;
	}

	public ColumnIdentifier getIdentifier()
	{
		return id;
	}

	public boolean isNull()
	{
		return (data == null);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (obj instanceof ColumnData)
		{
			final ColumnData other = (ColumnData) obj;
			return this.id.equals(other.id);
		}
		else if (obj instanceof ColumnIdentifier)
		{
			return this.id.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public String toString()
	{
		return id.getColumnName() + " = " + (data == null ? "NULL" : data.toString());
	}

}
