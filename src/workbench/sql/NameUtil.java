/*
 * NameUtil.java
 *
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
package workbench.sql;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Utility methods that can be used in an XSLT Script.
 *
 * To use them, add a namespace for this class:
 *
 * <tt>xmlns:wb="workbench.sql.NameUtil"</tt>
 *
 * then inside the XSLT, this can be used like this:
 *
 * <code>
 * &lt;xsl:variable name="tablename" select="wb:camelCaseToSnake(table-name)"/&gt;
 * </code>
 *
 * @author Thomas Kellerer
 */
public class NameUtil
{
  private static final String INVALID_CHARS = "- .:\\/\"'!%&()=?+*";

  /**
   * Cleanup an identifier and convert CamelCase to snake_case
   *
   * @param input  the identifier to cleanup
   * @return a clean identifier in lowercase
   */
  public static String camelCaseToSnake(String input)
  {
    if (input == null) return "";
    input = SqlUtil.removeObjectQuotes(input);

    StringBuilder result = new StringBuilder(input.length() + 5);
    char current = 0;
    char previous = 0;
    for (int i=0; i < input.length(); i++)
    {
      current = input.charAt(i);
      if (Character.isUpperCase(current) && (Character.isLowerCase(previous) || Character.isWhitespace(previous)) && previous != '_')
      {
        result.append('_');
      }
      if (Character.isWhitespace(current) || (!Character.isDigit(current) && !Character.isLetter(current)) || INVALID_CHARS.indexOf(current) > -1)
      {
        current = '_';
      }
      previous = current;
      result.append(Character.toLowerCase(current));
    }
    return result.toString();
  }

  /**
   * Cleanup an identifier and optionally convert to lowercase
   *
   * @param input  the identifier to cleanup
   * @return a clean identifier
   */
   public static String cleanupIdentifier(String input, String lowerCase)
  {
    if (input == null) return "";
    boolean toLowerCase = StringUtil.stringToBool(lowerCase);
    input = SqlUtil.removeObjectQuotes(input);
    if (toLowerCase)
    {
      input = input.toLowerCase();
    }
    return SqlUtil.cleanupIdentifier(input);
  }

   public static String quoteIfNeeded(String input)
   {
     return SqlUtil.quoteObjectname(input, false, true, '"');
   }
}
