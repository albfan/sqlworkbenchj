/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer.
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
	private final Pattern positionPattern;
	private final Pattern lineInfoPattern;
	private final Pattern columnInfoPattern;
	private final Pattern noNumbers = Pattern.compile("[^0-9]");
	private boolean oneBasedNumbers = true;

	public RegexErrorPositionReader(String positionRegex)
		throws PatternSyntaxException
	{
		positionPattern = Pattern.compile(positionRegex);
		lineInfoPattern = null;
		columnInfoPattern = null;
		LogMgr.logDebug("RegexErrorPositionReader.<init>", "Using regex for position: " + positionRegex);
	}

	public RegexErrorPositionReader(String lineRegex, String columnRegex)
		throws PatternSyntaxException
	{
		lineInfoPattern = lineRegex == null ? null : Pattern.compile(lineRegex);
		columnInfoPattern = columnRegex == null ? null : Pattern.compile(columnRegex);
		positionPattern = null;
		LogMgr.logDebug("RegexErrorPositionReader.<init>", "Using regex for line#: " + lineRegex + ", regex for column#: " + columnRegex);
	}

	/**
	 * Defines if the line and column numbers returned in the error message are one based (first line is 1)
	 * or zero based (first line is 0)
	 *
	 * @param flag    true: the first line (or column) is 1
	 *                false: the first line (or column) is 0
	 */
	public void setNumbersAreOneBased(boolean flag)
	{
		this.oneBasedNumbers = flag;
	}

	/**
	 * Retrieve an ErrorDescriptor from the given SQL and exception
	 *
	 * @param con   then connection on which the exception happened (may be used to retrieve more error information)
	 * @param sql   the failing SQL statement
	 * @param ex    the error that happens
	 * @return an ErrorDescriptor, null indicates the error could not be obtained
	 */
	@Override
	public ErrorDescriptor getErrorPosition(WbConnection con, String sql, Exception ex)
	{
		if (ex == null) return null;
		String msg = ex.getMessage();
		ErrorDescriptor result = getErrorPosition(sql, msg);
		if (result.getErrorPosition() > -1 && !con.getDbSettings().getErrorPosIncludesLeadingComments())
		{
			int startOffset = SqlUtil.getRealStart(sql);
			int offset = result.getErrorPosition();
			result.setErrorOffset(offset + startOffset);
		}
		return result;
	}

	public ErrorDescriptor getErrorPosition(String sql, String msg)
	{
		if (positionPattern != null)
		{
			ErrorDescriptor result = new ErrorDescriptor();
			result.setErrorOffset(getValueFromRegex(msg, positionPattern));
      result.setErrorMessage(msg);
			return result;
		}
		if (lineInfoPattern != null || columnInfoPattern != null)
		{
      ErrorDescriptor result = getPositionFromLineAndColumn(msg, sql);
      result.setErrorMessage(msg);
			return result;
		}
		return null;
	}

	private int getValueFromRegex(String msg, Pattern pattern)
	{
		if (pattern == null) return 0;
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

		if (position > 0 && oneBasedNumbers)
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

		result.setErrorPosition(line, column);

		int offset = SqlUtil.getErrorOffset(sql, result);
		if (offset > -1)
		{
			result.setErrorOffset(offset);
		}
		return result;
	}

	@Override
	public String enhanceErrorMessage(String sql, String originalMessage, ErrorDescriptor errorInfo)
	{
		String indicator = SqlUtil.getErrorIndicator(sql, errorInfo);
		if (indicator != null)
		{
			if (originalMessage == null) originalMessage = ""; // avoid a "null" string in the output
			if (StringUtil.isNonEmpty(originalMessage)) originalMessage += "\n\n";
			originalMessage += indicator;
		}
		return originalMessage;
	}

}
