/*
 * SqlFormatter.java
 *
 * Created on September 6, 2003, 5:58 PM
 */

package workbench.sql.formatter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  thomas
 */
public class SqlFormatter
{
	private static final Set LINE_BREAK_BEFORE = new HashSet();
	static
	{
		LINE_BREAK_BEFORE.add("SELECT");
		LINE_BREAK_BEFORE.add("SET");
		LINE_BREAK_BEFORE.add("FROM");
		LINE_BREAK_BEFORE.add("WHERE");
		LINE_BREAK_BEFORE.add("ORDER");
		LINE_BREAK_BEFORE.add("GROUP");
		LINE_BREAK_BEFORE.add("HAVING");
		LINE_BREAK_BEFORE.add("VALUES");
		LINE_BREAK_BEFORE.add("UNION");
		LINE_BREAK_BEFORE.add("MINUS");
		LINE_BREAK_BEFORE.add("INTERSECT");
		LINE_BREAK_BEFORE.add("REFRESH");
		LINE_BREAK_BEFORE.add("AS");
		LINE_BREAK_BEFORE.add("FOR");
	}

	private static final Set LINE_BREAK_AFTER = new HashSet();
	static
	{
		//LINE_BREAK_AFTER.add("UNION");
		LINE_BREAK_AFTER.add("MINUS");
		LINE_BREAK_AFTER.add("INTERSECT");
		LINE_BREAK_AFTER.add("AS");
		LINE_BREAK_AFTER.add("FOR");
		LINE_BREAK_AFTER.add("JOIN");
	}

	private static final Set SUBSELECT_START = new HashSet();
	static
	{
		SUBSELECT_START.add("IN");
		SUBSELECT_START.add("EXISTS");
	}

	// keywords terminating a WHERE clause
	private static final Set WHERE_TERMINAL = new HashSet();
	static
	{
		WHERE_TERMINAL.add("ORDER");
		WHERE_TERMINAL.add("GROUP");
		WHERE_TERMINAL.add("HAVING");
		WHERE_TERMINAL.add("UNION");
		WHERE_TERMINAL.add("INTERSECT");
		WHERE_TERMINAL.add("MINUS");
		WHERE_TERMINAL.add(";");
	}

	// keywords terminating the FROM part
	private static final Set FROM_TERMINAL = new HashSet();
	static
	{
		FROM_TERMINAL.addAll(WHERE_TERMINAL);
		FROM_TERMINAL.add("WHERE");
	}


	// keywords terminating an GROUP BY clause
	private static final Set BY_TERMINAL = new HashSet();
	static
	{
		BY_TERMINAL.addAll(WHERE_TERMINAL);
		BY_TERMINAL.add("SELECT");
		BY_TERMINAL.add("UPDATE");
		BY_TERMINAL.add("DELETE");
		BY_TERMINAL.add("INSERT");
		BY_TERMINAL.add("CREATE");
		BY_TERMINAL.add("GROUP");
		BY_TERMINAL.add(";");
	}

	private static final Set SELECT_TERMINAL = new HashSet(1);
	static
	{
		SELECT_TERMINAL.add("FROM");
	}

	private static final Set SET_TERMINAL = new HashSet();
	static
	{
		SET_TERMINAL.add("FROM");
		SET_TERMINAL.add("WHERE");
	}

	private String sql;
	private SQLLexer lexer;
	private StringBuffer result;
	private StringBuffer indent = null;
	private int realLength = 0;
	private int maxSubselectLength = 60;
	
	public SqlFormatter(String aScript, int maxLength)
	{
		this(aScript, 0, maxLength);
	}

	private SqlFormatter(String aScript, int indentCount, int maxSubselectLength)
	{
		this.sql = aScript;
		Reader in = new StringReader(this.sql);
		this.lexer = new SQLLexer(in);
		this.result = new StringBuffer(this.sql.length() + 100);
		if (indentCount > 0)
		{
			this.indent = new StringBuffer(indentCount);
			for (int i=0; i < indentCount; i++) this.indent.append(' ');
		}
		this.maxSubselectLength = maxSubselectLength;
	}

