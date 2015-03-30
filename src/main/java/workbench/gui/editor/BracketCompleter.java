/*
 * BracketCompleter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class BracketCompleter
	implements PropertyChangeListener
{
	private char[] openingBrackets;
	private String[] closingBrackets;

	public BracketCompleter()
	{
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_COMPLETE_CHARS);
		setCompletionPairs(Settings.getInstance().getProperty(GuiSettings.PROPERTY_COMPLETE_CHARS, null));
	}

	public BracketCompleter(String pairs)
	{
		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_COMPLETE_CHARS);
		setCompletionPairs(pairs);
	}

	public void setCompletionPairs(String pairs)
	{
		if (!isValidDefinition(pairs)) throw new IllegalArgumentException("Not a valid bracket pair definition");
		if (StringUtil.isBlank(pairs))
		{
			openingBrackets = null;
			closingBrackets = null;
			return;
		}

		int numPairs = pairs.length() / 2;
		openingBrackets = new char[numPairs];
		closingBrackets = new String[numPairs];
		for (int i=0, pair=0; i < pairs.length(); i += 2, pair++)
		{
			openingBrackets[pair] = pairs.charAt(i);
			closingBrackets[pair] = new String( new char[] { pairs.charAt(i + 1)} );
		}
	}

	@Override
	public String toString()
	{
		if (openingBrackets == null) return "";

		StringBuilder result = new StringBuilder(openingBrackets.length * 2);
		for (int i=0; i < openingBrackets.length; i++)
		{
			if (i > 0) result.append(", ");
			result.append(openingBrackets[i]);
			result.append('|');
			result.append(closingBrackets[i]);
		}
		return result.toString();
	}

	public char getOpeningChar(char input)
	{
		if (openingBrackets == null || closingBrackets == null) return 0;
		for (int i = 0; i < closingBrackets.length; i++)
		{
			if (closingBrackets[i].charAt(0) == input)
			{
				return openingBrackets[i];
			}
		}
		return 0;
	}

	public String getCompletionChar(char input)
	{
		int i = findChar(input);
		if (i < 0) return null;
		return closingBrackets[i];
	}

	private int findChar(char c)
	{
		if (openingBrackets == null) return -1;
		for (int i=0; i < openingBrackets.length; i++)
		{
			if (openingBrackets[i] == c) return i;
		}
		return -1;
	}

	public static boolean isValidDefinition(String input)
	{
		// empty string => no bracket completion
		if (StringUtil.isBlank(input)) return true;
		return input.length() % 2 == 0;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		String newpairs = (String)evt.getNewValue();
		setCompletionPairs(newpairs);
	}

	public void dispose()
	{
		Settings.getInstance().removePropertyChangeListener(this);
	}
}
