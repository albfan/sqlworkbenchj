/*
 * SelectColumn
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.util.List;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectColumn
	extends Alias
{
	private String baseTable;

	public SelectColumn(String value)
	{
		super(value);
		List<String> elements = StringUtil.stringToList(getObjectName(), ".", true, true, false, true);
		if (elements.size() > 1)
		{
			StringBuilder s = new StringBuilder(value.length());
			for (int i=0; i < elements.size() - 1; i++)
			{
				if (i > 0) s.append('.');
				s.append(elements.get(i));
			}
			baseTable = s.toString();
			objectName = elements.get(elements.size() - 1);
		}

	}

	/**
	 * Returns the table associated with this column. If the column expresssion did not specify a table,
	 * null is returned.
	 *
	 */
	public String getColumnTable()
	{
		return baseTable;
	}
}