	public String format()
		throws Exception
	{
		this.formatSql();
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

	private int indentNewline()
	{
		int pos = this.getCurrentLineLength();
		this.indentNewline(pos);
		return pos;
	}

	private void indentNewline(int pos)
	{
		this.appendNewline();
		for (int i = 0; i < pos; i ++) this.result.append(' ');
	}

	private void appendNewline()
	{
		if (this.result.length() == 0) return;
		this.result.append('\n');
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
	
	private void indent(char c)
	{
		this.result.append(c);
	}

	private void appendText(String text)
	{
		this.realLength += text.length();
		this.result.append(text);
	}

	private void appendText(StringBuffer text)
	{
		this.realLength += text.length();
		this.result.append(text);
	}

	private void indent(String text)
	{
		this.result.append(text);
	}
	
	private void indent(StringBuffer text)
	{
		this.result.append(text);
	}
	
	private void appendNonSeparator(String text)
	{
		this.realLength += text.length();
		if (!text.startsWith(" ") && !lastCharIsWhitespace()) this.result.append(' ');
		this.result.append(text);
	}

	private boolean needsWhitespace(SQLToken last, SQLToken current)
	{
		return this.needsWhitespace(last, current, false);
	}
	private boolean needsWhitespace(SQLToken last, SQLToken current, boolean ignoreStartOfline)
	{
		if (!ignoreStartOfline && this.isStartOfLine()) return false;
		if (last.getContents().equals("\"")) return false;
		if (last.getContents().equals(".") && current.isIdentifier()) return false;
		if (last.getContents().equals(")") && !current.isSeparator() ) return true;
		if ((last.isIdentifier()|| last.isLiteral()) && current.isOperator()) return true;
		if ((current.isIdentifier() || current.isLiteral()) && last.isOperator()) return true;
		if (current.isSeparator() || current.isOperator()) return false;
		if (last.isSeparator() || last.isOperator()) return false;
		return true;
	}

	private SQLToken processFrom()
		throws Exception
	{
		StringBuffer b = new StringBuffer("     ");
		StringBuffer oldIndent = this.indent;
		this.indent = null;
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		SQLToken lastToken = t;
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

			if (t.isSeparator() && text.equals("("))
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
				this.appendText(text);
				if (LINE_BREAK_AFTER.contains(text))
				{
					this.appendNewline();
					this.indent(b);
				}
			}
			lastToken = t;
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
		this.indent = oldIndent;
		return null;
	}

	private SQLToken processList(SQLToken last, int indentCount, Set terminalKeys)
		throws Exception
	{
		StringBuffer b = new StringBuffer(indentCount);
		for (int i=0; i < indentCount; i++) b.append(' ');

		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		SQLToken lastToken = last;

		while (t != null)
		{
			String text = t.getContents();
			if (t.isReservedWord() && terminalKeys.contains(text.toUpperCase()))
			{
				return t;
			}
			else if (t.isSeparator() && text.equals("("))
			{
				this.appendText("(");
				this.processFunctionCall();
			}
			else if (t.isSeparator() && text.equals(","))
			{
				this.appendText(",");
				this.appendNewline();
				this.indent(b);
			}
			else if (text.equals("*") && !lastToken.isSeparator())
			{
				this.appendText(" *");
			}
			/*
			else if (t.isOperator() || t.isSeparator())
			{
				this.appendText(text);
			}
			*/
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendText(text);
			}
			lastToken = t;
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
		return null;
	}

	private SQLToken processSubSelect(boolean addSelectKeyword)
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		int bracketCount = 1;
		StringBuffer subSql = new StringBuffer(250);

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

		boolean realSubSelect = false;
		
		while (t != null)
		{
			String text = t.getContents();
			if (t.isSeparator() && text.equals(")"))
			{
				bracketCount --;

				if (bracketCount == 0)
				{
					SqlFormatter f = new SqlFormatter(subSql.toString(), lastIndent, this.maxSubselectLength);
					String s = f.format();
					if (f.getRealLength() < this.maxSubselectLength) 
					{
						s = s.replaceAll(" *\n *", " ");
						this.appendText(s.trim());
					}
					else
					{
						this.appendText(s);
						this.appendNewline();
						for (int i=0; i < lastIndent; i++) this.indent(' ');
					}
					
					return t;
				}
			}
			else if (t.isSeparator() && text.equals("("))
			{
				bracketCount ++;
			}
			subSql.append(' ');
			subSql.append(text);
			t = (SQLToken)this.lexer.getNextToken();
		}
		return null;
	}

