/*
 * Created on December 9, 2002, 2:01 PM
 */
package workbench.sql;

import java.util.List;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class BatchRunner
{
	private List statements;
	private StatementRunner stmtRunner;
	private WbConnection connection;
	
	public BatchRunner(String aFilelist)
	{
		this.statements = StringUtil.stringToList(aFilelist, ",");
	}

	public void setProfile(String aProfilename)
		throws Exception
	{
		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		ConnectionProfile prof = mgr.getProfile(aProfilename);
		this.connection = mgr.getConnection(prof, "BatchRunner");
		this.stmtRunner = new StatementRunner();
		this.stmtRunner.setConnection(this.connection);
	}

	public void execute()
	{
		StatementRunnerResult result;
		String sql;
		for (int i=0; i < this.statements.size(); i++)
		{
			sql = (String)this.statements.get(i);
			try
			{
				this.stmtRunner.runStatement(sql, 0);
				result = this.stmtRunner.getResult();
				if (result.hasMessages())
				{
					String[] msg = result.getMessages();
					for (int m=0; m < msg.length; m++)
					{
						LogMgr.logInfo("BatchRunner", msg[m]);
					}
				}
			}
			catch (Throwable e)
			{
				LogMgr.logError("BatchRunner", "Error executing " + sql, e);
			}
		}
	}
	
}
