package workbench.sql.wbcommands;

import java.sql.SQLException;
import workbench.db.WbConnection;
import workbench.exception.WbException;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbDisableOraOutput extends SqlCommand
{
	public static final String VERB = "ENABLEOUT";
	
	public WbDisableOraOutput()
	{
	}
	
	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		aConnection.getMetadata().disableOutput();
		return result;
	}	
	
}
