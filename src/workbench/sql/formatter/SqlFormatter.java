/*
 * SqlFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.formatter;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import workbench.resource.Settings;
import workbench.sql.syntax.SqlKeywordHelper;
import workbench.sql.wbcommands.CommandTester;
import workbench.util.CharSequenceReader;
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class SqlFormatter
{
	private final Set<String> LINE_BREAK_BEFORE = new HashSet<String>();
	{
		LINE_BREAK_BEFORE.add("SELECT");
		LINE_BREAK_BEFORE.add("SET");
		LINE_BREAK_BEFORE.add("FROM");
		LINE_BREAK_BEFORE.add("WHERE");
		LINE_BREAK_BEFORE.add("ORDER BY");
		LINE_BREAK_BEFORE.add("GROUP BY");
		LINE_BREAK_BEFORE.add("HAVING");
		LINE_BREAK_BEFORE.add("VALUES");
		LINE_BREAK_BEFORE.add("UNION");
		LINE_BREAK_BEFORE.add("UNION ALL");
		LINE_BREAK_BEFORE.add("MINUS");
		LINE_BREAK_BEFORE.add("INTERSECT");
		LINE_BREAK_BEFORE.add("REFRESH");
		LINE_BREAK_BEFORE.add("AS");
		LINE_BREAK_BEFORE.add("FOR");
		LINE_BREAK_BEFORE.add("INNER JOIN");
		LINE_BREAK_BEFORE.add("RIGHT OUTER JOIN");
		LINE_BREAK_BEFORE.add("LEFT OUTER JOIN");
		LINE_BREAK_BEFORE.add("CROSS JOIN");
		LINE_BREAK_BEFORE.add("LEFT JOIN");
		LINE_BREAK_BEFORE.add("RIGHT JOIN");
		LINE_BREAK_BEFORE.add("START WITH");
		LINE_BREAK_BEFORE.add("CONNECT BY");
	}

	private final Set<String> LINE_BREAK_AFTER = new HashSet<String>();
	{
		LINE_BREAK_AFTER.add("UNION");
		LINE_BREAK_AFTER.add("UNION ALL");
		LINE_BREAK_AFTER.add("MINUS");
		LINE_BREAK_AFTER.add("INTERSECT");
		LINE_BREAK_AFTER.add("AS");
		LINE_BREAK_AFTER.add("FOR");
	}

	// keywords terminating a WHERE clause
	public static final Set<String> WHERE_TERMINAL = new HashSet<String>();
	static
	{
		WHERE_TERMINAL.add("ORDER BY");
		WHERE_TERMINAL.add("GROUP BY");
		WHERE_TERMINAL.add("HAVING");
		WHERE_TERMINAL.add("UNION");
		WHERE_TERMINAL.add("UNION ALL");
		WHERE_TERMINAL.add("INTERSECT");
		WHERE_TERMINAL.add("MINUS");
		WHERE_TERMINAL.add(";");
	}

	// keywords terminating the FROM part
	public static final Set<String> FROM_TERMINAL = new HashSet<String>();
	static
	{
		FROM_TERMINAL.addAll(WHERE_TERMINAL);
		FROM_TERMINAL.add("WHERE");
		FROM_TERMINAL.add("START WITH");
		FROM_TERMINAL.add("CONNECT BY");
	}


	// keywords terminating an GROUP BY clause
	private final Set<String> GROUP_BY_TERMINAL = new HashSet<String>();
	{
		GROUP_BY_TERMINAL.addAll(WHERE_TERMINAL);
		GROUP_BY_TERMINAL.add("SELECT");
		GROUP_BY_TERMINAL.add("UPDATE");
		GROUP_BY_TERMINAL.add("DELETE");
		GROUP_BY_TERMINAL.add("INSERT");
		GROUP_BY_TERMINAL.add("CREATE");
		GROUP_BY_TERMINAL.add("CREATE OR REPLACE");
	}
	
	private final Set<String> ORDER_BY_TERMINAL = new HashSet<String>();
	{
		ORDER_BY_TERMINAL.remove("GROUP BY");
		ORDER_BY_TERMINAL.add(";");
	}

	public static final Set<String> SELECT_TERMINAL = new HashSet<String>(1);
	static
	{
		SELECT_TERMINAL.add("FROM");
	}

	private final Set<String> SET_TERMINAL = new HashSet<String>();
	{
		SET_TERMINAL.add("FROM");
		SET_TERMINAL.add("WHERE");
	}

	private static final Set<String> TABLE_CONSTRAINTS_KEYWORDS = new HashSet<String>();
	{
		TABLE_CONSTRAINTS_KEYWORDS .add("FOREIGN KEY");
		TABLE_CONSTRAINTS_KEYWORDS .add("PRIMARY KEY");
		TABLE_CONSTRAINTS_KEYWORDS .add("CONSTRAINT");
	}
	
	private CharSequence sql;
	private SQLLexer lexer;
	private StringBuilder result;
	private StringBuilder indent = null;
	private StringBuilder leadingWhiteSpace = null;
	private int realLength = 0;
	private int maxSubselectLength = 60;
	private Set<String> dbFunctions = Collections.emptySet();
	private Set<String> dataTypes = Collections.emptySet();
	private int selectColumnsPerLine = 1;
	private static final String NL = "\n";

	public SqlFormatter(CharSequence aScript)
	{
		this(aScript, 0, Settings.getInstance().getFormatterMaxSubselectLength());
	}
	
	public SqlFormatter(CharSequence aScript, int maxSubselectLength)
	{
		this(aScript, 0, maxSubselectLength);
	}

	private SqlFormatter(CharSequence aScript, int indentCount, int maxSubselectLength)
	{
		this.sql = aScript;
		Reader in = new CharSequenceReader(this.sql);
		this.lexer = new SQLLexer(in);
		this.result = new StringBuilder(this.sql.length() + 100);
		if (indentCount > 0)
		{
			this.indent = new StringBuilder(indentCount);
			for (int i=0; i < indentCount; i++) this.indent.append(' ');
		}
		this.maxSubselectLength = maxSubselectLength;
		this.dbFunctions = new HashSet<String>();
		addStandardFunctions(dbFunctions);
	}

	public String getLineEnding()
	{
		return NL;
	}
	
	public void setMaxColumnsPerSelect(int cols)
	{
		this.selectColumnsPerLine = cols;
	}
	
	public void setDbDataTypes(Set<String> types)
	{
		this.dataTypes = types;
	}
	
	public void setDBFunctions(Set<String> functionNames)
	{
		this.dbFunctions = new HashSet<String>();
		if (functionNames != null)
		{
			this.dbFunctions.addAll(functionNames);
		}
		addStandardFunctions(dbFunctions);
	}
	
	private void addStandardFunctions(Set<String> functions)
	{
		functions.add("MIN");
		functions.add("MAX");
		functions.add("AVG");
		functions.add("SUM");
		functions.add("COUNT");
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
	
	public CharSequence getFormattedSql()
		throws Exception
	{
		saveLeadingWhitespace();
		if (this.sql.length() == 0) return sql;
		
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

	
	private void appendComment(String text)
	{
		if (text.startsWith("--"))
		{
			if (!this.isStartOfLine()) this.appendNewline();
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

	private void indent(StringBuilder text)
	{
		this.result.append(text);
	}

	private boolean needsWhitespace(SQLToken last, SQLToken current)
	{
		return this.needsWhitespace(last, current, false);
	}

	private boolean isDbFunction(String key)
	{
		if (dbFunctions == null) 
		{
			SqlKeywordHelper helper = new SqlKeywordHelper();
			dataTypes = helper.getSystemFunctions();
		}
		return this.dbFunctions.contains(key.toUpperCase());
	}
	
	private boolean isDatatype(String key)
	{
		if (dataTypes == null) 
		{
			SqlKeywordHelper helper = new SqlKeywordHelper();
			dataTypes = helper.getDataTypes();
		}
		return this.dataTypes.contains(key.toUpperCase());
	}
	
	/**
	 * 	Return true if a whitespace should be added before the current token.
	 */
	private boolean needsWhitespace(SQLToken last, SQLToken current, boolean ignoreStartOfline)
	{
		String lastV = last.getContents();
		String currentV = current.getContents();
		char lastChar = lastV.charAt(0);
		char currChar = currentV.charAt(0);
		if (last.isWhiteSpace()) return false;
		if (!ignoreStartOfline && this.isStartOfLine()) return false;
		boolean isCurrentOpenBracket = "(".equals(currentV);
		boolean isLastOpenBracket = "(".equals(lastV);
		boolean isLastCloseBracket = ")".equals(lastV);
		
		if (isCurrentOpenBracket && last.isIdentifier()) return false;
		if (isCurrentOpenBracket && isDbFunction(lastV)) return false;
		if (isCurrentOpenBracket && isDatatype(lastV)) return false;
		if (isCurrentOpenBracket && last.isReservedWord()) return true;
		if (isLastCloseBracket && currChar == ',') return false;
		if (isLastCloseBracket && (current.isIdentifier() || current.isReservedWord())) return true;

		if ((lastChar == '-' || lastChar == '+') && current.isLiteral() && StringUtil.isNumber(currentV)) return false;
		
		if (last.isLiteral() && (current.isIdentifier() || current.isReservedWord() || current.isOperator())) return true;

		//if (last.isLiteral() && current.isLiteral()) return false;
		
		if (currChar == '?') return true;
		if (currentV.equals("=")) return true;
		if (lastV.equals("=")) return true;
		
		if (lastChar == '.' && current.isIdentifier()) return false;
		if (isLastOpenBracket && current.isReservedWord()) return false;
		if (isLastCloseBracket && !current.isSeparator() ) return true;
		if ((last.isIdentifier() || last.isLiteral()) && current.isOperator()) return true;
		if ((current.isIdentifier() || current.isLiteral()) && last.isOperator()) return true;
		if (current.isSeparator() || current.isOperator()) return false;
		if (last.isOperator() && (current.isReservedWord() || current.isIdentifier() || current.isLiteral())) return true;
		if (last.isSeparator() || last.isOperator()) return false;
		return true;
	}

	private SQLToken processFrom(SQLToken last)
		throws Exception
	{
		StringBuilder b = new StringBuilder("     ");
		StringBuilder oldIndent = this.indent;
		//this.indent = null;
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = last;
		while (t != null)
		{
			String text = t.getContents();

			if (t.isReservedWord() && FROM_TERMINAL.contains(text.toUpperCase()))
			{
				this.indent = oldIndent;
				return t;
			}
			else if (lastToken.isSeparator() && lastToken.getContents().equals("(") && text.equalsIgnoreCase("SELECT") )
			{
				t = this.processSubSelect(true);
				continue;
			}

			if (t.isComment())
			{
				this.appendComment(text);
			}
			else if (t.isSeparator() && text.equals("("))
			{
				if ((!lastToken.isSeparator() || lastToken == t) && !this.lastCharIsWhitespace()) this.appendText(' ');
				this.appendText(text);
			}
			else if (t.isSeparator() && text.equals(")"))
			{
				this.appendText(text);
			}
			else if (t.isSeparator() && text.equals(","))
			{
				this.appendText(",");
				this.appendNewline();
				this.indent(b);
			}
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				if (LINE_BREAK_BEFORE.contains(text) && !text.equalsIgnoreCase("AS"))
				{
					this.appendNewline();
					this.indent(b);
				}
				this.appendText(text);
				if (LINE_BREAK_AFTER.contains(text) && !text.equalsIgnoreCase("AS"))
				{
					this.appendNewline();
					this.indent(b);
				}
			}
			lastToken = t;
			t = this.lexer.getNextToken(true, false);
		}
		this.indent = oldIndent;
		return null;
	}

	private SQLToken processList(SQLToken last, int indentCount, Set<String> terminalKeys)
		throws Exception
	{
		StringBuilder b = new StringBuilder(indentCount);
		for (int i=0; i < indentCount; i++) b.append(' ');

		int currentColumnCount = 0;
		boolean isSelect = last.getContents().equals("SELECT");
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = last;
		
		while (t != null)
		{
			String text = t.getContents();
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
				this.appendText(text);
				int caseIndent = indentCount;
				if (!isSelect) 
				{
					caseIndent = this.getCurrentLineLength() - 4;
					this.appendText(' ');
				}
				t = processCase(caseIndent);
				continue;
			}
			else if (t.isReservedWord() && terminalKeys.contains(text.toUpperCase()))
			{
				return t;
			}
			else if (t.isSeparator() && text.equals("("))
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendText("(");
				// an equal sign immediately followed by an opening 
				// bracket cannot be a function call (the function name 
				// is missing) so it has to be a sub-select
				if ("=".equals(lastToken.getContents()))
				{
					t = this.processSubSelect(false);
					this.appendText(t.getContents());
				}
				else
				{
					t = this.processFunctionCall(t);
					if (t == null) return null;
					if (t.isIdentifier())
					{
						this.appendText(' ');
						this.appendText(t.getContents());
					}
				}
			}
			else if (t.isSeparator() && text.equals(","))
			{
				this.appendText(',');
				currentColumnCount++;
				if (!isSelect || currentColumnCount >= selectColumnsPerLine)
				{
					currentColumnCount = 0;
					this.appendNewline();
					this.indent(b);
				}
				else
				{
					this.appendText(' ');
				}
			}
			else if (text.equals("*") && !lastToken.isSeparator())
			{
				this.appendText(" *");
			}
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendText(text);
			}
			lastToken = t;
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private SQLToken processSubSelect(boolean addSelectKeyword)
		throws Exception
	{
		SQLToken t = this.lexer.getNextToken();
		int bracketCount = 1;
		StringBuilder subSql = new StringBuilder(250);

		// this method gets called when then "parser" hits an
		// IN ( situation. If no SELECT is coming, we assume
		// its a list like IN ('x','Y')
		if (!"SELECT".equalsIgnoreCase(t.getContents()) && !addSelectKeyword)
		{
			return this.processInList(t);
		}

		if (addSelectKeyword)
		{
			subSql.append("SELECT ");
		}

		int lastIndent = this.getCurrentLineLength();

		while (t != null)
		{
			String text = t.getContents();
			if (text.equals(")"))
			{
				bracketCount --;

				if (bracketCount == 0)
				{
					appendSubSelect(subSql, lastIndent);
					return t;
				}
			}
			else if (t.isSeparator() && text.equals("("))
			{
				bracketCount ++;
			}
			subSql.append(text);
			t = this.lexer.getNextToken();
		}
		this.appendText(subSql);
		return t;
	}
	
	private void appendSubSelect(StringBuilder subSql, int lastIndent)
		throws Exception
	{
		SqlFormatter f = new SqlFormatter(subSql.toString(), lastIndent, this.maxSubselectLength);
		String s = f.getFormattedSql().toString();
		if (f.getRealLength() < this.maxSubselectLength)
		{
			s = s.replaceAll(" *" + SqlFormatter.NL + " *", " ");
		}
		this.appendText(s.trim());
	}
	
	private SQLToken processDecode(int indent)
		throws Exception
	{
		StringBuilder current = new StringBuilder(indent);

		for (int i=0; i < indent; i++) current.append(' ');
		
		StringBuilder b = new StringBuilder(indent + 2);
		for (int i=0; i < indent; i++) b.append(' ');
		b.append("      ");
		
		boolean newLinePending = false;
		
		SQLToken t = this.lexer.getNextToken(true,true);
		String text = null;
		int commaCount = 0;
		int bracketCount = 0;
		
		boolean inQuotes = false;
		while (t != null)
		{
			text = t.getContents();
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
					this.indent(b);
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
			else if (text.indexOf("\n") == -1 &&  text.indexOf("\r") == -1)
			{
				this.appendText(text);
			}
			t = this.lexer.getNextToken(true,true);
		}
		return null;
	}

	private SQLToken processCase(int indent)
		throws Exception
	{
		StringBuilder current = new StringBuilder(indent);

		for (int i=0; i < indent; i++) current.append(' ');
		
		StringBuilder b = new StringBuilder(indent + 2);
		for (int i=0; i < indent; i++) b.append(' ');
		b.append("  ");
		
		SQLToken last = null;
		SQLToken t = this.lexer.getNextToken(true,false);
		String text = null;
		while (t != null)
		{
			text = t.getContents();
			
			if ("SELECT".equals(text) && last.getContents().equals("("))
			{
				t = this.processSubSelect(true);
				if (t == null) return null;
				if (t.getContents().equals(")")) this.appendText(")");
			}
			else if ("WHEN".equals(text) || "ELSE".equals(text))
			{
				this.appendNewline();
				this.indent(b);
				this.appendText(text);
				appendText(' ');
			}
			else if ("THEN".equals(text))
			{
				if (last != null && this.needsWhitespace(last, t)) appendText(' ');
				this.appendText(text);
				appendText(' ');
			}
			else if ("END".equals(text) || "END CASE".equals(text))
			{
				this.appendNewline();
				this.indent(current);
				this.appendText(text);
				// Get the next token after the END. If that is the keyword AS, 
				// the CASE statement ist not yet ended and we have to add the AS keyword
				// and the alias that was given before returning to the caller
				t = this.lexer.getNextToken(true, false);
				if (t != null && t.getContents().equals("AS"))
				{
					this.appendText(' ');
					this.appendText(t.getContents());
					t = this.lexer.getNextToken(true, false);
					if (t != null) 
					{
						this.appendText(' ');
						this.appendText(t.getContents());
						t = this.lexer.getNextToken(true, false);
					}
				}
				this.appendNewline();
				return t;
			}
			else if (t.isComment())
			{
				this.appendComment(text);
			}
			else if (!t.isWhiteSpace())
			{
//				if (this.needsWhitespace(last, t)) this.appendText(' ');
				this.appendText(text);
			}
			last = t;
			t = this.lexer.getNextToken(true,false);
		}
		return null;
	}
	
	private SQLToken processWbCommand(int indent)
		throws Exception
	{
		StringBuilder b = new StringBuilder(indent);

		for (int i=0; i < indent; i++) b.append(' ');
		this.appendText(' ');

		SQLToken t = this.lexer.getNextToken(true,false);
		boolean first = true;
		boolean isParm = false;
		while (t != null)
		{
			String text = t.getContents();
			if (isParm) text = text.toLowerCase();
			if (text.equals("-"))
			{
				if (!first) 
				{
					this.appendNewline();
					this.indent(b);
				}
				isParm = true;
			}
			else
			{
				isParm = false;
			}
			this.appendText(text);
			t = this.lexer.getNextToken(true,false);
			first = false;
		}
		return null;
	}
	
	private SQLToken processBracketList(int indentCount)
		throws Exception
	{
		StringBuilder b = new StringBuilder(indentCount);

		for (int i=0; i < indentCount; i++) b.append(' ');

		this.appendText(b);
		SQLToken t = this.lexer.getNextToken(true,false);

		while (t != null)
		{
			String text = t.getContents();
			if (text.equals(")"))
			{
				this.appendNewline();
				//this.indent(b);
				this.appendText(")");
				return this.lexer.getNextToken();
			}
			else if (text.equals("("))
			{
				this.appendText(" (");
				t = this.processFunctionCall(t);
				continue;
			}
			else if (text.equals(","))
			{
				this.appendText(",");
				this.appendNewline();
				this.indent(b);
			}
			else if (!t.isWhiteSpace())
			{
				this.appendText(text);
				if (t.isComment()) this.appendText(' ');
			}
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private SQLToken processInList(SQLToken current)
		throws Exception
	{
		ArrayList<StringBuilder> list = new ArrayList<StringBuilder>(25);
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
					return this.lexer.getNextToken();
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

	private void appendCommaList(ArrayList aList)
	{
		int indentCount = this.getCurrentLineLength();
		StringBuilder ind = new StringBuilder(indentCount);
		for (int i=0; i < indentCount; i++) ind.append(' ');
		boolean newline = (aList.size() > 10);
		int count = aList.size();
		for (int i=0; i < count; i++)
		{
			this.appendText((StringBuilder)aList.get(i));
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
		if (remain.trim().length() == 0 && remain.length() == indentLength) return true;
		return false;
	}

	private void formatSql()
		throws Exception
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = t;
		CommandTester wbTester = new CommandTester();
		//if (this.indent != null) this.appendText(this.indent);
		while (t != null)
		{
			String word = t.getContents().toUpperCase();
			if (t.isComment())
			{
				String text = t.getContents();
				this.appendComment(text);
			}
			else if (t.isReservedWord())
			{
				if (lastToken.isComment() && !isStartOfLine()) this.appendNewline();

				if (LINE_BREAK_BEFORE.contains(word))
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
					this.appendText(word);
				}
				
				if (LINE_BREAK_AFTER.contains(word))
				{
					this.appendNewline();
				}

				if (word.equals("SELECT"))
				{
					lastToken = t;
					t = this.processList(t,"SELECT".length() + 1, SELECT_TERMINAL);
					if (t == null) return;
					continue;
				}

				if (word.equals("SET"))
				{
					lastToken = t;
					t = this.processList(t,"SET".length() + 4, SET_TERMINAL);
					if (t == null) return;
					continue;
				}
				
				if (word.equals("CREATE") || word.equals("CREATE OR REPLACE"))
				{
					lastToken = t;
					t = this.processCreate(t);
					if (t == null) return;
					continue;
				}				
				
				if (word.equals("FROM"))
				{
					lastToken = t;
					t = this.processFrom(t);
					if (t == null) return;
					continue;
				}
				
				if (word.equals("GROUP BY"))
				{
					lastToken = t;
					t = this.processList(lastToken, (word + " ").length(), GROUP_BY_TERMINAL);
					if (t == null) return;
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
					continue;
				}

				if (word.equalsIgnoreCase("INTO"))
				{
					lastToken = t;
					t = this.processIntoKeyword();
					continue;
				}

				if (word.equalsIgnoreCase("VALUES"))
				{
					// the next (non-whitespace token has to be a (
					t = this.lexer.getNextToken(false, false);
					if (t.isSeparator() && t.getContents().equals("("))
					{
						this.appendNewline();
						this.appendText("(");
						this.appendNewline();

						t = this.processBracketList(2);
					}
					if (t == null) return;
					continue;
				}
				
				if (wbTester.isWbCommand(word))
				{
					t = this.processWbCommand(word.length() + 1);
				}
				
			}
			else
			{
				if (LINE_BREAK_BEFORE.contains(word))
				{
					if (!isStartOfLine()) this.appendNewline();
				}

				if (word.equals("("))
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
						this.appendText(t.getContents());
					}
				}
			}
			lastToken = t;
			t = this.lexer.getNextToken(true, false);
		}
//		this.appendNewline();
//		this.appendNewline();
	}

	private SQLToken processWhere(SQLToken previousToken)
		throws Exception
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		SQLToken lastToken = previousToken;
		int bracketCount = 0;
		while (t != null)
		{
			String verb = t.getContents();

			if (t.isReservedWord() && WHERE_TERMINAL.contains(verb))
			{
				return t;
			}

			if (verb.equals(";"))
			{
				return t;
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
			else if (t.getContents().equals("("))
			{
				String lastWord = lastToken.getContents();
				
				if (lastWord != null) lastWord = lastWord.toUpperCase();
				if (!lastToken.isSeparator() && !this.dbFunctions.contains(lastWord)) this.appendText(' ');
				this.appendText(t.getContents());
				
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
			else if (bracketCount == 0 && t.isReservedWord() && (verb.equals("AND") || verb.equals("OR")) )
			{
				// TODO: this attempt to keep conditions in bracktes together results
				// in effectively no formatting when the whole WHERE clause is put 
				// between brackets (because bracketCount will never be zero until 
				// the end of the WHERE clause)
				if (!this.isStartOfLine()) this.appendNewline();
				this.appendText(verb);
				this.appendText("  ");
				if (verb.equals("OR")) this.appendText(' ');
			}
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendText(verb);
			}

			lastToken = t;
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private SQLToken skipComments()
		throws Exception
	{
		SQLToken next = lexer.getNextToken(true, false);
		if (!next.isComment()) return next;
		while (next != null) 
		{
			if (!next.isComment()) return next;
			this.appendComment(next.getContents());
			next = lexer.getNextToken(true, false);
		}
		return null;
	}
	
	private SQLToken processIntoKeyword()
		throws Exception
	{
		SQLToken t = this.lexer.getNextToken(false, false);
		// we expect an identifier now (the table name)
		// but to be able to handle "wrong statements" we'll
		// make sure everything's fine

		if (t.isIdentifier())
		{
			this.appendText(' ');
			this.appendText(t.getContents());
			t = this.lexer.getNextToken(false, false);
			if (t.getContents().equalsIgnoreCase("VALUES"))
			{
				// no column list to format here...
				return t;
			}
			else if (t.isSeparator() && t.getContents().equals("("))
			{
				this.appendNewline();
				this.appendText(t.getContents());
				this.appendNewline();
				return this.processBracketList(2);
			}
			return t;
		}
		else
		{
			return t;
		}
	}

	private SQLToken processFunctionCall(SQLToken last)
		throws Exception
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
				SQLToken l = t;
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
			this.appendText(t.getContents());

			if (bracketCount == 0)
			{
				return t;
			}
			lastToken = t;
			t = this.lexer.getNextToken(true, false);
		}
		return null;
	}

	private SQLToken processCreate(SQLToken previous)
		throws Exception
	{
		SQLToken t = this.lexer.getNextToken(true, false);
		String verb = t.getContents();
		int createStart = t.getCharBegin();
		
		if (verb.equals("TABLE"))
		{
			this.appendText(' ');
			this.appendText(t.getContents());
			this.appendText(' ');
			t = this.processCreateTable(t);
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

	private boolean isTableConstraint(String keyword)
	{
		return TABLE_CONSTRAINTS_KEYWORDS.contains(keyword);
	}
	
	private SQLToken processTableDefinition()
	{
		List<StringBuilder> cols = new ArrayList<StringBuilder>();
		SQLToken t = lexer.getNextToken(true, false);
		StringBuilder line = new StringBuilder(50);
		int maxColLength = 0;

		boolean isColname = true;
		int bracketCount = 0;
		
		SQLToken last = null;
		
		while (t != null)
		{
			String w = t.getContents();
			
			if (isTableConstraint(w))
			{
				// end of column definitions reached
				break;
			}
			
			if ("(".equals(w)) bracketCount ++;
			if (")".equals(w)) bracketCount --;

			// Closing bracket reached --> end of 
			if (bracketCount < 0)
			{
				cols.add(line);
				break;
			}

			if (!isColname && last != null && needsWhitespace(last, t, true))
			{
				line.append(' ');
			}
			line.append(w);
			
			if (isColname && t.isIdentifier())
			{
				if (w.length() > maxColLength) maxColLength = w.length();
				isColname = false;
			}

			if (w.equals(",") && bracketCount == 0)
			{
				cols.add(line);
				line = new StringBuilder(50);
				isColname = true;
			}

			last = t;
			t = this.lexer.getNextToken(true, false);
		}
		
		// Now process the collected column definitions
		for (StringBuilder col : cols)
		{
			int pos = StringUtil.findFirstWhiteSpace(col);
			String colname = col.substring(0, pos).trim();
			String def = col.substring(pos + 1).trim();
			appendText("  ");
			appendText(colname);
			while (pos < maxColLength)
			{
				this.appendText(' ');
				pos ++;
			}
			this.appendText("   ");
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
				appendText(w);
				
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
	
	/**
	 * Process a CREATE TABLE statement. 
	 * The CREATE TABLE has already been added!
	 */
	private SQLToken processCreateTable(SQLToken previous)
		throws Exception
	{
		SQLToken t = this.lexer.getNextToken(false, false);
		if (t == null) return t;
		
		// the next token has to be the table name, so 
		// we can simply write it out
		
		this.appendText(t.getContents());
		this.appendText(' ');
	
		t = this.lexer.getNextToken(false, false);
		if (t == null) return t;
		
		// this has to be the opening bracket before the table definition
		this.appendNewline();
		this.appendText('(');
		this.appendNewline();
		
		t = processTableDefinition();
		
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
		throws Exception
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
		StringBuilder myIndent = new StringBuilder(indentCount);
		for (int i=0; i<indentCount; i++) myIndent.append(' ');

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
		throws Exception
	{
		SQLToken t = this.lexer.getNextToken(false, false);
		SQLToken last = previous;
		int bracketCount = 0;
		StringBuilder definition = new StringBuilder(200);

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
				this.appendText(t.getContents());
				this.appendNewline();
			}
			else
			{
				this.appendText(t.getContents());
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
		throws Exception
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
			else if (t.isReservedWord() && "ON".equals(text))
			{
				this.appendNewline();
				this.indent("       ");
				this.appendText(text);
			}
			else
			{
				if (this.needsWhitespace(last, t)) this.appendText(' ');
				this.appendText(text);
			}
			t = this.lexer.getNextToken(true, false);
		}
		return t;
	}


}
