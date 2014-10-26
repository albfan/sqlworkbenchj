/*
 * SqlFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.sql.formatter;

import workbench.sql.lexer.SQLToken;
import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import workbench.resource.GeneratedIdentifierCase;
import workbench.resource.Settings;

import workbench.sql.CommandMapper;
import workbench.sql.SqlCommand;
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
public class SqlFormatter
{
	private final Set<String> LINE_BREAK_BEFORE = CollectionUtil.unmodifiableSet(
		"SELECT", "SET", "FROM", "WHERE", "ORDER BY", "GROUP BY", "HAVING", "VALUES",
		"UNION", "UNION ALL", "MINUS", "INTERSECT", "REFRESH", "AS", "FOR", "JOIN",
		"INNER JOIN", "RIGHT OUTER JOIN", "LEFT OUTER JOIN", "CROSS JOIN", "LEFT JOIN",
		"RIGHT JOIN", "START WITH", "CONNECT BY", "OUTER APPLY", "CROSS APPLY");

	private final Set<String> LINE_BREAK_AFTER = CollectionUtil.unmodifiableSet(
		"UNION", "UNION ALL", "MINUS", "INTERSECT", "AS", "FOR");

	public static final Set<String> HAVING_TERMINAL = CollectionUtil.unmodifiableSet(
		"ORDER BY", "GROUP BY", "UNION", "UNION ALL", "INTERSECT",
		"MINUS", "WINDOW", ";");

	// keywords terminating a WHERE clause
	public static final Set<String> WHERE_TERMINAL = CollectionUtil.unmodifiableSet(HAVING_TERMINAL, "HAVING", "WITH");

	// keywords terminating a HAVING clause

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

	private static final String NL = "\n";
	private boolean addColumnCommentForInsert;
	private boolean newLineForSubSelects;
	private GeneratedIdentifierCase keywordCase = GeneratedIdentifierCase.upper;
	private GeneratedIdentifierCase identifierCase = GeneratedIdentifierCase.asIs;
	private GeneratedIdentifierCase functionCase = GeneratedIdentifierCase.lower;
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

	SqlFormatter(CharSequence aScript)
	{
		this(aScript, 0, Settings.getInstance().getFormatterMaxSubselectLength(), null);
	}

	public SqlFormatter(CharSequence aScript, String dbId)
	{
		this(aScript, 0, Settings.getInstance().getFormatterMaxSubselectLength(), dbId);
	}

	public SqlFormatter(CharSequence aScript, int maxLength, String dbId)
	{
		this(aScript, 0, maxLength, dbId);
	}

	SqlFormatter(CharSequence aScript, int maxLength)
	{
		this(aScript, 0, maxLength, null);
	}

	public void setCatalogSeparator(char separator)
	{
		this.catalogSeparator = separator;
	}

	private SqlFormatter(CharSequence aScript, int indentCount, int maxLength, String dbId)
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
		newLineForSubSelects = Settings.getInstance().getFormatterSubselectInNewLine();
		addColumnCommentForInsert = Settings.getInstance().getFormatterAddColumnNameComment();
		keywordCase = Settings.getInstance().getFormatterKeywordsCase();
		addSpaceAfterComma = Settings.getInstance().getFormatterAddSpaceAfterComma();
		commaAfterLineBreak = Settings.getInstance().getFormatterCommaAfterLineBreak();
		addSpaceAfterLineBreakComma = Settings.getInstance().getFormatterAddSpaceAfterLineBreakComma();
		joinWrapping = Settings.getInstance().getFormatterJoinWrapStyle();
		indentInsert = Settings.getInstance().getFormatterIndentInsert();
		identifierCase = Settings.getInstance().getAutoCompletionPasteCase();
		setDbId(dbId);
	}

	public void setIdentifierCase(GeneratedIdentifierCase idCase)
	{
		this.identifierCase = idCase;
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
		keywords.addAll(helper.getKeywords());
		keywords.addAll(helper.getReservedWords());
		dataTypes.addAll(helper.getDataTypes());
		dbFunctions.addAll(helper.getSqlFunctions());
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
		this.result.append(SqlFormatter.NL);
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
		String text = t.getText();

		if (this.dbFunctions.contains(text))
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
		else if (this.isKeyword(text))
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

		appendText(text);
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
		char lastChar = lastText.charAt(0);
		char currChar = currentText.charAt(0);
		if (currChar == catalogSeparator) return false;
		if (!ignoreStartOfline && this.isStartOfLine()) return false;
		boolean isCurrentOpenBracket = "(".equals(currentText);
		boolean isLastOpenBracket = "(".equals(lastText);
		boolean isLastCloseBracket = ")".equals(lastText);

		if (lastChar == 'N' && currChar == '\'') return false; // handle N'foo' literals
		if (last.isComment() && lastText.startsWith("--")) return false;
		if (DATE_LITERALS.contains(lastText) && current.isLiteral()) return true;
		if (lastText.endsWith("'") && currentText.equals("''")) return false;
		if (lastText.endsWith("'") && currentText.equals("}")) return false;
		if (lastText.equals("''") && currentText.startsWith("'")) return false;
		if (lastChar == '\'' && currChar == '\'') return false;

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

		if (lastChar == catalogSeparator && current.isIdentifier()) return false;
		if (lastChar == catalogSeparator && currChar == '*') return true; // e.g. person.*
		if (lastChar == catalogSeparator && currChar == '[') return true; // e.g. p.[id] for the dreaded SQL Server "quotes"
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
				t = this.processSubSelect(true, 1, false);
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
				t = this.processSubSelect(true, bracketCount, true);
				continue;
			}

			if (t.isComment())
			{
				this.appendComment(text);
			}
			else if (text.equals("("))
			{
				if ((!lastToken.isSeparator() || lastToken == t) && !this.lastCharIsWhitespace()) this.appendText(' ');
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
					t = processInList(t);
					this.appendTokenText(t);
				}
			}
			else if (last.getContents().equals("(") && text.equalsIgnoreCase("SELECT") )
			{
				StringBuilder old = indent;
				indent = new StringBuilder(2);
				if (old != null) indent.append(old);
				indent.append("  ");
				t = this.processSubSelect(true, bracketCount, true);
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
						indent(indentPos - text.length() - 1);
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
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = last;

		while (t != null)
		{
			final String text = t.getContents();
			if (t.isComment())
			{
				this.appendComment(text);
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
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private SQLToken processBracketExpression()
	{
		SQLToken t = skipComments();
		if (t.getContents().equalsIgnoreCase("SELECT"))
		{
			return processSubSelect(true, 1, false);
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

	private SQLToken processSubSelect(boolean addSelectKeyword)
	{
		return processSubSelect(addSelectKeyword, 1, true);
	}

	private SQLToken processSubSelect(boolean addSelectKeyword, int currentBracketCount, boolean checkForList)
	{
		return processSubSelect(addSelectKeyword, currentBracketCount, checkForList, this.maxSubselectLength);
	}

	private SQLToken processSubSelect(boolean addSelectKeyword, int currentBracketCount, boolean checkForList, int maxSubLength)
	{
		SQLToken t = skipComments();
		int bracketCount = currentBracketCount;
		StringBuilder subSql = new StringBuilder(250);

		// this method gets called when then "parser" hits an
		// IN ( situation. If no SELECT is coming, we assume
		// its a list like IN ('x','Y')
		if (checkForList && !"SELECT".equalsIgnoreCase(t.getContents()) && !addSelectKeyword)
		{
			return this.processInList(t);
		}

		if (addSelectKeyword)
		{
			subSql.append("SELECT ");
		}

		int lastIndent = 0;
		if (newLineForSubSelects)
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
					appendSubSelect(subSql, lastIndent, maxSubLength);
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
		SqlFormatter f = new SqlFormatter(subSql.toString(), lastIndent, maxSubLength, this.dbId);
		f.setNewLineForSubselects(this.newLineForSubSelects);
		f.setFunctionCase(this.functionCase);
		f.setIdentifierCase(this.identifierCase);
		f.setKeywordCase(this.keywordCase);
		f.setAddColumnNameComment(this.addColumnCommentForInsert);
		f.setAddSpaceAfterCommInList(this.addSpaceAfterComma);
		f.setAddSpaceAfterLineBreakComma(this.addSpaceAfterLineBreakComma);
		f.setCommaAfterLineBreak(this.commaAfterLineBreak);
		f.setJoinWrapping(this.joinWrapping);
		f.setColumnsPerInsert(colsPerInsert);
		f.setColumnsPerUpdate(colsPerUpdate);
		String s = f.getFormattedSql();
		boolean useFormattedQuery = f.getRealLength() > maxSubLength;
		if (!useFormattedQuery)
		{
			s = s.replaceAll(" *" + SqlFormatter.NL + " *", " ").trim();
		}
		if (newLineForSubSelects && useFormattedQuery)
		{
			this.result.append(SqlFormatter.NL);  // do not use appendNewLine() as it will indent the new line
			this.appendText(StringUtil.padRight(" ", lastIndent));
		}
		this.appendText(s);
		if (newLineForSubSelects && useFormattedQuery)
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
				t = this.processSubSelect(true);
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
			else if ("END".equals(text) || "END CASE".equals(text))
			{
				this.appendNewline();
				this.indent(current);
				this.appendTokenText(t);
				// Get the next token after the END. If that is the keyword AS,
				// the CASE statement ist not yet ended and we have to add the AS keyword
				// and the alias that was given before returning to the caller
				t = this.lexer.getNextToken(true, false);
				if (t != null && (t.isIdentifier() || t.getContents().equals("AS")))
				{
					boolean aliasWithAs = t.getContents().equals("AS");
					this.appendText(' ');
					this.appendTokenText(t);
					t = this.lexer.getNextToken(true, false);
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
		int pos = result.lastIndexOf(SqlFormatter.NL);
		if (pos == len - NL.length()) return true;

		// Current text does not end with a newline, but
		// if the "current line" consist of the current indent, it
		// is considered as a "start of line" as well.
		String remain = result.substring(pos + NL.length());
		int indentLength = (indent == null ? 0 : indent.length());
		if (StringUtil.isWhitespace(remain) && remain.length() == indentLength) return true;
		return false;
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
					t = skipComments();//this.lexer.getNextToken(false, false);
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

	private SQLToken processMerge(SQLToken last)
	{
		// this should be the MERGE keyword
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
				t = processSubSelect(false, 1, false, 0);
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
						t = processSubSelect(false, 1, false);
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
				String lastWord = lastToken.getContents();

				if (!lastToken.isSeparator() && !isDbFunction(lastWord)) this.appendText(' ');
				appendText(t.getContents());

				SQLToken next = skipComments();
				if (next == null) return null;

				if ("SELECT".equals(next.getContents()))
				{
					t = this.processSubSelect(true);
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
				this.appendTokenText(t);
				if (!noBreakOnCondition)
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
		SQLToken t = this.lexer.getNextToken(false, false);
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

		if (t != null && t.getContents().equals("SELECT"))
		{
			t = processSubSelect(true);
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
		SQLToken t = this.lexer.getNextToken(true, false);
		String verb = t.getContents();

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
		else if (verb.equals("INDEX"))
		{
			this.appendText(' ');
			this.appendText(verb);
			//this.appendText(' ');
			return this.processCreateIndex(t);
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

			line.append(w);

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

		this.appendText(name);

		if (isQuotedIdentifier(name))
		{
			t = this.skipComments();
			if (t.getContents().equals("."))
			{
				appendText('.');
				// the following token must be another identifier, otherwise the syntax is not correct
				t = this.skipComments();
				appendTokenText(t);
				appendText(' ');
				t = this.skipComments();
			}
			else
			{
				appendText(' ');
			}
		}
		else
		{
			this.appendText(' ');
			t = this.skipComments();
		}

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
				SqlFormatter f = new SqlFormatter(select, this.dbId);
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
	 *	Process the elements in a () combination
	 *	Any bracket inside the brackets are assumed to be "function calls"
	 *  and just treated as further elements.
	 *	It is assumed that the passed SQLToken is the opening bracket
	 *
	 *  @return the token after the closing bracket
	 */
	private SQLToken processCommaList(SQLToken previous, int maxElements, int indentCount)
	{
		StringBuilder definition = new StringBuilder(200);
		SQLToken t = previous;
		SQLToken last = previous;
		int bracketCount = 0;

		while (t != null)
		{
			if (t.getContents().equals("(") )
			{
				if (bracketCount > 0)
				{
					definition.append('(');
				}
				bracketCount ++;
			}
			else if (t.getContents().equals(")"))
			{
				if (bracketCount == 1)
				{
					List elements = StringUtil.stringToList(definition.toString(), ",");
					this.outputElements(elements, maxElements, indentCount);
					return this.lexer.getNextToken(true, false);
				}
				else
				{
					definition.append(')');
				}
				bracketCount--;
			}
			else if (bracketCount > 0)
			{
				if (this.needsWhitespace(last, t, true))
				{
					definition.append(' ');
				}
				definition.append(t.getContents());
			}
			last = t;
			t = this.lexer.getNextToken(true, false);
		}
		return t;
	}

	/*
	 *	Output the elements of the given List comma separated
	 *  If the list contains more elements, then maxElements
	 *  each element will be put on a single line
	 *	If more then one line is "printed" they will be indented by
	 *  indentCount spaces
	 */
	private void outputElements(List elements, int maxElements, int indentCount)
	{
		String myIndent = StringUtil.padRight(" ", indentCount);

		int count = elements.size();

		if (count > maxElements)
		{
			this.appendNewline();
			this.indent(myIndent);
			this.appendText("(");
		}
		else
		{
			this.appendText(" (");
		}

		if (count > maxElements)
		{
			this.appendNewline();
			this.indent(myIndent);
			this.indent("  ");
		}

		for (int i=0; i < count; i++)
		{
			String text = (String)elements.get(i);
			this.appendText(text);
			if (i < count - 1)
			{
				if (count > maxElements)
				{
					this.appendText(',');
					this.appendNewline();
					this.indent(myIndent);
					this.indent("  ");
				}
				else
				{
					this.appendText(", ");
				}
			}
		}
		if (count > maxElements)
		{
			this.appendNewline();
			this.indent(myIndent);
		}
		this.appendText(")");
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
					t = this.processCommaList(t, 1, 0);
				}
				bracketCount ++;
			}

			if ("SELECT".equals(t.getContents()))
			{
				return t;
			}
			else if ("AS".equals(t.getContents()))
			{
				this.appendNewline();
				this.appendTokenText(t);
				this.appendNewline();
			}
			else
			{
				this.appendTokenText(t);
				if (this.needsWhitespace(last, t, true))
				{
					this.appendText(' ');
				}
			}
			last = t;
			t = this.lexer.getNextToken(false, false);
		}
		return t;
	}

	private SQLToken processCreateIndex(SQLToken previous)
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken last = previous;

		while (t != null)
		{
			String text = t.getContents();
			if (t.getContents().equals("(") )
			{
				return this.processCommaList(t, 5, 7);
			}
			else if ("ON".equalsIgnoreCase(text))
			{
				this.appendNewline();
				this.indent("       ");
				this.appendTokenText(t);
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


}
