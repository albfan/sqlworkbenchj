/*
 * ScriptCommandDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

import workbench.util.StringUtil;

/**
 * @author  Thomas Kellerer
 */
public class ScriptCommandDefinition
{
	private final String command;
	private final int textStartPosInScript;
	private final int textEndPosInScript;

	private int whiteSpaceStart = -1;

	private int indexInScript;

	public ScriptCommandDefinition(String c, int start, int end)
	{
		this(c, start, end, -1);
	}

	public ScriptCommandDefinition(String c, int start, int end, int index)
	{
		this.command = StringUtil.rtrim(c);
		this.textStartPosInScript = start;
		this.textEndPosInScript = end;
		this.indexInScript = index;
	}

	public void setWhitespaceStart(int start)
	{
		if (start != this.textStartPosInScript)
		{
			this.whiteSpaceStart = start;
		}
	}

	public String getSQL() { return this.command; }

	/**
	 * Returns the start of this command in the source script
	 * including potential whitespace characters before the
	 * real command. If setWhitespaceStart() has not been
	 * called, this is identical to getStartPositionInScript()
	 */
	public int getWhitespaceStart()
	{
		if (whiteSpaceStart != -1) return whiteSpaceStart;
		return this.textStartPosInScript;
	}

	public int getStartPositionInScript()
	{
		return this.textStartPosInScript;
	}

	public int getEndPositionInScript()
	{
		return this.textEndPosInScript;
	}

	public int getIndexInScript()
	{
		return this.indexInScript;
	}

	public void setIndexInScript(int index)
	{
		this.indexInScript = index;
	}

	@Override
	public String toString()
	{
		if (command == null) return "[" + textStartPosInScript + "," + textEndPosInScript + "]";
		return this.command;
	}
}
