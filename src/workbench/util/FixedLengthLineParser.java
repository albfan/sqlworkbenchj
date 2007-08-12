/*
 * FixedLengthLineParser.java
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
 *
 * @author support@sql-workbench.net
 */
public class FixedLengthLineParser
	implements LineParser
{
	private int currentColIndex;
	private int[] widths;
	private String line;
	private int currentLineIndex;
  private boolean trimValues  = false;
	
	public FixedLengthLineParser(int[] colWidths)
	{
		if (colWidths == null)
		{
			throw new IllegalArgumentException("Column widths may not be null");
		}
		this.widths = colWidths;
	}

	public void setTrimValues(boolean trimValues)
	{
		this.trimValues = trimValues;
	}
	
	public void setLine(String newLine)
	{
		this.line = newLine;
		this.currentColIndex = 0;
		this.currentLineIndex = 0;
	}

	public boolean hasNext()
	{
		return currentColIndex < widths.length;
	}

	public String getNext()
	{
		if (!hasNext())
		{
			return null;
		}
		int end = currentLineIndex + widths[currentColIndex];
		if (end > line.length()) return null;
		String result = line.substring(currentLineIndex, end);
		currentLineIndex += widths[currentColIndex];
		currentColIndex++;
		if (trimValues) return result.trim();
		return result;
	}
}
