/*
 * ScriptCommandDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;

/**
 * @author  support@sql-workbench.net
 */
public class ScriptCommandDefinition
{
	private final String command;
	private final int startPosInScript;
	private final int endPosInScript;
	private int indexInScript;
	
	public ScriptCommandDefinition(String c, int start, int end)
	{
		this(c, start, end, -1);
	}
	
	public ScriptCommandDefinition(String c, int start, int end, int index)
	{
		this.command = c;
		this.startPosInScript = start;
		this.endPosInScript = end;
		this.indexInScript = index;
	}
	
	public String getSQL() { return this.command; }
	public int getStartPositionInScript() { return this.startPosInScript; }
	public int getEndPositionInScript() { return this.endPosInScript; }
	public int getIndexInScript() { return this.indexInScript; }
	public void setIndexInScript(int index) { this.indexInScript = index; }
	
	public String toString() { return this.command; }
}
