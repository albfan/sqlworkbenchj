package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.SqlParameterPool;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbListVars extends SqlCommand
{
	public WbListVars()
	{
	}

	public String getVerb() { return "LISTVAR"; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(getVerb());
		result.addDataStore(SqlParameterPool.getInstance().getVariablesDataStore());
		result.setSuccess();
		return result;
	}

}