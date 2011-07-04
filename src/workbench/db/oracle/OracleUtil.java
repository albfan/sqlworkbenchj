/*
 * OracleUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleUtil
{

	private static Pattern upperCaseQuoted = Pattern.compile("\\\"([A-Z_]+)\\\"");

	/**
	 * Remove double quotes from all quoted uppercase identifiers.
	 *
	 * @param input the SQL text (typically retrieved using dbms_metadata)
	 * @return a clean version without quotes that are not needed.
	 */
	public static String cleanupQuotedIdentifiers(String input)
	{
		if (input == null) return input;
		Matcher m = upperCaseQuoted.matcher(input.trim());
		return m.replaceAll("$1"); // remove the double quotes from uppercase quoted identifiers
	}
}
