/*
 * ColumnWidthDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.sql.wbcommands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.db.ColumnIdentifier;
import workbench.util.StringUtil;

/**
 * Parse the argument for defining column widths for a fixed-width
 * import file.
 * @see WbImport#ARG_COL_WIDTHS
 * @author Thomas Kellerer
 */
public class ColumnWidthDefinition
{
	private Map<ColumnIdentifier, Integer> columnWidths;

	public ColumnWidthDefinition(String paramValue)
		throws MissingWidthDefinition
	{
		List<String> entries = StringUtil.stringToList(paramValue, ",", true, true);
		if (entries == null || entries.isEmpty())
		{
			return;
		}
		this.columnWidths = new HashMap<>();

		for (String def : entries)
		{
			String[] parms = def.split("=");

			if (parms == null || parms.length != 2)
			{
				throw new MissingWidthDefinition(def);
			}
			ColumnIdentifier col = new ColumnIdentifier(parms[0]);
			int width = StringUtil.getIntValue(parms[1], -1);
			if (width <= 0)
			{
				throw new MissingWidthDefinition(def);
			}
			this.columnWidths.put(col, Integer.valueOf(width));
		}
	}

	public Map<ColumnIdentifier, Integer> getColumnWidths()
	{
		return columnWidths;
	}
}
