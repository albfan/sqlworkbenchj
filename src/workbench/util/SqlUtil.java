/*
 * SqlUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
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
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.formatter.SqlFormatter;
import workbench.storage.ResultInfo;

/**
 * Methods for manipulating and analyzing SQL statements.
 */
public class SqlUtil
{
	//private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[$:\\\\/ \\.;,]");
	private static final Pattern SQL_IDENTIFIER = Pattern.compile("[a-zA-Z_][\\w\\$#@]*");
	
	/**
	 * Removes the SQL verb of this command. The verb is defined
	 * as the first "word" in the SQL string that is not a comment.
	 * 
	 * @see #getSqlVerb(CharSequence)
	 */
	public static String stripVerb(String sql)
	{
		String result = "";
		try
		{
			SQLLexer l = new SQLLexer(sql);
			SQLToken t = l.getNextToken(false, false);
			int pos = -1;
			if (t != null) pos = t.getCharEnd();
			if (pos > -1) result = sql.substring(pos).trim();
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlCommand.stripVerb()", "Error cleaning up SQL", e);
		}
		return result;
	}

	
	public static String quoteObjectname(String object)
	{
		return quoteObjectname(object, false);
	}
	
	public static String quoteObjectname(String aColname, boolean quoteAlways)
	{
		if (aColname == null) return null;
		if (aColname.length() == 0) return "";
		aColname = aColname.trim();
		
		boolean doQuote = quoteAlways;
		
		if (!quoteAlways)
		{
			Matcher m = SQL_IDENTIFIER.matcher(aColname);
			//doQuote = m.find() || Character.isDigit(aColname.charAt(0));;
			doQuote = !m.matches();
		}
		if (!doQuote) return aColname;
		StringBuilder col = new StringBuilder(aColname.length() + 2);
		col.append('"');
		col.append(aColname);
		col.append('"');
		return col.toString();
	}

	/**
	 * Returns the type that is beeing created e.g. TABLE, VIEW, PROCEDURE
	 */
	public static String getCreateType(CharSequence sql)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken t = lexer.getNextToken(false, false);
			String v = t.getContents();
			if (!v.equals("CREATE") && !v.equals("RECREATE") && !v.equals("CREATE OR REPLACE")) return null;
			SQLToken type = lexer.getNextToken(false, false);
			if (type == null) return null;
			
