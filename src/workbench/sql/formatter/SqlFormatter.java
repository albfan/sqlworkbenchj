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
import java.util.HashSet;
import java.util.Set;
import workbench.util.StringUtil;

/**
 *
 * @author  thomas
 */
public class SqlFormatter
{
	private static final Set LINE_BREAK_BEFORE = new HashSet(20);
	static
	{
		LINE_BREAK_BEFORE.add("SELECT");
		LINE_BREAK_BEFORE.add("FROM");
		LINE_BREAK_BEFORE.add("WHERE");
		LINE_BREAK_BEFORE.add("ORDER");
		LINE_BREAK_BEFORE.add("GROUP");
		LINE_BREAK_BEFORE.add("HAVING");
		LINE_BREAK_BEFORE.add("VALUES");
	}

	private static final Set LINE_BREAK_AFTER = new HashSet(20);
	static
	{
		LINE_BREAK_AFTER.add("UNION");
		LINE_BREAK_AFTER.add("MINUS");
		LINE_BREAK_AFTER.add("INTERSECT");
	}
	
	// keywords terminating a where clause
	private static final Set WHERE_TERMINAL = new HashSet(20);
	static
	{
		WHERE_TERMINAL.add("ORDER");
		WHERE_TERMINAL.add("GROUP");
		WHERE_TERMINAL.add("HAVING");
		WHERE_TERMINAL.add("UNION");
		WHERE_TERMINAL.add("INTERSECT");
		WHERE_TERMINAL.add("MINUS");
	}
	
	private String sql;
	private SQLLexer lexer;
	private StringBuffer result;
	private StringBuffer indent = null;
	
	public SqlFormatter(String aScript)
	{
		this(aScript, 0);
	}
	
	public SqlFormatter(String aScript, int indentCount)
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
	}
	
	public String format()
		throws Exception
	{
		this.formatSql();
		return this.result.toString();
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

	private void appendText(char c)
	{
		this.result.append(c);
	}
	
	private void appendText(String text)
	{
		this.result.append(text);
	}

	private void appendText(StringBuffer text)
	{
		this.result.append(text);
	}
	
	private SQLToken processList(int indentCount)
		throws Exception
	{
		StringBuffer b = new StringBuffer(indentCount);
		for (int i=0; i < indentCount; i++) b.append(' ');

		SQLToken t = (SQLToken)this.lexer.getNextToken();

		while (t != null)
		{
			String text = t.getContents();
			if (t.isReservedWord())
			{
				return t;
			}
			else if (t.isSeparator() && text.equals("("))
			{
				this.appendText(text);
				this.processFunctionCall();
			}
			else if (t.isSeparator() && text.equals(","))
			{
				this.appendText(",");
				this.appendNewline();
				this.appendText(b);
			}
			else
			{
				this.appendText(text);
			}
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
				this.appendText(")");
				return (SQLToken)this.lexer.getNextToken();
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
				this.appendText(b);
			}
			else if (!t.isWhiteSpace())
			{
				this.appendText(text);
			}
			t = (SQLToken)this.lexer.getNextToken();
		}
		return null;
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
			this.appendText(t.getContents());
			t = (SQLToken)this.lexer.getNextToken();
		}
	}

	private void formatSql()
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken();
		while (t != null)
		{
			if (t.isReservedWord())
			{
				String word = t.getContents().toUpperCase();
				if (LINE_BREAK_BEFORE.contains(word))
				{
					this.appendNewline();
					this.appendText(word);
				}
				else
				{
					this.appendText(t.getContents().toUpperCase());
				}
				
				if (LINE_BREAK_AFTER.contains(word))
				{
					this.appendNewline();
				}
				
				if (word.equals("SELECT"))
				{
					t = this.processList("SELECT".length());
					if (t == null) return;
					continue;
				}

				if (word.equals("FROM"))
				{
					t = this.processList("FROM".length());
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
				if (t.isSeparator() && t.getContents().equals("("))
				{
					this.appendText(t.getContents());
					this.processFunctionCall();
				}
				else 
				{
					this.appendText(t.getContents());
					if (t.isSeparator() && t.getContents().equals(";"))
					{
						this.appendNewline();
						this.appendNewline();
					}
				}
			}
			
			t = (SQLToken)this.lexer.getNextToken();
		}
	}

	private SQLToken processWhere(SQLToken previousToken)
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		SQLToken lastToken = previousToken;
		int bracketCount = 0;
		int lastIndent = 0;
		boolean subSelect = false;
		StringBuffer sub = null;
		this.appendText(" ");
		while (t != null)
		{
			if (bracketCount == 0)
			{
				String verb = t.getContents().toUpperCase();
				
				if (t.isReservedWord() && WHERE_TERMINAL.contains(verb))
				{
					return t;
				}
				if (t.isSeparator() && verb.equals(";"))
				{
					return t;
				}
				if (t.isReservedWord() && verb.equals("AND") || verb.equals("OR"))
				{
					this.appendNewline();
					this.appendText(verb);
					this.appendText("   ");
					if (verb.equals("OR")) this.appendText(' ');
				}
				else if (t.isSeparator() && t.getContents().equals("("))
				{
					bracketCount ++;
					if (bracketCount == 1 && 
					    lastToken.isReservedWord() && 
							lastToken.getContents().equalsIgnoreCase("IN"))
					{
						subSelect = true;
						sub = new StringBuffer();
						this.appendText("(");
						lastIndent = this.getCurrentLineLength();
					}
					else
					{
						this.appendText(" ");
						this.appendText(t.getContents());
					}
				}
				else
				{
					this.appendText(" ");
					this.appendText(t.getContents());
				}
			}
			else
			{
				if (t.isSeparator() && t.getContents().equals(")"))
				{
					bracketCount --;
				}
				if (bracketCount == 0 && subSelect)
				{
					//this.indentNewline(lastIndent);
					SqlFormatter f = new SqlFormatter(sub.toString(), lastIndent + 2);
					String s = f.format();
					this.appendText(" ");
					this.appendText(s);
					this.appendNewline();
					for (int k=0; k < lastIndent; k++)
					{
						this.appendText(' ');
					}
					this.appendText(")");
					subSelect = false;
					sub = null;
					lastIndent = 0;
				}
				else if (subSelect)
				{
					if (!t.isWhiteSpace())
					{
						sub.append(" ");
						sub.append(t.getContents());
					}
				}
				else
				{
					this.appendText(" ");
					this.appendText(t.getContents());
				}
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
			this.appendText(" ");
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
		SQLToken t = (SQLToken)this.lexer.getNextToken();
		while (t != null)
		{
			String text = t.getContents();
			if (t.isSeparator() && text.equals(")"))
			{
				bracketCount --;
			}
			this.appendText(t.getContents());
			if (bracketCount == 0) break;
			t = (SQLToken)this.lexer.getNextToken();
		}
	}
	
	public static void main(String[] args)
	{
		try
		{
//			String sql = "SELECT TOP col1, to_upper(bla,1), 'xxx', col3 " + 
//			" from test_table t1, table2 t2 WHERE col=1 " +
//			" and col2 = col3;";
			//String sql = "insert into test values ('x', 2);commit;";
			//String sql = "select * from (select * from person) AS t where nr2 = 2;";
			String sql = "select 1 from dual";
			SqlFormatter f = new SqlFormatter(sql);
			System.out.println(f.format());
//			System.out.println("----------------------------------");
//			sql = "UPDATE bla set column1='test' where xyz=42 AND ab in (SELECT x from t\nWHERE x = 6) OR y = 5;commit;" + 
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
