/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dbobjects;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import workbench.resource.GuiSettings;

import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.RegExComparator;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class QuickFilterExpressionBuilder
{
  private final ColumnComparator comparator = new RegExComparator();


  public ColumnExpression buildExpression(String userInput, boolean assumeWildcards)
  {
    return buildExpression(userInput, "*", assumeWildcards);
  }

  public ColumnExpression buildExpression(String userInput, String searchColumn, boolean assumeWildcards)
  {
    String pattern = getPattern(userInput, assumeWildcards);
    ColumnExpression col = new ColumnExpression(searchColumn, comparator, pattern);
    col.setIgnoreCase(true);
    return col;
  }

	private String getPattern(String input, boolean assumeWildcards)
		throws PatternSyntaxException
	{
		if (GuiSettings.getUseRegexInQuickFilter())
		{
			Pattern.compile(input);
			// no exception, so everything is OK
			return input;
		}

		String regex;

    List<String> elements = StringUtil.stringToList(input,",", true, true, false, false);

    for (int i=0; i < elements.size(); i++)
    {
      String element = elements.get(i);
      if (assumeWildcards && !containsWildcards(element))
      {
        element = "*" + element + "*";
      }
      String regexElement = StringUtil.wildcardToRegex(element, true);
      elements.set(i, regexElement);
    }
    regex = StringUtil.listToString(elements, "|",false, '"');

		// Test the "translated" pattern, if that throws an exception let the caller handle it
		Pattern.compile(regex);

		return regex;
	}

	private boolean containsWildcards(String filter)
	{
		if (filter == null) return false;
		return filter.indexOf('%') > -1 || filter.indexOf('*') > -1;
	}

}
