/*
 * FilteredProperties.java
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
