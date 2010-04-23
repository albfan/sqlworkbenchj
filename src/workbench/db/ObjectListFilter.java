/*
 * ObjectListFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.HashMap;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectListFilter
{
	private Map<String, ObjectNameFilter> filterMap = new HashMap<String, ObjectNameFilter>();

	public ObjectListFilter(String dbid)
	{
		String synRegex = Settings.getInstance().getProperty("workbench.db." + dbid + ".exclude.synonyms", null);
		addFilter(synRegex, "SYNONYM");
		ObjectNameFilter f = filterMap.get("SYNONYM");
		if (f != null)
		{
			filterMap.put("ALIAS", f);
		}

		String tableRegex = Settings.getInstance().getProperty("workbench.db." + dbid + ".exclude.tables", null);
		addFilter(tableRegex, "TABLE");

		String viewRegex = Settings.getInstance().getProperty("workbench.db." + dbid + ".exclude.views", null);
		addFilter(viewRegex, "VIEW");
	}

	private void addFilter(String regex, String type)
	{
		if (StringUtil.isNonBlank(regex) && StringUtil.isNonBlank(type))
		{
			ObjectNameFilter filter = new ObjectNameFilter();
			filter.setExpressionList(regex);
			filterMap.put(type, filter);
		}
	}

	public boolean isExcluded(String objectType, String objectName)
	{
		ObjectNameFilter filter = filterMap.get(objectType);
		if (filter == null) return false;
		return filter.isExcluded(objectName);
	}

}
