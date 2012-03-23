/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
