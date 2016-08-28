/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.postgres;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * A wrapper class to get a proper toString() method for a Postgres hstore Map.
 *
 * @author Thomas Kellerer
 */
public class HstoreMap
  implements Map
{
  private final Map original;

  public HstoreMap(Map source)
  {
    original = source;
  }

  @Override
  public int size()
  {
    return original.size();
  }

  @Override
  public boolean isEmpty()
  {
    return original.isEmpty();
  }

  @Override
  public boolean containsKey(Object key)
  {
    return original.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value)
  {
    return original.containsValue(value);
  }

  @Override
  public Object get(Object key)
  {
    return original.get(key);
  }

  @Override
  public Object put(Object key, Object value)
  {
    return original.put(key, value);
  }

  @Override
  public Object remove(Object key)
  {
    return original.remove(key);
  }

  @Override
  public void putAll(Map m)
  {
    original.putAll(m);
  }

  @Override
  public void clear()
  {
    original.clear();
  }

  @Override
  public Set keySet()
  {
    return original.keySet();
  }

  @Override
  public Collection values()
  {
    return original.values();
  }

  @Override
  public Set entrySet()
  {
    return original.entrySet();
  }

  @Override
  public String toString()
  {
    return HstoreSupport.getDisplay(original);
  }

}
