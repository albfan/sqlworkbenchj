/*
 * PoiHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.gui.components;

import workbench.log.LogMgr;


/**
 * Test if DbUnit classes are available on the classpath.
 *
 * @author Thomas Kellerer
 */
public class DbUnitHelper
{
	private static boolean tested;
	private static boolean available;

	public static boolean isDbUnitAvailable()
	{
		if (tested)
		{
			return available;
		}

		try
		{
			tested = true;
			Class.forName("org.dbunit.dataset.ITable");
			LogMgr.logInfo("DbUnitHelper.isDbUnitAvailable()", "DbUnit available");
			available = true;
		}
		catch (Throwable th)
		{
			LogMgr.logDebug("DbUnitHelper.isDbUnitAvailable()", "DbUnit not available");
			available = false;
		}
		return available;
	}
}
