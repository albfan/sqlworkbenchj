/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.gui.editor.SearchAndReplace;

/**
 *
 * @author Thomas Kellerer
 */
public class Replacer
{
  private final String searchValue;
  private final String replacement;
  private boolean isRegex;
  private Pattern replacePattern;
  private boolean ignoreCase = true;

  public Replacer(String value, String replaceWith, boolean ignoreCase, boolean useRegex)
  {
    this.searchValue = value;
    this.replacement = replaceWith;
    this.isRegex = useRegex;
    this.ignoreCase = ignoreCase;
    initPattern();
  }

  private void initPattern()
  {
    String pattern = SearchAndReplace.getSearchExpression(searchValue, ignoreCase, false, isRegex);
    replacePattern = Pattern.compile(pattern);
  }

  public void setIgnoreCase(boolean flag)
  {
    this.ignoreCase = flag;
    initPattern();
  }

  public String replace(String input)
  {
    if (StringUtil.isEmptyString(input)) return input;
    Matcher matcher = replacePattern.matcher(input);
    return matcher.replaceAll(replacement);
  }

}
