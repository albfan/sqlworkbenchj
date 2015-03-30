/*
 * OracleWarningsClearer.java
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
package workbench.db.oracle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLWarning;
import workbench.log.LogMgr;

/**
 * A hack for pre 10.x drivers to properly clear the warnings on an Oracle connection object.
 *
 * This should not be used on newer drivers.
 *
 * @author Thomas Kellerer
 */
public class OracleWarningsClearer
{
	private boolean methodAvailable = true;

	public void clearWarnings(Connection sqlConn)
	{
		if (!methodAvailable) return;

		// Older Oracle drivers (before  10.x) do NOT clear the warnings  when calling clearWarnings()

		// luckily the instance variable on the driver which holds the warnings is defined as public and thus we can
		// reset the warnings "manually"
		// This is done via reflection so that the Oracle driver does not need to be present when compiling
		Class ora = sqlConn.getClass();

		try
		{
			Field dbAccessField = ora.getField("db_access");
			Class dbAccessClass = dbAccessField.getType();
			Object dbAccess = dbAccessField.get(sqlConn);
			Method clearSettings = dbAccessClass.getMethod("setWarnings", new Class[] {SQLWarning.class} );
			LogMgr.logDebug("OracleWarningsClearer.clearWarnings()", "Trying to clear warnings");
			// the following line is equivalent to:
			// OracleConnection con = (OracleConnection)this.sqlConnection;
			// con.db_access.setWarnings(null);
			clearSettings.invoke(dbAccess, new Object[] { null });
		}
		catch (Throwable e)
		{
			methodAvailable = false;
		}
	}
}
