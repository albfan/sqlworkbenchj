package workbench.sql.wbcommands;

import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.LineTokenizer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbEnableOraOutput extends SqlCommand
{
	public static final String VERB = "ENABLEOUT";

	public WbEnableOraOutput()
	{
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException, Exception
	{
		this.checkVerb(aSql);

		LineTokenizer tok = new LineTokenizer(aSql.trim(), " ");
		long limit = -1;
		String verb = tok.nextToken(); // skip the verb

		// second token is the buffer size
		if (tok.hasMoreTokens())
		{
			String value = tok.nextToken();
			try
			{
				limit = Long.parseLong(value);
			}
			catch (NumberFormatException nfe)
			{
				limit = -1;
			}
		}
		aConnection.getMetadata().enableOutput(limit);
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		result.addMessage(ResourceMgr.getString("MsgDbmsOutputEnabled"));
		return result;
	}

}