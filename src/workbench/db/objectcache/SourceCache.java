/*
 * SourceCache.java
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

  public boolean addSource(String type, String key, CharSequence source)
  {
    if (shouldCache(type))
    {
      cachedSources.put(getRealKey(type, key), source);
      return true;
    }
    return false;
  }

  private String getRealKey(String type, String key)
  {
    if (type == null) return key;
    return "$" + type.toLowerCase() + "$/" + key;
  }

  public boolean shouldCache(String type)
  {
    if (type == null) return false;
    return Settings.getInstance().getBoolProperty("workbench.db." + dbid + ".source.cache." + type.toLowerCase(), false);
  }

  public void clear()
  {
    cachedSources.clear();
  }
}
