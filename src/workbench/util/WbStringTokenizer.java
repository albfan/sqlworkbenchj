/*
 * WbStringTokenizer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbStringTokenizer
{
	private String delimit;
	private boolean singleWordDelimiter;
	private String quoteChars;
	private boolean keepQuotes;
	private int maxDelim;
	private Reader input;
	private boolean endOfInput = false;
	private boolean delimNeedWhitespace = false;
	private boolean checkBrackets = false;
	
	public WbStringTokenizer()
	{
	}

	public WbStringTokenizer(String aSource, String delimiter)
	{
		this(delimiter, false, "\"", false);
		this.setSourceString(aSource);
	}

	public WbStringTokenizer(char aDelim, String quotingChars, boolean keep)
	{
		this.delimit = new String(new char[] { aDelim });
		this.singleWordDelimiter = false;
		this.quoteChars = quotingChars;
		this.keepQuotes = keep;
		this.endOfInput = true;
		this.maxDelim = this.delimit.length() - 1;		
	}

	/**
	 *	Create a new tokenizer.
	 *	If aDelim contains more then one character, the parameter isSingleDelimter indicates
	 *  whether the given delimiter string should be considered as one delimiter or a sequence
	 *  of possible delimiter characters.
	 *
	 *	Once the Tokenizer is created, the string to be tokenized can be set with
	 *	setSourceString()
	 *
	 */
	public WbStringTokenizer(String aDelim, boolean isSingleDelimiter, String quotingChars, boolean keep)
	{
		this.delimit = aDelim;
		this.singleWordDelimiter = isSingleDelimiter;
		this.quoteChars = quotingChars;
		this.keepQuotes = keep;
		this.endOfInput = true;
		this.maxDelim = this.delimit.length() - 1;		
	}
	
	public WbStringTokenizer(String input, String aDelim, boolean isSingleDelimiter, String quotingChars, boolean keep)
	{
		this.delimit = aDelim;
		this.singleWordDelimiter = isSingleDelimiter;
		this.quoteChars = quotingChars;
		this.keepQuotes = keep;
		this.maxDelim = this.delimit.length() - 1;		
		this.setSourceString(input);
	}

	public void setCheckBrackets(boolean flag) { this.checkBrackets = flag; }
	
	public void setDelimiter(String aDelimiter, boolean isSingleWord)
	{
		this.delimit = aDelimiter;
		this.singleWordDelimiter = isSingleWord;
	}

	public void setDelimiterNeedsWhitspace(boolean flag)
	{
		this.delimNeedWhitespace = flag;
	}
	
	public void setQuoteChars(String chars)
	{
		this.quoteChars = chars;
	}

	public void setKeepQuotes(boolean aFlag)
	{
		this.keepQuotes = aFlag;
	}

	public void setSourceFile(String aFilename)
		throws IOException, FileNotFoundException
	{
		BufferedReader reader = new BufferedReader(new FileReader(aFilename));
		this.setReader(reader);
	}

	public final void setSourceString(String aString)
	{
		StringReader reader = new StringReader(aString);
		this.setReader(reader);
	}

	public List<String> getAllTokens()
	{
		List<String> result = new LinkedList<String>();
		while (this.hasMoreTokens())
		{
			result.add(this.nextToken());
		}
		return result;
	}
	private void setReader(Reader aReader)
	{
		this.endOfInput = false;
		this.input = aReader;
	}

	public boolean hasMoreTokens()
	{
		return !this.endOfInput;
	}
	
	private static final char[] buf = new char[1];
	
	public String nextToken()
	{
		boolean inQuotes = false;
		StringBuilder current = null;
		String value = null;
		int delimIndex = 0;
		char lastQuote = 0;
		// if the input string directly starts with a delimiter
		// and delimNeedsWhitspace == true, setting lastToken to
		// a whitespace prevents returning the delimiter for the
		// first argument
		char lastToken = 9; 
		int bracketCount = 0;
		
		// the loop will be exited if a complete "word" is built
		// or the Reader is at the end of the file
		while (true)
		{
			try
			{
				// Reader.read() does not seem to throw an EOFException 
				// when using a StringReader, but the method with checking
				// the return value of read(char[]) seems to be reliable for 
				// a StringReader as well.
				int num = this.input.read(buf);
				this.endOfInput = (num == -1);
		
				// EOF detected
				if (endOfInput) 
				{
					if (current != null) return current.toString();
					else return null;
				}
				
				char token = buf[0];

				// Check for quote character
				if (quoteChars != null && quoteChars.indexOf(token) > -1)
				{
					if (inQuotes)
					{
						// Make sure it's the same quote character that started quoting
						if (token == lastQuote)
						{
							inQuotes = false;
							lastQuote = 0;
							if (keepQuotes) 
							{
								if (current == null) current = new StringBuilder();
								current.append(token);
							}
						}
						else
						{
							// quote character inside another quote character
							// we need to add it 
							if (current == null) current = new StringBuilder();
							current.append(token);
						}
					}
					else
					{
						// start quote mode
						lastQuote = token;
						inQuotes = true;
						if (keepQuotes) 
						{
							if (current == null) current = new StringBuilder();
							current.append(token);
						}
					}
					continue;
				}
				
				if (inQuotes)
				{
					// inside quotes, anything has to be added.
					if (current == null) current = new StringBuilder();
					current.append(token);
					continue;
				}
				
				if (this.checkBrackets) 
				{
					if (token == '(')
					{
						bracketCount ++;
					}
					else if (token == ')')
					{
						bracketCount --;
					}
					if (bracketCount > 0) 
					{
						if (current == null) current = new StringBuilder();
						current.append(token);
						continue;
					}
				}
					
				if (this.delimit.indexOf(token) > -1)
				{
					if (this.singleWordDelimiter)
					{
						if (token == this.delimit.charAt(delimIndex))
						{
							// advance the "pointer" until the end of the delimiter word
							if (delimIndex < maxDelim )
							{
								delimIndex ++;
								value = null;
							}
							else
							{
								delimIndex = 0;
								if (current == null) return "";
								value = current.toString();
								return value;
							}
						}
					}
					else
					{
						if (!delimNeedWhitespace || 
							   delimNeedWhitespace && Character.isWhitespace(lastToken) )
						{
							// found a new string to be split, return the current buffer
							if (current != null) 
							{
								value = current.toString();
								return value;
							}
						}
						else
						{
							if (current == null) current = new StringBuilder();
							current.append(token);
						}
					}
				}
				else
				{
					if (current == null) current = new StringBuilder();
					current.append(token);
				}
				lastToken = token;
			}
			catch (IOException e)
			{
				this.endOfInput = true;
				break;
			}
		}
		if (current == null) return null;
		return current.toString();
	}
	
}
