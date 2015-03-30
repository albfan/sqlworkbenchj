/*
 * Log4JHelper.java
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
package workbench.log;

import java.lang.reflect.Method;

/**
 * Test if Log4J classes are available on the classpath.
 * <br>
 * If Log4J is available, it will call
 * org.apache.log4j.Log4JLoggerFactory.setLoggerFqcn() passing LogMgr.class
 *
 * All this is done using Reflection, so Log4J does not need to be available.
 *
 *
 * @author Peter Franken
 * @author Thomas Kellerer
 */
public class Log4JHelper
{
	private static boolean tested;
	private static boolean available;

	public static boolean isLog4JAvailable()
	{
		if (tested) return available;
		try
		{
			tested = true;
			Class.forName("org.apache.log4j.Logger");
			Class factory = Class.forName("workbench.log.Log4JLoggerFactory");
			Method setLoggerFqcn = factory.getDeclaredMethod("setLoggerFqcn", new Class[] { Class.class });
			setLoggerFqcn.invoke(null, new Object[] { LogMgr.class } );
			available = true;
		}
		catch (Throwable th)
		{
			th.printStackTrace(System.err);
			available = false;
		}
		return available;
	}

}
