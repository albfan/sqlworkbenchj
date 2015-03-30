/*
 * OdfHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.exporter;

/**
 * Test if ODF Dom and Simple ODF classes are available on the classpath.
 *
 * @author Thomas Kellerer
 */
public class OdfHelper
{
	private static boolean tested;
	private static boolean available;

	public static boolean isSimpleODFAvailable()
	{
		if (tested)
		{
			return available;
		}

		try
		{
			tested = true;
			Class.forName("org.odftoolkit.simple.SpreadsheetDocument");
			available = true;
		}
		catch (Throwable th)
		{
			available = false;
		}
		return available;
	}

}
