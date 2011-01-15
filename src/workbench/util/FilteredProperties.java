/*
 * FilteredProperties.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import workbench.interfaces.PropertyStorage;

/**
 *
 * @author Thomas Kellerer
 */
public class FilteredProperties
	extends WbProperties
{
	private String filterPrefix;

	public FilteredProperties(PropertyStorage source, String prefix)
	{
		super();
		filterPrefix = prefix;
		for (String key : source.getKeys())
		{
			if (key.startsWith(prefix))
			{
				this.setProperty(key, source.getProperty(key, null));
			}
		}
	}

	public String getFilterPrefix()
	{
		return filterPrefix;
	}

	public void copyTo(PropertyStorage target, String newPrefix)
	{
		for (String key : getKeys())
		{
			String newKey = key.replace(filterPrefix, newPrefix);
			target.setProperty(newKey, getProperty(key));
		}
	}
}
