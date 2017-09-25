/*
 * DisplayLocale.java
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
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class DisplayLocale
  implements Comparable<DisplayLocale>
{
  private final WbLocale locale;
  private String display;
  private Locale displayLocale;

  public DisplayLocale()
  {
    this.locale = null;
  }

  public DisplayLocale(WbLocale l)
  {
    this.locale = l;
  }

  public Locale getLocale()
  {
    if (locale == null) return null;
    return locale.getLocale();
  }

  public void setDisplayLocale(Locale l)
  {
    displayLocale = l;
  }

  public boolean isEmpty()
  {
    return locale == null;
  }

  @Override
  public String toString()
  {
    if (display != null) return display;

    if (locale == null)
    {
      display = ResourceMgr.getString("LblDefaultIndicator");
    }
    else
    {
      StringBuilder s = new StringBuilder(20);
      String country = null;
      if (displayLocale == null)
      {
        s.append(locale.getLocale().getDisplayLanguage());
        country = locale.getLocale().getDisplayCountry();
      }
      else
      {
        s.append(locale.getLocale().getDisplayLanguage(displayLocale));
        country = locale.getLocale().getDisplayCountry(displayLocale);
      }
      if (!StringUtil.isEmptyString(country))
      {
        s.append(" (");
        s.append(country);
        s.append(')');
      }
      this.display = s.toString();
    }
    return this.display;
  }

  @Override
  public int compareTo(DisplayLocale other)
  {
    if (this.locale == null) return -1;
    if (other.locale == null) return 1;
    return this.locale.compareTo(other.locale);
  }

  @Override
  public boolean equals(Object other)
  {
    if (other == null) return false;

    if (other instanceof DisplayLocale)
    {
      DisplayLocale dl = (DisplayLocale)other;
      if (this.locale == null && dl.locale == null) return true;
      if (this.locale != null && dl.locale == null) return false;
      if (this.locale == null && dl.locale != null) return false;
      return locale.equals(dl.locale);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return locale.hashCode();
  }
}
