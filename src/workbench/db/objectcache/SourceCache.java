/*
 * SourceCache.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.objectcache;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import workbench.resource.Settings;
import workbench.util.CaseInsensitiveComparator;

/**
 *
 * @author Thomas Kellerer
 */
public class SourceCache
{
	private Map<String, CharSequence> cachedSources = Collections.synchronizedMap(new TreeMap<String, CharSequence>(CaseInsensitiveComparator.INSTANCE));
	private String dbid;

	public SourceCache(String id)
	{
		this.dbid = id;
	}

	public CharSequence getSource(String type, String key)
	{
		if (shouldCache(type))
		{
			return cachedSources.get(getRealKey(type, key));
		}
		return null;
	}

	public void addSource(String type, String key, CharSequence source)
	{
		if (shouldCache(type))
		{
			cachedSources.put(getRealKey(type, key), source);
		}
	}

	private String getRealKey(String type, String key)
	{
		if (type == null) return key;
		return "$" + type.toLowerCase() + "$/" + key;
	}

	public boolean shouldCache(String type)
	{
		return Settings.getInstance().getBoolProperty("workbench.db." + dbid + ".source.cache." + type, false);
	}

	public void clear()
	{
		cachedSources.clear();
	}
}
