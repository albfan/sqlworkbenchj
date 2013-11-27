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

import workbench.log.LogMgr;

import workbench.sql.ErrorDescriptor;

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
	private boolean numbersAreZeroBased;

	public RegexErrorPositionReader(String positionRegex)
		throws PatternSyntaxException
	{
		positionPattern = Pattern.compile(positionRegex);
		LogMgr.logDebug("RegexErrorPositionReader.<init>", "Using regex for position: " + positionRegex);
	}

	public RegexErrorPositionReader(String lineRegex, String columnRegex)
		throws PatternSyntaxException
	{
		lineInfoPattern = Pattern.compile(lineRegex);
		columnInfoPattern = Pattern.compile(columnRegex);
		LogMgr.logDebug("RegexErrorPositionReader.<init>", "Using regex for line#: " + lineRegex + ", regex for column#: " + columnRegex);
	}

	public void setNumbersAreZeroBased(boolean flag)
	{
		this.numbersAreZeroBased = flag;
	}

	@Override
	public ErrorDescriptor getErrorPosition(WbConnection con, String sql, Exception ex)
	{
		if (ex == null) return null;
		String msg = ex.getMessage();

		if (lineInfoPattern != null && columnInfoPattern != null)
		{
			return getPositionFromLineAndColumn(msg, sql);
		}
		if (positionPattern != null)
		{
			ErrorDescriptor result = new ErrorDescriptor();
			result.setErrorOffset(getValueFromRegex(msg, positionPattern));
			return result;
		}
		return null;
	}

	private int getValueFromRegex(String msg, Pattern pattern)
	{
		if (msg == null) return -1;
		int position = -1;

		Matcher lm = pattern.matcher(msg);
		if (lm.find())
		{
			String data = msg.substring(lm.start(), lm.end());
			LogMgr.logDebug("RegexErrorPositionReader.getValueFromRegex()", "Using " + data + " from message: " + msg);
			String lineInfo = noNumbers.matcher(data).replaceAll("");
			position = StringUtil.getIntValue(lineInfo, -1);
		}
		else
		{
			LogMgr.logDebug("RegexErrorPositionReader.getValueFromRegex()", "No match found for RegEx: \"" + pattern.pattern() + "\" in message: " + msg);
		}
		if (position > 0 && numbersAreZeroBased)
		{
			position --;
		}
		return position;
	}

	private ErrorDescriptor getPositionFromLineAndColumn(String msg, String sql)
	{
		ErrorDescriptor result = new ErrorDescriptor();
		int line = getValueFromRegex(msg, lineInfoPattern);
		int column = getValueFromRegex(msg, columnInfoPattern);

		int offset = StringUtil.getLineStartOffset(sql, line);
		if (offset < 0)
		{
			return null;
		}
		result.setErrorPosition(line, column);
		result.setErrorOffset(offset + column);
		return result;
	}

	@Override
	public String enhanceErrorMessage(String sql, String originalMessage, ErrorDescriptor errorInfo)
	{
		String indicator = SqlUtil.getErrorIndicator(sql, errorInfo);
		if (indicator != null)
		{
			originalMessage += "\n\n" + indicator;
		}
		return originalMessage;
	}

}
