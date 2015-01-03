/*
 * RegexReplacingModifier.java
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
package workbench.db.exporter;

import java.util.regex.Pattern;
import workbench.db.ColumnIdentifier;
import workbench.storage.RowData;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexReplacingModifier
	implements ExportDataModifier
{
	private Pattern searchPattern;
	private String replacement;

	public RegexReplacingModifier(String searchRegex, String replaceWith)
	{
		searchPattern = Pattern.compile(searchRegex);
		replacement = replaceWith;
	}

	public String getRegex()
	{
		return searchPattern.pattern();
	}

	public String getReplacement()
	{
		return replacement;
	}
	
	@Override
	public void modifyData(RowDataConverter converter, RowData row, long currentRowNumber)
	{
		int colCount = row.getColumnCount();

		for (int col=0; col < colCount; col ++)
		{
			ColumnIdentifier column = converter.getResultInfo().getColumn(col);
			if (converter.includeColumnInExport(col) && SqlUtil.isCharacterType(column.getDataType()))
			{
				String value = (String)row.getValue(col);
				if (value != null)
				{
					row.setValue(col, replacePattern(value));
				}
			}
		}
	}

	public String replacePattern(String value)
	{
		if (value == null) return null;
		if (searchPattern == null) return value;
		if (replacement == null) return value;
		
		return searchPattern.matcher(value).replaceAll(replacement);
	}
}
