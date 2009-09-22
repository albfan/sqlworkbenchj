/*
 * LexerBasedParser
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Pattern;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class LexerBasedParser
	implements ScriptIterator
{
	private File originalFile;
	private String fileEncoding;
	private SQLLexer lexer;
	private Reader input;
	private DelimiterDefinition delimiter = DelimiterDefinition.STANDARD_DELIMITER;
	private int lastStart = -1;
	private int currentStatementIndex;
	private boolean storeStatementText = true;
	private boolean returnLeadingWhitespace;
	private boolean emptyLineIsDelimiter;
	private int scriptLength;
	private boolean hasMoreCommands;
	private boolean checkOracleInclude;
	private boolean calledOnce;
	
	private static Pattern MULTI_LINE_PATTERN = Pattern.compile("(\r\n|\n\r|\r|\n)+[ \t\f]*(\r\n|\n\r|\r|\n)+");

	public LexerBasedParser()
	{
	}
	
	public LexerBasedParser(String script)
		throws IOException
	{
		setScript(script);
	}

	public LexerBasedParser(File f, String encoding)
		throws IOException
	{
		setFile(f, encoding);
	}

	public void setDelimiter(DelimiterDefinition def)
	{
		delimiter = def.createCopy();
	}

	public void setEmptyLineIsDelimiter(boolean flag)
	{
		emptyLineIsDelimiter = flag;
	}
	
	/**
	 * Controls if the actual SQL for each command returned by
	 * #getNextCommand() is stored in the ScriptCommandDefinition
	 * or if only start and end in the script should be stored. 
	 * 
	 * @param flag if true, the actual SQL is returned otherwise only the start and end
	 */
	public void setStoreStatementText(boolean flag)
	{
		storeStatementText = flag;
	}

	public void setReturnLeadingWhitespace(boolean flag)
	{
		returnLeadingWhitespace = flag;
	}
	
	public void done()
	{
		FileUtil.closeQuitely(input);
	}

	public ScriptCommandDefinition getNextCommand()
	{
		calledOnce = true;
		
		String delimiterString = delimiter.getDelimiter();
		try
		{
			// The
			StringBuilder sql = new StringBuilder(150);

			int previousEnd = -1;
			
			SQLToken token = lexer.getNextToken();
			boolean startOfLine = false;
			boolean singleLineCommand = false;

			while (token != null)
			{
				if (lastStart == -1) lastStart = token.getCharBegin();
				String text = token.getText();

				boolean checkForDelimiter = !delimiter.isSingleLine() || (delimiter.isSingleLine() && startOfLine);

				if (startOfLine && !singleLineCommand && checkOracleInclude && text.charAt(0) == '@')
				{
					singleLineCommand = true;
				}

				if (startOfLine && !token.isWhiteSpace())
				{
					startOfLine = false;
				}

				if (checkForDelimiter && delimiterString.equals(text))
				{
					break;
				}
				else if (checkForDelimiter && delimiterString.startsWith(text))
				{
					StringBuilder delim = new StringBuilder(delimiter.getDelimiter().length());
					delim.append(text);
					StringBuilder skippedText = null;
					if (storeStatementText)
					{
						skippedText = new StringBuilder();
						skippedText.append(text);
					}

					while ((token = lexer.getNextToken()) != null)
					{
						if (storeStatementText) skippedText.append(token.getText());
						if (token.isComment() || token.isWhiteSpace() || token.isLiteral()) break;
						delim.append(token.getText());
						if (delim.length() > delimiterString.length()) break;
						if (!delimiterString.startsWith(delim.toString())) break;
					}
					boolean delimiterMatched = delimiterString.equals(delim.toString());
					if (delimiterMatched)
					{
						break;
					}
					if (storeStatementText)
					{
						text += skippedText.toString();
					}
				}
				else if (text.charAt(0) == '\n' || text.charAt(0) == '\r')
				{
					if (singleLineCommand || (emptyLineIsDelimiter && isMultiLine(text)))
					{
						break;
					}
					startOfLine = true;
					singleLineCommand = false;
				}
				previousEnd = token.getCharEnd();
				token = lexer.getNextToken();
				
				sql.append(text);
			}
			if (previousEnd > 0)
			{
				ScriptCommandDefinition cmd = createCommandDef(sql, lastStart, previousEnd);
				cmd.setIndexInScript(currentStatementIndex);
				currentStatementIndex ++;
				lastStart = -1;
				hasMoreCommands = true;
				return cmd;
			}
			hasMoreCommands = false;
			return null;
		}
		catch (IOException e)
		{
			LogMgr.logError("LexerBasedParser.getNextCommand()", "Error parsing script", e);
			hasMoreCommands = false;
			return null;
		}
	}

	private ScriptCommandDefinition createCommandDef(StringBuilder sql, int start, int end)
	{
		if (returnLeadingWhitespace || !Character.isWhitespace(sql.charAt(0)) || sql.length() == 0)
		{
			String toStore = storeStatementText ? sql.toString() : null;
			return new ScriptCommandDefinition(toStore, start, end);
		}

		int i = 0;
		while (i < sql.length() && Character.isWhitespace(sql.charAt(i)))
		{
			i ++;
		}
		return new ScriptCommandDefinition(sql.substring(i), start + i, end);
	}

	private boolean isMultiLine(String text)
	{
		return MULTI_LINE_PATTERN.matcher(text).lookingAt();
	}

	@Override
	public int getScriptLength()
	{
		return scriptLength;
	}

	@Override
	public boolean hasMoreCommands()
	{
		return hasMoreCommands;
	}

	@Override
	public void setCheckForSingleLineCommands(boolean flag)
	{
	}

	@Override
	public void setAlternateLineComment(String comment)
	{
	}

	@Override
	public void setCheckEscapedQuotes(boolean flag)
	{
	}

	@Override
	public void setSupportOracleInclude(boolean flag)
	{
		checkOracleInclude = flag;
	}

	@Override
	public void setFile(File f, String encoding)
		throws IOException
	{
		scriptLength = (int)FileUtil.getCharacterLength(f, encoding);
		input = EncodingUtil.createBufferedReader(f, encoding);
		lexer = new SQLLexer(input);
		calledOnce = false;
		hasMoreCommands = (scriptLength > 0);
	}

	@Override
	public void setReturnStartingWhitespace(boolean flag)
	{
		returnLeadingWhitespace = flag;
	}

	@Override
	public void setScript(String script)
	{
		input = new StringReader(StringUtil.rtrim(script));
		lexer = new SQLLexer(input);
		scriptLength = script.length();
		calledOnce = false;
		hasMoreCommands = (scriptLength > 0);
	}

	@Override
	public void reset()
	{
		if (!calledOnce) return;
		
		try
		{
			if (originalFile != null)
			{
				FileUtil.closeQuitely(input);
				input = EncodingUtil.createBufferedReader(originalFile, fileEncoding);
			}
			else
			{
				input.reset();
			}
		}
		catch (IOException io2)
		{
			LogMgr.logError("LexerBasedParser.reset()", "Cannot re-open input stream", io2);
		}
	}
}
