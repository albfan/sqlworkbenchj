/*
 * ConstraintNameTester.java
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
package workbench.db.sqltemplates;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ConstraintNameTester
{
	private Pattern sysNamePattern;

	public ConstraintNameTester(String id)
	{
		String regex = Settings.getInstance().getProperty("workbench.db." + id + ".constraints.systemname", null);
		if (StringUtil.isNonEmpty(regex))
		{
			try
			{
				sysNamePattern = Pattern.compile(regex);
			}
			catch (Exception ex)
			{
				sysNamePattern = null;
				LogMgr.logError("ConstraintNameTester.isSystemConstraintName()", "Error in regex", ex);
			}
		}
	}

	public boolean isSystemConstraintName(String name)
	{
		if (name == null) return false;
		if (sysNamePattern == null) return false;

		Matcher m = sysNamePattern.matcher(name);
		return m.matches();
	}

}
