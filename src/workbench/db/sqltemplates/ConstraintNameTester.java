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
	private String dbId;

	public ConstraintNameTester(String id)
	{
		this.dbId = id;
	}

	public boolean isSystemConstraintName(String name)
	{
		if (name == null) return false;
		String regex = Settings.getInstance().getProperty("workbench.db." + dbId + ".constraints.systemname", null);
		if (StringUtil.isEmptyString(regex)) return false;

		try
		{
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(name);
			return m.matches();
		}
		catch (Exception e)
		{
			LogMgr.logError("ConstraintNameTester.isSystemConstraintName()", "Error in regex", e);
		}
		return false;
	}

}
