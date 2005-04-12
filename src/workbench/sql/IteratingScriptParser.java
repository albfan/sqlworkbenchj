/*
 * ScriptParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.interfaces.CharacterSequence;
import workbench.util.FileMappedSequence;
import workbench.util.SqlUtil;
import workbench.util.StringSequence;
import workbench.util.StringUtil;


/**
 *
 * @author  info@sql-workbench.net
 */
public class IteratingScriptParser
{
	private CharacterSequence script;
	private String delimiter = ";";
	private int delimiterLength = 1;
	private int scriptLength = -1;
	private int lastPos = 0;
	private int lastCommandEnd = -1;
	private boolean quoteOn = false;
	private boolean commentOn = false;
	private boolean blockComment = false;
	private boolean singleLineComment = false;
	private boolean startOfLine = true;
	private int lastNewLineStart = 0;
	private char lastQuote = 0;
	private boolean checkEscapedQuotes = true;

	/** Create an InteratingScriptParser
	 */
	public IteratingScriptParser()
	{
	}

	/**
	 *	Initialize a ScriptParser from a file.
	 *	The delimiter will be evaluated dynamically
	 */
	public IteratingScriptParser(File f)
		throws IOException
	{
		this.setFile(f);
	}
	

	/**
	 *	Create a ScriptParser for the given Script.
	 *	The delimiter to be used will be evaluated dynamically
	 */
	public IteratingScriptParser(String aScript)
		throws IOException
	{
		if (aScript == null) throw new IllegalArgumentException("Script may not be null");
		this.setScript(aScript);
	}

	public void setFile(File f)
		throws IOException
	{
		this.cleanup();
		this.script = new FileMappedSequence(f);
		this.scriptLength = (int)f.length();
		this.checkEscapedQuotes = false;
		this.reset();
	}

	private void cleanup()
	{
		if (this.script != null) this.script.done();
	}
	/**
	 *	Define the script to be parsed and the delimiter to be used.
	 *	If delim == null, it will be evaluated dynamically.
	 *	First the it will check if the script ends with the alternate delimiter
	 *	if this is not the case, the script will be checked if it ends with GO
	 *	If so, GO will be used (MS SQL Server script style)
	 *	If none of the above is true, ; (semicolon) will be used
	 */
	public void setScript(String aScript)
	{
		this.cleanup();
		this.script = new StringSequence(aScript);
		this.scriptLength = aScript.length();
		this.checkEscapedQuotes = false;
		this.reset();
	}
	
	private void reset()
	{
		lastCommandEnd = 0;
		lastPos = 0;
		quoteOn = false;
		commentOn = false;
		blockComment = false;
		singleLineComment = false;
		startOfLine = true;
		lastNewLineStart = 0;
		lastQuote = 0;
	}

	public void setDelimiter(String delim)
	{
		if (delim == null)
		{
			this.delimiter = ";";
			this.delimiterLength = 1;
		}
		else
		{
			this.delimiter = delim;
			this.delimiterLength = this.delimiter.length();
		}
	}

	public int getScriptLength()
	{
		return this.scriptLength;
	}
	
	public int findNextLineStart(int pos)
	{
		if (pos < 0) return pos;
		
		if (pos >= this.scriptLength) return pos;
		char c = this.script.charAt(pos);
		while (pos < this.scriptLength && (c == '\n' || c == '\r'))
		{
			pos ++;
			c = script.charAt(pos);
		}
		return pos;
	}

	public String getDelimiter()
	{
		return this.delimiter;
	}


	// These patterns cover the statements that
	// can be used in a single line without a delimiter
	// This is basically to make the parser as Oracle compatible as possible
	// while not breaking the SQL queries for other servers
	private static final Pattern[] SLC_PATTERNS =
         { Pattern.compile("(?m)^\\s*@.*$"),
					 Pattern.compile("(?mi)^\\s*SET\\s*\\w*\\s*((ON)|(OFF))\\s*;?\\s*$"),
					 Pattern.compile("(?mi)^\\s*ECHO\\s*((ON)|(OFF))\\s*;?\\s*$"),
					 Pattern.compile("(?mi)^\\s*SET\\s*TRANSACTION\\s*READ\\s*((WRITE)|(ONLY))\\s*;?\\s*$")
	       };

