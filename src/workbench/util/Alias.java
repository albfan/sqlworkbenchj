/*
 * Alias.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 * @author Thomas Kellerer
 */
public class Alias
{
	protected String objectName;
	private String alias;
	private String display;

	public Alias(String value)
	{
		if (StringUtil.isEmptyString(value)) throw new IllegalArgumentException("Identifier must not be empty");

		String tablename = null;
		String[] words = value.split("\\s");

		if (words.length > 0)
		{
			tablename = words[0].trim();
		}

		if (words.length == 2)
		{
			alias = words[1].trim();
		}
		else if (words.length == 3)
		{
			// Assuming "table AS t1" syntax
			if (words[1].equalsIgnoreCase("as"))
			{
				alias = words[2].trim();
			}
			else
			{
				alias = words[1].trim();
			}
		}
		else
		{
			this.alias = null;
		}

		objectName = tablename;
	}

	public final String getAlias()
	{
		return this.alias;
	}

	public final String getObjectName()
	{
		return objectName;
	}

	public final String getNameToUse()
	{
		if (alias == null) return objectName;
		return alias;
	}

	public String toString()
	{
		if (display == null)
		{
			if (alias == null) display = objectName;
			else display = alias + " (" + objectName + ")";
		}
		return display;
	}

}
