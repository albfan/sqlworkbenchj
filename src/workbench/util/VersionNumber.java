/*
 * VersionNumber.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 * @author support@sql-workbench.net
 */
public class VersionNumber
{
	private int major = -1;
	private int minor = -1;
	
	public VersionNumber(String number)
	{
		if (StringUtil.isEmptyString(number)) 
		{
			return;
		}
		
		if ("@BUILD_NUMBER@".equals(number))
		{
			major = 999;
		}
		else
		{
			try
			{
				String[] numbers = number.split("\\.");

				major = Integer.parseInt(numbers[0]);

				if (numbers.length > 1)
				{
					minor = Integer.parseInt(numbers[1]);
				}
				else
				{
					minor = -1;
				}
			}
			catch (Exception e)
			{
				minor = -1;
				major = -1;
			}
		}
	}

	public boolean isValid()
	{
		return this.major != -1;
	}

	public int getMajorVersion() { return this.major; }
	public int getMinorVersion() { return this.minor; }
	
	public boolean isNewerThan(VersionNumber other)
	{
		if (!this.isValid()) return false;
		if (this.major > other.major) return true;
		if (this.minor > other.minor) return true;
		return false;
	}
	
	public String toString()
	{
		if (minor == -1 && major == -1) return "n/a";
		
		if (minor == -1) return Integer.toString(major);
		return Integer.toString(major) + "." + Integer.toString(minor);
	}
}
