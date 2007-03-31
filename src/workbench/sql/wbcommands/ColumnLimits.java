/*
 * ColumnLimits.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
 * A class to parse a column size limit definition for the WbImport command
 * @author support@sql-workbench.net
 */
class ColumnLimits
{

	private Map<ColumnIdentifier, Integer> limits;

	/**
	 * Parses a parameter value for column limit definitions.
	 * e.g. description=100,firstname=10
	 * will "limit" the column description to 100 characters 
	 * and firstname to 10 characters.
	 */
	public ColumnLimits(String parameterValue)
		throws NumberFormatException
	{
		if (parameterValue == null) return;
		
		List<String> entries = StringUtil.stringToList(parameterValue, ",", true, true, false);
		if (entries.size() == 0) return;
		
		Map<ColumnIdentifier, Integer> lm = new HashMap<ColumnIdentifier, Integer>();
		for (String entry : entries)
		{
			String[] parts = entry.split("=");
			if (parts.length == 2 && parts[0] != null && parts[1] != null)
			{
				ColumnIdentifier col = new ColumnIdentifier(parts[0]);
				Integer l = Integer.valueOf(parts[1].trim());
				lm.put(col, l);
			}
		}
		this.limits = lm;
	}
	
	public Map<ColumnIdentifier, Integer> getLimits()
	{
		return limits;
	}
}
