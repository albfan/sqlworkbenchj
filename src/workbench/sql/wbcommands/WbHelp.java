package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.WbManager;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.exception.WbException;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.LineTokenizer;

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
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult("HELP");
		result.setSuccess();
		WbManager.getInstance().getCurrentWindow().showHelp();
		return result;
	}	
	
}
