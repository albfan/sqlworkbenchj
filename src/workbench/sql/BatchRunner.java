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
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

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
	private String successScript;
	private String errorScript;
	private String delimiter = ";";
	
	public BatchRunner(String aFilelist)
	{
		this.files = StringUtil.stringToList(aFilelist, ",");
	}

	public void setProfile(String aProfilename)
		throws Exception
	{
		LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MgsBatchConnecting") + " [" + aProfilename + "]");
		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		ConnectionProfile prof = mgr.getProfile(aProfilename);
		this.connection = mgr.getConnection(prof, "BatchRunner");
		this.stmtRunner = new StatementRunner();
		this.stmtRunner.setConnection(this.connection);
		LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchConnectOk"));
	}

	public void setSuccessScript(String aFilename)
	{
		this.successScript = aFilename;
	}
	
	public void setErrorScript(String aFilename)
	{
		this.errorScript = aFilename;
	}
	
	public void execute()
		throws IOException
	{
		String file = null;
		boolean error = false;
		WbStringTokenizer reader = new WbStringTokenizer(";",false, "\"'", true);
		
		for (int i=0; i < this.files.size(); i++)
		{
			file = (String)this.files.get(i);
			try
			{
				LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchProcessingFile") + " " + file);
				reader.setSourceFile(file);
				error = this.executeScript(reader);
				LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchProcessingFileDone") + " " + file);
			}
			catch (Exception e)
			{
				error = true;
				LogMgr.logError("BatchRunner", ResourceMgr.getString("MsgBatchScriptFileError") + " " + file, e);
			}
			if (error && abortOnError)
			{
				break;
			}
		}
		
		if (abortOnError && error)
		{
			try
			{
				if (this.errorScript != null)
				{
					LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchExecutingErrorScript") + this.errorScript);
					reader.setSourceFile(this.errorScript);
					this.executeScript(reader);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner.execute()", ResourceMgr.getString("MsgBatchScriptFileError") + this.errorScript, e);
			}
		}
		else 
		{
			try
			{
				if (this.successScript != null)
				{
					LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchExecutingSuccessScript") + this.successScript);
					reader.setSourceFile(this.successScript);
					this.executeScript(reader);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner.execute()", ResourceMgr.getString("MsgBatchScriptFileError") + this.successScript, e);
			}
		}
	}

	
	private boolean executeScript(WbStringTokenizer reader)
	{
		boolean error = false;
		StatementRunnerResult result = null;
		
		String sql = null;
		while (reader.hasMoreTokens())
		{
			sql = reader.nextToken();
			if (sql == null) continue;
			sql = sql.trim();
			if (sql.length() == 0) continue;
			
			try
			{
				LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchExecutingStatement") + " "  + sql);
				this.stmtRunner.runStatement(sql, 0);
				result = this.stmtRunner.getResult();
				if (result.hasMessages())
				{
					String[] msg = result.getMessages();
					for (int m=0; m < msg.length; m++)
					{
						if (msg[m] != null && msg[m].length() > 0)
							LogMgr.logInfo("BatchRunner", msg[m]);
					}
				}
				if (!result.isSuccess()) 
				{
					error = true;
				}
			}
			catch (Throwable e)
			{
				LogMgr.logError("BatchRunner", ResourceMgr.getString("MsgBatchStatementError") + " "  + sql, e);
				error = true;
				break;
			}
			if (error && abortOnError) break; 
		}
		return error;
	}
	
	public void done()
	{
		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		mgr.disconnectAll();
	}
	
	public void setAbortOnError(boolean aFlag)
	{
		this.abortOnError = aFlag;
	}
	
	public static void main(String[] args)
	{
		//WbManager.getInstance().initForBatch();
		BatchRunner runner = new BatchRunner("d:/projects/java/jworkbench/sql/test.sql");
		try
		{
			runner.setProfile("HSQLDB - Test Server");
			runner.execute();
			//runner.done();
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
		WbManager.getInstance().exitWorkbench();
	}
	
}
