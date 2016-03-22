/*
 * RegexModifier.java
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
package workbench.db.importer.modifier;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexModifier
  implements ImportValueModifier
{
  public Map<ColumnIdentifier, RegexDef> limits = new HashMap<ColumnIdentifier, RegexDef>();

  @Override
  public int getSize()
  {
    return limits.size();
  }

  /**
   * Define regex replacement for a column.
   * An existing mapping for that column will be overwritten.
   *
   * @param col the column for which to apply the substring
   * @param regex the regular expression to search for
   * @param replacement the replacement for the regex
   */
  public void addDefinition(ColumnIdentifier col, String regex, String replacement)
    throws PatternSyntaxException
  {
    RegexDef def = new RegexDef(regex, replacement);
    this.limits.put(col.createCopy(), def);
  }

  @Override
  public String modifyValue(ColumnIdentifier col, String value)
  {
    if (value == null) return null;
    RegexDef def = this.limits.get(col);
    if (def != null)
    {
      Matcher m = def.regex.matcher(value);
      return m.replaceAll(def.replacement);
    }
    return value;
  }

  private static class RegexDef
  {
    Pattern regex;
    String replacement;

    public RegexDef(String exp, String repl)
      throws PatternSyntaxException
    {
      regex = Pattern.compile(exp);
      replacement = repl;
    }
  }
}
