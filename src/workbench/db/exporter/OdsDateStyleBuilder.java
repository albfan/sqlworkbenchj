/*
 * OdsDateStyleBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.exporter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import workbench.util.HtmlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OdsDateStyleBuilder
{
	private String formatString;
	private List<String> elements = new ArrayList<>(5);

	public OdsDateStyleBuilder(String format)
	{
		this.formatString = format;
		parseFormat();
	}

	public OdsDateStyleBuilder(SimpleDateFormat formatter)
	{
		formatString = formatter.toPattern();
		parseFormat();
	}

	public String getXML(String indent)
	{
		StringBuilder result = new StringBuilder(elements.size() * 10);
		for (String element : elements)
		{
			result.append(indent);
			result.append(element);
			result.append('\n');
		}
		return result.toString();
	}

	private void parseFormat()
	{
		String formatterChars = "GyMwWdDFEaKkHhmsSzZ";
		StringBuilder currentText = new StringBuilder();
		int len = formatString.length();
		for (int pos=0; pos < len;)
		{
			char current = formatString.charAt(pos);
			if (formatterChars.indexOf(current) > -1)
			{
				if (currentText.length() > 0)
				{
					elements.add("<number:text>" + HtmlUtil.escapeXML(currentText.toString()) + "</number:text>");
					currentText = new StringBuilder();
				}
				int charCount = getCharacterCount(formatString, pos);
				String tag = getStyleFormat(current, charCount);
				if (tag != null)
				{
					elements.add(tag);
				}
				pos += charCount;
			}
			else
			{
				currentText.append(current);
				pos ++;
			}
		}
	}


	/**
	 * Counts the number of times the character at the position startAt is repeated in the input string.
	 *
	 * @param input    the string to check
	 * @param startAt  the starting position
	 * @return the number of times the character appears. At least 1 (the one at startAt)
	 */
	private int getCharacterCount(String input, int startAt)
	{
		char current = input.charAt(startAt);
		int pos = startAt;
		while (pos < input.length() && input.charAt(pos) == current)
		{
			pos ++;
		}
		return pos - startAt;
	}

	private String getStyleFormat(char formatChar, int count)
	{
		switch (formatChar)
		{
			case 'a':
				return "<number:am-pm />";
			case 'y':
				return getNumberTag("year", count > 3, false);
			case 'M':
				return getNumberTag("month", count > 3, count > 2);
			case 'd':
				return getNumberTag("day", count > 2, false);
			case 'h':
			case 'H':
				return getNumberTag("hours", true, false);
			case 'm':
				return getNumberTag("minutes", count > 2, false);
			case 'S':
			case 's':
				return getNumberTag("seconds", true, false);
		}
		return null;
	}

	private String getNumberTag(String type, boolean longStyle, boolean textStyle)
	{
		String result = "<number:" + type;
		if (textStyle)
		{
			result += " number:textual=\"true\"";
		}
		if (longStyle)
		{
			result += " number:style=\"long\"";
		}
		return result + "/>";
	}
}
