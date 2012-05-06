/*
 * SelectIntoVerifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectIntoVerifier
{
	private Pattern selectIntoPattern;

	public SelectIntoVerifier(String dbId)
	{
		String pattern = Settings.getInstance().getProperty("workbench.db." + dbId + ".selectinto.pattern", null);
		if (pattern != null)
		{
			try
			{
				this.selectIntoPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			}
			catch (Exception e)
			{
				LogMgr.logError("SelectIntoTester.initializePattern()", "Incorrect Pattern for detecting SELECT ... INTO <new table> specified", e);
				this.selectIntoPattern = null;
			}
		}
	}

	/**
	 * Checks if the given SQL string is actually some kind of table
	 * creation "disguised" as a SELECT.
	 * <br/>
	 * Whether a statement is identified as a SELECT into a new table
	 * is defined through the regular expression that can be set for
	 * the DBMS using the property:
	 * <tt>workbench.sql.[dbid].selectinto.pattern</tt>
	 *
	 * This method returns true if a Regex has been defined and matches the given SQL
	 */
	public boolean isSelectIntoNewTable(String sql)
	{
		if (this.selectIntoPattern == null) return false;
		if (StringUtil.isEmptyString(sql)) return false;

		int pos = SqlUtil.getKeywordPosition("SELECT", sql);
		if (pos > -1)
		{
			sql = sql.substring(pos);
		}
		Matcher m = selectIntoPattern.matcher(sql);
		return m.find();
	}

	/**
	 * Returns true if the current DBMS supports a SELECT syntax
	 * which creates a new table (e.g. SELECT .. INTO new_table FROM old_table)
	 *
	 * It simply checks if a regular expression has been defined to
	 * detect this kind of statements
	 *
	 * @see #isSelectIntoNewTable(String)
	 */
	public boolean supportsSelectIntoNewTable()
	{
		return this.selectIntoPattern != null;
	}

}
