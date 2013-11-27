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
public class ErrorDescriptor
{
	private int errorPosition = -1;
	private int errorLine = -1;
	private int errorColumn = -1;
	private DdlObjectInfo object;
	private String errorMessage;

	public ErrorDescriptor()
	{
	}

	/**
	 * Sets the error position as a column/line combination (relative to the start of the SQL statement).
	 *
	 * @param line     the line as reported by the DBMS
	 * @param column   the column as reported by the DBMS
	 */
	public void setErrorPosition(int line, int column)
	{
		this.errorLine = line;
		this.errorColumn = column;
	}

	public int getErrorLine()
	{
		return errorLine;
	}

	public int getErrorColumn()
	{
		return errorColumn;
	}

	public int getErrorPosition()
	{
		return errorPosition;
	}

	/**
	 * Sets the position of the error as an offset to the start of the SQL statement.
	 *
	 * @param position  the position inside the statement
	 * @see
	 */
	public void setErrorOffset(int position)
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

	public DdlObjectInfo getObjectInfo()
	{
		return object;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public boolean hasError()
	{
		return errorPosition > -1 || (errorLine > -1 && errorColumn > -1);
	}

	@Override
	public String toString()
	{
		String msg = errorMessage != null ? errorMessage : "";
		if (errorPosition > -1)
		{
			return msg + "\n\nat position " + errorPosition;
		}
		else if (errorLine > -1 && errorColumn > -1)
		{
			return msg + "\n\nat line: " + errorLine + ", column: " + errorColumn;
		}
		return "no error";
	}

}
