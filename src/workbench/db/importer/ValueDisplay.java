/*
 * ValueDisplay.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.importer;

/**
 *
 * @author Thomas Kellerer
 */
public class ValueDisplay
{
	private String display;

	public ValueDisplay(Object[] row)
	{
		int count = row.length;
		StringBuilder values = new StringBuilder(count * 20);
		values.append('{');

		for (int i=0; i < count; i++)
		{
			if (i > 0) values.append(',');
			values.append('[');
			if (row[i] == null)
			{
				values.append("NULL");
			}
			else
			{
				values.append(row[i].toString());
			}
			values.append(']');
		}
		values.append('}');
		display = values.toString();
	}

	@Override
	public String toString()
	{
		return display;
	}

}
