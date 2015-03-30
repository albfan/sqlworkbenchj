/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
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

import java.util.Collections;
import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.WbConnection;

import workbench.sql.parser.ParserType;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.formatter.WbSqlFormatter;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlParsingUtil
{
	private final SQLLexer lexer;

	private SqlParsingUtil()
	{
		lexer = SQLLexerFactory.createLexer();
	}

	public SqlParsingUtil(WbConnection conn)
	{
		this(ParserType.getTypeFromConnection(conn));
	}

	public SqlParsingUtil(ParserType type)
	{
		lexer = SQLLexerFactory.createLexer(type, "");
	}

	/**
	 * Thread safe singleton-instance.
	 * For a standard SQL parser, we keep a global instance for performance reasons.
	 *
	 */
	private static class LazyInstanceHolder
	{
		static final SqlParsingUtil instance = new SqlParsingUtil();
	}

	/**
	 *  Returns the SQL Verb for the given SQL string.
	 */
	public String getSqlVerb(String sql)
	{
		if (StringUtil.isEmptyString(sql)) return "";

		synchronized (lexer)
		{
			try
			{
				// Re-using an instance of SQLLexer is a lot faster than
				// creating a new one for each call of getSqlVerb
				lexer.setInput(sql);
				SQLToken t = lexer.getNextToken(false, false);
				if (t == null) return "";

				// The SQLLexer does not recognize @ as a keyword (which is basically
				// correct, but to support the Oracle style includes we'll treat it
				// as a keyword here.
				String v = t.getContents();
				if (v.charAt(0) == '@') return "@";

				return t.getContents();
			}
			catch (Exception e)
			{
				return "";
			}
		}
	}

	/**
	 * Extract the FROM part of a SQL statement. That is anything after the FROM
	 * up to (but not including) the WHERE, GROUP BY, ORDER BY, whichever comes first
	 */
	public String getFromPart(String sql)
	{
		synchronized (lexer)
		{
			return getFromPart(sql, lexer);
		}
	}

	/**
	 * Extract the FROM part of a SQL statement. That is anything after the FROM
	 * up to (but not including) the WHERE, GROUP BY, ORDER BY, whichever comes first
	 */
	public static String getFromPart(String sql, SQLLexer sqlLexer)
	{
		int fromPos = getKeywordPosition(Collections.singleton("FROM"), sql, 0, sqlLexer);
		if (fromPos == -1) return null;
		fromPos += "FROM".length();
		if (fromPos >= sql.length()) return null;
		int fromEnd = getKeywordPosition(WbSqlFormatter.FROM_TERMINAL, sql, fromPos, sqlLexer);
		if (fromEnd == -1)
		{
			return sql.substring(fromPos);
		}
		return sql.substring(fromPos, fromEnd);
	}


	/**
	 * Return the position of the FROM keyword in the given SQL
	 */
	public int getFromPosition(String sql)
	{
		Set<String> s = Collections.singleton("FROM");
		return getKeywordPosition(s, sql);
	}

	public int getJoinPosition(String sql)
	{
		return getKeywordPosition(SqlUtil.getJoinKeyWords(), sql);
	}

	public int getWherePosition(String sql)
	{
		Set<String> s = Collections.singleton("WHERE");
		return getKeywordPosition(s, sql);
	}

	public int getKeywordPosition(String keyword, CharSequence sql)
	{
		if (keyword == null) return -1;
		Set<String> s = Collections.singleton(keyword.toUpperCase());
		return getKeywordPosition(s, sql);
	}

	/**
	 * Returns the position of the first keyword found in the SQL input string.
	 *
	 * @param keywords the keywords to look for
	 * @param sql the SQL Statement to search.
	 * @return returns the position of the first keyword found
	 */
	public int getKeywordPosition(Set<String> keywords, CharSequence sql)
	{
		return getKeywordPosition(keywords, sql, 0);
	}

	/**
	 * Returns the position of the first keyword found in the SQL input string.
	 *
	 * @param keywords the keywords to look for
	 * @param sql      the SQL Statement to search.
	 * @param startPos start searching only after this position
	 * <p>
	 * @return returns the position of the first keyword found
	 */
	public int getKeywordPosition(Set<String> keywords, CharSequence sql, int startPos)
	{
		synchronized (lexer)
		{
			return getKeywordPosition(keywords, sql, startPos, lexer);
		}
	}

	public static int getKeywordPosition(Set<String> keywords, CharSequence sql, int startPos, SQLLexer sqlLexer)
	{
		if (StringUtil.isEmptyString(sql)) return -1;

		sqlLexer.setInput(sql);
		int pos = -1;
		try
		{
			SQLToken t = sqlLexer.getNextToken(false, false);
			int bracketCount = 0;
			while (t != null)
			{
				if (t.getCharBegin() < startPos)
				{
					t = sqlLexer.getNextToken(false, false);
					continue;
				}
				String value = t.getContents();
				if ("(".equals(value))
				{
					bracketCount ++;
				}
				else if (")".equals(value))
				{
					bracketCount --;
				}
				else if (bracketCount == 0)
				{
					if (keywords.contains(value))
					{
						pos = t.getCharBegin();
						break;
					}
				}
				t = sqlLexer.getNextToken(false, false);
			}
		}
		catch (Exception e)
		{
			pos = -1;
		}
		return pos;
	}

	/**
	 * Removes the SQL verb of this command. The verb is defined
	 * as the first "word" in the SQL string that is not a comment.
	 * @see #getSqlVerb(java.lang.String)
	 */
	public String stripVerb(String sql)
	{
		String result;
		try
		{
			synchronized (lexer)
			{
				lexer.setInput(sql);
				SQLToken t = lexer.getNextToken(false, false);
				int pos = -1;
				if (t != null) pos = t.getCharEnd();
				if (pos > -1) result = sql.substring(pos).trim();
				else result = "";
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlKeywordUtil.stripVerb()", "Error cleaning up SQL", e);
			result = "";
		}
		return result;
	}

	public static SqlParsingUtil getInstance(WbConnection conn)
	{
		if (conn == null)
		{
			return LazyInstanceHolder.instance;
		}
		return conn.getParsingUtil();
	}
}