			// check for CREATE FORCE VIEW 
			if (type.getContents().equals("FORCE"))
			{
				SQLToken t2 = lexer.getNextToken(false, false);
				if (t2 == null) return null;
				return t2.getContents();
			}
			return type.getContents();
		}
		catch (Exception e)
		{
			return null;
		}
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
	
	/**
	 *  Returns the SQL Verb for the given SQL string.
	 */
	public static String getSqlVerb(CharSequence sql)
	{
		if (StringUtil.isEmptyString(sql)) return "";
		
		SQLLexer l = new SQLLexer(sql);
		try
		{
			SQLToken t = l.getNextToken(false, false);
			
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
		ResultInfo result = null;

		try
		{
			stmt = conn.createStatementForQuery();
			stmt.setMaxRows(1);
			rs = stmt.executeQuery(sql);
			ResultSetMetaData meta = rs.getMetaData();
			result = new ResultInfo(meta, conn);
			List tables = getTables(sql, false);
			if (tables.size() == 1)
			{
				String table = (String)tables.get(0);
				TableIdentifier tbl = new TableIdentifier(table);
				result.setUpdateTable(tbl);
			}
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
				else if (bracketCount == 0 && (",".equals(v) || SqlFormatter.SELECT_TERMINAL.contains(v)))
				{
					String col = select.substring(lastColStart, t.getCharBegin());
					if (includeAlias)
					{
						result.add(col.trim());
					}
					else
					{
						result.add(striptColumnAlias(col));
					}
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
	
	public static String striptColumnAlias(String expression)
	{
		if (expression == null) return null;
		
		List elements = StringUtil.stringToList(expression, " ", true, true, true);
		
		return (String)elements.get(0);
	}
	
	public static List getTables(String aSql)
	{
		return getTables(aSql, false);
	}

	public static final Set<String> JOIN_KEYWORDS = new HashSet<String>(6);
	static
	{
			JOIN_KEYWORDS.add("INNER JOIN");
			JOIN_KEYWORDS.add("LEFT JOIN");
			JOIN_KEYWORDS.add("RIGHT JOIN");
			JOIN_KEYWORDS.add("LEFT OUTER JOIN");
			JOIN_KEYWORDS.add("RIGHT OUTER JOIN");
			JOIN_KEYWORDS.add("CROSS JOIN");
			JOIN_KEYWORDS.add("FULL JOIN");
			JOIN_KEYWORDS.add("FULL OUTER JOIN");
	}
	
	/**
	 * Returns a List of tables defined in the SQL query. If the 
	 * query is not a SELECT query the result is undefined
	 */
	public static List<String> getTables(String sql, boolean includeAlias)
	{
		String from = SqlUtil.getFromPart(sql);
		if (from == null || from.trim().length() == 0) return Collections.emptyList();
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
						if (JOIN_KEYWORDS.contains(s))
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
		return makeCleanSql(aSql, keepNewlines, '\'');
	}


	public static String makeCleanSql(String aSql, boolean keepNewlines, char quote)
	{
		return makeCleanSql(aSql, keepNewlines, false, quote);
	}

	/**
	 *	Replaces all white space characters with ' ' (But not inside
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

		// remove trailing semicolon
		if (aSql.charAt(count - 1) == ';') count --;
		char last = ' ';

		for (int i=0; i < count; i++)
		{
			char c = aSql.charAt(i);

			if (c == quote)
			{
				inQuotes = !inQuotes;
			}
			
			if (inQuotes)
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
				if ( c == '/' && i < count - 1 && aSql.charAt(i+1) == '*')
				{
					inComment = true;
					i++;
				}
				else if (c == '-' && i < count - 1 && aSql.charAt(i+1) == '-')
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
		if (s.endsWith(";")) s = s.substring(0, s.length() - 1);
		return s;
	}

	/**
	 * returns true if the passed data type (from java.sql.Types)
	 * indicates a data type which can hold numeric values with
	 * decimals
	 */
	public static final boolean isDecimalType(int aSqlType, int aScale, int aPrecision)
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
	public static final boolean isIntegerType(int aSqlType)
	{
		return (aSqlType == Types.BIGINT ||
		        aSqlType == Types.INTEGER ||
		        aSqlType == Types.SMALLINT ||
		        aSqlType == Types.TINYINT);
	}

	/**
	 * Returns true if the given JDBC type maps to the String class. This
	 * returns fals for CLOB data.
	 */
	public static final boolean isStringType(int aSqlType)
	{
		return (aSqlType == Types.VARCHAR || 
		        aSqlType == Types.CHAR ||
		        aSqlType == Types.LONGVARCHAR);
	}
	
	/**
	 * Returns true if the given JDBC type indicates some kind of 
	 * character data (including CLOBs)
	 */
	public static final boolean isCharacterType(int aSqlType)
	{
		return (aSqlType == Types.VARCHAR || 
		        aSqlType == Types.CHAR ||
		        aSqlType == Types.CLOB ||
		        aSqlType == Types.LONGVARCHAR);
	}
	
	/**
	 * 	Returns true if the passed datatype (from java.sql.Types)
	 *  can hold a numeric value (either with or without decimals)
	 */
	public static final boolean isNumberType(int aSqlType)
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
	
	public static final boolean isDateType(int aSqlType)
	{
		return (aSqlType == Types.DATE || aSqlType == Types.TIMESTAMP);
	}

	public static final boolean isClobType(int aSqlType)
	{
		return (aSqlType == Types.CLOB);
	}
	
	public static final boolean isClobType(int aSqlType, DbSettings dbInfo)
	{
		if (dbInfo == null || !dbInfo.longVarcharIsClob()) return isClobType(aSqlType);
		return (aSqlType == Types.CLOB || aSqlType == Types.LONGVARCHAR);
	}
	
	public static final boolean isBlobType(int aSqlType)
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
		try { rs.close(); } catch (Throwable th) {}
	}

	/**
	 *	Convenience method to close a Statement without a possible
	 *  SQLException
	 */
	public static void closeStatement(Statement stmt)
	{
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

	public static final String getTypeName(int aSqlType)
	{
		if (aSqlType == Types.ARRAY)
			return "ARRAY";
		else if (aSqlType == Types.BIGINT)
			return "BIGINT";
		else if (aSqlType == Types.BINARY)
			return "BINARY";
		else if (aSqlType == Types.BIT)
			return "BIT";
		else if (aSqlType == Types.BLOB)
			return "BLOB";
		else if (aSqlType == Types.BOOLEAN)
			return "BOOLEAN";
		else if (aSqlType == Types.CHAR)
			return "CHAR";
		else if (aSqlType == Types.CLOB)
			return "CLOB";
		else if (aSqlType == Types.DATALINK)
			return "DATALINK";
		else if (aSqlType == Types.DATE)
			return "DATE";
		else if (aSqlType == Types.DECIMAL)
			return "DECIMAL";
		else if (aSqlType == Types.DISTINCT)
			return "DISTINCT";
		else if (aSqlType == Types.DOUBLE)
			return "DOUBLE";
		else if (aSqlType == Types.FLOAT)
			return "FLOAT";
		else if (aSqlType == Types.INTEGER)
			return "INTEGER";
		else if (aSqlType == Types.JAVA_OBJECT)
			return "JAVA_OBJECT";
		else if (aSqlType == Types.LONGVARBINARY)
			return "LONGVARBINARY";
		else if (aSqlType == Types.LONGVARCHAR)
			return "LONGVARCHAR";
		else if (aSqlType == Types.NULL)
			return "NULL";
		else if (aSqlType == Types.NUMERIC)
			return "NUMERIC";
		else if (aSqlType == Types.OTHER)
			return "OTHER";
		else if (aSqlType == Types.REAL)
			return "REAL";
		else if (aSqlType == Types.REF)
			return "REF";
		else if (aSqlType == Types.SMALLINT)
			return "SMALLINT";
		else if (aSqlType == Types.STRUCT)
			return "STRUCT";
		else if (aSqlType == Types.TIME)
			return "TIME";
		else if (aSqlType == Types.TIMESTAMP)
			return "TIMESTAMP";
		else if (aSqlType == Types.TINYINT)
			return "TINYINT";
		else if (aSqlType == Types.VARBINARY)
			return "VARBINARY";
		else if (aSqlType == Types.VARCHAR)
			return "VARCHAR";
		else
			return "UNKNOWN";
	}
	
	/**
	 * Construct the SQL display name for the given SQL datatype.
	 * This is used when re-recreating the source for a table
	 */
	public static String getSqlTypeDisplay(String aTypeName, int sqlType, int size, int digits)
	{
		String display = aTypeName;

		switch (sqlType)
		{
			case Types.VARCHAR:
			case Types.CHAR:
				if ("text".equals(aTypeName) && size == Integer.MAX_VALUE) return aTypeName;
				if (size > 0) 
				{
					display = aTypeName + "(" + size + ")";
				}
				else
				{
					display = aTypeName;
				}
				break;
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.FLOAT:
				if (aTypeName.equalsIgnoreCase("money")) // SQL Server
				{
					display = aTypeName;
				}
				else if ((aTypeName.indexOf('(') == -1))
				{
					if (digits > 0 && size > 0)
					{
						display = aTypeName + "(" + size + "," + digits + ")";
					}
					else if (size <= 0 && digits > 0)
					{
						display = aTypeName + "(" + digits + ")";
					}
					else if (size > 0 && digits <= 0)
					{
						display = aTypeName + "(" + size + ")";
					}
				}
				break;

			case Types.OTHER:
				// Oracle specific datatypes
				if ("NVARCHAR2".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("NCHAR".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("UROWID".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				else if ("RAW".equalsIgnoreCase(aTypeName))
				{
					display = aTypeName + "(" + size + ")";
				}
				break;
			default:
				display = aTypeName;
				break;
		}
		return display;
	}
	
	public static CharSequence getWarnings(WbConnection con, Statement stmt, boolean retrieveOutputMsg)
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
			
			if (retrieveOutputMsg)
			{
				s = con.getOutputMessages();
				if (s != null && s.trim().length() > 0)
				{
					if (hasWarnings) msg.append('\n');
					msg = append(msg, s);
					if (!s.endsWith("\n")) msg.append("\n");
					hasWarnings = true;
				}
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
	
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Checking if all types defined by java.sql.Types are covered by getTypeName()...");
			System.out.println(System.getProperty("java.version"));
			Field fields[] = java.sql.Types.class.getDeclaredFields();
			for (int i=0; i < fields.length; i++)
			{
				int type = fields[i].getInt(null);
				if (getTypeName(type).equals("UNKNOWN"))
				{
					System.out.println("Type " + fields[i].getName() + " not included in getTypeName()!");
				}
			}
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
		System.out.println("Done.");
	}
}
