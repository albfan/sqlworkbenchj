package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbHelp extends SqlCommand
{
	public WbHelp()
	{
	}

	public String getVerb() { return "HELP"; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult("HELP");
		result.setSuccess();
		WbManager.getInstance().getCurrentWindow().showHelp();
		return result;
	}

}