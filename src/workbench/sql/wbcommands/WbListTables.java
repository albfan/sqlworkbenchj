/*
 * WbListTables.java
 *
 * Created on 16. November 2002, 15:33
 */

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
public class WbListTables extends SqlCommand
{
	public static final String VERB = "LIST";
	
	/** Creates a new instance of WbListTables */
	public WbListTables()
	{
	}

	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		DataStore ds = aConnection.getMetadata().getTables();
		result.addDataStore(ds);
		return result;
	}	
	
}
