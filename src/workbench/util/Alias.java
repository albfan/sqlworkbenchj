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

import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;

/**
 * @author Thomas Kellerer
 */
public class Alias
{
	protected String objectName;
	private String alias;
	private String display;

	/**
	 * Create a new Alias
	 * @param value  the SQL part that should be parsed
	 */
	public Alias(String value)
	{
		if (StringUtil.isEmptyString(value)) throw new IllegalArgumentException("Identifier must not be empty");

		SQLLexer lexer = new SQLLexer(value);
		StringBuilder name = new StringBuilder(value.length());
		SQLToken t = lexer.getNextToken(false, true);
		boolean objectNamePart = true;
		while (t != null)
		{
			if (t.isWhiteSpace())
			{
				objectNamePart = false;
			}
			if (objectNamePart)
			{
				name.append(t.getText());
			}
			else if ("AS".equals(t.getContents()))
			{
				objectNamePart = false;
			}
			else
			{
				alias = t.getText();
			}
			t = lexer.getNextToken(false, true);
		}
		objectName = name.toString();
	}

	/**
	 * Returned the alias defined for the object.
	 * Might be null
	 *
	 * @see #getAlias()
	 */
	public final String getAlias()
	{
		return this.alias;
	}

	/**
	 * Returns the object name that is aliased.
	 * @see #getAlias()
	 */
	public final String getObjectName()
	{
		return objectName;
	}

	/**
	 * Returns the name that has to be used inside the SQL statement.
	 * If an alias is defined, this will be the alias. The object name otherwise
	 *
	 * @see #getAlias()
	 * @see #getObjectName()
	 */
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
