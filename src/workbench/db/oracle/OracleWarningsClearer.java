/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.oracle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLWarning;
import workbench.db.WbConnection;
import workbench.log.LogMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleWarningsClearer
{
	private boolean methodAvailable = true;

	public void clearWarnings(WbConnection con)
	{
		if (!methodAvailable) return;

		// Older Oracle drivers (pre 10.x) do NOT clear the warnings
		// (as discovered when looking at the source code)

		// luckily the instance variable on the driver which holds the
		// warnings is defined as public and thus we can
		// reset the warnings "manually"
		// This is done via reflection so that the Oracle driver
		// does not need to be present when compiling
		Connection sqlConn = con.getSqlConnection();

		Class ora = sqlConn.getClass();

		if (ora.getName().equals("oracle.jdbc.driver.OracleConnection"))
		{
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
				// newer drivers do not seem to support this any more,
				// so after the first error, we'll skip this for the rest of the session
				methodAvailable = false;
			}
		}

	}
}