	private SQLToken processBracketList(int indentCount)
		throws Exception
	{
		StringBuffer b = new StringBuffer(indentCount);
		
		for (int i=0; i < indentCount; i++) b.append(' ');

		this.appendText(b);
		SQLToken t = (SQLToken)this.lexer.getNextToken(false,false);

		while (t != null)
		{
			String text = t.getContents();
			if (t.isSeparator() && text.equals(")"))
			{
				this.appendNewline();
				this.indent(b);
				this.appendText(")");
				return (SQLToken)this.lexer.getNextToken();
			}
			else if (t.isSeparator() && text.equals("("))
			{
				this.appendText(' ');
				this.appendText("(");
				this.processFunctionCall();
			}
			else if (t.isSeparator() && text.equals(","))
			{
				this.appendText(",");
				this.appendNewline();
				this.indent(b);
			}
			else if (!t.isWhiteSpace())
			{
				this.appendText(text);
			}
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
		return null;
	}

	private SQLToken processInList(SQLToken current)
		throws Exception
	{
		ArrayList list = new ArrayList(25);
		list.add(new StringBuffer(""));
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
					return (SQLToken)this.lexer.getNextToken();
				}
				else
				{
					StringBuffer b = (StringBuffer)list.get(elementcounter);
					if (b == null) 
					{
						b = new StringBuffer(text);
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
					list.add(new StringBuffer(""));
					elementcounter = list.size() - 1;
				}
			}
			else if (!t.isWhiteSpace())
			{
				StringBuffer b = (StringBuffer)list.get(elementcounter);
				if (b == null)
				{
					b = new StringBuffer(text);
					list.set(elementcounter, b);
				}
				else
				{
					b.append(text);
				}
			}
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
		return null;
	}
	
	private void appendCommaList(ArrayList aList)
	{
		int indent = this.getCurrentLineLength();
		StringBuffer ind = new StringBuffer(indent);
		for (int i=0; i < indent; i++) ind.append(' ');
		boolean newline = (aList.size() > 10);
		int count = aList.size();
		for (int i=0; i < count; i++)
		{
			this.appendText((StringBuffer)aList.get(i));
			if (i < count - 1) this.appendText(", ");
			if (newline) 
			{
				this.appendNewline();
				this.indent(ind);
			}
		}
		this.appendText(")");
	}
	
	private void advanceToOpeningBracket()
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		while (t != null)
		{
			if (t.isSeparator() && t.getContents().equals("("))
			{
				this.appendNewline();
				this.appendText(t.getContents());
				return;
			}
			this.appendText(' ');
			this.appendText(t.getContents());
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
	}

	private boolean isStartOfLine()
	{
		int len = this.result.length();
		if (len == 0) return true;
		return (this.result.charAt(len - 1) == '\n');
	}

	private boolean isLastCharWhitespace()
	{
		int len = this.result.length();
		if (len == 0) return true;
		return (this.result.charAt(len - 1) == ' ');
	}

