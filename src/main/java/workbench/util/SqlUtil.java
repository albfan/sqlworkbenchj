/*
 * SqlUtil.java
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
package workbench.util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

import workbench.interfaces.TextContainer;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbSettings;
import workbench.db.DropType;
import workbench.db.QuoteHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import workbench.sql.ErrorDescriptor;
import workbench.sql.formatter.WbSqlFormatter;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.parser.ParserType;
import workbench.sql.syntax.SqlKeywordHelper;

/**
 * Methods for manipulating and analyzing SQL statements.
 *
 * @author Thomas Kellerer
 */
public class SqlUtil
{
  /**
   * The RegEx Pattern to validate a String as a legal ANSI-SQL identifier.
   */
	public static final Pattern SQL_IDENTIFIER = Pattern.compile("[a-zA-Z][\\w\\$]*");

  private static class JoinKeywordsHolder
	{
		private static final Set<String> JOIN_KEYWORDS = Collections.unmodifiableSet(
				CollectionUtil.caseInsensitiveSet(
					"JOIN", "INNER JOIN", "NATURAL JOIN", "LEFT JOIN", "LEFT OUTER JOIN", "RIGHT JOIN",
					"RIGHT OUTER JOIN", "CROSS JOIN", "FULL JOIN", "FULL OUTER JOIN", "OUTER APPLY", "CROSS APPLY")
				);
	}
	public static Set<String> getJoinKeyWords()
	{
		return JoinKeywordsHolder.JOIN_KEYWORDS;
	}

	private static class KnownTypesHolder
	{
		protected final static Set<String> KNOWN_TYPES =
			Collections.unmodifiableSet(CollectionUtil.caseInsensitiveSet(
			"INDEX", "TABLE", "PROCEDURE", "FUNCTION", "VIEW", "PACKAGE", "PACKAGE BODY",
			"SYNONYM", "SEQUENCE", "ALIAS", "TRIGGER", "DOMAIN", "ROLE", "CAST", "AGGREGATE",
			"TABLESPACE", "TYPE", "USER", "MATERIALIZED VIEW LOG", "MATERIALIZED VIEW", "SNAPSHOT",
			"FLASHBACK ARCHIVE", "TYPE BODY", "CAST", "FOREIGN DATA WRAPPER", "OPERATOR", "SCHEMA", "EXTENSION",
			"DATABASE", "DATABASE LINK", "PFILE", "SPFILE", "SYSTEM"));
	}

  private static final Set<String> CHAR_TYPES_WITHOUT_LENGTH = CollectionUtil.caseInsensitiveSet("text", "tinytext", "mediumtext", "longtext");

  public static Set<String> getDMLVerbs()
	{
		return ModifyingVerbsHolder.DML_VERB;
	}

	private static class ModifyingVerbsHolder
	{
		private final static Set<String> DML_VERB = Collections.unmodifiableSet(CollectionUtil.caseInsensitiveSet("update", "delete"));
	}

	private static class TypesWithoutNamesHolder
	{
		private final static Set<String> TYPES =
			Collections.unmodifiableSet(CollectionUtil.treeSet("MATERIALIZED VIEW LOG", "SNAPSHOT LOG", "PFILE", "SPFILE", "SYSTEM"));
	}

	public static Set<String> getTypesWithoutNames()
	{
		return TypesWithoutNamesHolder.TYPES;
	}

	public static Set<String> getKnownTypes()
	{
		return KnownTypesHolder.KNOWN_TYPES;
	}

  private static final class SqlTypeFieldsHolder
  {
    private static final Field[] SQL_TYPES = java.sql.Types.class.getDeclaredFields();
  }

  private static Field[] getSqlTypeFields()
  {
    return SqlTypeFieldsHolder.SQL_TYPES;
  }

	public static String escapeQuotes(String value)
	{
		if (value == null) return null;
		return value.replace("'" ,"''");
	}

	public static String 	getPlainTypeName(String input)
	{
		int pos = input.indexOf('(');
		if (pos < 0) return input;
		return input.substring(0, pos);
	}

	/**
	 * Removes the SQL verb of this command. The verb is defined
	 * as the first "word" in the SQL string that is not a comment.
	 *
	 * @see #getSqlVerb(java.lang.String)
	 * @see SqlParsingUtil#stripVerb(java.lang.String)
	 */
	public static String stripVerb(String sql)
	{
		SqlParsingUtil util = SqlParsingUtil.getInstance(null);
		return util.stripVerb(sql);
	}

	/**
	 * Quote the given objectName if needed according to the SQL standard.
	 *
	 * @param object the name to quote
	 * @return the quoted version of the name.
	 */
	public static String quoteObjectname(String object)
	{
		return quoteObjectname(object, false);
	}

	/**
	 * Quote the given objectName if needed according to the SQL standard.
	 * If quoteAlways is true, then no name checking is performed.
	 *
	 * @param objectName the name to quote
	 * @param quoteAlways if true, the name is not tested for special characters
	 * @return the quoted version of the name.
	 */
	public static String quoteObjectname(String objectName, boolean quoteAlways)
	{
		return quoteObjectname(objectName, quoteAlways, false, '"');
	}

	/**
	 * Quote the given objectName if needed according to the SQL standard.
	 * If quoteAlways is true, then no name checking is performed.
	 *
	 * DbMetadata.quoteObjectName() should be preferred over this method because it will use
	 * the correct quoting character reported by the JDBC driver.
	 *
	 * @param objectName the name to quote
	 * @param quoteAlways if true, the name is not tested for special characters
	 * @param checkReservedWords if true, the value will be compared to a list of SQL 2003 reserved words.
	 *
	 * @return the quoted version of the name.
	 * @see DbMetadata#quoteObjectname(java.lang.String)
	 */
	public static String quoteObjectname(String objectName, boolean quoteAlways, boolean checkReservedWords, char quote)
	{
		if (objectName == null) return null;
		if (objectName.length() == 0) return "";

		if (objectName.charAt(0) == quote) return objectName;

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
			doQuote = !m.matches();
		}

		if (!doQuote && checkReservedWords) // no need to check for reserved words if we already need quoting
		{
			doQuote = SqlKeywordHelper.getDefaultReservedWords().contains(objectName);
		}

		if (!doQuote)
		{
			return objectName;
		}

