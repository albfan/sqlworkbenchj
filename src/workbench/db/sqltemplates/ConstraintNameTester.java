/*
 * ConstraintNameTester.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
