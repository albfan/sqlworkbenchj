/*
 * PropertiesCopier.java
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

import java.util.Enumeration;
import java.util.Properties;

/**
 *
 * @author Thomas Kellerer
 */
public class PropertiesCopier
{

	public void copyToSystem(Properties source)
	{
		copy(source, System.getProperties());
	}

	public void copy(Properties source, Properties target)
	{
		if (source == null || target == null) return;
		Enumeration keys = source.propertyNames();
		while (keys.hasMoreElements())
		{
			String key = (String)keys.nextElement();
			String value = source.getProperty(key);
			target.setProperty(key, value);
		}
	}

	public void removeFromSystem(Properties source)
	{
		remove(source, System.getProperties());
	}

	public void remove(Properties source, Properties target)
	{
		if (source == null || target == null) return;
		Enumeration keys = source.propertyNames();
		while (keys.hasMoreElements())
		{
			String key = (String)keys.nextElement();
			target.remove(key);
		}
	}

}
