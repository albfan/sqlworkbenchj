/*
 *  ReplaceModifier.java
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
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
