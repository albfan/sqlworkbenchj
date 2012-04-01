/*
 * Alias.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
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

	@Override
	public String toString()
	{
		if (display == null)
		{
			if (alias == null) display = objectName;
			else display = alias + " (" + objectName + ")";
		}
		return display;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 47 * hash + (this.objectName != null ? this.objectName.hashCode() : 0);
		hash = 47 * hash + (this.alias != null ? this.alias.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final Alias other = (Alias) obj;
		if ((this.objectName == null) ? (other.objectName != null) : !this.objectName.equals(other.objectName))
		{
			return false;
		}
		if ((this.alias == null) ? (other.alias != null) : !this.alias.equals(other.alias))
		{
			return false;
		}
		return true;
	}

}
