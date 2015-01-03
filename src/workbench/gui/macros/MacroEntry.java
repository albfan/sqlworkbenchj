/*
 * MacroEntry.java
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
package workbench.gui.macros;

/**
 * One Macro entry in the GUI dialogs
 *
 * @author Thomas Kellerer
 */
public class MacroEntry
{
	private String text;
	private String name;

	public MacroEntry(String aName, String aText)
	{
		this.text = aText;
		this.name = aName;
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	public final String getName()
	{
		return this.name;
	}

	public final void setName(String aName)
	{
		this.name = aName;
	}

	public final String getText()
	{
		return this.text;
	}

	public final void setText(String aText)
	{
		this.text = aText;
	}

}
