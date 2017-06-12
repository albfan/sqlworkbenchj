/*
 * WbSqlFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.sql.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import workbench.resource.GeneratedIdentifierCase;
import workbench.resource.Settings;

import workbench.sql.CommandMapper;
import workbench.sql.SqlCommand;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.syntax.SqlKeywordHelper;
import workbench.sql.wbcommands.CommandTester;

import workbench.util.ArgumentParser;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import static workbench.resource.GeneratedIdentifierCase.*;

/**
 * A class to format (pretty-print) SQL statements.
 *
 * @author Thomas Kellerer
 */
public class WbSqlFormatter
  implements SqlFormatter
{
	private final Set<String> LINE_BREAK_BEFORE = CollectionUtil.unmodifiableSet(
		"SELECT", "SET", "FROM", "WHERE", "ORDER BY", "GROUP BY", "HAVING", "VALUES",
		"UNION", "UNION ALL", "MINUS", "INTERSECT", "REFRESH", "AS", "FOR", "JOIN",
		"INNER JOIN", "RIGHT OUTER JOIN", "LEFT OUTER JOIN", "CROSS JOIN", "LEFT JOIN",
		"RIGHT JOIN", "START WITH", "CONNECT BY", "OUTER APPLY", "CROSS APPLY", "WINDOW");

	private final Set<String> LINE_BREAK_AFTER = CollectionUtil.unmodifiableSet(
		"UNION", "UNION ALL", "MINUS", "INTERSECT", "AS", "FOR");

  // keywords terminating a HAVING clause
	public static final Set<String> HAVING_TERMINAL = CollectionUtil.unmodifiableSet(
		"ORDER BY", "GROUP BY", "UNION", "UNION ALL", "INTERSECT",
		"MINUS", "WINDOW", ";");

	// keywords terminating a WHERE clause
	public static final Set<String> WHERE_TERMINAL = CollectionUtil.unmodifiableSet(HAVING_TERMINAL, "HAVING", "WITH");

	// keywords terminating the FROM part
	public static final Set<String> FROM_TERMINAL = CollectionUtil.unmodifiableSet(WHERE_TERMINAL, "WHERE", "START WITH", "CONNECT BY");

	// keywords terminating a a JOIN clause
	public static final Set<String> JOIN_TERMINAL = CollectionUtil.unmodifiableSet("WHERE", "ORDER BY", "GROUP BY", "UNION", "UNION ALL");

	// keywords terminating an GROUP BY clause
	private final Set<String> GROUP_BY_TERMINAL = CollectionUtil.caseInsensitiveSet(WHERE_TERMINAL,
		"SELECT", "UPDATE", "DELETE", "INSERT", "CREATE", "CREATE OR REPLACE");

	private final Set<String> CREATE_TABLE_TERMINAL = CollectionUtil.unmodifiableSet(
		"UNIQUE", "CONSTRAINT", "FOREIGN KEY", "PRIMARY KEY");

	private final Set<String> DATE_LITERALS = CollectionUtil.unmodifiableSet("DATE", "TIME", "TIMESTAMP");

	private final Set<String> ORDER_BY_TERMINAL = CollectionUtil.unmodifiableSet(";");

	public static final Set<String> SELECT_TERMINAL = CollectionUtil.unmodifiableSet("FROM");

	public static final Set<String> SET_TERMINAL = CollectionUtil.unmodifiableSet("FROM", "WHERE");

	public static final Set<String> QUERY_START = CollectionUtil.unmodifiableSet("SELECT", "WITH");

  private static final Set<String> INDEX_OPTIONS = CollectionUtil.unmodifiableSet(
    "UNIQUE", "BITMAP", "HASH", "CLUSTERED", "NONCLUSTERED", "FULLTEXT", "SPATIAL",
    "ONLINE", "OFFLINE");

	private CharSequence sql;
	private SQLLexer lexer;
	private StringBuilder result;
	private StringBuilder indent = null;
	private StringBuilder leadingWhiteSpace = null;
	private int realLength = 0;
	private int maxSubselectLength = 60;

	private Set<String> dbFunctions = CollectionUtil.caseInsensitiveSet();
	private Set<String> dataTypes = CollectionUtil.caseInsensitiveSet();
	private Set<String> keywords = CollectionUtil.caseInsensitiveSet();
	private Set<String> createTableTypes = CollectionUtil.caseInsensitiveSet();
	private Set<String> createViewTypes = CollectionUtil.caseInsensitiveSet();

	private static final String NL = "\n";
	private boolean addColumnCommentForInsert;
	private boolean newLineForSubSelects;
	private GeneratedIdentifierCase keywordCase = GeneratedIdentifierCase.upper;
	private GeneratedIdentifierCase identifierCase = GeneratedIdentifierCase.asIs;
	private GeneratedIdentifierCase functionCase = GeneratedIdentifierCase.lower;
	private GeneratedIdentifierCase dataTypeCase = GeneratedIdentifierCase.upper;
  private boolean indentWhereConditions;
	private boolean addSpaceAfterComma;
	private boolean commaAfterLineBreak;
	private boolean addSpaceAfterLineBreakComma;
	private boolean indentInsert = true;
	private JoinWrapStyle joinWrapping = JoinWrapStyle.onlyMultiple;
	private String dbId;
	private char catalogSeparator = '.';
	private int colsPerInsert = -1;
	private int colsPerUpdate = -1;
	private int colsPerSelect = -1;
  private char schemaSeparator = '.';

	WbSqlFormatter(CharSequence aScript)
	{
		this(aScript, 0, Settings.getInstance().getFormatterMaxSubselectLength(), null);
	}

	public WbSqlFormatter(CharSequence aScript, String dbId)
	{
		this(aScript, 0, Settings.getInstance().getFormatterMaxSubselectLength(), dbId);
	}

	public WbSqlFormatter(CharSequence aScript, int maxLength, String dbId)
	{
		this(aScript, 0, maxLength, dbId);
	}

	public WbSqlFormatter(int maxLength, String dbId)
	{
		this(null, 0, maxLength, dbId);
	}

	WbSqlFormatter(CharSequence aScript, int maxLength)
	{
		this(aScript, 0, maxLength, null);
	}

	public void setCatalogSeparator(char separator)
	{
		this.catalogSeparator = separator;
	}

	public void setSchemaSeparator(char separator)
	{
		this.schemaSeparator = separator;
	}

	private WbSqlFormatter(CharSequence aScript, int indentCount, int maxLength, String dbId)
	{
		this.sql = aScript;
		if (indentCount > 0)
		{
			this.indent = new StringBuilder(indentCount);
			for (int i=0; i < indentCount; i++) this.indent.append(' ');
		}
		maxSubselectLength = maxLength;
		dbFunctions = CollectionUtil.caseInsensitiveSet();
		functionCase = Settings.getInstance().getFormatterFunctionCase();
		dataTypeCase = Settings.getInstance().getFormatterDatatypeCase();
		newLineForSubSelects = Settings.getInstance().getFormatterSubselectInNewLine();
		addColumnCommentForInsert = Settings.getInstance().getFormatterAddColumnNameComment();
		keywordCase = Settings.getInstance().getFormatterKeywordsCase();
		addSpaceAfterComma = Settings.getInstance().getFormatterAddSpaceAfterComma();
		indentWhereConditions = Settings.getInstance().getFormatterIndentWhereConditions();
		commaAfterLineBreak = Settings.getInstance().getFormatterCommaAfterLineBreak();
		addSpaceAfterLineBreakComma = Settings.getInstance().getFormatterAddSpaceAfterLineBreakComma();
		joinWrapping = Settings.getInstance().getFormatterJoinWrapStyle();
		indentInsert = Settings.getInstance().getFormatterIndentInsert();
		identifierCase = Settings.getInstance().getAutoCompletionPasteCase();
		setDbId(dbId);
	}

  @Override
  public boolean supportsMultipleStatements()
  {
    return false;
  }

	public void setIdentifierCase(GeneratedIdentifierCase idCase)
	{
		this.identifierCase = idCase;
	}

  public void setIndentWhereCondition(boolean flag)
  {
    this.indentWhereConditions = flag;
  }

	public void setColumnsPerInsert(int cols)
	{
		if (cols > 0)
		{
			colsPerInsert = cols;
		}
	}

	private int getColumnsPerInsert()
	{
		if (colsPerInsert < 0)
		{
			return Settings.getInstance().getFormatterMaxColumnsInInsert();
		}
		return colsPerInsert;
	}

	public void setColumnsPerUpdate(int cols)
	{
		if (cols > 0)
		{
			colsPerUpdate = cols;
		}
	}

	private int getColumnsPerUpdate()
	{
		if (colsPerUpdate < 0)
		{
			return Settings.getInstance().getFormatterMaxColumnsInUpdate();
		}
		return colsPerUpdate;
	}

	public void setColumnsPerSelect(int cols)
	{
		if (cols > 0)
		{
			colsPerSelect = cols;
		}
	}

	private int getColumnsPerSelect()
	{
		if (colsPerSelect < 0)
		{
			return Settings.getInstance().getFormatterMaxColumnsInSelect();
		}
		return colsPerSelect;
	}

	public void setJoinWrapping(JoinWrapStyle style)
	{
		joinWrapping = style;
	}

	public void setNewLineForSubselects(boolean flag)
	{
		newLineForSubSelects = flag;
	}

	/**
	 * Controls if column names are added to the VALUES part of an INSERT statement.
	 */
	public void setAddColumnNameComment(boolean flag)
	{
		addColumnCommentForInsert = flag;
	}

	public void setKeywordCase(GeneratedIdentifierCase kwCase)
	{
		this.keywordCase = kwCase;
	}

	public void setFunctionCase(GeneratedIdentifierCase funcCase)
	{
		this.functionCase = funcCase;
	}

	public String getLineEnding()
	{
		return NL;
	}

	private void setDbId(String id)
	{
		this.dbId = id;
		SqlKeywordHelper helper = new SqlKeywordHelper(dbId);
		keywords.clear();
		dataTypes.clear();
		dbFunctions.clear();
    createTableTypes.clear();
    createViewTypes.clear();
		keywords.addAll(helper.getKeywords());
		keywords.addAll(helper.getReservedWords());
		dataTypes.addAll(helper.getDataTypes());
		dbFunctions.addAll(helper.getSqlFunctions());
    createTableTypes.addAll(helper.getCreateTableTypes());
    createViewTypes.addAll(helper.getCreateViewTypes());
    keywords.addAll(createTableTypes);
    keywords.addAll(createViewTypes);
	}

	private boolean isDbFunction(SQLToken token)
	{
		if (token == null) return false;
		return isDbFunction(token.getText());
	}

	private boolean isDbFunction(String key)
	{
		if (dbFunctions == null)
		{
			SqlKeywordHelper keyWords = new SqlKeywordHelper();
			dbFunctions = keyWords.getSqlFunctions();
		}
		return this.dbFunctions.contains(key);
	}

	private boolean isDatatype(String key)
	{
		return this.dataTypes.contains(key);
	}

	private boolean isKeyword(String verb)
	{
		return keywords.contains(verb);
	}

	public void setAddSpaceAfterCommInList(boolean flag)
	{
		addSpaceAfterComma = flag;
	}

	public void setCommaAfterLineBreak(boolean flag)
	{
		this.commaAfterLineBreak = flag;
	}

	public void setAddSpaceAfterLineBreakComma(boolean flag)
	{
		addSpaceAfterLineBreakComma = flag;
	}

	/**
	 * Adds the passed set of DBMS specific functions to the list
	 * of already existing database functions.
	 *
	 * Duplicates will be removed.
	 *
	 * This is for testing purposes only.
	 */
	void addDBFunctions(Set<String> functionNames)
	{
		this.dbFunctions.addAll(functionNames);
	}

	private void saveLeadingWhitespace()
	{
		if (this.sql.length() == 0) return;
		char c = this.sql.charAt(0);
		int pos = 0;
		if (!Character.isWhitespace(c)) return;

		this.leadingWhiteSpace = new StringBuilder(50);
		while (Character.isWhitespace(c))
		{
			this.leadingWhiteSpace.append(c);
			pos ++;
			if (pos >= sql.length()) break;
			c = this.sql.charAt(pos);
		}
		this.sql = this.sql.toString().trim();
	}

  @Override
	public String getFormattedSql(String sqlStatement)
  {
    this.sql = sqlStatement;
    return getFormattedSql();
  }

	public String getFormattedSql()
	{
		saveLeadingWhitespace();
		if (this.sql.length() == 0) return "";

		this.lexer = SQLLexerFactory.createLexerForDbId(dbId, this.sql);
		this.result = new StringBuilder(this.sql.length() + 100);

		this.formatSql();
		StringUtil.trimTrailingWhitespace(result);
		if (this.leadingWhiteSpace != null)
		{
			this.result.insert(0, this.leadingWhiteSpace);
		}
		return this.result.toString();
	}

	private int getRealLength()
	{
		return this.realLength;
	}

	public void setMaxSubSelectLength(int max)
	{
		this.maxSubselectLength = max;
	}

	private int getCurrentLineLength()
	{
		int c = this.result.length() - 1;
		int pos = 0;
		while (this.result.charAt(c) != '\n' && c > 0)
		{
			pos ++;
			c --;
		}
		return pos;
	}

	private void appendNewline()
	{
		if (this.result.length() == 0) return;
		this.result.append(WbSqlFormatter.NL);
		if (this.indent != null) this.result.append(indent);
	}


	private boolean lastCharIsWhitespace()
	{
		int len = this.result.length();
		if (len == 0) return false;
		char c = this.result.charAt(len -1);
		return Character.isWhitespace(c);
	}

	private void appendText(char c)
	{
		this.realLength++;
		this.result.append(c);
	}

	private void appendTokenText(SQLToken t)
	{
    if (t == null) return;
    appendText(getTokenText(t));
  }

	private void appendIdentifier(String text)
	{
    if (identifierCase == asIs || isQuotedIdentifier(text))
    {
      appendText(text);
    }
    else if (identifierCase == lower)
    {
      appendText(text.toLowerCase());
    }
    else if (identifierCase == upper)
    {
      appendText(text.toUpperCase());
    }
  }

	private String getTokenText(SQLToken t)
	{
		if (t == null) return null;
		String text = t.getText();

    if (isDbFunction(t))
		{
			if (functionCase == GeneratedIdentifierCase.lower)
			{
				text = text.toLowerCase();
			}
			else if (functionCase == GeneratedIdentifierCase.upper)
			{
				text = text.toUpperCase();
			}
		}
    else if (isDatatype(text))
    {
      if (dataTypeCase == GeneratedIdentifierCase.upper)
      {
        text = text.toUpperCase();
      }
      else if (dataTypeCase == GeneratedIdentifierCase.lower)
      {
        text = text.toLowerCase();
      }
    }
		else if (isKeyword(text))
		{
			if (keywordCase == GeneratedIdentifierCase.upper)
			{
				text = text.toUpperCase();
			}
			else if (keywordCase == GeneratedIdentifierCase.lower)
			{
				text = text.toLowerCase();
			}
		}
		else if (t.isIdentifier() && !isQuotedIdentifier(text))
		{
			if (identifierCase == lower)
			{
				text = text.toLowerCase();
			}
			else if (identifierCase == upper)
			{
				text = text.toUpperCase();
			}
		}

		return text;
	}

	private void appendComment(String text)
	{
		appendComment(text, true);
	}

	private void appendComment(String text, boolean checkStartOfLine)
	{
		if (text.startsWith("--"))
		{
			if (checkStartOfLine && !this.isStartOfLine()) this.appendNewline();
		}
		else
		{
			if (!this.lastCharIsWhitespace()) this.appendText(' ');
		}
		this.appendText(text);
		if (text.startsWith("--"))
		{
			this.appendNewline();
		}
		else
		{
			this.appendText(' ');
		}
	}

	private void appendText(String text)
	{
		this.realLength += text.length();
		this.result.append(text);
	}

	private void appendText(StringBuilder text)
	{
		if (text.length() == 0) return;
		this.realLength += text.length();
		this.result.append(text);
	}

	private void indent(String text)
	{
		this.result.append(text);
	}

	private void indent(int spaces)
	{
		for (int i=0; i < spaces; i++)
		{
			result.append(' ');
		}
	}

	private void indent(CharSequence text)
	{
		this.result.append(text);
	}

	private boolean needsWhitespace(SQLToken last, SQLToken current)
	{
		return this.needsWhitespace(last, current, false, false);
	}

	/**
	 * 	Return true if a whitespace should be added before the current token.
	 */
	private boolean needsWhitespace(SQLToken last, SQLToken current, boolean ignoreStartOfline)
	{
		return needsWhitespace(last, current, ignoreStartOfline, false);
	}
	private boolean needsWhitespace(SQLToken last, SQLToken current, boolean ignoreStartOfline, boolean possibleFunction)
	{
		if (last == null) return false;
		if (current.isWhiteSpace()) return false;
		if (last.isWhiteSpace()) return false;
		String lastText = last.getContents();
		String currentText = current.getContents();
		char lastChar = lastText.charAt(lastText.length() - 1);
		char currChar = currentText.charAt(0);
		if (isSchemaOrCatalogSeparator(currChar)) return false;
		if (!ignoreStartOfline && this.isStartOfLine()) return false;
		boolean isCurrentOpenBracket = "(".equals(currentText);
		boolean isLastOpenBracket = "(".equals(lastText);
		boolean isLastCloseBracket = ")".equals(lastText);

		if (lastText.equals("N") && currChar == '\'') return false; // handle N'foo' literals
		if (last.isComment() && lastText.startsWith("--")) return false;
		if (DATE_LITERALS.contains(lastText) && current.isLiteral()) return true;
		if (lastText.endsWith("'") && currentText.equals("''")) return false;
		if (lastText.endsWith("'") && currentText.equals("}")) return false;
		if (lastText.equals("''") && currentText.startsWith("'")) return false;
		if (lastChar == '\'' && currChar == '\'') return false;
		if (lastText.endsWith("]") && (current.isReservedWord() || current.isIdentifier() || current.isLiteral() || current.isOperator())) return true;

		if (isCurrentOpenBracket && isDbFunction(lastText)) return false;
		if (isCurrentOpenBracket && isDatatype(currentText)) return false;
		if (isCurrentOpenBracket && isKeyword(lastText)) return true;
		if (possibleFunction)
		{
			if (isCurrentOpenBracket && last.isIdentifier()) return false;
		}
		else
		{
			if (isCurrentOpenBracket && last.isIdentifier()) return true;
		}
		if (isLastCloseBracket && currChar == ',') return false;
		if (isLastCloseBracket && (current.isIdentifier() || isKeyword(currentText))) return true;

		if ((lastChar == '-' || lastChar == '+') && current.isLiteral() && StringUtil.isNumber(currentText)) return true;

		if (last.isLiteral() && (current.isIdentifier() || isKeyword(currentText) || current.isOperator())) return true;

		if (currChar == '?') return true;
		if (currChar == '=') return true;
		if (lastChar == '=') return true;
		if (lastChar == '[' && !last.isIdentifier()) return false;

    if (isSchemaOrCatalogSeparator(lastChar) && current.isIdentifier()) return false;
		if (isSchemaOrCatalogSeparator(lastChar) && currChar == '*') return false; // e.g. person.*
		if (isSchemaOrCatalogSeparator(lastChar) && currChar == '[') return false; // e.g. p.[id] for the dreaded SQL Server "quotes"
		if (last.isLiteral() && isCurrentOpenBracket) return true;
		if (isLastOpenBracket && isKeyword(currentText)) return false;
		if (isLastCloseBracket && !current.isSeparator() ) return true;
		if ((last.isIdentifier() || last.isLiteral()) && current.isOperator()) return true;
		if ((current.isIdentifier() || current.isLiteral()) && last.isOperator()) return true;
		if (current.isSeparator() || current.isOperator()) return false;
		if (last.isOperator() && (isKeyword(currentText) || current.isIdentifier() || current.isLiteral())) return true;
		if (last.isSeparator() || last.isOperator()) return false;

		return true;
	}

  private boolean isSchemaOrCatalogSeparator(char c)
  {
    return c == schemaSeparator || c == catalogSeparator;
  }

	private SQLToken processHaving(SQLToken last)
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = last;
		Set<String> newLines = CollectionUtil.caseInsensitiveSet("AND", "OR");
		int bracketCount = 0;
		while (t != null)
		{
			String word = t.getContents();
			if (word.equals("("))
			{
				bracketCount ++;
			}

			if (word.equals(")"))
			{
				bracketCount--;
			}

			if (word.equals("(") && isDbFunction(lastToken))
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendText('(');
				t = this.processFunctionCall(t);
				bracketCount = 0;
			}
			else if (word.equalsIgnoreCase("SELECT") && "(".equalsIgnoreCase(lastToken.getText()))
			{
				t = this.processSubSelect(t, 1, false);
				if (t == null) return t;
				continue;
			}
			else if (newLines.contains(word) && bracketCount == 0)
			{
				this.appendNewline();
				this.indent(3);
				this.appendTokenText(t);
			}
			else if (HAVING_TERMINAL.contains(t.getText()))
			{
				return t;
			}
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendTokenText(t);
			}

			lastToken = t;
			t = lexer.getNextToken(true, false);
		}
		return t;
	}

	private SQLToken processFrom(SQLToken last)
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = last;
		int bracketCount = 0;
		boolean inJoin = false;
		Set<String> joinKeywords = SqlUtil.getJoinKeyWords();

		while (t != null)
		{
			String text = t.getContents();
			if (!inJoin)
			{
				inJoin = joinKeywords.contains(text);
			}

			if (FROM_TERMINAL.contains(text.toUpperCase()))
			{
				return t;
			}

			if (lastToken.getContents().equals("(") && text.equalsIgnoreCase("SELECT") )
			{
				t = this.processSubSelect(t, bracketCount, true);
				continue;
			}

			if (t.isComment())
			{
				this.appendComment(text);
			}
			else if (text.equals("("))
			{
				if ((!lastToken.isSeparator() || lastToken == t) && !this.lastCharIsWhitespace() && !isDbFunction(lastToken)) this.appendText(' ');
				this.appendText(text);
				bracketCount ++;
			}
			else if (text.equals(")"))
			{
				this.appendText(text);
				bracketCount --;
			}
			else if (inJoin)
			{
				appendNewline();
				indent(2);
				this.appendTokenText(t);
				t = processJoin(t);
				continue;
			}
			else if (t.isSeparator() && text.equals(","))
			{
				if (!commaAfterLineBreak)
				{
					this.appendText(',');
				}
				if (!inJoin && bracketCount == 0)
				{
					this.appendNewline();
					this.indent(5);

					if (commaAfterLineBreak)
					{
						this.appendText(',');
						if (addSpaceAfterLineBreakComma)
						{
							appendText(' ');
						}
					}
				}
				else if (bracketCount > 0)
				{
					if (addSpaceAfterComma)
					{
						appendText(' ');
					}
				}
			}
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				if (LINE_BREAK_BEFORE.contains(text) && !text.equalsIgnoreCase("AS"))
				{
					this.appendNewline();
					if (joinKeywords.contains(text))
					{
						indent(2);
					}
					else
					{
						indent(5);
					}
				}
				this.appendTokenText(t);
				if (LINE_BREAK_AFTER.contains(text) && !text.equalsIgnoreCase("AS"))
				{
					this.appendNewline();
					this.indent(5);
				}
			}
			lastToken = t;
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

  private SQLToken processWindowDef(SQLToken last)
  {
    // this is the window keyword
    int bracketCount = 0;
    boolean afterWindow = false;
    SQLToken t = lexer.getNextToken(true, false);
    while (t != null)
    {
      if ("(".equals(t.getText()))
      {
        bracketCount++;
      }
      else if (")".equals(t.getText()) && bracketCount > 0)
      {
        bracketCount--;
        if (bracketCount == 0)
        {
          afterWindow = true;
        }
      }

      if (",".equals(t.getText()) && bracketCount == 0 && commaAfterLineBreak)
      {
        appendNewline();
        indent(7);
      }

      if (needsWhitespace(last, t))
      {
        appendText(' ');
      }

      appendTokenText(t);

      if (",".equals(t.getText()) && bracketCount == 0 && !commaAfterLineBreak)
      {
        appendNewline();
        indent(7);
      }

      last = t;

      t = lexer.getNextToken(true, false);

      if (t != null && afterWindow)
      {
        if (t.isReservedWord())
        {
          return t;
        }
        if (",".equals(t.getText()))
        {
          afterWindow = false;
        }
      }
    }
    return null;
  }


	private SQLToken processJoin(SQLToken last)
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		int indentPos = last.getText().length() + 2;
		int onPos = 0;
		int bracketCount = 0;

		while (t != null)
		{
			String text = t.getContents();
			if (JOIN_TERMINAL.contains(text) || SqlUtil.getJoinKeyWords().contains(text))
			{
				return t;
			}
			else if (t.isComment())
			{
				this.appendComment(t.getText());
			}
			else if ("(".equals(text))
			{
				appendText(' ');
				bracketCount ++;
				this.appendTokenText(t);
				if (last.getContents().equals("IN"))
				{
					t = this.lexer.getNextToken(true, false);
          if (t != null && QUERY_START.contains(t.getContents()))
          {
            appendTokenText(t);
            appendText(' ');
            t = processSubSelect(null, bracketCount, false);
          }
					else
          {
            t = processInList(t);
          }
					this.appendTokenText(t);
				}
			}
			else if (last.getContents().equals("(") && QUERY_START.contains(text) )
			{
				StringBuilder old = indent;
				indent = new StringBuilder(2);
				if (old != null) indent.append(old);
				indent.append("  ");
				t = this.processSubSelect(t, bracketCount, true);
				indent = old;
				continue;
			}
			else if (")".equals(text))
			{
				bracketCount --;
				appendText(')');
			}
			else if ("AND".equals(text) || "OR".equals(text))
			{
				if (joinWrapping != JoinWrapStyle.none)
				{
					if (onPos > 0)
					{
						String lb = NL + StringUtil.padRight(" ", indentPos - 3, ' ') + (indent == null ? "" : indent);
						this.result.insert(onPos, lb);
						realLength += lb.length();
						onPos = -1; // we only need this treatment for the first condition after the ON
					}
					appendNewline();
					indent(indentPos - text.length());
				}
				else
				{
					appendText(' ');
				}
				appendTokenText(t);
			}
			else
			{
				if ("ON".equals(text))
				{
					if (joinWrapping == JoinWrapStyle.onlyMultiple)
					{
						onPos = result.length();
					}
					else if (joinWrapping == JoinWrapStyle.always)
					{
						appendNewline();
						indent(indentPos - text.length());
					}
				}
				if (needsWhitespace(last, t)) appendText(' ');
				appendTokenText(t);
			}
			last = t;
			t =  this.lexer.getNextToken(true, false);
		}
		return t;
	}

	private SQLToken processList(SQLToken last, int indentCount, Set<String> terminalKeys)
	{
		String myIndent = StringUtil.padRight(" ", indentCount);

		int currentColumnCount = 0;
		boolean isSelect = last.getContents().equals("SELECT");

		int columnsPerLine = -1;
		if (isSelect)
		{
			columnsPerLine = getColumnsPerSelect();
		}
		else
		{
			columnsPerLine = getColumnsPerUpdate();
		}
		SQLToken t = this.lexer.getNextToken(true, true);
		SQLToken lastToken = last;
		SQLToken lastWhiteSpace = null;
		while (t != null)
		{
			if (t.isWhiteSpace())
			{
				lastWhiteSpace = t;
				t = this.lexer.getNextToken(true, true);
				continue;
			}

			final String text = t.getContents();
			if (t.isComment())
			{
				boolean needNewLine = false;
				if (lastWhiteSpace != null)
				{
					needNewLine = lastWhiteSpace.getText().contains("\n") || lastWhiteSpace.getText().contains("\r");
				}

				boolean lineComment = text.startsWith("--");
				boolean startOfLine = isStartOfLine();
				if (lineComment && needNewLine && !startOfLine)
				{
					appendNewline();
				}
				else if (!startOfLine)
				{
					appendText(' ');
				}
				this.appendText(text);
				if (lineComment)
				{
					appendNewline();
					indent(myIndent);
				}
			}
			else if (isSelect && "DECODE".equalsIgnoreCase(text))
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendText(text);
				t = processDecode(indentCount);
				continue;
			}
			else if ("CASE".equals(text))
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendTokenText(t);
				int caseIndent = indentCount;
				if (!isSelect)
				{
					caseIndent = this.getCurrentLineLength() - 4;
				}
				t = processCase(caseIndent);
				continue;
			}
			else if (terminalKeys.contains(text))
			{
				return t;
			}
			else if (text.equals("("))
			{
				// an equal sign immediately followed by an opening
				// bracket cannot be a function call (the function name
				// is missing) so it has to be a sub-select
				if ("=".equals(lastToken.getContents()) || ",".equals(lastToken.getContents()) || lastToken.isLiteral())
				{
					if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
					this.appendText("(");
					t = this.processBracketExpression();
					this.appendTokenText(t);
				}
				else
				{
					if (this.needsWhitespace(lastToken, t, false, true)) this.appendText(' ');
					this.appendText("(");
					t = this.processFunctionCall(t);
					if (t == null) return null;
					if (terminalKeys.contains(t.getText())) return t;
					if (t.isIdentifier() || t.isOperator())
					{
						this.appendText(' ');
						this.appendTokenText(t);
					}
					else if (isKeyword(t.getText()))
					{
						if (LINE_BREAK_BEFORE.contains(t.getText()))
						{
							appendNewline();
						}
						appendTokenText(t);
					}
					else
					{
						if (!")".equals(t.getText()))
						{
							appendTokenText(t);
						}
					}
				}
			}
			else if (text.equals(","))
			{
				currentColumnCount++;
				if (!commaAfterLineBreak || !needLineBreak(columnsPerLine, currentColumnCount))
				{
					this.appendText(',');
				}
				if (needLineBreak(columnsPerLine, currentColumnCount))
				{
					currentColumnCount = 0;
					this.appendNewline();
					this.indent(myIndent);
					if (commaAfterLineBreak)
					{
						this.appendText(',');
						if (addSpaceAfterLineBreakComma)
						{
							this.appendText(' ');
						}
					}
				}
				else
				{
					this.appendText(' ');
				}
			}
			else if (text.equals("*") && !lastToken.isSeparator() && !lastToken.isIdentifier())
			{
				this.appendText(" *");
			}
			else if (isLobParameter(text))
			{
				this.appendText(' ');
				this.appendText(text);
				t = processLobParameter();
			}
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendTokenText(t);
			}
			lastToken = t;
			lastWhiteSpace = null;
			t = this.lexer.getNextToken(true, true);
		}
		return null;
	}

	private SQLToken processBracketExpression()
	{
		SQLToken t = skipComments();
		if (QUERY_START.contains(t.getContents()))
		{
			return processSubSelect(t, 1, false);
		}
		return t;
	}

	private SQLToken processLobParameter()
	{
		SQLToken t = lexer.getNextToken(false, true);
		while (t != null)
		{
			String text = t.getText();
			if (text.equals("}"))
			{
				appendText(text);
				return t;
			}
			appendText(text);
			t = lexer.getNextToken(false, true);
		}
		return null;
	}

	private boolean needLineBreak(int columnsPerLine, int currentColumnCount)
	{
		return columnsPerLine > -1 && currentColumnCount >= columnsPerLine;
	}

	private SQLToken processSubSelect(SQLToken firstToken)
	{
		return processSubSelect(firstToken, 1, true);
	}

	private SQLToken processSubSelect(SQLToken firstToken, int currentBracketCount, boolean checkForList)
	{
		return processSubSelect(firstToken, currentBracketCount, checkForList, this.maxSubselectLength);
	}

	private SQLToken processSubSelect(SQLToken firstToken, int currentBracketCount, boolean checkForList, int maxSubLength)
  {
    return processSubSelect(firstToken, currentBracketCount, checkForList, maxSubLength, newLineForSubSelects);
  }

	private SQLToken processSubSelect(SQLToken firstToken, int currentBracketCount, boolean checkForList, int maxSubLength, boolean useNewLine)
	{
		SQLToken t = skipComments();
		int bracketCount = currentBracketCount;
		StringBuilder subSql = new StringBuilder(250);

		// this method gets called when then "parser" hits an
		// IN ( situation. If no SELECT is coming, we assume
		// its a list like IN ('x','Y')
		if (checkForList && !"SELECT".equalsIgnoreCase(t.getContents()) && firstToken == null)
		{
			return this.processInList(t);
		}

		if (firstToken != null)
		{
      subSql.append(getTokenText(firstToken));
      subSql.append(' ');
		}

		int lastIndent = 0;
		if (useNewLine)
		{
			lastIndent = indent == null ? 2 : indent.length() + 2;
		}
		else
		{
			lastIndent = this.getCurrentLineLength();
		}

		while (t != null)
		{
			String text = t.getContents();
			if (text.equals(")"))
			{
				bracketCount --;

				if (bracketCount == 0)
				{
					appendSubSelect(subSql, lastIndent, maxSubLength, useNewLine);
					return t;
				}
			}
			else if (t.isSeparator() && text.equals("("))
			{
				bracketCount ++;
			}
			subSql.append(text);
			t = this.lexer.getNextToken(true, true);
		}
		this.appendText(subSql);
		return t;
	}

	private void appendSubSelect(StringBuilder subSql, int lastIndent, int maxSubLength)
  {
    appendSubSelect(subSql, lastIndent, maxSubLength, this.newLineForSubSelects);
  }

	private void appendSubSelect(StringBuilder subSql, int lastIndent, int maxSubLength, boolean useNewline)
	{
		WbSqlFormatter f = new WbSqlFormatter(subSql.toString(), lastIndent, maxSubLength, this.dbId);
		f.setNewLineForSubselects(useNewline);
		f.setFunctionCase(this.functionCase);
		f.setIdentifierCase(this.identifierCase);
		f.setKeywordCase(this.keywordCase);
		f.setAddColumnNameComment(this.addColumnCommentForInsert);
		f.setAddSpaceAfterCommInList(this.addSpaceAfterComma);
		f.setAddSpaceAfterLineBreakComma(this.addSpaceAfterLineBreakComma);
    f.setIndentWhereCondition(this.indentWhereConditions);
		f.setCommaAfterLineBreak(this.commaAfterLineBreak);
		f.setJoinWrapping(this.joinWrapping);
		f.setColumnsPerInsert(colsPerInsert);
		f.setColumnsPerUpdate(colsPerUpdate);
		String s = f.getFormattedSql();
		boolean useFormattedQuery = f.getRealLength() > maxSubLength;
		if (!useFormattedQuery)
		{
			s = s.replaceAll(" *" + WbSqlFormatter.NL + " *", " ").trim();
		}
		if (useNewline && useFormattedQuery)
		{
			this.result.append(WbSqlFormatter.NL);  // do not use appendNewLine() as it will indent the new line
			this.appendText(StringUtil.padRight(" ", lastIndent));
		}
		this.appendText(s);
		if (useNewline && useFormattedQuery)
		{
			this.appendNewline();
		}
	}

	private SQLToken processDecode(int myIndent)
	{
		String current = StringUtil.padRight(" ", myIndent);

		StringBuilder decodeIndent = new StringBuilder(myIndent + 2);
		for (int i=0; i < myIndent; i++) decodeIndent.append(' ');
		decodeIndent.append("      ");

		SQLToken t = this.lexer.getNextToken(true,true);
		int commaCount = 0;
		int bracketCount = 0;

		boolean inQuotes = false;
		while (t != null)
		{
			final String text = t.getContents();
			if ("'".equals(text))
			{
				inQuotes = !inQuotes;
			}
			else if (")".equals(text))
			{
				bracketCount --;
			}
			else if ("(".equals(text))
			{
				bracketCount ++;
			}

			if (",".equals(text) && !inQuotes && bracketCount == 1) commaCount ++;

			if (",".equals(text) && !inQuotes && bracketCount == 1)
			{
				this.appendText(text);
				if (commaCount % 2 == 1)
				{
					this.appendNewline();
					this.indent(decodeIndent);
				}
			}
			else if (")".equalsIgnoreCase(text) && !inQuotes && bracketCount == 0)
			{
				this.appendNewline();
				this.indent(current);
				this.appendText(") ");
				t = this.lexer.getNextToken(true, false);
				return t;
			}
			else if (text.indexOf('\n') == -1 &&  text.indexOf('\r') == -1)
			{
				this.appendTokenText(t);
			}
			t = this.lexer.getNextToken(true,true);
		}
		return null;
	}

	private SQLToken processCase(int indentCount)
  {
    return processCase(indentCount, true);
  }
	private SQLToken processCase(int indentCount, boolean addFinalNewline)
	{
		String current = StringUtil.padRight(" ", indentCount);
		StringBuilder myIndent = new StringBuilder(indentCount + 2);
		for (int i=0; i < indentCount; i++) myIndent.append(' ');
		myIndent.append("  ");

		SQLToken last = null;
		SQLToken t = this.lexer.getNextToken(true,false);
		while (t != null)
		{
			final String text = t.getContents();

			if ("SELECT".equals(text) && last.getContents().equals("("))
			{
				t = this.processSubSelect(t);
				if (t == null) return null;
				if (t.getContents().equals(")")) this.appendText(")");
			}
			else if ("WHEN".equals(text) || "ELSE".equals(text))
			{
				this.appendNewline();
				this.indent(myIndent);
				this.appendTokenText(t);
			}
			else if ("THEN".equals(text))
			{
				if (last != null && this.needsWhitespace(last, t)) appendText(' ');
				this.appendTokenText(t);
			}
			else if ("CASE".equals(text))
			{
				this.appendNewline();
				this.indent(current.length() + 4);
				this.appendTokenText(t);
        last = t;
				t = this.processCase(current.length() + 4, false);
				//this.indent(current.length() + 2);
				//this.appendTokenText(t);
        continue;
			}
			else if ("END".equals(text) || "END CASE".equals(text))
			{
				this.appendNewline();
				this.indent(current);
				this.appendTokenText(t);
				this.appendText(' ');
				// Get the next token after the END. If that is the keyword AS,
				// the CASE statement ist not yet ended and we have to add the AS keyword
				// and the alias that was given before returning to the caller
				t = skipComments();
				if (t != null && (t.isIdentifier() || t.getContents().equals("AS")))
				{
					boolean aliasWithAs = t.getContents().equals("AS");
					this.appendTokenText(t);
					t = skipComments();
					if (aliasWithAs)
					{
						// the next token is the actual alias
						if (t != null)
						{
							this.appendText(' ');
							this.appendTokenText(t);
							t = this.lexer.getNextToken(true, false);
						}
					}
				}
        else if (addFinalNewline)
				{
					this.appendNewline();
				}
				return t;
			}
			else if (t.isComment())
			{
				this.appendComment(text);
			}
			else
			{
				if (last == null || this.needsWhitespace(last, t))
				{
					this.appendText(' ');
				}
				this.appendTokenText(t);
			}
			last = t;
			t = this.lexer.getNextToken(true,false);
		}
		return null;
	}

	private SQLToken processWbCommand(String wbVerb)
	{
		int indentCount = wbVerb.length() + 1;
		String myIndent = StringUtil.padRight(" ", indentCount);
		this.appendText(' ');

		CommandMapper mapper = new CommandMapper();

		SQLToken t = this.lexer.getNextToken(true,false);
		SqlCommand cmd = mapper.getCommandToUse(wbVerb);

		boolean first = true;
		boolean isParm = false;
		boolean inQuotes = false;
		while (t != null)
		{
			String text = inQuotes ? t.getText() : t.getContents();
			if (text.equals("'") || text.equals("\""))
			{
				inQuotes = !inQuotes;
			}

			if (isParm)
			{
				ArgumentParser p = cmd.getArgumentParser();
				if (p != null)
				{
					List<String> args = p.getRegisteredArguments();
					for (String regArg : args)
					{
						if (regArg.equalsIgnoreCase(text))
						{
							text = regArg;
							break;
						}
					}
				}
			}

			if (text.equals("-") && !inQuotes)
			{
				if (!first)
				{
					this.appendNewline();
					this.indent(myIndent);
				}
				isParm = true;
			}
			else
			{
				isParm = false;
			}

			// true, false should be written in lowercase for
			// WB Commands
			if (text.equalsIgnoreCase("true")) text = "true";
			if (text.equalsIgnoreCase("false")) text = "false";

			this.appendText(text);
			t = this.lexer.getNextToken(true,inQuotes);
			first = false;
		}
		return null;
	}

	private SQLToken processBracketList(int indentCount, int elementsPerLine, List<String> elementNames, boolean collectNames)
	{
		StringBuilder myIndent = new StringBuilder(indentCount);
		for (int i=0; i < indentCount; i++) myIndent.append(' ');

		this.appendNewline();
		if (elementsPerLine == 1)
		{
			this.appendText('(');
			this.appendNewline();
			this.appendText(myIndent);
		}
		else
		{
			this.appendText(myIndent);
			myIndent.append(' ');
			this.appendText("(");
		}

		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken last = null;
		int elementCount = 0;
		int bracketCount = 1;
		int elementIndex = 0;

		while (t != null)
		{
			final String text = t.getContents();
			if (text.equals(")"))
			{
				if (elementsPerLine == 1) this.appendNewline();
				this.appendText(")");
				SQLToken next = skipComments();
				if (next == null || !next.getText().equals(","))
				{
					return next;
				}
				appendText(next.getText());
				appendNewline();
				elementCount = 0;
				bracketCount --;
			}
			else if (text.equals("("))
			{
				if (bracketCount == 0)
				{
					if (elementsPerLine > 1)
					{
						appendText("  ");
					}
					this.appendText('(');
					if (elementsPerLine == 1)
					{
						appendNewline();
						indent(myIndent);
					}
					bracketCount ++;
				}
				else
				{
					this.appendText("(");
					t = this.processFunctionCall(t);
					if (!t.getContents().equals(")")) // can happen with functions that do not have parameters
					{
						continue;
					}
				}
			}
			else if (text.equals(","))
			{
				if (commaAfterLineBreak && (elementCount == 0 || elementCount >= elementsPerLine))
				{
					this.appendNewline();
					this.indent(myIndent);
					elementCount = 0;
				}
				this.appendText(",");
				elementCount ++;
				if (!commaAfterLineBreak && elementCount >= elementsPerLine)
				{
					this.appendNewline();
					this.indent(myIndent);
					elementCount = 0;
				}
				else if (!commaAfterLineBreak || (commaAfterLineBreak && addSpaceAfterLineBreakComma))
				{
					this.appendText(' ');
				}
				elementIndex ++;
			}
			else if (isLobParameter(text))
			{
				this.appendText(text);
				t = processLobParameter();
			}
			else if (t.isComment())
			{
				int len = getCurrentLineLength();
				appendComment(text, false);
				if (text.startsWith("--") && len > 0)
				{
					indent(StringUtil.padRight("", len));
				}
			}
			else if (!t.isWhiteSpace())
			{
				if (this.needsWhitespace(last, t))
				{
					appendText(' ');
				}
				if (!collectNames && elementIndex < elementNames.size())
				{
					if (commaAfterLineBreak && elementIndex == 0)
					{
						this.appendText(' ');
						if (addSpaceAfterLineBreakComma) this.appendText(' ');
					}
					this.appendText("/* " + elementNames.get(elementIndex) + " */ ");
				}
				this.appendTokenText(t);
				if (collectNames)
				{
					elementNames.add(t.getText());
				}
				if (t.isComment()) this.appendText(' ');
			}
			last = t;
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private boolean isLobParameter(String text)
	{
		return text.equalsIgnoreCase("{$blobfile") ||
			     text.equalsIgnoreCase("{$clobfile") ||
			     text.equalsIgnoreCase("$clobfile") ||
			     text.equalsIgnoreCase("$blobfile");
	}

	private SQLToken processInList(SQLToken current)
	{
		if (current == null) return null;
		List<StringBuilder> list = new ArrayList<>(25);
		list.add(new StringBuilder(""));
		SQLToken t = current;

		int bracketcount = 0;
		int elementcounter = 0;

		while (t != null)
		{
			String text = t.getContents();
			if (t.isSeparator() && text.equals(")"))
			{
				if (bracketcount == 0)
				{
					this.appendCommaList(list);
					return this.lexer.getNextToken(true, true);
				}
				else
				{
					StringBuilder b = list.get(elementcounter);
					if (b == null)
					{
						b = new StringBuilder(text);
						if (elementcounter < list.size()) list.set(elementcounter, b);
					}
					else
					{
						b.append(text);
					}
				}
			}
			else if (t.isSeparator() && text.equals("("))
			{
				bracketcount ++;
			}
			else if (t.isSeparator() && text.equals(","))
			{
				if (bracketcount == 0)
				{
					list.add(new StringBuilder(""));
					elementcounter = list.size() - 1;
				}
			}
			else if (!t.isWhiteSpace())
			{
				StringBuilder b = list.get(elementcounter);
				if (b == null)
				{
					b = new StringBuilder(text);
					if (t.isComment()) b.append(' ');
					list.set(elementcounter, b);
				}
				else
				{
					b.append(text);
					if (t.isComment()) b.append(' ');
				}
			}
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private void appendCommaList(List<StringBuilder> aList)
	{
		int indentCount = this.getCurrentLineLength();
		String ind = StringUtil.padRight(" ", indentCount);
		boolean newline = (aList.size() > 10);
		int count = aList.size();
		for (int i=0; i < count; i++)
		{
			this.appendText(aList.get(i));
			if (i < count - 1) this.appendText(", ");
			if (newline)
			{
				this.appendNewline();
				this.indent(ind);
			}
		}
		this.appendText(")");
	}

	private boolean isStartOfLine()
	{
		int len = this.result.length();
		if (len == 0) return true;

		// simulates endsWith() on a StringBuilder
		int pos = result.lastIndexOf(WbSqlFormatter.NL);
		if (pos == len - NL.length()) return true;

		String remain = result.substring(pos + NL.length());
		return remain.isEmpty() || StringUtil.isWhitespace(remain);
	}

	private void formatSql()
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = t;
		CommandTester wbTester = new CommandTester();

		List<String> insertColumns = new ArrayList<>();
		boolean firstToken = true;

		while (t != null)
		{
			final String word = t.getContents().toUpperCase();
			if (t.isComment())
			{
				String text = t.getContents();
				this.appendComment(text);
			}
			else if (isKeyword(word) || wbTester.isWbCommand(word))
			{
				if (lastToken.isComment() && !isStartOfLine()) this.appendNewline();

				if (LINE_BREAK_BEFORE.contains(word) && !lastToken.getContents().equals("("))
				{
					if (!isStartOfLine()) this.appendNewline();

					// For UPDATE statements
					if ("SET".equals(word))
					{
						this.indent("   ");
					}
				}
				else
				{
					//if (!lastToken.isSeparator() && lastToken != t && !isStartOfLine()) this.appendText(' ');
					if (needsWhitespace(lastToken, t)) this.appendText(' ');
				}

				if (wbTester.isWbCommand(word))
				{
					this.appendText(wbTester.formatVerb(word));
				}
				else
				{
					this.appendTokenText(t);
				}

				if (LINE_BREAK_AFTER.contains(word))
				{
					this.appendNewline();
				}

				if (word.equals("WITH") && firstToken)
				{
					lastToken = t;
					t = this.processCTE(t);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("MERGE"))
				{
					lastToken = t;
					t = this.processMerge(t);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("SELECT"))
				{
					lastToken = t;
					t = this.processList(t,"SELECT".length() + 1, SELECT_TERMINAL);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("INSERT"))
				{
					lastToken = t;
					t = this.processInsert(t);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("SET"))
				{
					lastToken = t;
					t = this.processList(t,"SET".length() + 4, SET_TERMINAL);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("CREATE") || word.equals("CREATE OR REPLACE"))
				{
					lastToken = t;
					t = this.processCreate();
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("GRANT") || word.equals("REVOKE"))
				{
					lastToken = t;
					t = this.processGrantRevoke(t);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("FROM"))
				{
					lastToken = t;
					t = this.processFrom(t);
					if (t == null) return;
					firstToken = false;
					continue;
				}

        if (word.equals("WINDOW"))
        {
          lastToken = t;
          t = processWindowDef(t);
          if (t == null) return;
          firstToken = false;
          continue;
        }

				if (word.equals("GROUP BY"))
				{
					lastToken = t;
					t = this.processList(lastToken, (word + " ").length(), GROUP_BY_TERMINAL);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("HAVING"))
				{
					lastToken = t;
					t = this.processHaving(lastToken);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equals("ORDER BY"))
				{
					lastToken = t;
					t = this.processList(lastToken, (word + " ").length(), ORDER_BY_TERMINAL);
					if (t == null) return;
				}

				if (word.equalsIgnoreCase("WHERE"))
				{
					lastToken = t;
					t = this.processWhere(t);
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (word.equalsIgnoreCase("INTO"))
				{
					lastToken = t;
					t = this.processIntoKeyword(insertColumns);
					firstToken = false;
					continue;
				}

				if (word.equalsIgnoreCase("VALUES"))
				{
					// the next (non-whitespace token has to be a (
					t = skipComments();
					if (t != null && t.getContents().equals("("))
					{
						t = this.processBracketList(indentInsert ? 2 : 0, getColumnsPerInsert(), insertColumns, false);
					}
					if (t == null) return;
					firstToken = false;
					continue;
				}

				if (wbTester.isWbCommand(word))
				{
					t = this.processWbCommand(word);
				}
			}
			else
			{
				if (LINE_BREAK_BEFORE.contains(word))
				{
					if (!isStartOfLine()) this.appendNewline();
				}

				if (word.equals("(") && isDbFunction(lastToken))
				{
					if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
					this.appendText('(');
					t = this.processFunctionCall(t);
				}
				else
				{
					if (word.equals(";"))
					{
						this.appendText(word);
						this.appendNewline();
						this.appendNewline();
					}
					else
					{
						if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
						this.appendTokenText(t);
					}
				}
			}
			lastToken = t;
			t = this.lexer.getNextToken(true, false);
			firstToken = false;
		}
	}

  private SQLToken processInsert(SQLToken last)
  {

		SQLToken t = skipComments();

		if (t == null) return t;

		// this should be the INTO keyword, if not something is wrong
		if (!t.getContents().equals("INTO")) return t;
		this.appendText(' ');
		this.appendTokenText(t);

		List<String> insertColumns = new ArrayList<>();
    t = this.processIntoKeyword(insertColumns);

    if (t.getContents().equals("SELECT"))
    {
      return t;
    }

    if (t.getContents().equals("VALUES"))
    {
      this.appendNewline();
      this.appendTokenText(t);

      t = skipComments();
      if (t != null && t.getContents().equals("("))
      {
        t = this.processBracketList(indentInsert ? 2 : 0, getColumnsPerInsert(), insertColumns, false);
      }
      return t;
    }

    // maybe an insert select with useless parentheses around the select
    appendNewline();

    return t;
  }

	private SQLToken processMerge(SQLToken last)
	{
		SQLToken t = skipComments();

		if (t == null) return t;
		String verb = t.getContents();

		// this should be the INTO keyword, if not something is wrong
		if (!verb.equals("INTO")) return t;
		this.appendText(' ');
		this.appendTokenText(t);

		boolean nextIsSelect = true;
		boolean afterThen = false;
		t = skipComments();
		List<String> insertColumns = new ArrayList<>();
		while (t != null)
		{
			String text = t.getContents();
			if (text.equals("(") && nextIsSelect)
			{
				this.appendNewline();
				this.appendText('(');
				this.appendNewline();
				this.indent("  ");
				t = processSubSelect(null, 1, false, 0);
				nextIsSelect = false;

				if (t == null) return null;
				if (")".equals(t.getText()))
				{
					this.appendNewline();
				}
			}
			if ("SET".equals(text))
			{
				appendTokenText(t);
				Set<String> terminals = CollectionUtil.caseInsensitiveSet();
				terminals.addAll(SET_TERMINAL);
				terminals.add("WHEN");
				t = this.processList(t,"SET".length() + 3, terminals);
				continue;
			}
			else if (text.equalsIgnoreCase("VALUES") || text.equalsIgnoreCase("INSERT"))
			{
				afterThen = false;
				this.indent = new StringBuilder("  ");
				appendNewline();
				appendTokenText(t);
				// the next (non-whitespace token has to be a (
				t = skipComments();
				boolean collect = text.equalsIgnoreCase("INSERT") && addColumnCommentForInsert;
				if (t != null && t.getContents().equals("("))
				{
					t = this.processBracketList(2, getColumnsPerInsert(), insertColumns, collect);
				}
				this.indent = null;
				if (t == null) return null;
				continue;
			}
			else
			{
				if ("WHEN".equals(text) || "USING".equals(text))
				{
					this.appendNewline();
				}

				if ("THEN".equals(text))
				{
					afterThen = true;
				}

				if (needsWhitespace(last, t))
				{
					this.appendText(' ');
				}
				appendTokenText(t);
				if (afterThen && "UPDATE".equals(text))
				{
					this.appendNewline();
					this.indent("  ");
					afterThen = false;
				}
			}
			last = t;
			t = this.lexer.getNextToken(true, false);
		}
		return t;
	}

	private SQLToken processCTE(SQLToken previousToken)
	{
		if (previousToken == null) return null;

		SQLToken lastToken = null;

		this.appendText(' ');

		SQLToken t = skipComments();

		int bracketCount = 0;
		boolean afterAs = false;

		while (t != null)
		{
			String verb = t.getContents();

			if (verb.equals("("))
			{
				bracketCount ++;
				if (!afterAs) this.appendText(' ');
				this.appendText('(');

				if (bracketCount == 1)
				{
					if (afterAs)
					{
						StringBuilder oldIndent = this.indent;
						this.indent = new StringBuilder(indent == null ? "" : indent).append("  ");
						this.appendNewline();
						t = processSubSelect(null, 1, false, maxSubselectLength, false);
						this.indent = oldIndent;
						this.appendNewline();
						if (t == null) return t;
						appendText(t.getContents());

						// check if multiple CTEs are defined
						t = skipComments();
						if (t == null) return t;

						if (!t.getText().equals(","))
						{
							return t;
						}
						appendText(t.getContents());
  					appendNewline();
						bracketCount --;
						afterAs = false;
					}
					else
					{
						t = skipComments();
						t = processInList(t);
						bracketCount --;
						continue;
					}
				}
			}
			else if (verb.equals("AS"))
			{
				if (needsWhitespace(lastToken, t)) appendText(' ');
				this.appendText(verb);
				this.appendNewline();
				afterAs = true;
			}
			else if (verb.equals(")"))
			{
				bracketCount --;
				this.appendText(")");
			}
			else if (t.isComment())
			{
				this.appendComment(verb);
			}
			else
			{
				if (needsWhitespace(lastToken, t)) appendText(' ');
				appendText(verb);
			}
			lastToken = t;
			t = skipComments();
		}
		return null;
	}


	private SQLToken processWhere(SQLToken previousToken)
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = previousToken;
		int bracketCount = 0;

		boolean noBreakOnCondition = false;
		while (t != null)
		{
			String verb = t.getContents();

			if (WHERE_TERMINAL.contains(verb))
			{
				if ("WITH".equals(verb))
				{
					// for e.g. view definitions using "WITH CHECK OPTION"
					this.appendNewline();
				}
				return t;
			}

			if (verb.equals(";"))
			{
				return t;
			}

			if (verb.equals("BETWEEN"))
			{
				noBreakOnCondition = true;
			}

			if (verb.equals(")"))
			{
				bracketCount --;
				this.appendText(")");
			}
			else if (t.isComment())
			{
				this.appendComment(verb);
			}
			else if (verb.equals("("))
			{
				if (needsWhitespace(lastToken, t, false, true)) this.appendText(' ');
				appendText(t.getContents());

				SQLToken next = skipComments();
				if (next == null) return null;

				if (QUERY_START.contains(next.getContents()))
				{
					t = this.processSubSelect(next);
					if (t == null) return null;
					if (t.getContents().equals(")")) this.appendText(")");
				}
				else
				{
					bracketCount ++;
					lastToken = t;
					t = next;
					continue;
				}
			}
			else if (bracketCount == 0 && (verb.equalsIgnoreCase("AND") || verb.equalsIgnoreCase("OR")) )
			{
				if (noBreakOnCondition)
				{
					this.appendText(' ');
				}
				else
				{
					// TODO: this attempt to keep conditions in bracktes together, results
					// in effectively no formatting when the whole WHERE clause is put
					// between brackets (because bracketCount will never be zero until
					// the end of the WHERE clause)
					if (!this.isStartOfLine()) this.appendNewline();
				}

        if (indentWhereConditions)
        {
          if (verb.equals("OR")) this.appendText("   ");
          if (verb.equals("AND")) this.appendText("  ");
        }
				this.appendTokenText(t);
				if (!noBreakOnCondition && !indentWhereConditions)
				{
					this.appendText("  ");
					if (verb.equals("OR")) this.appendText(' ');
				}
				noBreakOnCondition = false;
			}
			else if (verb.equals(","))
			{
				appendText(',');
				if (addSpaceAfterComma) appendText(' ');
			}
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendTokenText(t);
			}

			lastToken = t;
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private SQLToken skipComments()
	{
		SQLToken next = lexer.getNextToken(true, false);
		if (next == null) return null;
		if (!next.isComment()) return next;
		while (next != null)
		{
			if (!next.isComment()) return next;
			this.appendComment(next.getContents());
			next = lexer.getNextToken(true, false);
		}
		return null;
	}

	private SQLToken processIntoKeyword(List<String> columnNames)
	{
		SQLToken t = skipComments();
		// we expect an identifier now (the table name)
		// but to be able to handle "wrong statements" we'll
		// make sure everything's fine

		if (t.isIdentifier())
		{
			this.appendText(' ');
			this.appendTokenText(t);
			t = this.lexer.getNextToken(false, false);
			if (t.getContents().equalsIgnoreCase("VALUES"))
			{
				// no column list to format here...
				return t;
			}
			else if (t.isSeparator() && t.getContents().equals("("))
			{
				columnNames.clear();
				return this.processBracketList(indentInsert ? 2 : 0, getColumnsPerInsert(), columnNames, addColumnCommentForInsert);
			}
		}
		return t;
	}

	private SQLToken processFunctionCall(SQLToken last)
	{
		int bracketCount = 1;
		SQLToken t = this.lexer.getNextToken(true, false);

		if (t != null && QUERY_START.contains(t.getContents()))
		{
			t = processSubSelect(t);
			if (t == null) return null;
			if (t.getContents().equals(")"))
			{
				this.appendText(')');
				t = this.lexer.getNextToken(true, false);
			}
			return t;
		}

		SQLToken lastToken = last;
		while (t != null)
		{
			String text = t.getContents();
			if (text.equals(")"))
			{
				bracketCount --;
			}
			if (text.equals("("))
			{
				bracketCount ++;
			}
			if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
			this.appendTokenText(t);

			if (bracketCount == 0)
			{
				return t;
			}
			lastToken = t;
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private SQLToken processGrantRevoke(SQLToken previous)
	{
		SQLToken last = previous;
		SQLToken t = this.lexer.getNextToken(true, false);
		boolean nextIsIdentifier = false;
		while (t != null)
		{
			String verb = t.getContents();
			if (verb.equalsIgnoreCase("ON") || verb.equalsIgnoreCase("TO") || verb.equalsIgnoreCase("FROM"))
			{
				this.appendNewline();
				this.indent(2);
				this.appendTokenText(t);
				nextIsIdentifier = true;
			}
			else
			{
				if (needsWhitespace(last, t))
				{
					this.appendText(' ');
				}

				if (nextIsIdentifier)
				{
					// don't change the case of the identifiers, leave them as they were entered by the user
					this.appendText(t.getText());
				}
				else
				{
					this.appendText(verb);
				}
				nextIsIdentifier = false;
			}
			t = this.lexer.getNextToken(true, false);
		}
		return t;
	}

	private SQLToken processCreate()
	{
		SQLToken t = skipComments();
		String verb = t.getContents();

    while (createTableTypes.contains(verb) || createViewTypes.contains(verb))
    {
      appendText(' ');
      appendTokenText(t);
      t = skipComments();
      if (t != null) verb = t.getContents();
    }

		if (verb.equals("TABLE"))
		{
			this.appendText(' ');
			this.appendTokenText(t);
			this.appendText(' ');
			t = this.processCreateTable();
			return t;
		}
		else if (verb.equals("VIEW") || verb.equals("SNAPSHOT"))
		{
			this.appendText(' ');
			this.appendText(verb);
			this.appendText(' ');
			return this.processCreateView(t);
		}
    else if (INDEX_OPTIONS.contains(verb))
    {
      SQLToken opt = t;
      String option = verb;
      appendText(' ');
      while (INDEX_OPTIONS.contains(option))
      {
        appendTokenText(opt);
        appendText(' ');
        opt = skipComments();
        if (opt == null) return null;
        option = opt.getContents();
      }
      if ("INDEX".equals(option))
      {
        this.appendTokenText(opt);
        this.appendText(' ');
        return processCreateIndex();
      }
    }
		else if (verb.equals("INDEX"))
		{
			this.appendText(' ');
			this.appendTokenText(t);
			this.appendText(' ');
			return this.processCreateIndex();
		}
		else if (verb.equals("OR"))
		{
			// Check for Oracle's CREATE OR REPLACE
			this.appendText(' ');
			this.appendText(verb);
			t = this.lexer.getNextToken(true, false);
			if (t == null) return t;
			verb = t.getContents();
			if (verb.equals("REPLACE"))
			{
				this.appendText(' ');
				this.appendText(verb);
				this.appendText(' ');
				return this.processCreateView(t);
			}
		}

		return t;
	}

	private SQLToken processTableDefinition()
	{
		List<StringBuilder> cols = new ArrayList<>();
		SQLToken t = lexer.getNextToken(true, false);
		StringBuilder line = new StringBuilder(50);
		int maxColLength = 0;

		boolean isColname = true;
		boolean inDatatype = false;
		int tokenCount = 0;

		int bracketCount = 0;

		SQLToken last = null;

		while (t != null)
		{
			String w = t.getContents();
			if (last != null && last.getText().equals(",") && CREATE_TABLE_TERMINAL.contains(w))
			{
				break;
			}

			if ("(".equals(w))
			{
				bracketCount ++;
			}
			else if (")".equals(w))
			{
				bracketCount --;
			}
			else if (!t.isComment())
			{
				// only count "real" tokens
				tokenCount ++;
			}

			inDatatype = (bracketCount == 1 && tokenCount == 2);

			// Closing bracket reached --> end of create table statement
			if (bracketCount < 0)
			{
				cols.add(line);
				break;
			}

			if (isColname || (needsWhitespace(last, t, true) && !inDatatype))
			{
				line.append(' ');
			}

      line.append(getTokenText(t));

			if (isColname)
			{
				if (w.length() > maxColLength) maxColLength = w.length();
				isColname = false;
			}

			if (w.equals(",") && bracketCount == 0)
			{
				cols.add(line);
				line = new StringBuilder(50);
				isColname = true;
				tokenCount = 0;
			}

			last = t;
			t = this.lexer.getNextToken(true, false);
		}

		// Now process the collected column definitions
		for (StringBuilder col : cols)
		{
			SQLLexer lex = SQLLexerFactory.createLexerForDbId(dbId, col.toString());
			SQLToken column = lex.getNextToken(false, false);
			if (column == null) continue;
			String colname = column.getContents();

			int len = colname.length();
			String def = col.substring(column.getCharEnd()).trim();

			appendText("  ");
			appendText(colname);
			if (isKeyword(colname))
			{
				appendText(" ");
			}
			else
			{
				while (len < maxColLength)
				{
					this.appendText(' ');
					len ++;
				}
				this.appendText("   ");
			}
			appendText(def);
			appendNewline();
		}

		if (t == null) return t;

		// now the definitions are added
		// check if we need to process more
		if (!t.getContents().equals(")"))
		{
			appendText("  ");
			bracketCount = 0;
			last = null;

			while (t != null)
			{
				String w = t.getContents();
				if ("(".equals(w)) bracketCount ++;

				if (")".equals(w))
				{
					if (bracketCount == 0)
					{
						break;
					}
					else
					{
						bracketCount --;
					}
				}

				if (last != null && needsWhitespace(last, t, true))
				{
					appendText(' ');
				}
				appendTokenText(t);

				if (",".equals(w) && bracketCount == 0)
				{
					this.appendNewline();
					this.appendText("  ");
				}

				last = t;
				t = this.lexer.getNextToken(true, false);
			}
			appendNewline();
		}

		appendText(')');
		appendNewline();
		t = this.lexer.getNextToken(false, false);

		return t;
	}

	private boolean isQuotedIdentifier(String name)
	{
		return SqlUtil.isQuotedIdentifier(name);
	}

  private boolean isEndOfIdentifier(SQLToken token, Set<String> nameTerminal)
  {
    if (token == null) return true;

    boolean isTerminal;
    if (nameTerminal != null)
    {
      isTerminal = nameTerminal.contains(token.getContents());
    }
    else
    {
      isTerminal = token.isReservedWord() || token.isOperator() || token.isSeparator() || token.isLiteral() || token.isNumberLiteral();
    }
    return isTerminal;
  }

  private SQLToken appendMultipartIdentifier(SQLToken part, Set<String> nameTerminal)
  {
    if (part == null) return null;

    String name = part.getContents();
    appendIdentifier(name);

    part = collapseWhiteSpace();
    if (part == null) return null;

    while (!isEndOfIdentifier(part, nameTerminal))
    {
      if (part.isIdentifier())
      {
        appendIdentifier(part.getContents());
      }
      else
      {
        appendText(part.getContents());
      }
      part = collapseWhiteSpace();
    }
    return part;
  }

  private SQLToken collapseWhiteSpace()
  {
    SQLToken t = lexer.getNextToken(true, true);
    if (t == null) return t;

    boolean wasSpace = false;
    while (t.isWhiteSpace())
    {
      wasSpace = true;
      t = lexer.getNextToken(false, true);
    }
    if (wasSpace) appendText(' ');
    return t;
  }

	/**
	 * Process a CREATE TABLE statement.
	 * The CREATE TABLE has already been added!
	 */
	private SQLToken processCreateTable()
	{
		SQLToken t = this.lexer.getNextToken(false, false);
		if (t == null) return t;

		// the next token has to be the table name, so
		// we can simply write it out

		String name = t.getContents();

		// Postgres, MySQL
		if (name.equals("IF NOT EXISTS"))
		{
			this.appendText(name);
			this.appendText(" ");
			t = lexer.getNextToken(false, false);
			if (t == null) return null;
			name = t.getContents();
		}

    t = this.appendMultipartIdentifier(t, CollectionUtil.caseInsensitiveSet("(", "AS"));
		if (t == null) return t;

		if (t.getContents().equals("AS"))
		{
			this.appendNewline();
			this.appendText("AS");
			this.appendNewline();
			// this must be followed by a SELECT query
			t = skipComments();
			if (t == null) return t;
			if (t.getContents().equals("SELECT"))
			{
				CharSequence select = this.sql.subSequence(t.getCharBegin(), sql.length());
				WbSqlFormatter f = new WbSqlFormatter(select, this.dbId);
				String formattedSelect = f.getFormattedSql();
				appendText(formattedSelect);
				return null;
			}
		}
		else if (t.getContents().equals("("))
		{
			this.appendNewline();
			this.appendText('(');
			this.appendNewline();

			t = processTableDefinition();
		}

		return t;
	}

	/**
	 * Format a CREATE VIEW statement
	 */
	private SQLToken processCreateView(SQLToken previous)
	{
		SQLToken t = this.lexer.getNextToken(false, false);
		SQLToken last = previous;
		int bracketCount = 0;

		while (t != null)
		{
			if (t.getContents().equals("(") )
			{
				if (bracketCount == 0)
				{
					// start of column definitions...
          appendNewline();
          appendText('(');
					t = processCommaList(1, 2);
          if (t == null) return t;
				}
        else
        {
          bracketCount ++;
        }
			}

			if ("SELECT".equals(t.getContents()))
			{
				return t;
			}
			else if ("AS".equals(t.getContents()))
			{
				appendNewline();
				appendTokenText(t);
				appendNewline();
			}
			else
			{
				appendTokenText(t);
				if (needsWhitespace(last, t, true))
				{
					appendText(' ');
				}
			}
			last = t;
			t = this.lexer.getNextToken(false, false);
		}
		return t;
	}

	private SQLToken processCreateIndex()
	{
		SQLToken t = skipComments();
    if (t == null) return null;


		SQLToken last = t;
    if (t.getContents().equalsIgnoreCase("CONCURRENTLY"))
    {
      appendText(' ');
      appendTokenText(t);
      t = skipComments();
      if (t == null) return null;
    }

    t = appendMultipartIdentifier(t, null);
    if (t == null) return t;

    appendNewline();
    indent("  ");
    appendTokenText(t);
    appendText(' ');

    t = skipComments();

    t = appendMultipartIdentifier(t, null);

		while (t != null)
		{
			if (t.getContents().equals("(") )
			{
        appendText('(');
				t = this.processCommaList(10, 4);
        appendTokenText(t);
			}
			else
			{
				if (this.needsWhitespace(last, t)) this.appendText(' ');
				this.appendTokenText(t);
			}
			t = this.lexer.getNextToken(true, false);
		}
		return t;
	}

	/**
	 * Process the elements inside parentheses.
	 * Any parentheses inside assumed to be "function calls" and just treated as further elements
   * (and commas inside parentheses are not considered).
   *
	 *	It is assumed that the passed SQLToken is the first token after the opening parentheses
	 *
	 *  @return the token after the closing bracket
	 */
  private SQLToken processCommaList(int maxElements, int indentCount)
	{
    List<StringBuilder> elements = new ArrayList<>();
    StringBuilder element = new StringBuilder(30);
    int bracketCount = 0;
    SQLToken t = lexer.getNextToken(true, true);
    while (t != null)
    {
      String text = t.getText();
      if ("(".equals(text))
      {
        bracketCount ++;
      }
      else if (")".equals(text))
      {
        if (bracketCount == 0)
        {
          elements.add(element);
          break;
        }
        bracketCount --;
      }
      if (",".equals(text) && bracketCount == 0)
      {
        elements.add(element);
        element = new StringBuilder(30);
      }
      else
      {
        if (t.isWhiteSpace() && (text.indexOf('\n') > -1 || text.indexOf('\r') > -1))
        {
          element.append(' ');
        }
        else
        {
          element.append(getTokenText(t));
        }
      }
      t = lexer.getNextToken(true, true);
    }
    outputCommaElements(elements, maxElements, indentCount);
		return t;
	}



	/*
	 * Output the elements of the given List separated by commas.
   *
	 * If the list contains more elements, then a maximum of maxElements elements will be put on a single line
	 * If more then one line is "printed" they will be indented by indentCount spaces.
   *
   * Any opening or closing parentheses must be inserted by the caller.
	 */
	private void outputCommaElements(List<StringBuilder> elements, int maxElements, int indentCount)
	{
		String myIndent = StringUtil.padRight(" ", indentCount);

		int count = elements.size();

		if (count > maxElements)
		{
			this.appendNewline();
		}

		for (int i=0; i < count; i++)
		{
			String text = elements.get(i).toString().trim();
      boolean isFirst = i == 0;
      boolean isLast = i == count -1;
      if (count > maxElements)
      {
        appendCommaElementWithNewline(text, myIndent, isFirst, isLast);
      }
      else
      {
        appendText(text);
        if (!isLast)
        {
          appendText(", ");
        }
      }
		}

		if (count > maxElements)
		{
			this.appendNewline();
		}
	}

  private void appendCommaElementWithNewline(String value, String indent, boolean isFirst, boolean isLast)
  {
    if (commaAfterLineBreak)
    {
      if (isFirst)
      {
        indent(indent);
      }
      else
      {
        appendNewline();
        indent(indent);
        appendText(',');
        if (addSpaceAfterLineBreakComma)
        {
          appendText(' ');
        }
      }
      appendText(value);
    }
    else
    {
      appendText(indent);
      appendText(value);
      if (!isLast)
      {
        appendText(',');
        appendNewline();
      }
    }
  }


}
