/*
 * LineEnding.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.util;

import workbench.resource.ResourceMgr;

/**
 * @author  info@sql-workbench.net
 */
public class LineEnding
{
	public static final LineEnding UNIX = new LineEnding("\n");
	public static final LineEnding WIN = new LineEnding("\r\n");
	public static final LineEnding DEFAULT = new LineEnding(null);
	
	private final String lineEnd;
	private boolean isDefault = false;
	private LineEnding(String ending)
	{
		isDefault = (ending == null);
		if (isDefault)
		{
			lineEnd = StringUtil.LINE_TERMINATOR;
		}
		else
		{
			lineEnd = ending;
		}
	}
	
	public String toString()
	{
		if (isDefault)
			return ResourceMgr.getString("LabelPlatformDefaultLineEnding");
		if ("\n".equals(lineEnd)) 
			return ResourceMgr.getString("LabelUnixLineEnding");
		else 
			return ResourceMgr.getString("LabelWindLineEnding");
	}
	
	public String getLineEnding()
	{
		return lineEnd;
	}
	
}
