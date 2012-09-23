/*
 * QuoteHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
	 * @see SqlUtil#removeQuoting(java.lang.String)
	 */
	public static final QuoteHandler STANDARD_HANDLER = new QuoteHandler()
	{
		@Override
		public boolean isQuoted(String name)
		{
			if (StringUtil.isEmptyString(name)) return false;
			if (name.trim().charAt(0) == '"') return true;
			return false;
		}

		@Override
		public String removeQuotes(String name)
		{
			return SqlUtil.removeQuoting(name);
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
