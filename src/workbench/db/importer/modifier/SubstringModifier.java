/*
 * SubstringModifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.importer.modifier;

import java.util.HashMap;
import java.util.Map;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class SubstringModifier
	implements ImportValueModifier
{
	private Map<ColumnIdentifier, ColumnValueSubstring> limits =  new HashMap<ColumnIdentifier, ColumnValueSubstring>();

	@Override
	public int getSize()
	{
		return limits.size();
	}

	/**
	 * Define substring limits for a column.
	 * An existing mapping for that column will be overwritten.
	 *
	 * @param col the column for which to apply the substring
	 * @param start the start of the substring
	 * @param end the end of the substring
	 */
	public void addDefinition(ColumnIdentifier col, int start, int end)
	{
		ColumnValueSubstring s = new ColumnValueSubstring(start, end);
		this.limits.put(col.createCopy(), s);
	}

	@Override
	public String modifyValue(ColumnIdentifier col, String value)
	{
		ColumnValueSubstring s = this.limits.get(col);
		if (s != null)
		{
			return s.getSubstring(value);
		}
		return value;
	}

	/**
	 * For testing purposes to allow access to the actual "modifier"
	 */
	public ColumnValueSubstring getSubstring(ColumnIdentifier col)
	{
		return this.limits.get(col);
	}

}

