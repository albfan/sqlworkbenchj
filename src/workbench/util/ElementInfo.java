/*
 * ElementInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
		this.elementValue = StringUtil.isBlank(value) ? null : value;
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
