/*
 * ActionDisplay.java
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
package workbench.gui.settings;

/**
 * A wrapper class to display an Action for the {@link ShortcutEditor}
 * It simply holds a text and a tooltip
 *
 * @see ActionDisplayRenderer
 * @see ShortcutEditor
 * @author Thomas Kellerer
 */
public class ActionDisplay
	implements Comparable
{

	public String text;
	public String tooltip;

	public ActionDisplay(String txt, String tip)
	{
		text = txt;
		tooltip = tip;
	}

	@Override
	public int compareTo(Object other)
	{
		ActionDisplay a = (ActionDisplay)other;
		return text.compareToIgnoreCase(a.text);
	}

	@Override
	public String toString()
	{
		return text;
	}
}
