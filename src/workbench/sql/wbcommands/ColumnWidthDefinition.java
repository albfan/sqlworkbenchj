/*
 * ColumnWidthDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
 * @author support@sql-workbench.net
 */
public class ColumnWidthDefinition
{
	private Map<ColumnIdentifier, Integer> columnWidths;

	public ColumnWidthDefinition(String paramValue)
		throws MissingWidthDefinition
	{
		List<String> entries = StringUtil.stringToList(paramValue, ",", true, true);
		if (entries == null || entries.size() == 0)
		{
			return;
		}
		this.columnWidths = new HashMap<ColumnIdentifier, Integer>();

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
