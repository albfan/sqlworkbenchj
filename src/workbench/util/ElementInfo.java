/*
 *  ElementInfo.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

/**
 *
 * @author Thomas Kellerer
 */
public class ElementInfo
{
	private String elementValue;
	private int startInStatement;
	private int endInStatement;

	public ElementInfo(String value, int startPos, int endPos)
	{
		this.elementValue = value;
		this.startInStatement = startPos;
		this.endInStatement = endPos;
	}

	public String getElementValue()
	{
		return elementValue;
	}

	public int getEndPosition()
	{
		return endInStatement;
	}

	public int getStartPosition()
	{
		return startInStatement;
	}

	public void setOffset(int offset)
	{
		startInStatement += offset;
		endInStatement += offset;
	}

}
