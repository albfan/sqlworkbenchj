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
public class WbListCatalogs extends SqlCommand
{
	public final String VERB;
	
	public static final WbListCatalogs LISTDB = new WbListCatalogs("LISTDB");
	public static final WbListCatalogs LISTCAT = new WbListCatalogs("LISTCAT");
	
	private WbListCatalogs(String verb)
	{
		this.VERB = verb;
	}
	
	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql) 
		throws SQLException, WbException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		DataStore ds = aConnection.getMetadata().getCatalogInformation();
		result.addDataStore(ds);
		result.setSuccess();
		return result;
	}	
	
}
