/*
 * ScriptCommandDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
	private DelimiterDefinition delimiterUsed;
	private boolean delimiterNeeded;

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

	public String getSQL()
	{
		return this.command;
	}

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

	public boolean getDelimiterNeeded()
	{
		return delimiterNeeded;
	}

	public void setDelimiterNeeded(boolean flag)
	{
		this.delimiterNeeded = flag;
	}

	public DelimiterDefinition getDelimiterUsed()
	{
		return delimiterUsed;
	}

	public void setDelimiterUsed(DelimiterDefinition delimiter)
	{
		this.delimiterUsed = delimiter;
	}

	@Override
	public String toString()
	{
		if (command == null) return "[" + textStartPosInScript + "," + textEndPosInScript + "]";
		return this.command;
	}
}
