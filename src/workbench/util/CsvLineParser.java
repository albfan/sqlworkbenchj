/*
 * CsvLineParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 * A class to efficiently parse a delimited line of data. 
 * 
 * A quoted delimiter is recognized, line data spanning multiple lines (i.e.
 * data with embedded \n) is not recognized.
 *
 * @author  support@sql-workbench.net
 */
public class CsvLineParser
{
	private String lineData = null;
	private int len = 0;
	private int current = 0;
	private char delimiter;
	private char quoteChar = 0;
	private boolean returnEmptyStrings = false;
	private boolean trimValues = false;
	private boolean oneMore = false;
	private QuoteEscapeType escapeType = QuoteEscapeType.none;
	
	public CsvLineParser(char delimit)
	{
		this.delimiter = delimit;
	}
	
	public CsvLineParser(char delimit, char quote)
	{
		this.delimiter = delimit;
		this.quoteChar = quote;
	}
	
	public CsvLineParser(String line, char delimit, char quote)
	{
		this.setLine(line);
		this.delimiter = delimit;
		this.quoteChar = quote;
	}
	
	public void setLine(String line)
	{
		this.lineData = line;
		this.len = this.lineData.length();
		this.current = 0;
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
	
	public boolean hasNext()
	{
		return oneMore || current < len;
	}
	
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
			if (!inQuotes && (c == delimiter))
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
					if (next == '"') 
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
		
		String next = null;
		if (current - endOffset > beginField)
		{
			next = this.lineData.substring(beginField, current - endOffset);
		}
		
		this.current ++; // skip the delimiter
		if (current == len && lineData.charAt(current-1) == delimiter)
		{
			// if the line ends with the delimiter, we have one more
			// (empty) element
			oneMore = true;
		}
		
		if (this.escapeType == QuoteEscapeType.escape)
		{
			next = StringUtil.replace(next, "\\", "");
		}
		else if (this.escapeType == QuoteEscapeType.duplicate)
		{
			next = StringUtil.replace(next, "\"\"", "\"");
		}
		
		if (this.returnEmptyStrings && next == null) next = StringUtil.EMPTY_STRING;
		if (trimValues && next != null) return next.trim();
		else return next;
	}

	public void setTrimValues(boolean trimValues)
	{
		this.trimValues = trimValues;
	}

}
