/*
 * DelimiterDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import java.util.regex.Pattern;
import workbench.util.StringUtil;

/**
 * Encapsulate the alternate delimiter
 * @author Thomas Kellerer
 */
public class DelimiterDefinition
{
	/**
	 * The default delimiter for ANSI SQL: a semicolon
	 */
	public static final DelimiterDefinition STANDARD_DELIMITER = new DelimiterDefinition(";", false);
	
	/**
	 * A default alternate delimiter. This is Oracle's slash on a single line
	 */
	public static final DelimiterDefinition DEFAULT_ALTERNATE_DELIMITER = new DelimiterDefinition("/", true);

	/**
	 * A default alternate delimiter that matches SQL Server's GO command
	 */
	public static final DelimiterDefinition DEFAULT_MS_DELIMITER = new DelimiterDefinition("GO", true);
	
	private String delimiter;
	private boolean singleLineDelimiter;
	private boolean changed = false;

	public DelimiterDefinition()
	{
		this.delimiter = "";
		this.singleLineDelimiter = false;
		this.changed = false;
	}

	public DelimiterDefinition(String delim, boolean single)
	{
		setDelimiter(delim);
		this.singleLineDelimiter = single;
		this.changed = false;
	}

	public DelimiterDefinition createCopy()
	{
		DelimiterDefinition copy = new DelimiterDefinition(this.delimiter, singleLineDelimiter);
		copy.changed = false;
		return copy;
	}
	
	public boolean isEmpty()
	{
		return (this.delimiter == null || this.delimiter.trim().length() == 0);
	}
	
	public boolean isStandard()
	{
		return this.delimiter.equals(";");
	}

	public void resetChanged()
	{
		this.changed = false;
	}
	
	public static DelimiterDefinition parseCmdLineArgument(String arg)
	{
		if (StringUtil.isEmptyString(arg)) return null;

		String delim = null;
		final boolean single;
		int pos = arg.indexOf(':');
		if (pos == -1)
		{
			pos = arg.indexOf(';', 1);
		}

		if (pos > -1)
		{
			String type = arg.substring(pos + 1);
			single = "nl".equalsIgnoreCase(type);
			delim = arg.substring(0, pos);
		}
		else
		{
			delim = arg;
			single = false;
		}
		
		return new DelimiterDefinition(delim, single);
	}

	public String toString()
	{
		return delimiter;
	}
	
	public String getDelimiter() 
	{ 
		return this.delimiter; 
	}
	
	public void setDelimiter(String d) 
	{ 
		if (d == null) return;
		if (!StringUtil.equalString(this.delimiter, d)) 
		{
			this.delimiter = d.trim();
			this.changed = true;
		} 
	}
	
	public boolean isChanged()
	{
		return this.changed;
	}
	
	public boolean isSingleLine() 
	{ 
		return this.singleLineDelimiter; 
	}
	
	public void setSingleLine(boolean flag) 
	{ 
		if (flag != this.singleLineDelimiter)
		{
			this.singleLineDelimiter = flag; 
			this.changed = true;
		}
	}
	
	/**
	 * Return true if the given SQL script ends
	 * with this delimiter
	 * @param sql 
	 */
	public boolean terminatesScript(String sql)
	{
		if (StringUtil.isEmptyString(sql)) return false;
		if (this.isSingleLine())
		{
			Pattern p = Pattern.compile("(?i)[\\r\\n|\\n]+[ \t]*" + StringUtil.quoteRegexMeta(this.delimiter) + "[ \t]*[\\r\\n|\\n]*$");
			return p.matcher(sql.trim()).find();
		}
		else
		{
			return sql.trim().endsWith(this.delimiter);
		}
		
	}

	public boolean equals(String other)
	{
		return StringUtil.equalStringIgnoreCase(this.delimiter, other);
	}
	
	public boolean equals(Object other)
	{
		if (other == null) return false;
		
		if (other instanceof DelimiterDefinition)
		{
			DelimiterDefinition od = (DelimiterDefinition)other;
			if (this.singleLineDelimiter == od.singleLineDelimiter)
			{
				return StringUtil.equalStringIgnoreCase(this.delimiter, od.delimiter);
			}
			return false;
		}
		else if (other instanceof String)
		{
			return StringUtil.equalStringIgnoreCase(this.delimiter, (String)other);
		}
		return false;
	}
	
	public int hashCode()
	{
		return (this.delimiter + Boolean.toString(this.singleLineDelimiter)).hashCode();
	}
	
}
