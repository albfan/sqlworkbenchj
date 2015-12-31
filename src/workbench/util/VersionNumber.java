/*
 * VersionNumber.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 * @author Thomas Kellerer
 */
public class VersionNumber
{
	private int major = -1;
	private int minor = -1;
	private int patchLevel = -1;

	public VersionNumber(int majorVersion, int minorVersion)
	{
		this.major = majorVersion;
		this.minor = minorVersion;
	}

	public VersionNumber(int majorVersion, int minorVersion, int patch)
	{
		this.major = majorVersion;
		this.minor = minorVersion;
		this.patchLevel = patch;
	}

	public VersionNumber(String number)
	{
		if (StringUtil.isEmptyString(number))
		{
			return;
		}

		if ("@BUILD_NUMBER@".equals(number))
		{
			major = 999;
			minor = 999;
		}
		else
		{
			try
			{
				String[] numbers = number.split("\\.");

				major = Integer.parseInt(numbers[0]);

				if (numbers.length > 1)
				{
					minor = parseValue(numbers[1]);
				}
				if (numbers.length > 2)
				{
					patchLevel = parseValue(numbers[2]);
				}
			}
			catch (Exception e)
			{
				minor = -1;
				major = -1;
			}
		}
	}

	private int parseValue(String value)
	{
		if (value.indexOf('-') > -1)
		{
			String plain = value.substring(0, value.indexOf('-'));
			return Integer.parseInt(plain);
		}
		return Integer.parseInt(value);
	}

	public boolean isValid()
	{
		return this.major != -1;
	}

	public int getMajorVersion()
	{
		return this.major;
	}

	public int getMinorVersion()
	{
		return this.minor;
	}

	public int getPatchLevel()
	{
		return patchLevel;
	}

	public boolean isNewerThan(VersionNumber other)
	{
		if (!this.isValid()) return false;
		if (this.major > other.major) return true;
		if (this.major == other.major)
		{
			if (this.minor == other.minor)
			{
				return (this.patchLevel > other.patchLevel);
			}
			return (this.minor > other.minor);
		}
		return false;
	}

	public boolean isNewerOrEqual(VersionNumber other)
	{
		if (isNewerThan(other)) return true;
		if (this.major == other.major && this.minor == other.minor && this.patchLevel == other.patchLevel) return true;
		return false;
	}

	@Override
	public String toString()
	{
		if (major == -1) return "n/a";
		if (major == 999) return "999";

		if (patchLevel == -1 && minor != -1)
		{
			return Integer.toString(major) + "." + Integer.toString(minor);
		}
		if (minor == -1 && patchLevel == -1) return Integer.toString(major);

		return Integer.toString(major) + "." + Integer.toString(minor) + "." + Integer.toString(patchLevel);
	}

	public static VersionNumber getJavaVersion()
	{
		String version = System.getProperty("java.version", null);
		if (version == null)
		{
			version = System.getProperty("java.runtime.version");
		}

		try
		{
			String[] elements = version.split("\\.");
			return new VersionNumber(Integer.valueOf(elements[0]), Integer.valueOf(elements[1]));
		}
		catch (Throwable th)
		{
			// that's the minimum version this application needs
			return new VersionNumber(1,6);
		}
	}
}