	public boolean hasMoreCommands()
	{
		return this.lastPos < this.scriptLength;
	}
	
	/**
	 *	Parse the given SQL Script into a List of single SQL statements.
	 *	Returns the index of the statement indicated by the currentCursorPos
	 */
	public ScriptCommandDefinition getNextCommand()
	{
		int pos;
		String currChar;
		for (pos = this.lastPos; pos < this.scriptLength; pos++)
		{
			currChar = this.script.substring(pos, pos + 1).toUpperCase();
			char firstChar = currChar.charAt(0);

			// ignore quotes in comments
			if (!commentOn && (firstChar == '\'' || firstChar == '"'))
			{
				if (!quoteOn)
				{
					lastQuote = firstChar;
					quoteOn = true;
				}
				else if (firstChar == lastQuote)
				{
					if (pos > 1)
					{
						// check if the current quote char was escaped
						if (!this.checkEscapedQuotes || this.script.charAt(pos - 1) != '\\')
						{
							lastQuote = 0;
							quoteOn = false;
						}
					}
					else
					{
						lastQuote = 0;
						quoteOn = false;
					}
				}
			}

			if (quoteOn) continue;

			// now check for comment start
			if (!quoteOn && pos < scriptLength - 1)
			{
				if (!commentOn)
				{
					char next = this.script.charAt(pos + 1);

					if (firstChar == '/' && next == '*')
					{
						blockComment = true;
						singleLineComment = false;
						commentOn = true;
						//pos ++; // ignore the next character
					}
					else if (startOfLine && (firstChar == '#' || (firstChar == '-' && next == '-')))
					{
						singleLineComment = true;
						blockComment = false;
						commentOn = true;
					}
				}
				else
				{
					if (singleLineComment)
					{
						if (firstChar == '\r' || firstChar == '\n')
						{
							singleLineComment = false;
							blockComment = false;
							commentOn = false;
							startOfLine = true;
							lastNewLineStart = pos;
							
							// don't include the comment in the next command
							lastPos = pos + 1;
							continue;
						}
					}
					else if (blockComment)
					{
						char last = this.script.charAt(pos - 1);
						if (firstChar == '/' && last == '*')
						{
							blockComment = false;
							singleLineComment = false;
							commentOn = false;
							continue;
						}
					}
				}
			}

 			if (!quoteOn && !commentOn)
			{
				if (this.delimiterLength > 1 && pos + this.delimiterLength < scriptLength)
				{
					currChar = this.script.substring(pos, pos + this.delimiterLength).toUpperCase();
				}

				if ((currChar.equals(this.delimiter) || (pos == scriptLength)))
				{
					if (lastPos >= pos) 
					{
						lastPos ++;
						continue;
					}
					startOfLine = true;
					this.lastNewLineStart = pos + 1;
					this.lastPos = pos + this.delimiterLength;
					int start = lastCommandEnd;
					this.lastCommandEnd = lastPos;
					ScriptCommandDefinition c = this.createCommand(start, pos);
					if (c == null) continue;
					return c;
				}
				else
				{
					// check for single line commands...
					if (firstChar == '\r' || firstChar == '\n' )
					{
						String line = this.script.substring(lastNewLineStart, pos).trim();
						boolean slcFound = false;
						
						int commandStart = lastNewLineStart;
						int commandEnd = pos;
						int newEndPos = lastNewLineStart + line.length();
						
						lastNewLineStart = pos;
						startOfLine = true;
						
						for (int pi=0; pi < SLC_PATTERNS.length; pi++)
						{
							String clean = SqlUtil.makeCleanSql(line, false, false, '\'');
							Matcher m = SLC_PATTERNS[pi].matcher(clean);
							
							if (m.matches())
							{
								slcFound = true;
								break;
							}
						}

						if (slcFound)
						{
							lastPos = pos;
							this.lastCommandEnd = commandEnd + 1;
							return createCommand(commandStart, commandEnd);
						}
						continue;
					}
					else
					{
						startOfLine = false;
					}
				}
			}
			
		} // end loop for next statement

		ScriptCommandDefinition c = null;
		if (lastPos < pos && !commentOn && !quoteOn)
		{
			String value = this.script.substring(lastPos, scriptLength).trim();
			int endpos = scriptLength;
			if (value.endsWith(this.delimiter))
			{
				endpos = endpos - this.delimiterLength;
			}
			c = createCommand(lastPos, endpos);
		}
		this.lastPos = scriptLength;
		return c;
	}

