/*
 * Created on December 9, 2002, 2:01 PM
 */
package workbench.sql;

import java.io.IOException;
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
	private List files;
	private StatementRunner stmtRunner;
	private WbConnection connection;
	private boolean abortOnError = false;
	
	public BatchRunner(String aFilelist)
	{
		this.files = StringUtil.stringToList(aFilelist, ",");
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
		String file;
		boolean error = false;
		for (int i=0; i < this.files.size(); i++)
		{
			if (error && abortOnError) break; 
			file = (String)this.files.get(i);
			try
			{
				ScriptReader reader = new ScriptReader(file);
				sql = reader.getNextStatement();
				while (sql != null)
				{
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
						error = false;
						break;
					}
				}
				try { reader.close(); } catch (Throwable th) {}
			}
			catch (IOException e)
			{
				LogMgr.logError("BatchRunner", "Error reading script file " + file, e);
				break;
			}
		}
	}
	
	public void setAbortOnError(boolean aFlag)
	{
		this.abortOnError = aFlag;
	}
	
}
