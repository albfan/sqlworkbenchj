/*
 * StatementError.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

import workbench.util.DdlObjectInfo;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementError
{
	private int errorPosition;
	private int errorLine;
	private int positionInLine;
	private final Exception cause;
	private DdlObjectInfo object;

	public StatementError(Exception error)
	{
		this.cause = error;
	}

	public void setErrorPositionInLine(int line, int positionInLine)
	{
		this.errorLine = line;
		this.positionInLine = positionInLine;
	}

	public int getErrorLine()
	{
		return errorLine;
	}

	public int getPositionInLine()
	{
		return positionInLine;
	}

	public int getErrorPosition()
	{
		return errorPosition;
	}

	public void setErrorPosition(int position)
	{
		this.errorPosition = position;
	}

	public void setObjectInfo(DdlObjectInfo info)
	{
		if (info != null && info.isValid())
		{
			object = info;
		}
		else
		{
			object = null;
		}
	}

	public Exception getErrorCause()
	{
		return cause;
	}

	public DdlObjectInfo getObjectInfo()
	{
		return object;
	}
}