		StringBuilder col = new StringBuilder(objectName.length() + 2);
		col.append(quote);
		col.append(objectName);
		col.append(quote);
		return col.toString();
	}

	public static char getCatalogSeparator(WbConnection conn)
	{
		if (conn == null) return '.';
		DbMetadata meta = conn.getMetadata();
		if (meta == null) return '.';
		return meta.getCatalogSeparator();
	}

	public static char getSchemaSeparator(WbConnection conn)
	{
		if (conn == null) return '.';
		DbMetadata meta = conn.getMetadata();
		if (meta == null) return '.';
		return meta.getSchemaSeparator();
	}

	/**
	 * Removes quote characters from the start and the end of a string.
	 *
	 * It does take the idiotic MySQL backticks and SQL Server's imbecile [..] quoting into account.
	 *
	 * @param input the string from which the quotes should be removed
	 * @return the input with quotes removed
   *
	 * @see workbench.util.StringUtil#trimQuotes(java.lang.String)
	 */
	public static String removeObjectQuotes(String input)
	{
		if (input == null) return input;

    input = input.trim();
		int len = input.length();
		if (len < 2) return input;

		char firstChar = input.charAt(0);
		char lastChar = input.charAt(len - 1);

		if ( (firstChar == '"' && lastChar == '"') ||
				 (firstChar == '`' && lastChar == '`') || /* workaround the idiotic MySQL quoting */
 			   (firstChar == '[' && lastChar == ']')    /* workaround the idiotic SQL Server quoting */
				 )
		{
			return input.substring(1, len - 1);
		}
		return input;
	}

	public static boolean isQuotedIdentifier(String input)
	{
		if (input == null) return false;

    input = input.trim();
		int len = input.length();
    if (len < 2) return false;

		char firstChar = input.charAt(0);
		char lastChar = input.charAt(len - 1);

		if ( (firstChar == '"' && lastChar == '"') ||
				 (firstChar == '`' && lastChar == '`') || /* workaround the idiotic MySQL quoting */
 			   (firstChar == '[' && lastChar == ']')    /* workaround the idiotic SQL Server quoting */
				 )
		{
			return true;
		}
		return false;
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
		return getDDLObjectInfo(sql, null);
	}

	public static DdlObjectInfo getDDLObjectInfo(CharSequence sql, WbConnection conn)
	{
		if (StringUtil.isEmptyString(sql)) return null;

		DdlObjectInfo info = new DdlObjectInfo(sql, conn);
		if (info.isValid())
		{
			return info;
		}
		return null;
	}

  public static boolean objectNamesAreEqual(DbObject dbo1, DbObject dbo2)
  {
    if (dbo1 == null || dbo2 == null) return false;

    // only compare names of objects of the same type
    String type1 = dbo1.getObjectType();
    String type2 = dbo2.getObjectType();
    if (type1 == null || type2 == null) return false;
    if (StringUtil.compareStrings(type1, type2, true) != 0) return false;

    // only compare the catalog if both objects have one
    String cat1 = removeObjectQuotes(dbo1.getCatalog());
    String cat2 = removeObjectQuotes(dbo2.getCatalog());
    if (StringUtil.isNonEmpty(cat1) && StringUtil.isNonEmpty(cat2))
    {
      if (!cat1.equalsIgnoreCase(cat2)) return false;
    }

    String schema1 = removeObjectQuotes(dbo1.getSchema());
    String schema2 = removeObjectQuotes(dbo2.getSchema());
    if (StringUtil.isNonEmpty(schema1) && StringUtil.isNonEmpty(schema2))
    {
      if (!schema1.equalsIgnoreCase(schema2)) return false;
    }

    String name1 = removeObjectQuotes(dbo1.getObjectName());
    String name2 = removeObjectQuotes(dbo2.getObjectName());
    return StringUtil.equalStringIgnoreCase(name1, name2);
  }


	/**
	 * Compare two object names for equality.
	 *
	 * If at least one of them is a quoted identified, comparison is done case-sensitive.
	 *
	 * If both are non-quoted, comparison is case insensitive.
	 *
	 * This does not take into account non-standard DBMS that support unquoted case-sensitive identifiers.
	 *
	 * @param one     the first object name
	 * @param other   the second object name
	 * @return true if both names are identical considering quoted identifiers
	 *
	 * @see #isQuotedIdentifier(java.lang.String)
	 */
	public static boolean objectNamesAreEqual(String one, String other)
	{
    boolean firstEmpty = StringUtil.isEmptyString(one);
    boolean secondEmpty = StringUtil.isEmptyString(other);

		if (firstEmpty && secondEmpty) return true;
		if (firstEmpty || secondEmpty) return false;

		boolean firstQuoted = isQuotedIdentifier(one);
		boolean secondQuoted = isQuotedIdentifier(other);

		if (firstQuoted || secondQuoted)
		{
			return removeObjectQuotes(one).equals(removeObjectQuotes(other));
		}
		return removeObjectQuotes(one).equalsIgnoreCase(removeObjectQuotes(other));
	}

	/**
	 * Escapes any underscore in the passed name with the escape character defined by the connection.
	 *
	 * @param name the object identifier to test
	 * @param conn the connection, if null no escaping is done
	 * @return an escaped version of the name
	 *
	 * @see WbConnection#getSearchStringEscape()
	 * @see #escapeUnderscore(java.lang.String, java.lang.String)
	 * @see DbSettings#doEscapeSearchString()
	 */
	public static String escapeUnderscore(String name, WbConnection conn)
	{
		if (name == null) return null;
		if (name.indexOf('_') == -1) return name;
		if (conn == null) return name;
		if (!conn.getDbSettings().doEscapeSearchString()) return name;

		String escape = conn.getSearchStringEscape();
		return escapeUnderscore(name, escape);
	}

	/**
	 * Escapes any underscore in the passed name with the given escape character.
	 *
	 * @param name the object identifier to test
	 * @param escape the esacpe character used by connection
	 * @return an escaped version of the name
	 *
	 * @see WbConnection#getSearchStringEscape()
	 * @see #escapeUnderscore(java.lang.String, workbench.db.WbConnection)
	 */
	public static String escapeUnderscore(String name, String escape)
	{
		if (name == null) return null;
		if (StringUtil.isEmptyString(escape)) return name;
		if (name.indexOf('_') == -1) return name;

		String escaped = escape + "_";
		// already escaped.
		if (name.indexOf(escaped) > -1) return name;

		// Only the underscore is replaced as the % character is not allowed in SQL identifiers
		return StringUtil.replace(name, "_", escaped);
	}

	public static void appendEscapeClause(StringBuilder sql, WbConnection con, String searchValue)
	{
    String escape = getEscapeClause(con, searchValue);
    if (escape.length() > 0)
    {
      sql.append(escape);
    }
	}

	public static String getEscapeClause(WbConnection con, String searchValue)
	{
		if (searchValue == null) return "";
		if (searchValue.indexOf('_') < 0) return "";
		if (searchValue.indexOf('%') < 0) return ""; // no LIKE will be used then anyway
		if (con == null) return "";
		if (!con.getDbSettings().doEscapeSearchString()) return "";
		String escape = con.getSearchStringEscape();
		if (StringUtil.isEmptyString(escape)) return "";
		return " ESCAPE '" + escape + "'";
	}

	/**
	 * Returns the type that is beeing created e.g. TABLE, VIEW, PROCEDURE
	 */
	public static String getCreateType(CharSequence sql)
	{
		DdlObjectInfo info = getDDLObjectInfo(sql);
		if (info == null) return null;
		return info.getObjectType();
	}

	public static String getDeleteTable(CharSequence sql)
	{
		return getDeleteTable(sql, '.', null);
	}

	/**
	 * If the given SQL is a DELETE [FROM] returns
	 * the table from which rows will be deleted
	 */
	public static String getDeleteTable(CharSequence sql, char catalogSeparator, WbConnection conn)
	{
		try
		{
			StringBuilder tableName = new StringBuilder();
			SQLLexer lexer = SQLLexerFactory.createLexer(conn, sql);

			SQLToken t = lexer.getNextToken(false, false);
			if (!t.getContents().equals("DELETE")) return null;
			t = lexer.getNextToken(false, false);

			// If the next token is not the FROM keyword (which is optional)
			// then it must be the table name.
			if (t == null) return null;

			if (!t.getContents().equals("FROM"))
			{
				tableName.append(t.getContents());
				appendCurrentTablename(lexer, tableName, catalogSeparator);
				return tableName.toString();
			}

			t = lexer.getNextToken(false, false);
			if (t == null) return null;
			tableName.append(t.getContents());
			appendCurrentTablename(lexer, tableName, catalogSeparator);

			t = lexer.getNextToken(false, false);
			if (t != null && t.getContents().charAt(0) == catalogSeparator)
			{
				// found a table name with a non-standard catalog separator (the . will be recognized by the lexer)
				tableName.append(t.getContents());
				t = lexer.getNextToken(false, false);
				if (t != null && t.isIdentifier())
				{
					tableName.append(t.getContents());
				}
			}
			return tableName.toString();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static SQLToken appendCurrentTablename(SQLLexer lexer, StringBuilder tableName, char catalogSeparator)
	{
		try
		{
			SQLToken t = lexer.getNextToken(false, false);
			if (t != null && t.getContents().charAt(0) == catalogSeparator)
			{
				// found a table name with a non-standard catalog separator (the . will be recognized by the lexer)
				tableName.append(t.getContents());
				t = lexer.getNextToken(false, false);
				if (t != null && t.isIdentifier())
				{
					tableName.append(t.getContents());
				}
			}
			return t;
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	/**
	 * If the given SQL is an TRUNCATE TABLE INTO...
	 * returns the target table, otherwise null
	 */
	public static String getTruncateTable(CharSequence sql, WbConnection conn)
	{
		return getTruncateTable(sql, '.', conn);
	}

	public static String getTruncateTable(CharSequence sql, char catalogSeparator, WbConnection conn)
	{
		return getDmlTable(sql, catalogSeparator, "TRUNCATE", "TABLE", conn);
	}

	/**
	 * If the given SQL command is an UPDATE command, return
	 * the table that is updated, otherwise return null;
	 */
	public static String getUpdateTable(CharSequence sql, WbConnection conn)
	{
		return getUpdateTable(sql, '.', null);
	}

	public static String getUpdateTable(CharSequence sql, char catalogSeparator, WbConnection conn)
	{
		return getDmlTable(sql, catalogSeparator, "UPDATE", null, conn);
	}

	/**
	 * If the given SQL is an INSERT INTO...
	 * returns the target table, otherwise null
	 */
	public static String getInsertTable(CharSequence sql, WbConnection conn)
	{
		return getInsertTable(sql, '.', conn);
	}

	public static String getInsertTable(CharSequence sql, char catalogSeparator, WbConnection conn)
	{
		return getDmlTable(sql, catalogSeparator, "INSERT", "INTO", conn);
	}

	private static String getDmlTable(CharSequence sql, char catalogSeparator, String verb, String secondKeyword, WbConnection conn)
	{
		try
		{
			StringBuilder tableName = new StringBuilder();
			SQLLexer lexer = SQLLexerFactory.createLexer(conn, sql);

			SQLToken t = lexer.getNextToken(false, false);
			if (t == null || !t.getContents().equals(verb)) return null;
			t = lexer.getNextToken(false, false);
			if (secondKeyword != null)
			{
				if (t == null || !t.getContents().equals(secondKeyword)) return null;
				t = lexer.getNextToken(false, false);
			}
			if (t == null) return null;
			tableName.append(t.getContents());
			appendCurrentTablename(lexer, tableName, catalogSeparator);
			return tableName.toString();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Append a semicolon to the passed SQL unless it already is terminated with one.
	 *
	 * @param sql the statement
	 * @return the statement with a semicolon at the end
	 */
	public static String addSemicolon(String sql)
	{
		int index = findSemicolonFromEnd(sql);
		if (index > -1) return sql;
		return sql + ";";
	}

	/**
	 * Remove any semicolon at the end of the string.
	 *
	 * The input is also trimmed from whitespaces after the last semicolon (if any)
	 * @param sql
	 * @return the input string without a trailing semicolon
	 */
	public static String trimSemicolon(String sql)
	{
    if (StringUtil.isEmptyString(sql)) return sql;
		int index = findSemicolonFromEnd(sql);

		if (index > -1)
		{
			return sql.substring(0, index);
		}
		return sql;
	}

	/**
	 * Finds the first semicolon at the end of the input string.
	 *
	 * The search from the end stops at the first non-whitespace character.
	 *
	 * @param input
	 * @return -1, no semicolon found otherwise the position of the semicolon.
	 */
	private static int findSemicolonFromEnd(String input)
	{
		if (input == null) return -1;
		int len = input.length();
		if (len == 0) return -1;

		int index = -1;

		for (int i=(len - 1); i > 0; i--)
		{
			char c = input.charAt(i);

			if (c == ';')
			{
				index = i;
				break;
			}
			if (!Character.isWhitespace(c)) break;
		}
		return index;
	}

	public static String getSqlVerb(String sql)
	{
		return SqlParsingUtil.getInstance(null).getSqlVerb(sql);
	}

	/**
	 * Returns the columns for the result set defined by the passed query.
	 *
	 * This method will actually execute the given SQL query, but will
	 * not retrieve any rows (using setMaxRows(1).
	 * @param sql the query
	 * @param conn the connection to use
	 * @see #getResultInfoFromQuery(java.lang.String, workbench.db.WbConnection)
	 */
	public static List<ColumnIdentifier> getResultSetColumns(String sql, WbConnection conn)
		throws SQLException
	{
		if (conn == null) return null;

		ResultInfo info = getResultInfoFromQuery(sql, conn);
		if (info == null) return null;

		int count = info.getColumnCount();
		ArrayList<ColumnIdentifier> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			result.add(info.getColumn(i));
		}
		return result;
	}


	/**
	 * Returns information about the result set generated by the passed query.
	 * <br/>
	 * If DbSettings.usePreparedStatementForQueryInfo() is true, the SQL will be prepared
	 * and the ResultSet info will be obtained from PreparedStatement.getMetaData(). As this
	 * is not supported by all JDBC drivers, the behaviour can be controlled through DbSettings.
	 * <br/>
	 * If using a PreparedStatement throws an error (or is disabled) the query will be executed,
	 * but Statement.setMaxRows() will be used to limit the result set to one row.
	 * <br/>
	 * If the passed SQL contains exactly one table, this table will be set as the updateTable in the
	 * returned ResultInfo.
	 * <br/>
	 *
	 * @param sql the query
	 * @param conn the connection to use
	 *
	 * @see #getResultSetColumns(java.lang.String, workbench.db.WbConnection)
	 * @see workbench.db.DbSettings#usePreparedStatementForQueryInfo()
	 * @see workbench.storage.ResultInfo#ResultInfo(java.sql.ResultSetMetaData, workbench.db.WbConnection)
	 */
	public static ResultInfo getResultInfoFromQuery(String sql, WbConnection conn)
		throws SQLException
	{
		if (conn == null) return null;

		ResultSet rs = null;
		Statement stmt = null;
		PreparedStatement pstmt = null;
		final ResultInfo result;
		Savepoint sp = null;

		try
		{
			if (conn.getDbSettings().useSavePointForDML())
			{
				sp = conn.setSavepoint();
			}

			ResultSetMetaData meta = null;
			if (conn.getDbSettings().usePreparedStatementForQueryInfo())
			{
				try
				{
					pstmt = conn.getSqlConnection().prepareStatement(sql);
					meta = pstmt.getMetaData();
				}
				catch (Exception e)
				{
					LogMgr.logError("SqlUtil.getResultInfoFromQuery()", "Could not obtain result info from prepared statement", e);
					closeStatement(pstmt);
					pstmt = null;
					meta = null;
				}
			}

			if (meta == null)
			{
				stmt = conn.createStatementForQuery();
				stmt.setMaxRows(1);
				rs = stmt.executeQuery(trimSemicolon(sql));
				meta = rs.getMetaData();
			}

			result = new ResultInfo(meta, conn);
			List<Alias> tables = getTables(sql, false, conn);
			if (tables.size() == 1)
			{
				Alias table = tables.get(0);
				TableIdentifier tbl = new TableIdentifier(table.getObjectName(), conn);
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
			closeStatement(pstmt);
			closeAll(rs, stmt);
		}
		return result;
	}

	/**
	 * Parse the given SQL SELECT query and return the columns defined in the column list.
	 *
	 * If the SQL string does not start with SELECT an empty List is returned.
	 *
	 * @param select the SQL String to parse
	 * @param includeAlias if false, the "raw" column names will be returned, otherwise
	 *       the column name including the alias (e.g. "p.name AS person_name"
	 * @return a List of column names
	 * @see #getColumnEntries(java.lang.String, boolean)
	 */
	public static List<String> getSelectColumns(String select, boolean includeAlias, WbConnection conn)
	{
		List<ElementInfo> entries = getColumnEntries(select, includeAlias, conn);
		List<String> result = new ArrayList<>(entries.size());
		for (ElementInfo entry : entries)
		{
			result.add(entry.getElementValue());
		}
		return result;
	}

	/**
	 * Parse the given SQL SELECT query and return the columns defined
	 * in the select list, including their start and end position inside
	 * the SQL string.
	 *
	 * If the SQL string does not start with SELECT an empty List is returned.
	 *
	 * @param select the SQL String to parse
	 * @param includeAlias if false, the "raw" column names will be returned, otherwise
	 *       the column name including the alias (e.g. "p.name AS person_name"
	 * @return a List of ElementInfos
	 * @see #getSelectColumns(java.lang.String, boolean)
	 *
	 */
	public static List<ElementInfo> getColumnEntries(String select, boolean includeAlias, WbConnection conn)
	{
		List<ElementInfo> result = new LinkedList<>();
		try
		{
			SQLLexer lexer = SQLLexerFactory.createLexer(conn, select);
			SQLToken t = lexer.getNextToken(false, false);

			if (t == null) return Collections.emptyList();

			String word = t.getContents();

			if (! ("SELECT".equalsIgnoreCase(word) || "WITH".equalsIgnoreCase(word) ))
			{
				return Collections.emptyList();
			}

			if ("WITH".equals(word))
			{
				t = skipCTE(lexer);
			}
			else
			{
				t = lexer.getNextToken(false, false);
			}

			if (t == null) return Collections.emptyList();

			boolean ignoreFirstBracket = false;

			// Skip a potential DISTINCT at the beginning
			if (t.getContents().equals("DISTINCT") || t.getContents().equals("DISTINCT ON"))
			{
				// Postgres DISTINCT ON extension...
				ignoreFirstBracket = t.getContents().equals("DISTINCT ON");
				t = lexer.getNextToken(false, false);
				if (t == null) return Collections.emptyList();
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
					&& (",".equals(v) || WbSqlFormatter.SELECT_TERMINAL.contains(v)))
				{
					String col = select.substring(lastColStart, t.getCharBegin());

					if (ignoreFirstBracket && bracketCount == 0 && col.trim().endsWith(")"))
					{
						// When dealing with Postgres' DISTINCT ON, the last column
						// inside the brackets will be extracted including the bracket
						col = col.substring(0, col.length() - 1);
					}

					if (includeAlias)
					{
						col = col.trim();
					}
					else
					{
						col = stripColumnAlias(col);
					}
					result.add(new ElementInfo(col, lastColStart, t.getCharBegin()));

					if (WbSqlFormatter.SELECT_TERMINAL.contains(v))
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
				t = lexer.getNextToken(false, false);
			}
			if (lastColStart > -1)
			{
				// no FROM was found, so assume it's a partial SELECT x,y,z
				String col = select.substring(lastColStart);
				if (includeAlias)
				{
					result.add(new ElementInfo(col.trim(), lastColStart, select.length()));
				}
				else
				{
					result.add(new ElementInfo(stripColumnAlias(col), lastColStart, select.length()));
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

	private static SQLToken skipCTE(SQLLexer lexer)
	{
		SQLToken token = lexer.getNextToken(false, false);
		int bracketCount = 0;
		while (token != null)
		{
			String text = token.getContents();
			if ("(".equals(text))
			{
				bracketCount ++;
			}
			else if (")".equals(text))
			{
				bracketCount --;
			}

			if ("SELECT".equals(text) && bracketCount == 0)
			{
				return lexer.getNextToken(false, false);
			}
			token = lexer.getNextToken(false, false);
		}
		return token;
	}

	/**
	 * Return parameter literals in the passed sql.
	 *
	 * @param sql the function call that could contain parameter literals
	 * @return all provided parameter literals
	 */
	public static List<String> getFunctionParameters(String sql)
	{
		if (StringUtil.isBlank(sql)) return Collections.emptyList();
		List<String> params = CollectionUtil.arrayList();

		try
		{
			SQLLexer lexer = SQLLexerFactory.createLexer(sql);
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

	public static String stripColumnAlias(String expression)
	{
		if (StringUtil.isEmptyString(expression)) return null;

    int length = expression.length();
    StringBuilder result = new StringBuilder(length);

    char quoteStart = 0;
    boolean inQuotes = false;
    int bracketCount = 0;

    for (int i=0; i < length; i++)
    {
      char c = expression.charAt(i);

      if (Character.isWhitespace(c) && !inQuotes && bracketCount == 0) break;

      if (inQuotes)
      {
        if (c == quoteStart || (quoteStart == '[' && c == ']'))
        {
          inQuotes = false;
        }
      }
      else
      {
        if (c == '"' || c == '`' || c == '[')
        {
          quoteStart = c;
          inQuotes = true;
        }
      }

      if (!inQuotes)
      {
        if (c == '(')
        {
          bracketCount ++;
        }
        if (c == ')')
        {
          bracketCount--;
        }
      }
      result.append(c);
    }
    return result.toString();
	}

	public static List<Alias> getTables(String sql, boolean includeAlias, WbConnection conn)
	{
		return getTables(sql, includeAlias, getCatalogSeparator(conn), getSchemaSeparator(conn), conn);
	}

	public static List<Alias> getTables(String sql, boolean includeAlias, char catalogSeparator, char schemaSeparator, WbConnection conn)
	{
		ParserType type = ParserType.getTypeFromConnection(conn);
		return getTables(sql, includeAlias, catalogSeparator, schemaSeparator, type);
	}

	public static List<Alias> getTables(String sql, boolean includeAlias, char catalogSeparator, char schemaSeparator, ParserType parserType)
	{
		TableListParser parser = new TableListParser(catalogSeparator, schemaSeparator, parserType);
		return parser.getTables(sql, includeAlias);
	}

	/**
	 * Checks if the given SQL if either not a DML statement or - if it is, contains a WHERE clause
	 * @param sql the sql to check
	 * @return true if the sql is not a DML or it contains a WHERE clause.
	 */
	public static boolean isUnRestrictedDML(String sql, WbConnection conn)
	{
		if (StringUtil.isEmptyString(sql)) return false;

		SqlParsingUtil util = SqlParsingUtil.getInstance(conn);
		String verb = util.getSqlVerb(sql);
		if (getDMLVerbs().contains(verb))
		{
			return util.getWherePosition(sql) < 0;
		}
		return false;
	}

	public static String makeCleanSql(String aSql, boolean keepNewlines)
	{
		return makeCleanSql(aSql, keepNewlines, false, false, true);
	}

	public static String makeCleanSql(String aSql, boolean keepNewlines, boolean keepComments)
	{
		return makeCleanSql(aSql, keepNewlines, keepComments, false, true);
	}

	/**
	 * Replaces all white space characters with a single space and removes single line and multi line comments.
   *
   * Whitespace is not removed inside string literals.
   *
	 * @param aSql                     The sql script to "clean out"
   * @param keepNewlines             if true, newline characters (\n) are kept
   * @param keepComments             if true, comments (single line, block comments) are kept
   * @param checkNonStandardComments check for non-standard MySQL single line comments
   * @param removeSemicolon          if true, a trailing semicolon will be removed
   * @return String
 	 */
	public static String makeCleanSql(String aSql, boolean keepNewlines, boolean keepComments, boolean checkNonStandardComments, boolean removeSemicolon)
	{
		if (aSql == null) return null;
		aSql = aSql.trim();
		int count = aSql.length();
		if (count == 0) return aSql;
		boolean inComment = false;
		boolean inQuotes = false;
		boolean lineComment = false;
    char quoteStart = 0;

		StringBuilder newSql = new StringBuilder((int)(count * 0.8));

		char last = ' ';

		int start = StringUtil.findFirstNonWhitespace(aSql);

		for (int i=start; i < count; i++)
		{
			char c = aSql.charAt(i);

			if (c == '\'' || c == '"')
			{
        if (!inQuotes)
        {
          quoteStart = c;
          inQuotes = true;
        }
        else if (c == quoteStart)
        {
          inQuotes = false;
          quoteStart = 0;
        }
			}

			if (inQuotes && (!inComment && !keepComments))
			{
				newSql.append(c);
				last = c;
				continue;
			}

			if (checkNonStandardComments && (last == '\n' || last == '\r' || i == 0 ) && (c == '#'))
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
						if (c == '\n' && newSql.length() > 0)
						{
							newSql.append(' ');
						}
					}
					else if (c != '\n' && (c < 32 || (c > 126 && c < 145) || c == 255))
					{
						newSql.append(' ');
					}
					else
					{
						appendNoLeadingWhitespace(newSql, c);
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
		StringUtil.trimTrailingWhitespace(newSql);
		if (removeSemicolon && StringUtil.endsWith(newSql, ';'))
		{
			StringUtil.removeFromEnd(newSql, 1);
			StringUtil.trimTrailingWhitespace(newSql);
		}
		return newSql.toString();
	}

	private static void appendNoLeadingWhitespace(StringBuilder target, char c)
	{
		if (c <= 32 && target.length() == 0) return;
		target.append(c);
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

	public static boolean isClobType(int sqlType, String dbmsType, DbSettings dbInfo)
	{
		boolean treatLongVarcharAsClob = (dbInfo == null ? false : dbInfo.longVarcharIsClob());
		if (isClobType(sqlType, treatLongVarcharAsClob))
		{
			return true;
		}
		if (dbInfo != null)
		{
			return dbInfo.isClobType(dbmsType);
		}
		return false;
	}

	public static boolean isXMLType(int type)
	{
		return (type == Types.SQLXML);
	}

	public static boolean isClobType(int aSqlType, boolean treatLongVarcharAsClob)
	{
		if (treatLongVarcharAsClob)
		{
			return (aSqlType == Types.CLOB ||
							aSqlType == Types.NCLOB ||
							aSqlType == Types.LONGVARCHAR ||
							aSqlType == Types.LONGNVARCHAR);
		}
		else
		{
			 return isClobType(aSqlType);
		}
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
    clearWarnings(rs);
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

  /**
   * Returns the name of the java.sql.Types constant for the given type value.
   *
   * @param sqlType  the data type corresponding to the java.sql.Types constants
   * @return the name of the type
   */
	public static String getTypeName(int sqlType)
	{
    try
    {
      for (Field field : getSqlTypeFields())
      {
        int type = field.getInt(null);
        if (type == sqlType) return field.getName();
      }
    }
    catch (Throwable th)
    {
    }
    return "OTHER";
  }

	/**
	 * Construct the SQL display name for the given SQL datatype.
	 * This is used when re-recreating the source for a table
	 */
	public static String getSqlTypeDisplay(String typeName, int sqlType, int size, int digits)
	{
		if (typeName == null) return "";

		String display = typeName;

		switch (sqlType)
		{
			case Types.VARCHAR:
			case Types.CHAR:
			case Types.NVARCHAR:
			case Types.NCHAR:
				// Postgres' text datatype and MySQL's XXXtext types do not have a size parameter
				if (CHAR_TYPES_WITHOUT_LENGTH.contains(typeName)) return typeName;

				if (size > 0 && typeName.indexOf('(') == -1)
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
				break;

			case Types.DECIMAL:
			case Types.NUMERIC:
				// SQL Server
				if ("money".equalsIgnoreCase(typeName)) return typeName;

				if ((typeName.indexOf('(') == -1))
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

	public static boolean ignoreWarning(WbConnection con, SQLWarning warning)
	{
		if (warning == null || con == null) return true;
		DbSettings dbs = con.getDbSettings();
		if (dbs == null) return false;
		Set<Integer> codes = dbs.getInformationalWarningCodes();
		if (codes.contains(warning.getErrorCode())) return true;

		Set<String> states = dbs.getInformationalWarningStates();
		return states.contains(warning.getSQLState());
	}

	public static CharSequence getWarnings(WbConnection con, Statement stmt)
	{
		if (con == null) return null;

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
			Set<String> added = new HashSet<>();
			StringBuilder msg = null;
			String s = null;

			int count = 0;
			int maxLoops = con.getDbSettings().getMaxWarnings();

			SQLWarning warn = (stmt == null ? null : stmt.getWarnings());

			while (warn != null)
			{
        if (ignoreWarning(con, warn) == false)
        {
          count ++;
          s = warn.getMessage();
          if (s != null && s.length() > 0)
          {
            msg = append(msg, s);
            if (!StringUtil.endsWith(s,'\n')) msg.append('\n');
            added.add(s);
          }
        }

        // prevent endless loop
        if (count > maxLoops)
        {
          LogMgr.logWarning("SqlUtil.getWarnings()", "Breaking out of loop because" + maxLoops + " iterations reached!");
          break;
        }

        // prevent endless loop
				if (warn == warn.getNextWarning()) break;
				warn = warn.getNextWarning();
			}

      // now process warnings on the connection object
			warn = con.getSqlConnection().getWarnings();

			count = 0;
			while (warn != null)
			{
        if (ignoreWarning(con, warn) == false)
        {
          s = warn.getMessage();

          // Some JDBC drivers duplicate the warnings between
          // the statement and the connection.
          // This check is here to prevent adding them twice
          if (!added.contains(s))
          {
            msg = append(msg, s);
            if (!StringUtil.endsWith(s,'\n')) msg.append('\n');
          }
        }
        // prevent endless loop
        if (count > maxLoops)
        {
          LogMgr.logWarning("SqlUtil.getWarnings()", "Breaking out of loop because" + maxLoops + " iterations reached!");
          break;
        }
				// prevent endless loop
				if (warn == warn.getNextWarning()) break;
				warn = warn.getNextWarning();
			}

			// make sure the warnings are cleared from both objects!
			clearWarnings(con, stmt);
			StringUtil.trimTrailingWhitespace(msg);
			return msg;
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("SqlUtil.getWarnings()", "Error retrieving warnings", e);
			return null;
		}
	}

  public static void clearWarnings(ResultSet rs)
  {
    try
    {
      if (rs != null) rs.clearWarnings();
    }
    catch (Throwable th)
    {
    }
  }

	public static void clearWarnings(WbConnection con, Statement stmt)
	{
    clearWarnings(con);
    clearWarnings(stmt);
	}

	public static void clearWarnings(WbConnection con)
	{
		try
		{
			if (con != null) con.clearWarnings();
		}
		catch (Throwable th)
		{
		}
	}

	public static void clearWarnings(Statement stmt)
	{
		try
		{
			if (stmt != null) stmt.clearWarnings();
		}
		catch (Throwable th)
		{
		}
	}

	private static StringBuilder append(StringBuilder msg, CharSequence s)
	{
		if (msg == null) msg = new StringBuilder(100);
		msg.append(s);
		return msg;
	}

	public static String buildExpression(WbConnection conn, DbObject object)
	{
		if (object == null) return null;
		return buildExpression(conn, object.getCatalog(), object.getSchema(), object.getObjectName());
	}

	/**
	 * Return a fully qualified name string for the database object.
	 *
	 * Unlike {@link #buildExpression(workbench.db.WbConnection, workbench.db.DbObject)} it will
	 * not leave out some parts if they are not needed (e.g. because the catalog or schema is the
	 * "current" and is currently not needed.
	 *
	 * @param conn    the connection to use, may be null
	 * @param object  the object
	 * @return a fully qualified name for the object.
	 *
	 * @see #buildExpression(workbench.db.WbConnection, workbench.db.DbObject)
	 * @see DbMetadata#ignoreCatalog(java.lang.String)
	 * @see DbMetadata#ignoreSchema(java.lang.String)
	 */
	public static String fullyQualifiedName(WbConnection conn, DbObject object)
	{
		if (object == null) return null;
		StringBuilder result = new StringBuilder(30);
		QuoteHandler quoter = (conn != null ? conn.getMetadata() : QuoteHandler.STANDARD_HANDLER);

		boolean supportsCatalogs = (conn != null ? conn.getDbSettings().supportsCatalogs() : true);
		boolean supportsSchemas = (conn != null ? conn.getDbSettings().supportsSchemas() : true);

		char catalogSeparator = SqlUtil.getCatalogSeparator(conn);
		char schemaSeparator = SqlUtil.getSchemaSeparator(conn);
		if (supportsCatalogs && StringUtil.isNonEmpty(object.getCatalog()))
		{
			result.append(quoter.quoteObjectname(object.getCatalog()));
			result.append(catalogSeparator);
		}
		if (supportsSchemas && StringUtil.isNonEmpty(object.getSchema()))
		{
			result.append(quoter.quoteObjectname(object.getSchema()));
			result.append(schemaSeparator);
		}
		result.append(quoter.quoteObjectname(object.getObjectName()));
		return result.toString();
	}

	/**
	 * Get a fully qualified expression for the given table.
	 *
	 * The individual parts will be quoted if this is required.
	 *
	 * @param conn     the connection to use, may be null
	 * @param catalog  the catalog to use, may be null
	 * @param schema   the schema to use, may be null
	 * @param name     the object name. Must not be null
	 *
	 * @return a full qualfied name with all non-null elements and optionally quoted.
	 */
	public static String buildExpression(WbConnection conn, String catalog, String schema, String name)
	{
    if (StringUtil.isEmptyString(name)) return null;

		StringBuilder result = new StringBuilder(30);
		DbMetadata meta = (conn != null ? conn.getMetadata() : null);
		if (meta == null)
		{
			if (StringUtil.isNonEmpty(catalog))
			{
				result.append(SqlUtil.quoteObjectname(catalog, false));
				result.append('.');
			}
			if (StringUtil.isNonEmpty(schema))
			{
				result.append(SqlUtil.quoteObjectname(schema, false));
				result.append('.');
			}
			result.append(SqlUtil.quoteObjectname(name, false));
		}
		else
		{
			char catalogSeparator = meta.getCatalogSeparator();
			char schemaSeparator = meta.getSchemaSeparator();
			if (StringUtil.isNonEmpty(catalog) && !conn.getMetadata().ignoreCatalog(catalog))
			{
				result.append(meta.quoteObjectname(catalog));
				result.append(catalogSeparator);
			}
			if (StringUtil.isNonEmpty(schema) && !conn.getMetadata().ignoreSchema(schema))
			{
				result.append(meta.quoteObjectname(schema));
				result.append(schemaSeparator);
			}
			result.append(meta.quoteObjectname(name));
		}
		return result.toString();
	}

	/**
	 * Check if this column can potentially contain multiline values.
	 * XML, CLOB and large VARCHAR columns are considered to contain multine values.
	 * This is used to detect which renderer to use in the result set display.
	 *
	 * @param column the column to test
	 */
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

	public static void appendAndCondition(StringBuilder baseSql, String column, String value, WbConnection con)
	{
		if (StringUtil.isNonEmpty(value) && StringUtil.isNonEmpty(column))
		{
			baseSql.append(" AND ");
			appendExpression(baseSql, column, value, con);
		}
	}

	/**
	 * Appends an AND condition for the given column. If the value contains
	 * a wildcard the condition will use LIKE, otherwise =
	 *
	 * @param baseSql
	 * @param column
	 * @param value
	 */
	public static void appendExpression(StringBuilder baseSql, String column, String value, WbConnection con)
	{
		if (StringUtil.isEmptyString(value)) return;
		if (StringUtil.isEmptyString(column)) return;

		baseSql.append(column);
		boolean isLike = false;
		if (value.indexOf('%') > -1)
		{
			baseSql.append(" LIKE '");
			baseSql.append(escapeUnderscore(value, con));
			isLike = true;
		}
		else
		{
			baseSql.append(" = '");
			baseSql.append(value);
		}
		baseSql.append("'");
		if (isLike && con != null)
		{
			appendEscapeClause(baseSql, con, value);
		}
	}

	/**
	 * Remove all invalid characters from the input string.
	 *
	 * Only letters, digits and the underscore will be retained. This
	 * can be used to create SQL identifiers from any input
	 *
	 * @param identifier
	 * @return a clean version of the input string
	 */
	public static String cleanupIdentifier(String identifier)
	{
    if (identifier == null) return "";
		return identifier.replaceAll("[^A-Za-z0-9_]+", "");
	}

	public static String getIsolationLevelName(int level)
	{
		switch (level)
		{
			case Connection.TRANSACTION_READ_COMMITTED:
				return "READ COMMITTED";
			case Connection.TRANSACTION_READ_UNCOMMITTED:
				return "READ UNCOMMITTED";
			case Connection.TRANSACTION_REPEATABLE_READ:
				return "REPEATABLE READ";
			case Connection.TRANSACTION_SERIALIZABLE:
				return "SERIALIZABLE";
			case Connection.TRANSACTION_NONE:
				return "NONE";
			default:
				return "unknown";
		}
	}

	public static String replaceParameters(CharSequence sql, Object ... values)
	{
		if (values == null) return null;
		if (values.length == 0) return sql.toString();

		int valuePos = 0;
		SQLLexer lexer = SQLLexerFactory.createLexer(sql);
		SQLToken t = lexer.getNextToken(true, true);
		StringBuilder result = new StringBuilder(sql.length() + values.length * 5);

		while (t != null)
		{
			if (t.getText().equals("?") && valuePos < values.length)
			{
				Object v = values[valuePos];

				if (v instanceof String)
				{
					result.append('\'');
					result.append(v.toString());
					result.append('\'');
				}
				else if (v != null)
				{
					result.append(v.toString());
				}
				else
				{
					result.append("NULL");
				}
				valuePos ++;
			}
			else
			{
				result.append(t.getText());
			}
			t = lexer.getNextToken(true, true);
		}
		return result.toString();
	}

	public static void dumpResultSetInfo(String methodName, ResultSetMetaData meta)
	{
		if (!LogMgr.isDebugEnabled()) return;

		if (meta == null) return;
		try
		{
			int columns = meta.getColumnCount();
			StringBuilder out = new StringBuilder(columns * 20);
			out.append(methodName);
			out.append(" returned: ");
			for (int col=1; col <= columns; col++)
			{
				if (col > 1) out.append(", ");
				out.append(Integer.toString(col));
				out.append(": [");
				out.append(meta.getColumnName(col));
				out.append(']');
			}
			LogMgr.logDebug("SqlUtil.dumpResultSetInfo()", out.toString());
		}
		catch (Exception e)
		{
			LogMgr.logWarning("SqlUtil.dumpResultSetInfo()", "Could not access ResultSetMetaData", e);
		}
	}

	public static SQLToken getOperatorBeforeCursor(String sql, int cursor)
	{
		if (StringUtil.isBlank(sql)) return null;
		SQLLexer lexer = SQLLexerFactory.createLexer(sql);
		List<SQLToken> tokens = new ArrayList<>();
		SQLToken token = lexer.getNextToken(false, false);
		while (token != null)
		{
			tokens.add(token);
			token = lexer.getNextToken(false, false);
			if (token != null && token.getCharEnd() > cursor) break;
		}
		Set<String> comparisonOperators  = CollectionUtil.caseInsensitiveSet("IN", "ANY", "ALL");
		for (int i = tokens.size() - 1; i >= 0; i--)
		{
			SQLToken tk = tokens.get(i);
			if (tk.getCharEnd() <= cursor && (tk.isOperator() || comparisonOperators.contains(tk.getContents())))
			{
				return tokens.get(i);
			}
		}
		return null;
	}

	public static String getBaseTypeName(String dbmsType)
	{
		if (dbmsType == null) return null;
		int pos = dbmsType.indexOf('(');
		if (pos == -1)
		{
			pos = dbmsType.indexOf('[');
		}
		if (pos == -1)
		{
			pos = dbmsType.indexOf(' ');
		}
		if (pos > -1)
		{
			return dbmsType.substring(0, pos);
		}
		return dbmsType;
	}

	public static String getErrorIndicator(String sql, ErrorDescriptor errorPosition)
	{
		if (errorPosition == null) return null;

		int offset = errorPosition.getErrorPosition();
		if (offset < 0) return null;

		int start = StringUtil.getLineStart(sql, offset);
		int end = StringUtil.getLineEnd(sql, offset);

		String msg = null;
		if (start > -1 && end > start)
		{
			String line = sql.substring(start, end);
			String indicator = StringUtil.padRight("", offset - start) + "^";
			msg = line + "\n" + indicator;
		}
		return msg;
	}

	public static int getRealStart(String sql)
	{
		if (StringUtil.isEmptyString(sql)) return 0;
		SQLLexer lexer = SQLLexerFactory.createLexer(sql);
		SQLToken token = lexer.getNextToken(false, false);
		if (token == null)
		{
			return 0;
		}
		return token.getCharBegin();
	}

	/**
	 * Calculate the line and column determined by the error position stored in the ErrorDescriptor.
	 *
	 * If no error position is stored in the descriptor or it already contains a line and column
	 * nothing will be changed.
	 *
	 * The ErrorDescriptor will be updated with the calculated values
	 *
	 * @param sql    the original SQL statement
	 * @param error  the error descriptor.
	 */
	public static void calculateErrorLine(String sql, ErrorDescriptor error)
	{
		if (error == null) return;
		if (StringUtil.isEmptyString(sql)) return;
		if (error.getErrorLine() > -1 && error.getErrorColumn() > -1) return;
		int errorPos = error.getErrorPosition();
		if (errorPos == -1) return;

		int length = sql.length();
		int pos = 0;
		int currentLine = 0;
		int currentColumn =  0;

		while (pos < length)
		{
			if (pos == errorPos)
			{
				error.setErrorPosition(currentLine, currentColumn);
				return;
			}
			char c = sql.charAt(pos);
			if (c == '\r' || c == '\n')
			{
				pos++;

				// check if this is a Windows/DOS newline
				if (c == '\r' && pos < length && sql.charAt(pos) == '\n')
				{
					// multi-character newline --> skip the \n
					pos++;
				}
				currentLine++;
				currentColumn = 0;
			}
			else
			{
				currentColumn ++;
				pos ++;
			}
		}
	}

	/**
	 * Return the offset of the error location based on the line/column information.
	 *
	 * If the error descriptor already contains an error offset, that offset is returned.
	 *
	 * @param sql    the original SQL statement
	 * @param error  the error descriptor
	 * @return the offset into the SQL statement
	 *
	 * @see ErrorDescriptor#getErrorPosition()
	 */
	public static int getErrorOffset(String sql, ErrorDescriptor error)
	{
		if (error == null) return -1;
		if (error.getErrorPosition() > -1) return error.getErrorPosition();
		if (StringUtil.isEmptyString(sql)) return -1;
		int length = sql.length();
		int errorLine = error.getErrorLine();
		int errorColumn = error.getErrorColumn();

		int currentLine = 0;
		int currentColumn = 0;
		int pos = 0;
		while (pos < length)
		{
			if (currentLine == errorLine && currentColumn == errorColumn)
			{
				return pos;
			}

			char c = sql.charAt(pos);
			if (c == '\r' || c == '\n')
			{
				pos ++;

				// check if this is a Windows/DOS newline
				if (c == '\r' && pos < length && sql.charAt(pos) == '\n')
				{
					// multi-character newline --> skip the \n
					pos ++;
				}
				currentLine ++;
				currentColumn = 0;
			}
			else
			{
				currentColumn ++;
				pos ++;
			}
		}
		return -1;
	}

	public static DataStore getResult(WbConnection conn, String sql)
	{
		return getResult(conn, sql, conn.getDbSettings().useSavePointForDML());
	}

	public static DataStore getResult(WbConnection conn, String sql, boolean useSavepoint)
	{
		try
		{
			return getResultData(conn, sql, useSavepoint);
		}
		catch (SQLException ex)
		{
			LogMgr.logError("SqlUtil.getResult()", "Could not retrieve results", ex);
		}
		return null;
	}

	public static DataStore getResultData(WbConnection conn, String sql, boolean useSavepoint)
		throws SQLException
	{
		ResultSet rs = null;
		Statement stmt = null;
		Savepoint sp = null;
		DataStore ds = null;
		try
		{
			sp = (useSavepoint ? conn.setSavepoint() : null);
			stmt = conn.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			ds = new DataStore(rs, true);
			ds.setGeneratingSql(sql);
			ds.resetStatus();
			conn.releaseSavepoint(sp);
		}
		catch (SQLException ex)
		{
			conn.rollback(sp);
			throw ex;
		}
		finally
		{
			closeAll(rs, stmt);
		}
		return ds;
	}

  public static String getIdentifierAtCursor(TextContainer display, WbConnection conn)
  {
    String text = display.getSelectedText();
    if (StringUtil.isEmptyString(text))
    {
			// Use valid SQL characters including schema/catalog separator and the quote character
      // for the list of allowed (additional) word characters so that fully qualified names (public.foo)
      // and quoted names ("public"."Foo") are identified correctly

      // this will not cover more complex situations where non-standard names are used inside quotes (e.g. "Stupid Name")
      // but will cover most of the usual situations
      String wordChars = "_$";

      char schemaSeparator = SqlUtil.getSchemaSeparator(conn);
      wordChars += schemaSeparator;
      char catSeparator = SqlUtil.getCatalogSeparator(conn);

      if (catSeparator != schemaSeparator)
      {
        wordChars += catSeparator;
      }

      // now add the quote character used by the DBMS
      wordChars += conn.getMetadata().getIdentifierQuoteCharacter();

      if (conn.getMetadata().isSqlServer())
      {
        // add the stupid Microsoft quoting stuff
        wordChars += "[]";
      }

      text = display.getWordAtCursor(wordChars);
    }
    return text;
  }


  public static boolean isReplaceDDL(String sql, WbConnection connection, DropType dropType)
  {
    if (sql == null) return false;
    if (connection == null) return false;
    String verb = connection.getParsingUtil().getSqlVerb(sql);
    return isReplaceDDL(verb, dropType);
  }

  public static boolean isReplaceDDL(String verb, DropType dropType)
  {
    if (dropType == DropType.none) return false;
    if (verb == null) return false;
    return verb.equals("CREATE OR REPLACE") || verb.equalsIgnoreCase("REPLACE");
  }
}
