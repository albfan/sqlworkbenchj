package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.SqlParameterPool;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbRemoveVar extends SqlCommand
{
	public WbRemoveVar()
	{
	}

	public String getVerb() { return "WBVARDELETE"; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(getVerb());
		String var = aSql.trim().substring(this.getVerb().length()).trim();

		String msg = null;

		if (var == null || var.length() == 0)
		{
			result.addMessage(ResourceMgr.getString("ErrorVarRemoveWrongParameter"));
			result.setFailure();
			return result;
		}
		else
		{
			boolean removed = SqlParameterPool.getInstance().removeValue(var);
			if (removed)
			{
				msg = ResourceMgr.getString("MsgVarDefVariableRemoved");
			}
			else
			{
				msg = ResourceMgr.getString("MsgVarDefVariableNotRemoved");
			}
			msg = msg.replaceAll("%var%", var);
		}

		result.addMessage(msg);
		result.setSuccess();

		return result;
	}

}