package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbDisableOraOutput extends SqlCommand
{
	public static final String VERB = "DISABLEOUT";

	public WbDisableOraOutput()
	{
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		aConnection.getMetadata().disableOutput();
		result.addMessage(ResourceMgr.getString("MsgDbmsOutputDisabled"));
		return result;
	}

}