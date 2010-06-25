/*
 * SqlUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.formatter.SqlFormatter;
import workbench.storage.ResultInfo;

/**
 * Methods for manipulating and analyzing SQL statements.
 *
 * @author Thomas Kellerer
 */
public class SqlUtil
{
	private static final Pattern SQL_IDENTIFIER = Pattern.compile("[a-zA-Z_][\\w\\$#@]*");

	private static final SQLLexer LEXER_INSTANCE = new SQLLexer("");

	private static class JoinKeywordsHolder
	{
		protected static final Set<String> JOIN_KEYWORDS = Collections.unmodifiableSet(
				CollectionUtil.caseInsensitiveSet(
					"JOIN", "INNER JOIN", "NATURAL JOIN", "LEFT JOIN", "LEFT OUTER JOIN", "RIGHT JOIN",
					"RIGHT OUTER JOIN", "CROSS JOIN", "FULL JOIN", "FULL OUTER JOIN")
				);
	}
	public static Set<String> getJoinKeyWords()
	{
		return JoinKeywordsHolder.JOIN_KEYWORDS;
	}

	private static class KnownTypesHolder
	{
		protected final static Set<String> KNOWN_TYPES =
			Collections.unmodifiableSet(CollectionUtil.hashSet(
			"INDEX", "TABLE", "PROCEDURE", "FUNCTION", "VIEW", "PACKAGE", "PACKAGE BODY",
			"SYNONYM", "SEQUENCE", "ALIAS", "TRIGGER", "DOMAIN", "ROLE", "CAST", "AGGREGATE",
			"TABLESPACE", "TYPE", "USER", "MATERIALIZED VIEW LOG", "MATERIALIZED VIEW", "SNAPSHOT",
			"FLASHBACK ARCHIVE"));
	}

	private static class TypesWithoutNamesHolder
	{
		protected final static Set<String> TYPES =
			Collections.unmodifiableSet(CollectionUtil.hashSet(
			"MATERIALIZED VIEW LOG", "SNAPSHOT LOG"));
	}

	public static Set<String> getTypesWithoutNames()
	{
		return TypesWithoutNamesHolder.TYPES;
	}

	public static Set<String> getKnownTypes()
	{
		return KnownTypesHolder.KNOWN_TYPES;
	}

	public static String escapeQuotes(String value)
	{
		if (value == null) return null;
		return value.replace("'" ,"''");
	}