	private ScriptCommandDefinition createCommand(int startPos, int endPos)
	{
		String value = null;

		if (endPos == -1)
		{
			endPos = scriptLength;
		}
		
		value = this.script.substring(startPos, endPos).trim();
		if (value.length() == 0) return null;
		
		int offset = this.getRealStartOffset(value);
		if (offset > 0) value = value.substring(offset);

		//String clean = SqlUtil.makeCleanSql(value, false);
		//if (clean.equalsIgnoreCase(this.delimiter)) return null;
		
		ScriptCommandDefinition c = new ScriptCommandDefinition(value, startPos, endPos);
		
		return c;
	}

	/**
	 *	Check for the real beginning of the statement identified by
	 *	startPos/endPos. This method will return the actual start of the
	 *	command with leading comments trimmed
	 */
	private int getRealStartOffset(String sql)
	{
		int len = sql.length();
		int pos = 0;
		
		boolean inComment = false;
		boolean inQuotes = false;
		char last = 0;
		
		for (int i=0; i < len - 1; i++)
		{
			char c = sql.charAt(i);
			inQuotes = c == '\'';
			if (inQuotes) continue;
			//if (Character.isWhitespace(c)) continue;

			if ( c == '/' && sql.charAt(i+1) == '*')
			{
				inComment = true;
				// skip the start at the next position
				i++;
				last = '*';
				continue;
			}
			
			if (c == '-' && sql.charAt(i+1) == '-')
			{
				i+= 2;
				c = sql.charAt(i);
				// ignore rest of line for -- style comments
				while (c != '\n' && c != '\r' && i < len - 1)
				{
					i++;
					c = sql.charAt(i);
				}
				while (i < len && Character.isWhitespace(sql.charAt(i+1)))
				{
					i++;
				}
				continue;
			}			
			
			if (inComment && c == '*' && sql.charAt(i+1) == '/')
			{
				inComment = false;
				i += 2;
				while (i < len - 1 && Character.isWhitespace(sql.charAt(i)))
				{
					i++;
				}
			}
			
			if (!inComment)
			{
				pos = i;
				break;
			}
		}
		
		return pos;
	}

	public void setCheckEscapedQuotes(boolean flag)
	{
		this.checkEscapedQuotes = flag;
	}

	public void done()
	{
		this.script.done();
	}
	
