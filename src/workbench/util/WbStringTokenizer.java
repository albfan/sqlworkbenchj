/*
 * Created on December 14, 2002, 10:11 PM
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbStringTokenizer
{
	private String delimit;
	private boolean singleWordDelimiter;
	private String quoteChars;
	private boolean keepQuotes;
	private StreamTokenizer tokenizer;
	private int lastToken;
	private int maxDelim;

	public WbStringTokenizer()
	{
	}

	public WbStringTokenizer(String aSource, String delimiter)
	{
		this(delimiter, false, "\"", false);
		this.setSourceString(aSource);
	}

	public WbStringTokenizer(char aDelim, String quoteChars, boolean keepQuotes)
	{
		this.delimit = new String(new char[] { aDelim });
		this.singleWordDelimiter = false;
		this.quoteChars = quoteChars;
		this.keepQuotes = keepQuotes;
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
	public WbStringTokenizer(String aDelim, boolean isSingleDelimiter, String quoteChars, boolean keepQuotes)
	{
		this.delimit = aDelim;
		this.singleWordDelimiter = isSingleDelimiter;
		this.quoteChars = quoteChars;
		this.keepQuotes = keepQuotes;
		this.maxDelim = this.delimit.length() - 1;		
	}

	public void setDelimiter(String aDelimiter, boolean isSingleWord)
	{
		this.delimit = aDelimiter;
		this.singleWordDelimiter = isSingleWord;
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
		this.initTokenizer(reader);
	}

	public void setSourceString(String aString)
	{
		StringReader reader = new StringReader(aString);
		this.initTokenizer(reader);
	}

	private void initTokenizer(Reader aReader)
	{
		this.lastToken = 0;
		this.tokenizer = new StreamTokenizer(aReader);
		this.tokenizer.resetSyntax();
		this.tokenizer.wordChars(32,255);
		
		for (int i=0; i< this.delimit.length(); i++)
		{
			this.tokenizer.ordinaryChar(this.delimit.charAt(i));
		}
		for (int i=0; i< this.quoteChars.length(); i++)
		{
			this.tokenizer.quoteChar(this.quoteChars.charAt(i));
		}
		this.tokenizer.eolIsSignificant(false);
	}

	public boolean hasMoreTokens()
	{
		return this.lastToken != StreamTokenizer.TT_EOF;
	}

	public String nextToken()
	{
		int token;
		String value = null;
		StringBuffer next = null;
		boolean inDelimit = false;
		int delimIndex = 0;
		StringBuffer current = null;
		boolean tokenFound = false;
		String result = null;

		try
		{
			while (true)
			{
				token = this.tokenizer.nextToken();
				this.lastToken = token;

				switch (token)
				{
					case StreamTokenizer.TT_EOF:
						if (current != null)
							return current.toString();
						else
							return null;
					case StreamTokenizer.TT_WORD:
						if (current == null) current = new StringBuffer();
						current.append(this.tokenizer.sval);
						break;
					default:
						if (quoteChars.indexOf(token) > -1)
						{
							if (current == null) current = new StringBuffer();
							if (keepQuotes) current.append((char)token);
							current.append(this.tokenizer.sval);
							if (keepQuotes) current.append((char)token);
						}
						else if (this.delimit.indexOf((char)token) > -1)
						{
							if (this.singleWordDelimiter)
							{
								if (token == this.delimit.charAt(delimIndex))
								{
									if (delimIndex < maxDelim )
									{
										delimIndex ++;
										inDelimit = true;
										value = null;
									}
									else
									{
										delimIndex = 0;
										inDelimit = false;
										value = current.toString();
										return value;
									}
								}
							}
							else
							{
								if (current != null) value = current.toString();
								return value;
							}
							break;
						}
						else
						{
							if (current == null) current = new StringBuffer();
							current.append(token);
						}
						break;
				}
			}
		}
		catch (IOException e)
		{
			return null;
		}

	}

	public static void main(String[] args)
	{
		String test = "spool -t type \t-f \"file name.sql\" -b tablename;select * from test where name='test';";
		//split(test, " \t", false, "\"'", true);
		//System.out.println("---");
		//test = "spool -t type \t-f \"file name.sql\" -b tablename./select * from test where name='test'./";
		//test = "spool /type=text /file=\"file name.sql\" /table=tablename";
		try
		{
			WbStringTokenizer tok = new WbStringTokenizer("FROM",true, "\"'", true);
			tok.setSourceString("SELECT 'from', col1, col2 FROM test WHERE x=u");
			System.out.println(tok.nextToken());
			System.out.println(tok.nextToken());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("done.");
	}
}
