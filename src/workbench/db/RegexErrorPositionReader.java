/*
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

package workbench.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexErrorPositionReader
	implements ErrorPositionReader
{
	private Pattern positionPattern;
	private Pattern lineInfoPattern;
	private Pattern columnInfoPattern;
	private final Pattern noNumbers = Pattern.compile("[^0-9]");

	public RegexErrorPositionReader(String positionRegex)
		throws PatternSyntaxException
	{
		positionPattern = Pattern.compile(positionRegex);
	}

	public RegexErrorPositionReader(String lineRegex, String columnRegex)
		throws PatternSyntaxException
	{
		lineInfoPattern = Pattern.compile(lineRegex);
		columnInfoPattern = Pattern.compile(columnRegex);
	}

	@Override
	public int getErrorPosition(WbConnection con, String sql, Exception ex)
	{
		if (ex == null) return -1;
		String msg = ex.getMessage();

		if (lineInfoPattern != null && columnInfoPattern != null)
		{
			return getPositionFromLineAndColumn(msg, sql);
		}
		if (positionPattern != null)
		{
			return getValueFromRegex(msg, positionPattern);
		}
		return -1;
	}

	private int getValueFromRegex(String msg, Pattern pattern)
	{
		if (msg == null) return -1;
		int position = -1;

		Matcher lm = pattern.matcher(msg);
		if (lm.find())
		{
			String lineInfo = noNumbers.matcher(msg.substring(lm.start(), lm.end())).replaceAll("");
			position = StringUtil.getIntValue(lineInfo, -1) - 1;
		}

		return position;
	}

	private int getPositionFromLineAndColumn(String msg, String sql)
	{
		int line = getValueFromRegex(msg, lineInfoPattern);
		int column = getValueFromRegex(msg, columnInfoPattern);

		int offset = StringUtil.getLineStartOffset(sql, line);
		if (offset < 0)
		{
			return -1;
		}
		return offset + column;
	}


	@Override
	public String enhanceErrorMessage(String sql, String originalMessage, int errorPosition)
	{
		String indicator = SqlUtil.getErrorIndicator(sql, errorPosition);
		if (indicator != null)
		{
			originalMessage += "\n\n" + indicator;
		}
		return originalMessage;
	}

}
