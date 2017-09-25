/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.util.HashMap;
import java.util.Map;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class HstoreSupport
{

  /**
   * Create a display string for a hstore value.
   *
   * @param data                 the source data
   */
  public static String getDisplay(Map<String, String> data)
  {
    return toString(data, false);
  }

  /**
   * Create a hstore literal to be used in a SQL statement from a Map.
   *
   * @param data                 the source data
   * @return
   */
  public static String getLiteral(Map<String, String> data)
  {
    return toString(data, true);
  }

  private static String toString(Map<String, String> data, boolean includeCast)
  {
    if (CollectionUtil.isEmpty(data)) return "";

    int count = 0;

    StringBuilder result = new StringBuilder(data.size() * 20);


    if (includeCast) result.append('\'');

    for (Map.Entry<String, String> entry : data.entrySet())
    {
      if (count > 0) result.append(", ");

      appendHstoreValue(result, entry.getKey(), includeCast);

      result.append("=>");

      appendHstoreValue(result, entry.getValue(), includeCast);

      count ++;
    }

    if (includeCast)
    {
      result.append("'::hstore");
    }
    return result.toString();
  }

  private static void appendHstoreValue(StringBuilder sb, String val, boolean escapeSingleQuotes)
  {
    if (val == null)
    {
      sb.append("NULL");
      return;
    }

    sb.append('"');
    for (int pos = 0; pos < val.length(); pos++)
    {
      char ch = val.charAt(pos);
      if (escapeSingleQuotes && ch == '\'')
      {
        sb.append('\'');
      }
      else if (ch == '"' || ch == '\\')
      {
        sb.append('\\');
      }
      sb.append(ch);
    }
    sb.append('"');
  }


  /**
   * Parse a hstore literal into a Map.
   *
   * @param literal the hstore literal to parse
   *
   * @return the map from the hstore literal, null if the input was null
   */
  public static Map<String, String> parseLiteral(String literal)
  {
    Map<String, String> result = new HashMap<>();

    literal = StringUtil.trimToNull(literal);
    if (literal == null) return null;

    int len = literal.length();

    // we need at least 4 characters for a valid literal "k=>v"
    if (len < 4) return result;

    if (literal.toLowerCase().endsWith("::hstore"))
    {
      literal = literal.substring(0, literal.indexOf("::"));
    }
    len = literal.length();

    // not a valid literal, return an empty map
    if (literal.indexOf("=>") < 0) return result;

    // trim leading and trailing single quotes, in case the user entered them
    if (literal.charAt(0) == '\'' && literal.charAt(len - 1) == '\'')
    {
      literal = literal.substring(1, len - 1);
    }

    len = literal.length();

    boolean inQuotes = false;
    boolean wasQuoted = false;
    final int STATE_KEY = 1;
    final int STATE_VALUE = 2;
    int state = STATE_KEY;

    int lastStart = 0;
    int lastEnd = -1;

    char last = 0;
    String key = null;
    String val = null;

    for (int i=0; i < len; i++)
    {
      char c = literal.charAt(i);

      if (c == '"' && last != '\\')
      {
        if (inQuotes)
        {
          lastEnd = i;
        }
        else
        {
          lastStart = i + 1;
          wasQuoted = true;
        }
      }

      if (c == '"' && last != '\\')
      {
        inQuotes = !inQuotes;
      }

      if (state == STATE_KEY && c == '>' && last == '=' && !inQuotes)
      {
        key = literal.substring(lastStart, lastEnd == -1 ? i - 1 : lastEnd);
        state = STATE_VALUE;
        lastStart = i + 1;
        lastEnd = -1;
        wasQuoted = false;
      }
      else if ((!inQuotes && c == ',' && state == STATE_VALUE) || (i == len - 1))
      {
        int end = lastEnd;
        if (end < 0)
        {
          end = i == len - 1 ? len : i;
        }
        val = literal.substring(lastStart, end);
        if ("null".equalsIgnoreCase(val) && !wasQuoted)
        {
          val = null;
        }
        state = STATE_KEY;
        result.put(unescape(key), unescape(val));

        wasQuoted = false;
        lastEnd = 0;
        val = null;
        key = null;
      }
      last = c;
    }
    return result;
  }

  private static String unescape(String input)
  {
    if (input == null) return null;
    if (input.indexOf('\\') < 0) return input;
    StringBuilder sb = new StringBuilder(input.length());
    for (int i=0; i < input.length(); i++)
    {
      char c = input.charAt(i);
      if (c == '\\') continue;
      sb.append(c);
    }
    return sb.toString();
  }
}
