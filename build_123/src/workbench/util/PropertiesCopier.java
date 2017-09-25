/*
 * PropertiesCopier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import java.util.Enumeration;
import java.util.Properties;

/**
 *
 * @author Thomas Kellerer
 */
public class PropertiesCopier
{

  public void copyToSystem(Properties source)
  {
    copy(source, System.getProperties());
  }

  public void copy(Properties source, Properties target)
  {
    if (source == null || target == null) return;
    Enumeration keys = source.propertyNames();
    while (keys.hasMoreElements())
    {
      String key = (String)keys.nextElement();
      String value = source.getProperty(key);
      target.setProperty(key, value);
    }
  }

  public void removeFromSystem(Properties source)
  {
    remove(source, System.getProperties());
  }

  /**
   * Removes all properties from target that are present in source.
   */
  public void remove(Properties source, Properties target)
  {
    if (source == null || target == null) return;
    Enumeration keys = source.propertyNames();
    while (keys.hasMoreElements())
    {
      String key = (String)keys.nextElement();
      target.remove(key);
    }
  }

}
