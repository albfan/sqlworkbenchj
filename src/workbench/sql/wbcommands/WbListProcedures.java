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
public class WbListProcedures 
	extends SqlCommand
{
	public static final String VERB = "LISTPROCS";
	
	public WbListProcedures()
	{
	}
	
	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		DataStore ds = aConnection.getMetadata().getProcedures(null, null);
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}	
	
}
