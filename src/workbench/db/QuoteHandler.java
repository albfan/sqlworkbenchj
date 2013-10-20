/*
 * QuoteHandler.java
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
package workbench.db;

import java.util.regex.Matcher;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public interface QuoteHandler
{
	/**
	 * Check if the given name is already quoted.
	 * @param name  the SQL name to check
	 * @return true if it's quoted, false otherwise
	 */
	boolean isQuoted(String name);

	/**
	 * Removes the quotes from the SQL name if any are present.
	 * @param name  the SQL name to change
	 * @return the SQL name without quotes
	 */
	String removeQuotes(String name);

	/**
	 * Encloses the given object name in double quotes if necessary.
	 * @param name the SQL name to quote
	 */
	String quoteObjectname(String name);

	/**
	 * Checks if the given SQL name needs quoting.
	 *
	 * @param name  the SQL name to check
	 * @return true if it needs quoting, false otherwise
	 */
	boolean needsQuotes(String name);

	/**
	 * A QuoteHandler implementing ANSI quoting.
	 *
	 * @see SqlUtil#SQL_IDENTIFIER
	 * @see SqlUtil#quoteObjectname(java.lang.String)
	 * @see SqlUtil#removeObjectQuotes(java.lang.String)
	 */
	QuoteHandler STANDARD_HANDLER = new QuoteHandler()
	{
		@Override
		public boolean isQuoted(String name)
		{
			if (StringUtil.isEmptyString(name)) return false;
			name = name.trim();
			return name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"';
		}

		@Override
		public String removeQuotes(String name)
		{
			return SqlUtil.removeObjectQuotes(name);
		}

		@Override
		public String quoteObjectname(String name)
		{
			return SqlUtil.quoteObjectname(name, false, true, '"');
		}

		@Override
		public boolean needsQuotes(String name)
		{
			Matcher m = SqlUtil.SQL_IDENTIFIER.matcher(name);
			return !m.matches();
		}
	};
}
