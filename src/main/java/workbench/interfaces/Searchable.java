/*
 * Searchable.java
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
package workbench.interfaces;

import java.util.List;

import workbench.gui.editor.SearchResult;


/**
 *
 * @author  Thomas Kellerer
 */
public interface Searchable
{
	int find();
	int findNext();
	int findPrevious();

  /**
   * Return all matches for the given search expression.
   *
   * contextLines controls the number of lines before and after a "hit" are returned. <br/>
   *
   * If contextLines = 1, 3 lines will be returned for each occurance.<br/>
   * If contextLines = 2, 5 lines will be returned for each occurance.<br/>
   *
   * @param expression     the expression to search for
   * @param ignoreCase     if true search is case insensitive
   * @param wholeWord      if false partial matches in a string are returned
   * @param isRegex        if true, <tt>expression</tt> is assumed to be a RegEx
   * @param contextLines   the number of lines before and after the search hit to return for each match.
   * @return
   */
  List<SearchResult> findAll(String expression, boolean ignoreCase, boolean wholeWord, boolean isRegex, int contextLines);
}
