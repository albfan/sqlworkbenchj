/*
 * RegexModifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer.modifier;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author support@sql-workbench.net
 */
public class RegexModifier
	implements ImportValueModifier
{
	public Map<ColumnIdentifier, RegexDef> limits = new HashMap<ColumnIdentifier, RegexDef>();

	public int getSize()
	{
		return limits.size();
	}

	/**
	 * Define regex replacement for a column.
	 * An existing mapping for that column will be overwritten.
	 *
	 * @param col the column for which to apply the substring
	 * @param regex the regular expression to search for
	 * @param replacement the replacement for the regex
	 */
	public void addDefinition(ColumnIdentifier col, String regex, String replacement)
		throws PatternSyntaxException
	{
		RegexDef def = new RegexDef(regex, replacement);
		this.limits.put(col.createCopy(), def);
	}

	public String modifyValue(ColumnIdentifier col, String value)
	{
		if (value == null) return null;
		RegexDef def = this.limits.get(col);
		if (def != null)
		{
			Matcher m = def.regex.matcher(value);
			return m.replaceAll(def.replacement);
		}
		return value;
	}

	private static class RegexDef
	{
		Pattern regex;
		String replacement;

		public RegexDef(String exp, String repl)
			throws PatternSyntaxException
		{
			regex = Pattern.compile(exp);
			replacement = repl;
		}
	}
}
