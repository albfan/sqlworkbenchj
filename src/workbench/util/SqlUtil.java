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
import workbench.sql.formatter.Token;

public class SqlUtil
{
	private static Pattern specialCharPattern = Pattern.compile("[$ ]");
	private static Pattern selectPattern = Pattern.compile("^\\s*SELECT\\s+.*",Pattern.CASE_INSENSITIVE);
	
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

	public static boolean isSelect(String sql)
	{
		Matcher m = selectPattern.matcher(sql);
		return m.find();
	}
	
	public static String getSqlVerb(String aStatement)
	{
		if (aStatement == null) return "";
		String s = aStatement.trim();
		if (s.length() == 0) return "";
		if (s.charAt(0) == '@') return "@";
		int pos = StringUtil.findFirstWhiteSpace(s);
		if (pos > -1)
		{
			return s.substring(0, pos).trim().toUpperCase();
		}
		return s.toUpperCase();
	}
	
	public static List getResultSetColumns(String sql, WbConnection conn)
		throws SQLException
	{
		if (conn == null) return null;

		ResultSet rs = null;
		Statement stmt = null;
		ArrayList result = null;

		try
		{
			stmt = conn.createStatement();
			stmt.setMaxRows(1);
			rs = stmt.executeQuery(sql);
			ResultSetMetaData meta = rs.getMetaData();
			int count = meta.getColumnCount();
			result = new ArrayList(count);
			for (int i=0; i < count; i++)
			{
				String name = meta.getColumnName(i + 1);
				if (name == null) name = meta.getColumnLabel(i + 1);
				if (name == null) continue;

				int type = meta.getColumnType(i + 1);
				ColumnIdentifier col = new ColumnIdentifier(name, type);
				result.add(col);
			}
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
		return result;
	}

	private static String stripTableAlias(String table)
	{
		int pos = StringUtil.findFirstWhiteSpace(table);
		if (pos > -1) return table.substring(0, pos - 1);
		return table;
	}
	
	public static List getTables(String aSql)
	{
		return getTables(aSql, false);
	}
	
	public static List getTables(String sql, boolean includeAlias)
	{
		String from = SqlUtil.getFromPart(sql);
		if (from == null || from.trim().length() == 0) return Collections.EMPTY_LIST;
		List result = new LinkedList();
		try
		{
				SQLLexer lex = new SQLLexer(from);
				Token t = lex.getNextToken(false, false);
				Set js = new HashSet();
				js.add("INNER");
				js.add("CROSS");
				js.add("NATURAL");
				js.add("FULL");
				js.add("RIGHT");
				js.add("LEFT");
				int lastStart = 0;
				int lastJoinStart = 0;
				boolean hadJoin = false;
				boolean isJoinSyntax = false;
				while (t != null)
				{
					String s = t.getContents();
					if (js.contains(s))
					{
						lastJoinStart = t.getCharBegin();
						// if we find a JOIN keyword
						// we assume the FROM clause is in the new ANSI syntax
						// this assumes that even with the new syntax, a comma does
						// not occur before any of the JOIN keywords!
						isJoinSyntax = true;
					}
					else if (",".equals(s))
					{
						if (isJoinSyntax)
						{
							lastStart = t.getCharEnd();
							lastJoinStart = -1;
						}
						else if (lastStart > -1)
						{
							String table = from.substring(lastStart, t.getCharBegin()).trim();
							result.add(table);
							lastStart = t.getCharEnd();
						}
					}
					else if ("JOIN".equals(s) && lastStart > -1 && lastJoinStart > lastStart)
					{
						String table = from.substring(lastStart, lastJoinStart).trim();
						if (includeAlias)
						{
							result.add(table);
						}
						else
						{
							result.add(stripTableAlias(table));
						}
						lastStart = t.getCharEnd();
						hadJoin = true;
					}
					else if ("ON".equals(s) && lastStart > -1)
					{
						String table = from.substring(lastStart, t.getCharBegin()).trim();
						if (includeAlias)
						{
							result.add(table);
						}
						else
						{
							result.add(stripTableAlias(table));
						}
						// reset the start markers until a new JOIN keyword is detected
						lastStart = -1;
						lastJoinStart = -1;
					}
					
					t = lex.getNextToken(false, false);
				}
				if (lastStart < from.length() && lastStart > -1)
				{
					String table = from.substring(lastStart).trim();
					if (includeAlias)
					{
						result.add(table);
					}
					else
					{
						result.add(stripTableAlias(table));
					}
				}
		}
		catch (Exception e)
		{
			LogMgr.logError("SqlUtil.getTable()", "Error parsing sql", e);
		}
		return result;
	}


//	public static final Pattern FROM_PATTERN = Pattern.compile("\\sFROM\\s|\\sFROM$", Pattern.CASE_INSENSITIVE);
//	public static final Pattern WHERE_PATTERN = Pattern.compile("\\sWHERE\\s|\\sWHERE$", Pattern.CASE_INSENSITIVE);
//	private static final Pattern GROUP_PATTERN = Pattern.compile("\\sGROUP\\s", Pattern.CASE_INSENSITIVE);
//	private static final Pattern ORDER_PATTERN = Pattern.compile("\\sORDER\\s", Pattern.CASE_INSENSITIVE);
//	private static final Pattern JOIN_PATTERN = Pattern.compile("\\sJOIN\\s", Pattern.CASE_INSENSITIVE);
//		
//	/**
//	 * Return the list of tables which are in the FROM list of the given SQL statement.
//	 */
//	public static List getTables(String aSql, boolean includeAlias)
//	{
//		int fromPos = getFromPosition(aSql);
//		int pos = -1;
//		if (fromPos == -1) return Collections.EMPTY_LIST;
//		int fromEnd = fromPos + 5;
//
//		int nextVerb = StringUtil.findPattern(WHERE_PATTERN, aSql, fromPos);
//
//		if (nextVerb == -1) nextVerb = StringUtil.findPattern(GROUP_PATTERN, aSql, fromPos);
//		if (nextVerb == -1) nextVerb = StringUtil.findPattern(ORDER_PATTERN, aSql, fromPos);
//		if (nextVerb == -1) nextVerb = aSql.length();
//		if (nextVerb < fromEnd) return Collections.EMPTY_LIST;
//		
//		String fromList = aSql.substring(fromEnd, nextVerb);
//		boolean joinSyntax = (StringUtil.findPattern(JOIN_PATTERN, aSql, fromPos) > -1);
//		ArrayList result = new ArrayList();
//		if (joinSyntax)
//		{
//			StringTokenizer tok = new StringTokenizer(fromList, " ");
//			// first token after the FROM clause is the first table
//			// we can add it right away
//			if (tok.hasMoreTokens())
//			{
//				result.add(tok.nextToken().trim());
//			}
//			boolean nextIsTable = false;
//			while (tok.hasMoreTokens())
//			{
//				String s = tok.nextToken();
//				if (nextIsTable)
//				{
//					result.add(s.trim());
//					nextIsTable = false;
//				}
//				else
//				{
//					nextIsTable = ("JOIN".equalsIgnoreCase(s));
//				}
//			}
//		}
//		else
//		{
//			StringTokenizer tok = new StringTokenizer(fromList, ",");
//			pos = -1;
//			while (tok.hasMoreTokens())
//			{
//				String table = tok.nextToken().trim();
//				if (table.length() == 0) continue;
//				if (!includeAlias)
//				{
//					pos = table.indexOf(' ');
//					if (pos != -1)
//					{
//						table = table.substring(0, pos);
//					}
//				}
//				result.add(makeCleanSql(table, false));
//			}
//		}
//
//		return result;
//	}

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

			SQLToken t = (SQLToken)lexer.getNextToken(false, false);
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

				t = (SQLToken)lexer.getNextToken(false, false);
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

//	private static int findClosingBracket(String value, int startPos)
//	{
//		int len = value.length();
//		boolean inQuotes = false;
//		for (int pos=startPos; pos < len; pos++)
//		{
//			char c = value.charAt(pos);
//			if (c == '\'') 
//			{
//				inQuotes = !inQuotes;
//			}
//			if (!inQuotes)
//			{
//				if (c == ')') return pos;
//			}
//		}
//		return -1;
//	}
//	
//	private static final int skipQuotes(String aString, int aStartpos)
//	{
//		char c = aString.charAt(aStartpos);
//		while (c != '\'')
//		{
//			aStartpos ++;
//			c = aString.charAt(aStartpos);
//		}
//		return aStartpos + 1;
//	}

	public static final String getJavaPrimitive(String aClass)
	{
		if (aClass == null) return null;
		int pos = aClass.lastIndexOf('.');
		if (pos >= 0)
		{
			aClass = aClass.substring(pos + 1);
		}
		if (aClass.equals("Integer"))
		{
			return "int";
		}
		else if (aClass.equals("Long"))
		{
			return "long";
		}
		else if (aClass.equals("Boolean"))
		{
			return "boolean";
		}
		else if (aClass.equals("Character"))
		{
			return "char";
		}
		else if (aClass.equals("Float"))
		{
			return "float";
		}
		else if (aClass.equals("Double"))
		{
			return "double";
		}
		return null;
	}

	public static final String getJavaClass(int aSqlType, int aScale, int aPrecision)
	{
		if (aSqlType == Types.BIGINT)
			return "java.math.BigInteger";
		else if (aSqlType == Types.BOOLEAN)
			return "Boolean";
		else if (aSqlType == Types.CHAR)
			return "Character";
		else if (aSqlType == Types.DATE)
			return "java.util.Date";
		else if (aSqlType == Types.DECIMAL)
			return getDecimalClass(aSqlType, aScale, aPrecision);
		else if (aSqlType == Types.DOUBLE)
			return getDecimalClass(aSqlType, aScale, aPrecision);
		else if (aSqlType == Types.FLOAT)
			return getDecimalClass(aSqlType, aScale, aPrecision);
		else if (aSqlType == Types.INTEGER)
			return "Integer";
		else if (aSqlType == Types.JAVA_OBJECT)
			return "Object";
		else if (aSqlType == Types.NUMERIC)
			return getDecimalClass(aSqlType, aScale, aPrecision);
		else if (aSqlType == Types.REAL)
			return getDecimalClass(aSqlType, aScale, aPrecision);
		else if (aSqlType == Types.SMALLINT)
			return "Integer";
		else if (aSqlType == Types.TIME)
			return "java.sql.Time";
		else if (aSqlType == Types.TIMESTAMP)
			return "java.sql.Timestamp";
		else if (aSqlType == Types.TINYINT)
			return "Integer";
		else if (aSqlType == Types.VARCHAR)
			return "String";
		else if (aSqlType == Types.LONGVARCHAR)
			return "String";
		else
			return null;
	}

	private static final String getDecimalClass(int aSqlType, int aScale, int aPrecision)
	{
		if (aPrecision == 0)
		{
			if (aScale < 11)
			{
				return "java.lang.Integer";
			}
			if (aScale >= 11 && aScale < 18)
			{
				return "java.lang.Long";
			}
			else
			{
				return "java.math.BigInteger";
			}
		}
		else
		{
			if (aScale < 11)
			{
				return "java.lang.Float";
			}
			if (aScale >= 11 && aScale < 18)
			{
				return "java.lang.Double";
			}
			else
			{
				return "java.math.BigDecimal";
			}
		}
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
		return (aSqlType == Types.DATE ||
						aSqlType == Types.TIMESTAMP);
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
	
	private static void validateStaticTypes()
	{
		try
		{
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
		validateStaticTypes();
		System.out.println("Done.");
	}
}
