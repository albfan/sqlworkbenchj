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
		LINE_BREAK_AFTER.add("UNION");
		LINE_BREAK_AFTER.add("MINUS");
		LINE_BREAK_AFTER.add("INTERSECT");
		LINE_BREAK_AFTER.add("AS");
		LINE_BREAK_AFTER.add("FOR");
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


	private boolean lastCharIsWhitespace()
	{
		int len = this.result.length();
		if (len == 0) return false;
		char c = this.result.charAt(len -1);
		return Character.isWhitespace(c);
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

	private void appendNonSeparator(String text)
	{
		if (!text.startsWith(" ") && !lastCharIsWhitespace()) this.result.append(' ');
		this.result.append(text);
	}

	private boolean needsWhitespace(SQLToken last, SQLToken current)
	{
		if (this.isStartOfLine()) return false;
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
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
		SQLToken lastToken = t;
		while (t != null)
		{
			String text = t.getContents();

			if (t.isReservedWord() && FROM_TERMINAL.contains(text.toUpperCase()))
			{
				return t;
			}
			else if (lastToken.isSeparator() && lastToken.getContents().equals("(") && text.equalsIgnoreCase("SELECT") )
			{
				t = this.processSubSelect(true);
				continue;
			}

			if (t.isSeparator() && text.equals("("))
			{
				if (!lastToken.isSeparator() || lastToken == t) this.appendText(' ');
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
				this.appendText(b);
			}
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
				this.appendText(b);
			}
			else if (text.equals("*"))
			{
				this.appendText(" *");
			}
			else if (t.isOperator() ||t.isSeparator())
			{
				this.appendText(text);
			}
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

	private SQLToken processSubSelect(boolean selectMissing)
		throws Exception
	{
		SQLToken t = (SQLToken)this.lexer.getNextToken();
		int bracketCount = 1;
		StringBuffer subSql = new StringBuffer(250);

		if (selectMissing)
		{
			subSql.append("SELECT ");
		}

		int lastIndent = this.getCurrentLineLength();
		this.appendNewline();

		for (int k=0; k < lastIndent + 1; k++)
		{
			this.appendText(' ');
		}

		while (t != null)
		{
			String text = t.getContents();
			if (t.isSeparator() && text.equals(")"))
			{
				bracketCount --;

				if (bracketCount == 0)
				{
					SqlFormatter f = new SqlFormatter(subSql.toString(), lastIndent + 1);
					String s = f.format();
					this.appendText(s);
					this.appendNewline();
					for (int k=0; k < lastIndent; k++)
					{
						this.appendText(' ');
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
				this.appendText(b);
			}
			else if (!t.isWhiteSpace())
			{
				this.appendText(text);
			}
			t = (SQLToken)this.lexer.getNextToken(false, false);
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
		SQLToken t = (SQLToken)this.lexer.getNextToken(false, false);
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

				if (t.isSeparator() && t.getContents().equals("("))
				{
					this.appendText(" (");
					this.processFunctionCall();
				}
				else
				{
					if (t.isSeparator() && t.getContents().equals(";"))
					{
						this.appendText(t.getContents());
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
			this.appendText(t.getContents());
			if (bracketCount == 0)
			{
				this.appendText(' ');
				break;
			}
			t = (SQLToken)this.lexer.getNextToken(false, false);
		}
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

//			String sql = "SELECT 1 FROM table WHERE (bv_user_profile.number_login=0 OR bv_user_profile.number_login IS NULL)";

			String sql = "SELECT \n t1.col1, nvl(to_upper(bla,1),1), 'xxx'||col4, col3 " +
			" \nfrom test_table t1, table2 t2\nWHERE t2.col=1 " +
			" and col2 = 'f';\n";
//			String sql = "select * \nfrom (select * from person) AS t \nwhere nr2 = 2;";
//			String sql = "insert into test values ('x', 2);commit;";
//			String sql = "SELECT * from test, table22";
//			String sql = "select * \nfrom (select * from person) AS t \nwhere t.nr2 = 2;";
//			String sql = "select 1 from dual";
//			String sql = "UPDATE bla set column1='test',col2=NULL, col4=222 where xyz=42 AND ab in (SELECT x from t\nWHERE x = 6) OR y = 5;commit;";
			SqlFormatter f = new SqlFormatter(sql);
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
