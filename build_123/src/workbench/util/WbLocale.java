/*
 * WbLocale.java
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

import java.util.Locale;

/**
 *
 * @author Thomas Kellerer
 */
public class WbLocale
  implements Comparable<WbLocale>
{
  private final Locale locale;

  public WbLocale(Locale l)
  {
    this.locale = l;
  }

  public Locale getLocale()
  {
    return locale;
  }

  @Override
  public String toString()
  {
    String lang = StringUtil.capitalize(locale.getDisplayLanguage(locale));
    return lang;
  }

  @Override
  public int compareTo(WbLocale other)
  {
    return this.toString().compareTo(other.toString());
  }

  @Override
  public boolean equals(Object other)
  {
    if (other == null) return false;
    if (locale == null) return false;
    if (other instanceof WbLocale)
    {
      return this.locale.equals(((WbLocale)other).locale);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return locale.hashCode();
  }
}