	private void formatSql()
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken(true, false);
		SQLToken lastToken = t;
		while (t != null)
		{
			if (t.isReservedWord())
			{
				String word = t.getContents().toUpperCase();
				if (LINE_BREAK_BEFORE.contains(word))
				{
					if (!isStartOfLine()) this.appendNewline();
					this.appendText(word);
				}
				else
				{
					if (!lastToken.isSeparator() && lastToken != t) this.appendText(' ');
					this.appendText(word);
				}

				if (LINE_BREAK_AFTER.contains(word))
				{
					this.appendNewline();
				}
				
				if (word.equals("ALL") && lastToken.isReservedWord() && lastToken.getContents().equals("UNION"))
				{
					this.appendNewline();
				}

				if (word.equals("SELECT"))
				{
					t = this.processList(t,"SELECT".length() + 1, SELECT_TERMINAL);
					if (t == null) return;
					continue;
				}

				if (word.equals("SET"))
				{
					t = this.processList(t,"SET".length() + 1, SET_TERMINAL);
					if (t == null) return;
					continue;
				}
				if (word.equals("FROM"))
				{
					t = this.processFrom();
					if (t == null) return;
					continue;
				}

				if (word.equals("CREATE"))
				{
					t = this.processCreate(t);
					if (t == null) return;
					continue;
				}
				if (word.equals("BY") && lastToken.isReservedWord()	&& lastToken.getContents().equals("GROUP"))
				{
					t = this.processList(lastToken, "GROUP BY ".length(), BY_TERMINAL);
					if (t == null) return;
					continue;
				}

				if (word.equalsIgnoreCase("WHERE"))
				{
					t = this.processWhere(t);
					if (t == null) return;
					continue;
				}

				if (word.equalsIgnoreCase("INTO"))
				{
					t = this.processIntoKeyword();
					continue;
				}

				if (word.equalsIgnoreCase("VALUES"))
				{
					// the next (non-whitespace token has to be a (
					t = (SQLToken)this.lexer.getNextToken(false, false);
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
			}
			else
			{
				String word = t.getContents().toUpperCase();
				boolean newLine = false;
				if (LINE_BREAK_BEFORE.contains(word))
				{
					if (!isStartOfLine()) this.appendNewline();
				}

				if (t.isSeparator() && word.equals("("))
				{
					this.appendText(" (");
					this.processFunctionCall();
				}
				else
				{
					if (t.isSeparator() && word.equals(";"))
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
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
		this.appendNewline();
		this.appendNewline();
	}

	private SQLToken processWhere(SQLToken previousToken)
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		SQLToken lastToken = previousToken;
		int bracketCount = 0;
		boolean bracketChange = false;
		while (t != null)
		{
			String verb = t.getContents();

			if (t.isReservedWord() && WHERE_TERMINAL.contains(verb))
			{
				return t;
			}

			if (t.isSeparator() && verb.equals(";"))
			{
				return t;
			}

			if (t.isSeparator() && verb.equals(")"))
			{
				bracketCount --;
			}

			if (bracketCount == 0 && t.isReservedWord() && (verb.equals("AND") || verb.equals("OR")) )
			{
				this.appendNewline();
				this.appendText(verb);
				this.appendText("  ");
				if (verb.equals("OR")) this.appendText(' ');
			}
			else if (t.isSeparator() && t.getContents().equals("("))
			{
				bracketCount ++;
				String lastWord = lastToken.getContents();
				if (lastToken.isReservedWord() && SUBSELECT_START.contains(lastWord))
				{
					this.appendText(" (");
					t = this.processSubSelect(false);
					if (t == null) return null;
					continue;
				}

				if (!lastToken.isSeparator()) this.appendText(' ');
				this.appendText(t.getContents());
			}
			else
			{
				if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
				this.appendText(t.getContents());
			}

			lastToken = t;
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
		return null;
	}

	private SQLToken processIntoKeyword()
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		// we expect an identifier now (the table name)
		// but to be able to handle "wrong statements" we'll
		// make sure everything's fine

		if (t.isIdentifier())
		{
			this.appendText(' ');
			this.appendText(t.getContents());
			t = (SQLToken)this.lexer.getNextToken(false, false);
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

	private void processFunctionCall()
		throws Exception
	{
		int bracketCount = 1;
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		SQLToken lastToken = t;
		while (t != null)
		{
			String text = t.getContents();
			if (t.isSeparator() && text.equals(")"))
			{
				bracketCount --;
			}
			if (t.isSeparator() && text.equals("("))
			{
				bracketCount ++;
			}
			if (this.needsWhitespace(lastToken, t)) this.appendText(' ');
			this.appendText(t.getContents());
			if (bracketCount == 0)
			{
				this.appendText(' ');
				break;
			}
			lastToken = t;
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
	}

	private SQLToken processCreate(SQLToken previous)
		throws Exception
	{
		//this.appendText(previous.getContents());
		SQLToken t = (SQLToken)this.lexer.getNextToken(true, false);
		this.appendText(' ');
		SQLToken last = previous;
		while (t != null)
		{
			String verb = t.getContents().toUpperCase();
			this.appendText(t.getContents());
			if (this.needsWhitespace(last, t))
			{
				this.appendText(' ');
			}
			if (verb.equals("TABLE"))
			{
				t = this.processCreateTable(t);
				return t;
			}
			else if (verb.equals("VIEW"))
			{
				return this.processCreateView(t);
			}
			else 
			{
				this.appendText(t.getContents());
				if (this.needsWhitespace(last, t))
				{
					this.appendText(' ');
				}
			}
			t = (SQLToken)this.lexer.getNextToken(true, false);
		}
		return t;
	}
	
	private SQLToken processCreateTable(SQLToken previous)
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		SQLToken last = previous;
		int bracketCount = 0;
		StringBuffer definition = new StringBuffer(200);
		
		while (t != null)
		{
			if (t.getContents().equals("(") )
			{
				if (bracketCount == 0)
				{
					// start of table definition
					this.appendNewline();
					this.appendText('(');
					this.appendNewline();
				}
				else
				{
					definition.append('(');
				}
				bracketCount ++;
			}
			else if (t.getContents().equals(")"))
			{
				if (bracketCount == 1)
				{
					// end of table definition
					this.outputFormattedColumnDefs(definition);
					this.appendNewline();
					//this.appendText(')');
					//this.appendNewline();
					return t;
				}
				else
				{
					definition.append(')');
				}
				bracketCount--;
			}
			else if (bracketCount > 0)
			{
				// collect the table definition so that it's easier to format it 
				if (this.needsWhitespace(last, t, true))
				{
					definition.append(' ');
				}
				definition.append(t.getContents());
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
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
		return t;
	}
	
	private void outputFormattedColumnDefs(StringBuffer source)
	{
		ArrayList cols = new ArrayList();
		int size = source.length();
		int bracketCount = 0;
		int lastPos = 0;
		for (int i=0; i < size; i++)
		{
			char c = source.charAt(i);
			if (c == ',' && bracketCount == 0)
			{
				cols.add(source.substring(lastPos, i));
				lastPos = i + 1;
			}
			else if (c == '(')
			{
				bracketCount ++;
			}
			else if (c == ')')
			{
				bracketCount --;
			}
		}
		cols.add(source.substring(lastPos, size));
		
		// second pass, find the longest column name
		int count = cols.size();
		int width = 0;
		
		for (int i=0; i < count; i++)
		{
			String def = (String)cols.get(i);
			//System.out.println("def="+ def);
			int pos = def.indexOf(' ');
			if (pos > width) width = pos;
		}
		width += 2;
		
		// third pass, output the definitions aligned to the longest column name
		for (int i=0; i < count; i++)
		{
			String def = (String)cols.get(i);
			int pos = def.indexOf(' ');
			String col = def.substring(0, pos);
			String type = def.substring(pos + 1);
			this.indent("  ");
			this.appendText(col);
			while (pos < width)
			{
				this.appendText(' ');
				pos ++;
			}
			this.appendText(type);
			if (i < count - 1) 
			{
				this.appendText(',');
				this.appendNewline();
			}
		}
	}
	
	private SQLToken processCreateView(SQLToken last)
	{
		return last;
	}
	
	private SQLToken processCreateOther(SQLToken last)
	{
		return last;
	}
	
	public static void main(String[] args)
	{
		try
		{
//			String sql="Select count(*) \n" +
//           "FROM \n" +
//           "bv_user, \n" +
//           "bv_user_profile, \n" +
//           "bv_cow_uprof \n" +
//           "WHERE    bv_user.user_id = bv_user_profile.user_id \n" +
//           "AND    bv_user_profile.user_id = bv_cow_uprof.user_id \n" +
//           "AND    bv_cow_uprof.user_role = 1 \n" +
//           "AND    bv_cow_uprof.hp_internal_user = 'F' \n" +
//           "AND    bv_user.user_state = 0 \n" +
//           "AND    (bv_cow_uprof.tier <> 1 or bv_cow_uprof.tier is null) \n" +
//           "AND    ( \n" +
//           "        ( \n" +
//           "         (bv_user_profile.number_login = 0 \n" +
//           "        OR \n" +
//           "         bv_user_profile.number_login IS NULL) \n" +
//           "         AND \n" +
//           "         (bv_user.modif_time < add_months(sysdate, -3) \n" +
//           "         OR \n" +
//           "          bv_user.modif_time IS NULL) \n" +
//           "        ) \n" +
//           "    OR \n" +
//           "        (bv_user_profile.number_login > 0 \n" +
//           "         AND \n" +
//           "         (bv_user_profile.last_login_date is NULL \n" +
//           "         OR \n" +
//           "         bv_user_profile.last_login_date < add_months(sysdate, -6) \n" +
//           "         ) \n" +
//           "        ) \n" +
//           "    ) \n" +
//           "AND NOT EXISTS \n" +
//           "      (SELECT 1 \n" +
//           "       FROM cow_coop_funds_claims claim \n" +
//           "       WHERE claim.mind_location_id = bv_cow_uprof.mind_location_id \n" +
//           "      ) \n" +
//           "AND NOT EXISTS \n" +
//           "      (SELECT 1 \n" +
//           "       FROM cow_coop_funds_fs fs \n" +
//           "       WHERE fs.cust_mind_location_id = bv_cow_uprof.mind_location_id \n" +
//           "         OR \n" +
//           "         fs.user_id = bv_cow_uprof.user_id \n" +
//           "      ) \n" +
//           "AND NOT EXISTS \n" +
//           "      (SELECT 1 \n" +
//           "       FROM cow_coop_funds_pa pa \n" +
//           "       WHERE pa.mind_location_id = bv_cow_uprof.mind_location_id \n" +
//           "      ) \n" +
//           "AND NOT EXISTS \n" +
//           "      (SELECT 1 \n" +
//           "       FROM sellto_confirmation sto \n" +
//           "       WHERE sto.mind_company_id = bv_cow_uprof.mind_company_id \n" +
//           "      ) \n" +
//           "AND NOT EXISTS \n" +
//           "      (SELECT 1 \n" +
//           "       FROM sellout_confirmation sout \n" +
//           "       WHERE sout.mind_company_id = bv_cow_uprof.mind_company_id \n" +
//           "      ) \n" +
//           "AND    NOT EXISTS \n" +
//           "    (SELECT 1 \n" +
//           "     FROM     bv_cow_uprof bvc \n" +
//           "     WHERE    bv_cow_uprof.user_id <> bvc.user_id \n" +
//           "     AND    bv_cow_uprof.mind_location_id = bvc.mind_location_id \n" +
//           "     ) \n";

//					String sql = "SELECT      count(*)     FROM      COL_editorial     WHERE COL_EDITORIAL.oid NOT IN (select BV_CONTENT_REF.oid FROM BV_CONTENT_REF) and deleted='0'";
					
//					String sql="SELECT substr(to_char(s.pct, '99.00'), 2) || '%'  load, \n" + 
//           "       p.sql_text, \n" + 
//           "       p.piece, \n" + 
//           "       s.executions executes, \n" + 
//           "       s.ranking \n" + 
//           "FROM \n" + 
//           "    (  \n" + 
//           "      SELECT address, disk_reads, executions, pct, rank() over (ORDER BY disk_reads DESC)  ranking \n" + 
//           "      FROM \n" + 
//           "      ( \n" + 
//           "        SELECT address, disk_reads, executions, 100 * ratio_to_report(disk_reads) over ()  pct \n" + 
//           "        FROM sys.v_$sql  \n" + 
//           "        WHERE command_type != 47 \n" + 
//           "      )  \n" + 
//           "      WHERE disk_reads > 50 * executions \n" + 
//           "  )  s, \n" + 
//           "  sys.v_$sqltext  p \n" + 
//           "WHERE s.ranking <= 5  \n" + 
//           "AND   p.address = s.address \n" + 
//           "ORDER BY s.address, p.piece \n";

//			String sql="SELECT username, value || ' bytes' \"Current UGA memory\", stat.* \n" + 
//           "   FROM v$session sess, v$sesstat stat, v$statname name \n" + 
//           "WHERE sess.sid = stat.sid \n" + 
//           "   AND stat.statistic# = name.statistic# \n" + 
//           "   AND name.name = 'session uga memory' \n" + 
//           "   and username = 'BVUSER' \n";			
//				String sql="select bug_id, short_desc  \n" + 
//           "from bugs \n" + 
//           "where rep_platform in ('CONSULTANCY', 'ENHANCEMENT') \n" + 
//           "and bug_status not in ('CLOSED','RESOLVED') \n" + 
//           "and reporter = 47 \n";			
//				String sql = "SELECT * from v_$test";
//			String sql = "SELECT 1 FROM table WHERE (bv_user_profile.number_login=0 OR bv_user_profile.number_login IS NULL)";

//			String sql = "SELECT \n t1.col1, nvl(to_upper(bla,1),1), 'xxx'||col4, col3 " +
//			" \nfrom test_table t1, table2 t2\nWHERE t2.col=1 " +
//			" and col2 = 'f';\n";
//			String sql = "select * \nfrom (select * from person) AS t \nwhere nr2 = 2;";
//			String sql = "insert into test values ('x', 2);commit;";
//			String sql = "SELECT * from test, table22";
//			String sql = "select * \nfrom (select * from person) AS t \nwhere t.nr2 = 2;";
//			String sql = "select 1 from dual";
//			String sql="SELECT * \n" + 
//           "FROM (SELECT id, \n" + 
//           "             VALUE \n" + 
//           "      FROM userprops \n" + 
//           "      WHERE NAME= 'city' \n" + 
//           "      ) city \n";
			//String sql = "UPDATE bla set column1='test',col2=NULL, col4=222 where xyz=42 AND ab in (SELECT x from t\nWHERE x = 6) OR y = 5;commit;";
//			String sql="SELECT city.id, \n" + 
//           "       city.value, \n" + 
//           "       state.value \n" + 
//           "FROM (SELECT id, value FROM userprops WHERE NAME= 'city') city LEFT OUTER JOIN \n" + 
//           " (SELECT id, value FROM userprops WHERE NAME= 'state') state ON city.id = state.id  \n" + 
//           " -- the city\n";			
			String sql = "create table tk_test (nr integer, name varchar(100), price number(23,4))";
			SqlFormatter f = new SqlFormatter(sql,60);
			System.out.println(f.format());
//			System.out.println("----------------------------------");
//			"insert into test (col1, col2) values ('x', to_date(2,'XXXX'));commit;" 	 ;
//			f = new SqlFormatter(sql);
//			System.out.println(f.format());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
		}
	}

}
