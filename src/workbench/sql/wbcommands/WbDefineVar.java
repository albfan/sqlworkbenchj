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
public class WbDefineVar extends SqlCommand
{
	public WbDefineVar()
	{
	}

	public String getVerb() { return "VARDEF"; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(getVerb());
		String sql = aSql.trim().substring(this.getVerb().length()).trim();
		
		String msg = null;
		
		WbStringTokenizer tok = new WbStringTokenizer("=", true, "\"'", false);
		tok.setSourceString(sql);
		String value = null;
		String var = null;
		
		if (tok.hasMoreTokens()) var = tok.nextToken();
		
		if (var == null)
		{
			result.addMessage("ErrorVarDefWrongParameter");
			result.setFailure();
			return result;
		}
		
		if (tok.hasMoreTokens()) value = tok.nextToken();

		if (value != null)
		{
			msg = ResourceMgr.getString("MsgVarDefVariableDefined");
			SqlParameterPool.getInstance().setParameterValue(var, value);
			msg = msg.replaceAll("%var%", var);
			msg = msg.replaceAll("%value%", value);
			msg = msg.replaceAll("%varname%", StringUtil.quoteRegexMeta(SqlParameterPool.getInstance().buildVarName(var)));
		}
		else
		{
			msg = ResourceMgr.getString("MsgVarDefVariableRemoved");
			msg = msg.replaceAll("%var%", var);
		}
		
		result.addMessage(msg);
		result.setSuccess();
		
		return result;
	}

}