	public static void main(String args[])
	{
		String sql = null;
		try
		{
//			sql = "@include.sql \n" +
//				     "delete from person; \n" +
//             "commit\n; \n" +
//             "set transaction read only \n" +
//						 "-- include file \n" +
//             "@c:/temp/test_insert.sql \n" +
//						 "set feedback off \n" +
//             " \n" +
//             "wbexport -type=text -file=\"d:/temp/test-1.txt\" -delimiter=, -header=true; \n" +
//             "select firstname, lastname, 'test-1' \n" +
//             "from person \n" +
//             "group by firstname, lastname\n;\n" +
//						 "UPDATE table \n SET column=value;\n" +
//						 "commit;";
					sql = "select * from all_tables; \n" + 
								"select * from user_tab_privs where grantee='MYUSER'; \n" + 
								"select * from dba_tab_privs;-- where grantee='MYUSER';";			
//					sql = "select * from dba_tab_privs;-- where grantee='MYUSER';";
//String sql = "select year(request_date) as year, month(request_date) as month, filename, count(*) \n" + 
//             "from wb_downloads \n" + 
//             "where filename like 'Workbench-Build%' \n" + 
//             "group by year(request_date), month(request_date), filename \n" + 
//             "order by 1 asc, 2 asc \n" + 
//             "; \n" + 
//             " \n" + 
//             "-- Dev build downloads \n" + 
//             "select year(request_date) year, month(request_date) month, count(*) downloads \n" + 
//             "from wb_downloads \n" + 
//             "where filename = 'Workbench.jar' \n" + 
//             "and http_status = '200' \n" + 
//             "and size > 550000 \n" + 
//             "--and year(request_date) = 2004 \n" + 
//             "and type <> 'WbUpdateCheck' \n" + 
//             "group by year(request_date), month(request_date) \n" + 
//             "order by 1 asc, 2 asc \n" + 
//             "; \n" + 
//             " \n" + 
//             "-- Source downloads \n" + 
//             "select year(request_date) year, month(request_date) month, count(*) downloads \n" + 
//             "from wb_downloads \n" + 
//             "where filename like 'WorkbenchSrc%' \n" + 
//             "and http_status = '200' \n" + 
//             "and size > 500000 \n" + 
//             "and type <> 'WbUpdateCheck' \n" + 
//             "group by year(request_date), month(request_date) \n" + 
//             "order by 1 asc, 2 asc, 3 asc \n" + 
//             ";";
//		 String sql = 
//			            "; \n" +
//				           "wbfeedback off; \n" + 
//									 "@c:/temp/2005-03-15_bv_cow_uprof.sql; \n" + 
//									 " \n" + 
//									 "select count(*)  \n" + 
//									 "from bv_cow_uprof \n" + 
//									 "; \n" + 
//									 " \n" + 
//									 "truncate table bv_cow_uprof \n" + 
//									 ";";
//String sql = "-- create the database \n" +
//	"-- another comment\n" +
//	"wbfeedback off;\n" +
//	"create table TempDiasMain ( \n" + 
//			 "   MediaObjKey    integer not null,    -- media object key (number) \n" + 
//			 "   AuthorAge      varchar(12),         -- Year + Day/100 or \"o.D.\" or errors \n" + 
//			 "   AuthorAgeInfo  varchar(10),         -- \"J\", \"M\", \"M-O\", \"He\", \"Fe\", \"So\", ... \n" + 
//			 "   PageFormat     varchar(5),          -- \"A4\", \"A5\", \"A6\", ... \n" + 
//			 "   CommentFlag    varchar(5),          -- \"K\" if there is a comment \n" + 
//			 "   RelMediaObjKey integer,             -- link to related media object \n" + 
//			 "   DateCreated    varchar(16),         -- dd-mm-yyyy (contains errors) \n" + 
//			 "      -- If day/month is unknown: --yyyy, if day is unknown: -mm-yyyy \n" + 
//			 "      -- Bemerkung von Dieter: Sollte später nicht mehr vorkommen. \n" + 
//			 "      --    Datierungen müssen immer vollständig sein. \n" + 
//			 "   Title          varchar(500), \n" + 
//			 "   Comment1       varchar(2000),       -- \"content\" comment \n" + 
//			 "   Comment2       varchar(2000),       -- not content-related comment \n" + 
//			 "   AuthorKey      integer,             -- may be 0 or Null \n" + 
//			 "   MediaFileType  varchar(5));         -- \"JPG\", \"WAV\", \"MOV\", ... \n" +
//	     "wbfeedback on;\n" + 
//			 "commit;\n";

//			String sql = "/* \n testing\n*/\nWBEXPORT -file=bla -type=text\n;\nSELECT bla from bla;\n";
			//String sql = "drop index idx_pa_date\n;\n\ncreate index idx_pa_date on partner_pro_pa (start_date, end_date)\n;\n\ncreate or replace view v_active_pa \nas\nSELECT PARTNER_PRO_ID,\n       PURCHASE_AGREE_NO,\n       START_DATE,\n       END_DATE,\n       STATUS\nFROM PARTNER_PRO_PA\nWHERE end_date >= sysdate\nAND   start_date < sysdate\n;\n\n\nselect distinct PURCHASE_AGREE_NO, cac_cd\nfrom PARTNER_PA_CAC\norder by 1\n;\n\nSELECT count(*)\nFROM rpl_user.partner_pro_pa;\n;\n\nselect * from v_active_pa\n;\n";
//			String sql = "SELECT count(*)\nFROM rpl_user.partner_pro_pa\n;\n;\nselect * from v_active_pa\n;\n";
			//File f = new File("d:/projects/jworkbench/testdata/statements.sql");
		  IteratingScriptParser p = new IteratingScriptParser(sql);
			ScriptCommandDefinition c  = p.getNextCommand();
			while (c != null)
			{
				System.out.println(c + "\n************************************************");
				c  = p.getNextCommand();
			}
			p.done();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}

		System.out.println("*** Done.");
	}

}
