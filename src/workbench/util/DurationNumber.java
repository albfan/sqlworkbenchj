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

package workbench.util;

/**
 * A class to handle durations based on units.
 *
 * @author Thomas Kellerer
 */
public class DurationNumber
{
	private final char UNIT_SECONDS = 's';
	private final char UNIT_MINUTES = 'm';
	private final char UNIT_HOURS = 'h';
	private final char UNIT_DAYS = 'd';

	private final long ONE_SECOND = 1000L;
	private final long ONE_MINUTE = ONE_SECOND * 60;
	private final long ONE_HOUR = ONE_MINUTE * 60;
	private final long ONE_DAY = ONE_HOUR * 24;

	public DurationNumber()
	{
	}

	public boolean isValid(String definition)
	{
		if (StringUtil.isBlank(definition))
		{
			return false;
		}
		String pattern = "^[0-9]+[smhd]{1}$";
		return definition.trim().toLowerCase().matches(pattern);
	}

	public long parseDefinition(String definition)
	{
		if (StringUtil.isEmptyString(definition)) return 0;

		definition = definition.trim().toLowerCase().replace(" ", "");
		if (definition.isEmpty()) return 0;

		if (definition.length() == 1)
		{
			return StringUtil.getLongValue(definition, 0);
		}

		char lastChar = definition.charAt(definition.length() - 1);
		long value = -1;
		if (Character.isDigit(lastChar))
		{
			value = StringUtil.getLongValue(definition, 0);
		}
		else
		{
			value = StringUtil.getLongValue(definition.substring(0, definition.length() - 1), 0);
		}

		switch (lastChar)
		{
			case UNIT_SECONDS:
				return value * ONE_SECOND;
			case UNIT_MINUTES:
				return value * ONE_MINUTE;
			case UNIT_HOURS:
				return value * ONE_HOUR;
			case UNIT_DAYS:
				return value * ONE_DAY;
		}
		return value;
	}

}
