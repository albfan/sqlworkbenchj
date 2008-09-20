/*
 * WbLocale.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Locale;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbLocale
	implements Comparable<WbLocale>
{
	private final Locale locale;
	
	public WbLocale(Locale l)
	{
		this.locale = l;
	}
	
	public Locale getLocale() 
	{
		return locale;
	}
	
	public String toString()
	{
		String lang = StringUtil.capitalize(locale.getDisplayLanguage(locale));
		return lang;
	}
	
	public int compareTo(WbLocale other)
	{
		return this.toString().compareTo(other.toString());
	}
	
	public boolean equals(Object other)
	{
		if (other == null) return false;
		if (locale == null) return false;
		if (other instanceof WbLocale)
		{
			return this.locale.equals(((WbLocale)other).locale);
		}
		return false;
	}
	
	public int hashCode()
	{
		return locale.hashCode();
	}
}
