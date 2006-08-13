/*
 * SqlUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.sql.formatter.SqlFormatter;

public class SqlUtil
{
	private static Pattern specialCharPattern = Pattern.compile("[$:\\/ \\.;,]");
	
	public static String quoteObjectname(String aColname)
	{
		if (aColname == null) return null;
		Matcher m = specialCharPattern.matcher(aColname);
		boolean b = m.find();
		if (!b) return aColname.trim();
		StringBuffer col = new StringBuffer(aColname.length() + 5);
		col.append('"');
		col.append(aColname.trim());
		col.append('"');
		return col.toString();
	}

	public static String getInsertTable(String sql)
	{
		try
		{
			SQLLexer lexer = new SQLLexer(sql);
			SQLToken t = lexer.getNextToken(false, false);
			if (!t.getContents().equals("INSERT")) return null;
			t = lexer.getNextToken(false, false);
			if (!t.getContents().equals("INTO")) return null;
			t = lexer.getNextToken(false, false);
			return t.getContents();
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static String getSqlVerb(String sql)
	{
		if (sql == null) return "";
		String s = sql.trim();
		if (s.length() == 0) return "";
		
		// The SQLLexer does not recognize @ as a keyword (which is basically
		// correct, but to support the Oracle style includes we'll treat it
		// as a keyword here.
		if (s.charAt(0) == '@') return "@";
		
		SQLLexer l = new SQLLexer(sql);
		try
		{
			SQLToken t = l.getNextToken(false, false);
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
	public static List getResultSetColumns(String sql, WbConnection conn)
		throws SQLException
	{
		if (conn == null) return null;

		ResultSet rs = null;
		Statement stmt = null;
		ArrayList result = null;

		try
		{
			stmt = conn.createStatementForQuery();
			stmt.setMaxRows(1);
			rs = stmt.executeQuery(sql);
			ResultSetMetaData meta = rs.getMetaData();
			int count = meta.getColumnCount();
			result = new ArrayList(count);
			for (int i=0; i < count; i++)
			{
				String name = meta.getColumnName(i + 1);
				if (name == null) name = meta.getColumnLabel(i + 1);
				if (name == null) name = "Col" + (i + 1);

				int type = meta.getColumnType(i + 1);
				ColumnIdentifier col = new ColumnIdentifier(name, type);
				result.add(col);
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
	 * in the column list.
	 * @param select the SQL String to parse
	 * @param includeAlias if false, the "raw" column names will be returned, otherwise
	 *       the column name including the alias (e.g. "p.name AS person_name"
	 * @return a List of String objecs. 
	 */
	public static List getSelectColumns(String select, boolean includeAlias)
	{
		List result = new LinkedList();
		try
		{
			SQLLexer lex = new SQLLexer(select);
			SQLToken t = lex.getNextToken(false, false);
			if (!"SELECT".equalsIgnoreCase(t.getContents())) return Collections.EMPTY_LIST;
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
				else if (bracketCount == 0 && (",".equals(v) || SqlFormatter.SELECT_TERMINAL.contains(t.getContents())))
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
					if (SqlFormatter.SELECT_TERMINAL.contains(t.getContents()))
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
			return Collections.EMPTY_LIST;
		}
		
		return result;
	}
	
	public static String striptColumnAlias(String expression)
	{
		if (expression == null) return null;
		
		String result = expression.trim();
		int pos = StringUtil.findFirstWhiteSpace(result);
		if (pos > -1)
		{
			result = result.substring(0, pos).trim();
		}
		return result;
	}
	
	public static List getTables(String aSql)
	{
		return getTables(aSql, false);
	}

	public static final Set JOIN_KEYWORDS = new HashSet(6);
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
	public static List getTables(String sql, boolean includeAlias)
	{
		String from = SqlUtil.getFromPart(sql);
		if (from == null || from.trim().length() == 0) return Collections.EMPTY_LIST;
		List result = new LinkedList();
		try
		{
				SQLLexer lex = new SQLLexer(from);
				SQLToken t = (SQLToken)lex.getNextToken(false, false);

				boolean collectTable = true;
				StringBuffer currentTable = new StringBuffer();
				
				while (t != null)
				{
					String s = t.getContents();
					if (JOIN_KEYWORDS.contains(s))
					{
						collectTable = true;
						if (currentTable.length() > 0)
						{
							result.add(getTableDefinition(currentTable.toString(), includeAlias));
							currentTable = new StringBuffer();
						}
					}
					else if (",".equals(s))
					{
							collectTable = true;
							result.add(getTableDefinition(currentTable.toString(), includeAlias));
							currentTable = new StringBuffer();
					}
					else if ("ON".equals(s))
					{
						collectTable = false;
						result.add(getTableDefinition(currentTable.toString(), includeAlias));
						currentTable = new StringBuffer();
					}
					else if (collectTable && !s.equals("("))
					{
						int size = currentTable.length();
						if (size > 0 && !s.equals(".") && currentTable.charAt(size-1) != '.') currentTable.append(' ');
						currentTable.append(s);
					}
					
					t = (SQLToken)lex.getNextToken(false, false);
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
		Set s = new HashSet();
		s.add("FROM");
		return getKeywordPosition(s, sql);
	}
	
	public static int getWherePosition(String sql)
	{
		Set s = new HashSet();
		s.add("WHERE");
		return getKeywordPosition(s, sql);
	}
	
	public static int getKeywordPosition(String keyword, String sql)
	{
		if (keyword == null) return -1;
		Set s = new HashSet();
		s.add(keyword.toUpperCase());
		return getKeywordPosition(s, sql);
	}
	
	public static int getKeywordPosition(Set keywords, String sql)
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

		StringBuffer newSql = new StringBuffer(count);

		// remove trailing semicolon
		if (aSql.charAt(count - 1) == ';') count --;
		char last = ' ';

		for (int i=0; i < count; i++)
		{
			char c = aSql.charAt(i);

			inQuotes = c == quote;
			if (!inQuotes && (last == '\n' || last == '\r' || i == 0 ) && (c == '#'))
			{
				lineComment = true;
			}

			if (!(inComment || lineComment) || keepComments)
			{
				if ( c == '/' && i < count - 1 && aSql.charAt(i+1) == '*' & !inQuotes)
				{
					inComment = true;
					i++;
				}
				else if (c == '-' && i < count - 1 && aSql.charAt(i+1) == '-' && !inQuotes)
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
					if (c == '\n' && !keepNewlines)
					{
						newSql.append(' ');
					}
					else if (c < 32 || (c > 126 && c < 145) || c == 255)
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

	public static final boolean isIntegerType(int aSqlType)
	{
		return (aSqlType == Types.BIGINT ||
		        aSqlType == Types.INTEGER ||
		        aSqlType == Types.SMALLINT ||
		        aSqlType == Types.TINYINT);
	}
	
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
		return (aSqlType == Types.LONGVARCHAR || aSqlType == Types.CLOB);
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
		try { rs.close(); } catch (Throwable th) {}
		try { stmt.close(); } catch (Throwable th) {}
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

	public static boolean isValidType(int sqlType)
	{
		return !getTypeName(sqlType).equals("UNKNOWN");
	}
	
	public static String getWarnings(WbConnection con, Statement stmt, boolean retrieveOutputMsg)
	{
		try
		{
			// some DBMS return warnings on the connection rather then on the
			// statement. We need to check them here as well. Then some of
			// the DBMS return the same warnings on the Statement AND the
			// Connection object.
			// For this we keep a list of warnings which have been added
			// from the statement. They will not be added when the Warnings from
			// the connection are retrieved
			ArrayList added = new ArrayList();
			StringBuffer msg = new StringBuffer(100);
			String s = null;
			SQLWarning warn = stmt.getWarnings();
			boolean hasWarnings = warn != null;
			int count = 0;
			while (warn != null)
			{
				count ++;
				s = warn.getMessage();
				if (s != null && s.length() > 0)
				{
					msg.append(s);
					if (!s.endsWith("\n")) msg.append('\n');
				}
				added.add(s);
				if (count > 25) break; // prevent endless loop
				warn = warn.getNextWarning();
			}
			
			if (retrieveOutputMsg)
			{
				if (hasWarnings) msg.append('\n');

				s = con.getOutputMessages();
				if (s.length() > 0)
				{
					msg.append(s);
					if (!s.endsWith("\n")) msg.append("\n");
					hasWarnings = true;
				}
			}
			warn = con.getSqlConnection().getWarnings();
			hasWarnings = hasWarnings || (warn != null);
			count = 0;
			while (warn != null)
			{
				s = warn.getMessage();
				if (!added.contains(s))
				{
					msg.append(s);
					if (!s.endsWith("\n")) msg.append('\n');
				}
				if (count > 25) break; // prevent endless loop
				warn = warn.getNextWarning();
			}

			// make sure the warnings are cleared from both objects!
			stmt.clearWarnings();
			con.clearWarnings();

			return msg.toString();
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static void main(String args[])
	{
		try
		{
			System.out.println(System.getProperty("java.version"));
			Field fields[] = java.sql.Types.class.getDeclaredFields();
			for (int i=0; i < fields.length; i++)
			{
				int type = fields[i].getInt(null);
				if (!isValidType(type))
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
