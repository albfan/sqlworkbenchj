/*
 * CsvLineParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.util;

import workbench.db.importer.TextFileParser;

/**
 * A class to efficiently parse a delimited line of data.
 *
 * A quoted delimiter is recognized, line data spanning multiple lines (i.e.
 * data with embedded \n) is not recognized.
 *
 * @author  Thomas Kellerer
 */
public class CsvLineParser
	implements LineParser
{
	private String lineData = null;
	private int len = 0;
	private int current = 0;
	private String delimiter = TextFileParser.DEFAULT_DELIMITER;
	private int delimiterLength;
	private char quoteChar = 0;
	private boolean returnEmptyStrings = false;
	private boolean trimValues = false;
	private boolean oneMore = false;
	private QuoteEscapeType escapeType = QuoteEscapeType.none;
	private boolean unquotedEmptyIsNull = false;

	public CsvLineParser(char delimit)
	{
		delimiter = String.valueOf(delimit);
		delimiterLength = 1;
	}

	public CsvLineParser(char delimit, char quote)
	{
		delimiter = String.valueOf(delimit);
		delimiterLength = 1;
		quoteChar = quote;
	}

	public CsvLineParser(String delimit)
	{
		delimiter = delimit;
		delimiterLength = delimiter.length();
	}

	public CsvLineParser(String delimit, char quote)
	{
    if (delimit != null)
    {
      delimiter = delimit;
      delimiterLength = delimiter.length();
    }
		quoteChar = quote;
	}

  public QuoteEscapeType getEscapeType()
  {
    return escapeType;
  }

  public char getQuoteChar()
  {
    return quoteChar;
  }

	@Override
	public void setLine(String line)
	{
		this.lineData = line;
		this.len = this.lineData.length();
		this.current = 0;
	}


	/**
	 * Controls if an unquoted empty string is treated as a null value
	 * or an empty string.
	 *
	 * If this is set to true, returnEmptyStrings is set to false as well.
	 *
	 * @param flag
	 * @see #setReturnEmptyStrings(boolean)
	 */
	public void setUnquotedEmptyStringIsNull(boolean flag)
	{
		unquotedEmptyIsNull = flag;
		if (flag) returnEmptyStrings = false;
	}

	private boolean isDelimiter(char current, int currentIndex)
	{
		if (delimiterLength == 1)
		{
			return (current == delimiter.charAt(0));
		}

		if (current == delimiter.charAt(0))
		{
			for (int i=1; i < delimiterLength; i++)
			{
				if (i + currentIndex < this.lineData.length())
				{
					if (delimiter.charAt(i) != lineData.charAt(currentIndex + 1))
					{
						return false;
					}
				}
				else
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Controls how empty strings are returned. If this is set to
	 * true, than an empty element is returned as an empty string
	 * otherwise an empty element is returend as null
	 */
	public void setReturnEmptyStrings(boolean flag)
	{
		this.returnEmptyStrings = flag;
	}

	public void setQuoteEscaping(QuoteEscapeType type)
	{
		this.escapeType = type;
	}

	@Override
	public boolean hasNext()
	{
		return oneMore || current < len;
	}

	@Override
	public String getNext()
	{
		// The line ends with the delimiter
		// so we have to return an empty string
		if (oneMore)
		{
			oneMore = false;
			if (returnEmptyStrings) return "";
			else return null;
		}

		int beginField = current;
		boolean inQuotes = false;
		int endOffset = 0;
		while (current < len)
		{
			char c = this.lineData.charAt(current);
			if (!inQuotes && (isDelimiter(c, current)))
			{
				break;
			}

			if (c == this.quoteChar)
			{
				// don't return the quote at the end
				if (inQuotes) endOffset = 1;

				// don't return the quote at the beginning
				if (current == beginField) beginField ++;

				if (this.escapeType == QuoteEscapeType.escape)
				{
					char last = 0;
					if (current > 1) last = this.lineData.charAt(current - 1);
					if (last != '\\')
					{
						inQuotes = !inQuotes;
					}
				}
				else if (this.escapeType == QuoteEscapeType.duplicate)
				{
					char next = 0;
					if (current < lineData.length() - 1) next = this.lineData.charAt(current + 1);
					if (next == quoteChar)
					{
						current ++;
					}
					else
					{
						inQuotes = !inQuotes;
					}
				}
				else
				{
					inQuotes = !inQuotes;
				}
			}
			current ++;
		}

		boolean hadQuotes = endOffset > 0;

		String next = null;
		if (current - endOffset > beginField)
		{
			next = this.lineData.substring(beginField, current - endOffset);
		}

		this.current += delimiterLength; // skip the delimiter
		if (current == len && isDelimiter(lineData.charAt(current-delimiterLength), current - delimiterLength))
		{
			// if the line ends with the delimiter, we have one more
			// (empty) element
			oneMore = true;
		}

		if (next != null)
		{
			if (this.escapeType == QuoteEscapeType.escape)
			{
				String quoteString = new String(new char[] { quoteChar } );
				next = StringUtil.replace(next, "\\" + quoteChar, quoteString);
			}
			else if (this.escapeType == QuoteEscapeType.duplicate)
			{
				String two = new String(new char[] { quoteChar, quoteChar} );
				String one = new String(new char[] { quoteChar} );
				next = StringUtil.replace(next, two, one);
			}
		}

		if (hadQuotes && unquotedEmptyIsNull && next == null) return StringUtil.EMPTY_STRING;
		if (this.returnEmptyStrings && next == null) next = StringUtil.EMPTY_STRING;
		if (trimValues && next != null) return next.trim();
		else return next;
	}

	@Override
	public void setTrimValues(boolean trimValues)
	{
		this.trimValues = trimValues;
	}

}
