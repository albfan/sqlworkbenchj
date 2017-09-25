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
package workbench.gui.editor;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ClipboardCleaner
{
  private final String toReplace;
  private final String replacements;

  public ClipboardCleaner()
  {
    //              ?      ?     ?      ?    ?     ?     «     »
    toReplace =    "\u2013\u2013\u2019\u2018\u201c\u201d\u00ab\u00bb";
    replacements = "--''\"\"''";
  }

  public String cleanupText(String input)
  {
    StringBuilder result = new StringBuilder(StringUtil.makePlainLinefeed(input));

    boolean inQuotes = false;
    for (int i=0; i < result.length(); i++)
    {
      char ch = result.charAt(i);
      if (ch == '\'')
      {
        inQuotes = !inQuotes;
      }

      if (!inQuotes)
      {
        int replacementIndex = toReplace.indexOf(ch);
        if (replacementIndex > -1)
        {
          char newValue = replacements.charAt(replacementIndex);
          result.setCharAt(i, newValue);
        }
      }
    }
    return result.toString();
  }
}
