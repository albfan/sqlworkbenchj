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
import workbench.resource.ResourceMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class DisplayLocale
	implements Comparable<DisplayLocale>
{
	private final WbLocale locale;
	private String display;
	private Locale displayLocale;

	public DisplayLocale()
	{
		this.locale = null;
	}

	public DisplayLocale(WbLocale l)
	{
		this.locale = l;
	}
	
	public Locale getLocale()
	{
		if (locale == null) return null;
		return locale.getLocale();
	}

	public void setDisplayLocale(Locale l)
	{
		displayLocale = l;
	}

	public boolean isEmpty()
	{
		return locale == null;
	}
	
	public String toString()
	{
		if (display != null) return display;

		if (locale == null) 
		{
			display = ResourceMgr.getString("LblDefaultIndicator");
		}
		else
		{
			StringBuffer s = new StringBuffer(20);
			String country = null;
			if (displayLocale == null)
			{
				s.append(locale.getLocale().getDisplayLanguage());
				country = locale.getLocale().getDisplayCountry();
			}
			else
			{
				s.append(locale.getLocale().getDisplayLanguage(displayLocale));
				country = locale.getLocale().getDisplayCountry(displayLocale);
			}
			if (!StringUtil.isEmptyString(country))
			{
				s.append(" (");
				s.append(country);
				s.append(')');
			}
			this.display = s.toString();
		}
		return this.display;
	}
	
	public int compareTo(DisplayLocale other)
	{
		if (this.locale == null) return -1;
		if (other.locale == null) return 1;
		return this.locale.compareTo(other.locale);
	}
	
	public boolean equals(Object other)
	{
		if (other == null) return false;
		
		if (other instanceof DisplayLocale)
		{
			DisplayLocale dl = (DisplayLocale)other;
			if (this.locale == null && dl.locale == null) return true;
			if (this.locale != null && dl.locale == null) return false;
			if (this.locale == null && dl.locale != null) return false;
			return locale.equals(dl.locale);
		}
		return false;
	}
	
	public int hashCode()
	{
		return locale.hashCode();
	}
}
