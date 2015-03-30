/*
 * AggregatingMap.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author Thomas Kellerer
 */
public class AggregatingMap<K extends Object, V extends Object>
{
	private final Map<K, Set<V>> content;
	private final boolean sorted;

	public AggregatingMap(boolean sorted)
	{
		this.sorted = sorted;
		if (sorted)
		{
			content = new TreeMap<>();
		}
		else
		{
			content = new HashMap<>();
		}
	}

	public AggregatingMap(Map<K, Set<V>> map)
	{
		if (map == null) throw new NullPointerException("Content cannot be null");
		content = map;
		sorted = false;
	}

	public Set<V> addValue(K key, Set<V> values)
	{
		Set<V> current = content.get(key);
		if (current == null)
		{
			current = createValueSet();
			content.put(key, current);
		}
		current.addAll(values);
		return current;
	}

	private Set<V> createValueSet()
	{
		if (sorted)
		{
			return new TreeSet<>();
		}
		return new HashSet<>();
	}
	public Set<V> addValue(K key, V value)
	{
		Set<V> current = content.get(key);
		if (current == null)
		{
			current = createValueSet();
			content.put(key, current);
		}
		current.add(value);
		return current;
	}

	public Set<V> get(K key)
	{
		Set<V> result = content.get(key);
		if (result == null)
		{
			return Collections.emptySet();
		}
		return result;
	}

	public Map<K, Set<V>> getMap()
	{
		return content;
	}

	public Set<Map.Entry<K, Set<V>>> entrySet()
	{
		return content.entrySet();
	}

	public void addAllValues(Map<K, Set<V>> data)
	{
		for (Map.Entry<K, Set<V>> entry : data.entrySet())
		{
			addValue(entry.getKey(), entry.getValue());
		}
	}

	public void addAll(Map<K, V> data)
	{
		for (Map.Entry<K, V> entry : data.entrySet())
		{
			addValue(entry.getKey(), entry.getValue());
		}
	}

	public int size()
	{
		return content.size();
	}
}