	private static void resetLexerInstance(String sql)
		throws IOException
	{
		LEXER_INSTANCE.reset(new StringReader(sql),0,0);
	}
	/**
	 * Removes the SQL verb of this command. The verb is defined
	 * as the first "word" in the SQL string that is not a comment.
	 * @see #getSqlVerb(java.lang.String) 
	 */
	public static String stripVerb(String sql)
	{
		String result = "";
		try
		{
			synchronized (LEXER_INSTANCE)
			{
				resetLexerInstance(sql);
				SQLToken t = LEXER_INSTANCE.getNextToken(false, false);
				int pos = -1;
				if (t != null) pos = t.getCharEnd();
				if (pos > -1) result = sql.substring(pos).trim();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlUtil.stripVerb()", "Error cleaning up SQL", e);
		}
		return result;
	}


	public static String quoteObjectname(String object)
	{
		return quoteObjectname(object, false);
	}

	public static String quoteObjectname(String objectName, boolean quoteAlways)
	{
		if (objectName == null) return null;
		if (objectName.length() == 0) return "";
		if (objectName.charAt(0) == '"') return objectName;

		objectName = objectName.trim();
		if (objectName.charAt(0) == '[' && objectName.charAt(objectName.length() - 1) == ']')
		{
			// assume this is already quoted using SQL Server's idiotic non-standard way of using "quotes"
			return objectName;
		}

		boolean doQuote = quoteAlways;

		if (!quoteAlways)
		{
			Matcher m = SQL_IDENTIFIER.matcher(objectName);
			//doQuote = m.find() || Character.isDigit(aColname.charAt(0));;
			doQuote = !m.matches();
		}
		if (!doQuote) return objectName;
		StringBuilder col = new StringBuilder(objectName.length() + 2);
		col.append('"');
		col.append(objectName);
		col.append('"');
		return col.toString();
	}

	/**
	 * Extract the name of the created or dropped object.
	 *
	 * @return a structure that contains the type (e.g. TABLE, VIEW) and the name of the created object
	 *         null, if the SQL statement is not a DDL CREATE statement
	 *
	 */
	public static DdlObjectInfo getDDLObjectInfo(CharSequence sql)
	{
		SQLLexer lexer = new SQLLexer(sql);
		SQLToken t = lexer.getNextToken(false, false);

		if (t == null) return null;
		String verb = t.getContents();

		if (!verb.startsWith("CREATE") && !verb.equals("DROP") && !verb.equals("RECREATE")) return null;

		try
		{
			DdlObjectInfo info = new DdlObjectInfo();

			boolean typeFound = false;
			SQLToken token = lexer.getNextToken(false, false);
			while (token != null)
			{
				String c = token.getContents();
				if (getKnownTypes().contains(c))
				{
					typeFound = true;
					info.objectType = c;
					break;
				}
				token = lexer.getNextToken(false, false);
			}

			if (!typeFound) return null;

			// if a type was found we assume the next keyword is the name
			if (!getTypesWithoutNames().contains(info.objectType))
			{
				SQLToken name = lexer.getNextToken(false, false);
				if (name == null) return null;
				if (name.getContents().equals("IF NOT EXISTS") || name.getContents().equals("IF EXISTS"))
				{
					name = lexer.getNextToken(false, false);
					if (name == null) return null;
				}
				info.objectName = name.getContents();
			}
			return info;
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlUtil.getDDLObjectInfo()", "Error finding object info", e);
			return null;
		}
	}

	public static class DdlObjectInfo
	{
		public String objectType;
		public String objectName;

		public String toString()
		{
			return "Type: " + objectType + ", name: " + objectName;
		}

		public String getDisplayType()
		{
			return StringUtil.capitalize(objectType);
		}
	}

	/**
	 * Returns the type that is beeing created e.g. TABLE, VIEW, PROCEDURE
	 */
	public static String getCreateType(CharSequence sql)
	{
		DdlObjectInfo info = getDDLObjectInfo(sql);
		if (info == null) return null;
		return info.objectType;
	}

	/**
	 * If the given SQL is a DELETE [FROM] returns
	 * the table from which rows will be deleted
	 */
	public static String getDeleteTable(CharSequence sql)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken t = lexer.getNextToken(false, false);
			if (!t.getContents().equals("DELETE")) return null;
			t = lexer.getNextToken(false, false);
			// If the next token is not the FROM keyword (which is optional)
			// then it must be the table name.
			if (t == null) return null;
			if (!t.getContents().equals("FROM")) return t.getContents();
			t = lexer.getNextToken(false, false);
			if (t == null) return null;
			return t.getContents();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * If the given SQL is an INSERT INTO...
	 * returns the target table, otherwise null
	 */
	public static String getInsertTable(CharSequence sql)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken t = lexer.getNextToken(false, false);
			if (t == null || !t.getContents().equals("INSERT")) return null;
			t = lexer.getNextToken(false, false);
			if (t == null || !t.getContents().equals("INTO")) return null;
			t = lexer.getNextToken(false, false);
			if (t == null) return null;
			return t.getContents();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * If the given SQL command is an UPDATE command, return
	 * the table that is updated, otherwise return null;
	 */
	public static String getUpdateTable(CharSequence sql)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken t = lexer.getNextToken(false, false);
			if (t == null || !t.getContents().equals("UPDATE")) return null;
			t = lexer.getNextToken(false, false);
			if (t == null) return null;
			return t.getContents();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static String addSemicolon(String sql)
	{
		if (sql == null) return null;
		sql = sql.trim();
		if (sql.endsWith(";")) return sql;
		return sql + ";";
	}
	
	public static String trimSemicolon(String sql)
	{
		if (sql == null) return null;
		String newSql = sql.trim();
		if (newSql.length() == 0) return sql;
		if (newSql.endsWith(";"))
		{
			return newSql.substring(0, newSql.length() - 1);
		}
		return sql;
	}

	/**
	 *  Returns the SQL Verb for the given SQL string.
	 */
	public static String getSqlVerb(String sql)
	{
		if (StringUtil.isEmptyString(sql)) return "";

		synchronized (LEXER_INSTANCE)
		{
			try
			{
				// Re-using an instance of SQLLexer is a lot faster than
				// creating a new one for each call of getSqlVerb
				resetLexerInstance(sql);
				SQLToken t = LEXER_INSTANCE.getNextToken(false, false);
				if (t == null) return "";

				// The SQLLexer does not recognize @ as a keyword (which is basically
				// correct, but to support the Oracle style includes we'll treat it
				// as a keyword here.
				String v = t.getContents();
				if (v.charAt(0) == '@') return "@";

				return t.getContents().toUpperCase();
			}
			catch (Exception e)
			{
				return "";
			}
		}
	}

	/**
	 * Returns the columns for the result set defined by the passed
	 * query.
	 * This method will actually execute the given SQL query, but will
	 * not retrieve any rows (using setMaxRows(1).
	 */
	public static List<ColumnIdentifier> getResultSetColumns(String sql, WbConnection conn)
		throws SQLException
	{
		if (conn == null) return null;

		ResultInfo info = getResultInfoFromQuery(sql, conn);
		if (info == null) return null;

		int count = info.getColumnCount();
		ArrayList<ColumnIdentifier> result = new ArrayList<ColumnIdentifier>(count);
		for (int i = 0; i < count; i++)
		{
			result.add(info.getColumn(i));
		}
		return result;
	}

	public static ResultInfo getResultInfoFromQuery(String sql, WbConnection conn)
		throws SQLException
	{
		if (conn == null) return null;

		ResultSet rs = null;
		Statement stmt = null;
		final ResultInfo result;
		Savepoint sp = null;

		try
		{
			if (conn.getDbSettings().useSavePointForDML())
			{
				sp = conn.setSavepoint();
			}
			stmt = conn.createStatementForQuery();
			stmt.setMaxRows(1);
			rs = stmt.executeQuery(trimSemicolon(sql));
			ResultSetMetaData meta = rs.getMetaData();
			result = new ResultInfo(meta, conn);
			List tables = getTables(sql, false);
			if (tables.size() == 1)
			{
				String table = (String)tables.get(0);
				TableIdentifier tbl = new TableIdentifier(table);
				result.setUpdateTable(tbl);
			}
			conn.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			conn.rollback(sp);
			throw e;
		}
		finally
		{
			closeAll(rs, stmt);
		}
		return result;
	}

	private static String getTableDefinition(String table, boolean keepAlias)
	{
		if (keepAlias) return table;
		int pos = StringUtil.findFirstWhiteSpace(table);
		if (pos > -1) return table.substring(0, pos);
		return table;
	}

	/**
	 * Parse the given SQL SELECT query and return the columns defined
	 * in the column list. If the SQL string does not start with SELECT
	 * returns an empty List
	 * @param select the SQL String to parse
	 * @param includeAlias if false, the "raw" column names will be returned, otherwise
	 *       the column name including the alias (e.g. "p.name AS person_name"
	 * @return a List of String objecs.
	 */
	public static List<String> getSelectColumns(String select, boolean includeAlias)
	{
		List<String> result = new LinkedList<String>();
		try
		{
			SQLLexer lex = new SQLLexer(select);
			SQLToken t = lex.getNextToken(false, false);
			if (!"SELECT".equalsIgnoreCase(t.getContents())) return Collections.emptyList();
			t = lex.getNextToken(false, false);

			boolean ignoreFirstBracket = false;

			// Skip a potential DISTINCT at the beginning
			if (t.getContents().equals("DISTINCT") || t.getContents().equals("DISTINCT ON"))
			{
				// Postgres DISTINCT ON extension...
				ignoreFirstBracket = t.getContents().equals("DISTINCT ON");
				t = lex.getNextToken(false, false);
			}

			int lastColStart = t.getCharBegin();
			int bracketCount = 0;
			boolean nextIsCol = true;

			while (t != null)
			{
				String v = t.getContents();
				if ("(".equals(v))
				{
					bracketCount ++;
				}
				else if (")".equals(v))
				{
					bracketCount --;
				}
				else if ( (bracketCount == 0 || (ignoreFirstBracket && bracketCount == 1))
					&& (",".equals(v) || SqlFormatter.SELECT_TERMINAL.contains(v)))
				{
					String col = select.substring(lastColStart, t.getCharBegin());

					if (ignoreFirstBracket && bracketCount == 0 && col.trim().endsWith(")"))
					{
						// When dealing with Postgres' DISTINCT ON the last column
						// inside the brackets will be extracted including the bracket
						col = col.substring(0, col.length() - 1);
					}

					if (includeAlias)
					{
						col = col.trim();
					}
					else
					{
						col = striptColumnAlias(col);
					}
					result.add(col);

					if (SqlFormatter.SELECT_TERMINAL.contains(v))
					{
						nextIsCol = false;
						lastColStart = -1;
						break;
					}
					nextIsCol = true;
				}
				else if (nextIsCol)
				{
					lastColStart = t.getCharBegin();
					nextIsCol = false;
				}
				t = lex.getNextToken(false, false);
			}
			if (lastColStart > -1)
			{
				// no FROM was found, so assume it's a partial SELECT x,y,z
				String col = select.substring(lastColStart);
				if (includeAlias)
				{
					result.add(col.trim());
				}
				else
				{
					result.add(striptColumnAlias(col));
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlUtil.getColumnsFromSelect()", "Error parsing SELECT statement", e);
			return Collections.emptyList();
		}

		return result;
	}

	public static List<String> getFunctionParameters(String sql)
	{
		if (StringUtil.isBlank(sql)) return Collections.emptyList();
		List<String> params = CollectionUtil.arrayList();
		
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			// skip until the first opening bracket
			SQLToken t = lexer.getNextToken(false, false);
			while (t != null && !t.getContents().equals("("))
			{
				t = lexer.getNextToken(false, false);
			}
			if (t == null) return Collections.emptyList();

			int bracketCount = 0;

			int lastParamStart = (t.getCharEnd());
			
			while (t != null)
			{
				String text = t.getContents();
				if (text.equals("(") )
				{
					bracketCount ++;
				}
				else if (text.equals(")"))
				{
					if (bracketCount == 1)
					{
						int end = t.getCharBegin();
						if (end > lastParamStart)
						{
							String param = sql.substring(lastParamStart, end);
							params.add(param.trim());
						}
						break;
					}
					bracketCount--;
				}
				else if (bracketCount == 1)
				{
					if (text.equals(","))
					{
						int end = t.getCharBegin();
						if (end > lastParamStart)
						{
							String param = sql.substring(lastParamStart, end);
							params.add(param.trim());
						}
						lastParamStart = t.getCharEnd();
					}
				}
				t = lexer.getNextToken(true, false);
			}
		}
		catch (Exception e)
		{
			return Collections.emptyList();
		}
		return params;
	}
	
	public static String striptColumnAlias(String expression)
	{
		if (expression == null) return null;

		List elements = StringUtil.stringToList(expression, " ", true, true, true);

		return (String)elements.get(0);
	}

	public static List<String> getTables(String aSql)
	{
		return getTables(aSql, false);
	}

	/**
	 * Returns a List of tables defined in the SQL query. If the
	 * query is not a SELECT query the result is undefined
	 */
	public static List<String> getTables(String sql, boolean includeAlias)
	{
		String from = getFromPart(sql);
		if (StringUtil.isBlank(from)) return Collections.emptyList();
		List<String> result = new LinkedList<String>();
		try
		{
			SQLLexer lex = new SQLLexer(from);
			SQLToken t = lex.getNextToken(false, false);

			boolean collectTable = true;
			StringBuilder currentTable = new StringBuilder();
			int bracketCount = 0;
			boolean subSelect = false;
			int subSelectBracketCount = -1;

			while (t != null)
			{
				String s = t.getContents();

				if (s.equals("SELECT") && bracketCount > 0)
				{
					subSelect = true;
					subSelectBracketCount = bracketCount;
				}

				if ("(".equals(s))
				{
					bracketCount ++;
				}
				else if (")".equals(s))
				{
					if (subSelect && bracketCount == subSelectBracketCount)
					{
						subSelect = false;
					}
					bracketCount --;
					t = lex.getNextToken(false, false);
					continue;
				}

				if (!subSelect)
				{
					if (getJoinKeyWords().contains(s))
					{
						collectTable = true;
						if (currentTable.length() > 0)
						{
							result.add(getTableDefinition(currentTable.toString(), includeAlias));
							currentTable = new StringBuilder();
						}
					}
					else if (",".equals(s))
					{
							collectTable = true;
							result.add(getTableDefinition(currentTable.toString(), includeAlias));
							currentTable = new StringBuilder();
					}
					else if ("ON".equals(s))
					{
						collectTable = false;
						result.add(getTableDefinition(currentTable.toString(), includeAlias));
						currentTable = new StringBuilder();
					}
					else if (collectTable && !s.equals("("))
					{
						int size = currentTable.length();
						if (size > 0 && !s.equals(".") && currentTable.charAt(size-1) != '.') currentTable.append(' ');
						currentTable.append(s);
					}
				}
				t = lex.getNextToken(false, false);
			}

			if (currentTable.length() > 0)
			{
				result.add(getTableDefinition(currentTable.toString(), includeAlias));
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlUtil.getTable()", "Error parsing sql", e);
		}
		return result;
	}

	/**
	 * Extract the FROM part of a SQL statement. That is anything after the FROM
	 * up to (but not including) the WHERE, GROUP BY, ORDER BY, whichever comes first
	 */
	public static String getFromPart(String sql)
	{
		int fromPos = getFromPosition(sql);
		if (fromPos == -1) return null;
		fromPos += "FROM".length();
		if (fromPos >= sql.length()) return null;
		int fromEnd = getKeywordPosition(SqlFormatter.FROM_TERMINAL, sql);
		if (fromEnd == -1)
		{
			return sql.substring(fromPos);
		}
		return sql.substring(fromPos, fromEnd);
	}

	/**
	 * Return the position of the FROM keyword in the given SQL
	 */
	public static int getFromPosition(String sql)
	{
		Set<String> s = new HashSet<String>();
		s.add("FROM");
		return getKeywordPosition(s, sql);
	}

	public static int getWherePosition(String sql)
	{
		Set<String> s = new HashSet<String>();
		s.add("WHERE");
		return getKeywordPosition(s, sql);
	}

	public static int getKeywordPosition(String keyword, CharSequence sql)
	{
		if (keyword == null) return -1;
		Set<String> s = new HashSet<String>();
		s.add(keyword.toUpperCase());
		return getKeywordPosition(s, sql);
	}

	public static int getKeywordPosition(Set<String> keywords, CharSequence sql)
	{
		int pos = -1;
		try
		{
			SQLLexer lexer = new SQLLexer(sql);

			SQLToken t = lexer.getNextToken(false, false);
			int bracketCount = 0;
			while (t != null)
			{
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

				t = lexer.getNextToken(false, false);
			}
		}
		catch (Exception e)
		{
			pos = -1;
		}
		return pos;
	}

	public static String makeCleanSql(String aSql, boolean keepNewlines)
	{
		return makeCleanSql(aSql, keepNewlines, false, '\'');
	}

	public static String makeCleanSql(String aSql, boolean keepNewlines, boolean keepComments)
	{
		return makeCleanSql(aSql, keepNewlines, keepComments, '\'');
	}

	/**
	 *	Replaces all white space characters with a single space (But not inside
	 *	string literals) and removes -- style and Java style comments
	 *	@param aSql The sql script to "clean out"
	 *  @param keepNewlines if true, newline characters (\n) are kept
	 *  @param keepComments if true, comments (single line, block comments) are kept
	 *  @param quote The quote character
	 *	@return String
	 */
	public static String makeCleanSql(String aSql, boolean keepNewlines, boolean keepComments, char quote)
	{
		if (aSql == null) return null;
		aSql = aSql.trim();
		int count = aSql.length();
		if (count == 0) return aSql;
		boolean inComment = false;
		boolean inQuotes = false;
		boolean lineComment = false;

		StringBuilder newSql = new StringBuilder(count);

		char last = ' ';

		for (int i=0; i < count; i++)
		{
			char c = aSql.charAt(i);

			if (c == quote)
			{
				inQuotes = !inQuotes;
			}

			if (inQuotes && (!inComment && !keepComments))
			{
				newSql.append(c);
				last = c;
				continue;
			}

			if ((last == '\n' || last == '\r' || i == 0 ) && (c == '#'))
			{
				lineComment = true;
			}

			if (!(inComment || lineComment) || keepComments)
			{
				if (!keepComments && c == '/' && i < count - 1 && aSql.charAt(i+1) == '*')
				{
					inComment = true;
					i++;
				}
				else if (!keepComments && c == '-' && i < count - 1 && aSql.charAt(i+1) == '-')
				{
					// ignore rest of line for -- style comments
					while (c != '\n' && i < count - 1)
					{
						i++;
						c = aSql.charAt(i);
					}
				}
				else
				{
					if ((c == '\n' || c == '\r') && !keepNewlines)
					{
						// only replace the \n, \r are simply removed
						// thus replacing \r\n with only one space
						if (c == '\n') newSql.append(' ');
					}
					else if (c != '\n' && (c < 32 || (c > 126 && c < 145) || c == 255))
					{
						newSql.append(' ');
					}
					else
					{
						newSql.append(c);
					}
				}
			}
			else
			{
				if ( c == '*' && i < count - 1 && aSql.charAt(i+1) == '/')
				{
					inComment = false;
					i++;
				}
				else if (c == '\n' || c == '\r' && lineComment)
				{
					lineComment = false;
				}
			}
			last = c;
		}
		String s = newSql.toString().trim();
		if (s.endsWith(";")) s = s.substring(0, s.length() - 1).trim();
		return s;
	}

	public static boolean isSelectIntoNewTable(Pattern testPattern, String sql)
	{
		if (testPattern == null) return false;
		if (sql == null || sql.length() == 0) return false;
		int pos = SqlUtil.getKeywordPosition("SELECT", sql);
		if (pos > -1)
		{
			sql = sql.substring(pos);
		}
		Matcher m = testPattern.matcher(sql);
		return m.find();
	}

	/**
	 * returns true if the passed data type (from java.sql.Types)
	 * indicates a data type which can hold numeric values with
	 * decimals
	 */
	public static boolean isDecimalType(int aSqlType, int aScale, int aPrecision)
	{
		if (aSqlType == Types.DECIMAL ||
		    aSqlType == Types.DOUBLE ||
		    aSqlType == Types.FLOAT ||
		    aSqlType == Types.NUMERIC ||
		    aSqlType == Types.REAL)
		{
			return (aScale > 0);
		}
		else
		{
			return false;
		}
	}

	/**
	 * returns true if the passed JDBC data type (from java.sql.Types)
	 * indicates a data type which maps to a integer type
	 */
	public static boolean isIntegerType(int aSqlType)
	{
		return (aSqlType == Types.BIGINT ||
		        aSqlType == Types.INTEGER ||
		        aSqlType == Types.SMALLINT ||
		        aSqlType == Types.TINYINT);
	}

	/**
	 * Returns true if the given JDBC type indicates some kind of
	 * character data (including CLOBs)
	 */
	public static boolean isCharacterType(int aSqlType)
	{
		return (aSqlType == Types.VARCHAR ||
		        aSqlType == Types.CHAR ||
		        aSqlType == Types.CLOB ||
		        aSqlType == Types.LONGVARCHAR ||
		        aSqlType == Types.NVARCHAR ||
		        aSqlType == Types.NCHAR ||
		        aSqlType == Types.LONGNVARCHAR ||
		        aSqlType == Types.NCLOB);
	}

	/**
	 * Returns true if the given JDBC type is a CHAR or VARCHAR type
	 * @param aSqlType
	 */
	public static boolean isCharacterTypeWithLength(int aSqlType)
	{
		return (aSqlType == Types.VARCHAR ||
		        aSqlType == Types.CHAR ||
		        aSqlType == Types.NVARCHAR ||
		        aSqlType == Types.NCHAR);
	}

	/**
	 * 	Returns true if the passed datatype (from java.sql.Types)
	 *  can hold a numeric value (either with or without decimals)
	 */
	public static boolean isNumberType(int aSqlType)
	{
		return (aSqlType == Types.BIGINT ||
		        aSqlType == Types.INTEGER ||
		        aSqlType == Types.DECIMAL ||
		        aSqlType == Types.DOUBLE ||
		        aSqlType == Types.FLOAT ||
		        aSqlType == Types.NUMERIC ||
		        aSqlType == Types.REAL ||
		        aSqlType == Types.SMALLINT ||
		        aSqlType == Types.TINYINT);
	}

	public static boolean isDateType(int aSqlType)
	{
		return (aSqlType == Types.DATE || aSqlType == Types.TIMESTAMP);
	}

	public static boolean isClobType(int aSqlType)
	{
		return (aSqlType == Types.CLOB || aSqlType == Types.NCLOB);
	}

	public static boolean isClobType(int aSqlType, String dbmsType, DbSettings dbInfo)
	{
		if (dbInfo != null && dbInfo.isClobType(dbmsType)) return true;
		boolean treatLongVarcharAsClob = (dbInfo == null ? false : dbInfo.longVarcharIsClob());
		return isClobType(aSqlType, treatLongVarcharAsClob);
	}

	public static boolean isXMLType(int type)
	{
		return (type == Types.SQLXML);
	}
	
	public static boolean isClobType(int aSqlType, boolean treatLongVarcharAsClob)
	{
		if (!treatLongVarcharAsClob) return isClobType(aSqlType);

		return (aSqlType == Types.CLOB ||
			      aSqlType == Types.NCLOB ||
			      aSqlType == Types.LONGVARCHAR ||
						aSqlType == Types.LONGNVARCHAR
						);
	}

	public static boolean isBlobType(int aSqlType)
	{
		return (aSqlType == Types.BLOB ||
		        aSqlType == Types.BINARY ||
		        aSqlType == Types.LONGVARBINARY ||
		        aSqlType == Types.VARBINARY);
	}

	/**
	 *	Convenience method to close a ResultSet without a possible
	 *  SQLException
	 */
	public static void closeResult(ResultSet rs)
	{
		if (rs == null) return;
		try { rs.close(); } catch (Throwable th) {}
	}

	/**
	 *	Convenience method to close a Statement without a possible
	 *  SQLException
	 */
	public static void closeStatement(Statement stmt)
	{
		if (stmt == null) return;
		try { stmt.close(); } catch (Throwable th) {}
	}

	/**
	 *	Convenience method to close a ResultSet and a Statement without
	 *  a possible SQLException
	 */
	public static void closeAll(ResultSet rs, Statement stmt)
	{
		closeResult(rs);
		closeStatement(stmt);
	}

	public static String getTypeName(int sqlType)
	{
		switch (sqlType)
		{
			case Types.ARRAY:
				return "ARRAY";

			case Types.BIGINT:
				return "BIGINT";

			case Types.BINARY:
				return "BINARY";

			case Types.BIT:
				return "BIT";

			case Types.BLOB:
				return "BLOB";

			case Types.BOOLEAN:
				return "BOOLEAN";

			case Types.CHAR:
				return "CHAR";

			case Types.CLOB:
				return "CLOB";

			case Types.DATALINK:
				return "DATALINK";

			case Types.DATE:
				return "DATE";

			case Types.DECIMAL:
				return "DECIMAL";

			case Types.DISTINCT:
				return "DISTINCT";

			case Types.DOUBLE:
				return "DOUBLE";

			case Types.FLOAT:
				return "FLOAT";

			case Types.INTEGER:
				return "INTEGER";

			case Types.JAVA_OBJECT:
				return "JAVA_OBJECT";

			case Types.LONGVARBINARY:
				return "LONGVARBINARY";

			case Types.LONGVARCHAR:
				return "LONGVARCHAR";

			case Types.NULL:
				return "NULL";

			case Types.NUMERIC:
				return "NUMERIC";

			case Types.OTHER:
				return "OTHER";

			case Types.REAL:
				return "REAL";

			case Types.REF:
				return "REF";

			case Types.SMALLINT:
				return "SMALLINT";

			case Types.STRUCT:
				return "STRUCT";

			case Types.TIME:
				return "TIME";

			case Types.TIMESTAMP:
				return "TIMESTAMP";

			case Types.TINYINT:
				return "TINYINT";

			case Types.VARBINARY:
				return "VARBINARY";

			case Types.VARCHAR:
				return "VARCHAR";

			case Types.NCLOB:
				return "NCLOB";

			case Types.SQLXML:
				return "SQLXML";

			case Types.NCHAR:
				return "NCHAR";

			case Types.NVARCHAR:
				return "NVARCHAR";

			case Types.LONGNVARCHAR:
				return "LONGNVARCHAR";

			case Types.ROWID:
				return "ROWID";

			default:
				return "UNKNOWN";
		}
	}

	/**
	 * Construct the SQL display name for the given SQL datatype.
	 * This is used when re-recreating the source for a table
	 */
	public static String getSqlTypeDisplay(String typeName, int sqlType, int size, int digits)
	{
		String display = typeName;

		switch (sqlType)
		{
			case Types.VARCHAR:
			case Types.CHAR:
			case Types.NVARCHAR:
			case Types.NCHAR:
				// Postgres' text datatype does not have a size parameter
				if ("text".equals(typeName)) return "text";

				if (size > 0)
				{
					display = typeName + "(" + size + ")";
				}
				break;

			case Types.DOUBLE:
			case Types.REAL:
				display = typeName;
				break;

			case Types.FLOAT:
				if (size > 0)
				{
					display = typeName + "(" + size + ")";
				}
				else
				{
					display = typeName;
				}
				break;

			case Types.DECIMAL:
			case Types.NUMERIC:
				if ("money".equalsIgnoreCase(typeName)) // SQL Server
				{
					display = typeName;
				}
				else if ((typeName.indexOf('(') == -1))
				{
					if (digits > 0 && size > 0)
					{
						display = typeName + "(" + size + "," + digits + ")";
					}
					else if (size <= 0 && digits > 0)
					{
						display = typeName + "(" + digits + ")";
					}
					else if (size > 0 && digits <= 0)
					{
						display = typeName + "(" + size + ")";
					}
				}
				break;

			case Types.OTHER:
				if (typeName.toUpperCase().startsWith("NVARCHAR"))
				{
					display = typeName + "(" + size + ")";
				}
				else if ("NCHAR".equalsIgnoreCase(typeName))
				{
					display = typeName + "(" + size + ")";
				}
				else if ("UROWID".equalsIgnoreCase(typeName))
				{
					display = typeName + "(" + size + ")";
				}
				else if ("RAW".equalsIgnoreCase(typeName))
				{
					display = typeName + "(" + size + ")";
				}
				break;

			default:
				display = typeName;
				break;
		}
		return display;
	}

	public static CharSequence getWarnings(WbConnection con, Statement stmt)
	{
		try
		{
			// some DBMS return warnings on the connection rather then on the
			// statement. We need to check them here as well. Then some of
			// the DBMS return the same warnings on the Statement AND the
			// Connection object (and MySQL returns an error as the Exception itself
			// and additionally as a warning on the Statement...)
			// For this we keep a list of warnings which have been added
			// from the statement. They will not be added when the Warnings from
			// the connection are retrieved
			Set<String> added = new HashSet<String>();
			StringBuilder msg = null;
			String s = null;
			SQLWarning warn = (stmt == null ? null : stmt.getWarnings());
			boolean hasWarnings = warn != null;
			int count = 0;

			while (warn != null)
			{
				count ++;
				s = warn.getMessage();
				if (s != null && s.length() > 0)
				{
					msg = append(msg, s);
					if (!s.endsWith("\n")) msg.append('\n');
					added.add(s);
				}
				if (count > 15) break; // prevent endless loop
				warn = warn.getNextWarning();
			}

			warn = (con == null ? null : con.getSqlConnection().getWarnings());
			hasWarnings = hasWarnings || (warn != null);
			count = 0;
			while (warn != null)
			{
				s = warn.getMessage();
				// Some JDBC drivers duplicate the warnings between
				// the statement and the connection.
				// This is to prevent adding them twice
				if (!added.contains(s))
				{
					msg = append(msg, s);
					if (!s.endsWith("\n")) msg.append('\n');
				}
				if (count > 25) break; // prevent endless loop
				warn = warn.getNextWarning();
			}

			// make sure the warnings are cleared from both objects!
			con.clearWarnings();
			stmt.clearWarnings();
			StringUtil.trimTrailingWhitespace(msg);
			return msg;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private static StringBuilder append(StringBuilder msg, CharSequence s)
	{
		if (msg == null) msg = new StringBuilder(100);
		msg.append(s);
		return msg;
	}

	public static String buildExpression(WbConnection conn, String catalog, String schema, String name)
	{
		StringBuilder result = new StringBuilder(30);
		DbMetadata meta = conn.getMetadata();
		if (!StringUtil.isEmptyString(catalog) && !conn.getMetadata().ignoreCatalog(catalog))
		{
			result.append(meta.quoteObjectname(catalog));
			result.append('.');
		}
		if (!StringUtil.isEmptyString(schema) && !conn.getMetadata().ignoreSchema(schema))
		{
			result.append(meta.quoteObjectname(schema));
			result.append('.');
		}
		result.append(meta.quoteObjectname(name));
		return result.toString();
	}

	public static boolean isMultiLineColumn(ColumnIdentifier column)
	{
		if (column == null) return false;

		int charLength = 0;
		int sqlType = column.getDataType();

		if (isClobType(sqlType) || isXMLType(sqlType))
		{
			charLength = Integer.MAX_VALUE;
		}
		else if (isCharacterType(sqlType))
		{
			charLength = column.getColumnSize();
		}
		else
		{
			return false;
		}

		int sizeThreshold = GuiSettings.getMultiLineThreshold();
		return charLength >= sizeThreshold;
	}

}
