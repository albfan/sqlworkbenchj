/*
 * Created on December 9, 2002, 2:01 PM
 */
package workbench.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
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
	private boolean showResultSets = false;
	
	public BatchRunner(String aFilelist)
	{
		this.files = StringUtil.stringToList(aFilelist, ",");
	}

	public void showResultSets(boolean flag)
	{
		this.showResultSets = flag;
	}
	
	public void setProfile(String aProfilename)
		throws Exception
	{
		LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchConnecting") + " [" + aProfilename + "]");
		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		ConnectionProfile prof = mgr.getProfile(aProfilename);
		if (prof == null)
		{
			LogMgr.logError("BatchRunner", ResourceMgr.getString("ErrorConnectionError"),null);
			throw new IllegalArgumentException("Could not find profile " + aProfilename);
		}
		this.setProfile(prof);
	}

	public void setProfile(ConnectionProfile aProfile)
		throws Exception
	{
		if (aProfile == null)
		{
			LogMgr.logWarning("BatchRunner.setProfile()", "Called with a <null> profile!");
			return;
		}

		ConnectionMgr mgr = WbManager.getInstance().getConnectionMgr();
		this.connection = mgr.getConnection(aProfile, "BatchRunner");
		this.stmtRunner = new StatementRunner();
		this.stmtRunner.setConnection(this.connection);
		LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchConnectOk"));
	}

	public void setSuccessScript(String aFilename)
	{
		if (aFilename == null) return;
		File f = new File(aFilename);
		if (f.exists() && !f.isDirectory())
			this.successScript = aFilename;
		else
			this.successScript = null;
	}

	public void setErrorScript(String aFilename)
	{
		if (aFilename == null) return;
		File f = new File(aFilename);
		if (f.exists() && !f.isDirectory())
			this.errorScript = aFilename;
		else
			this.errorScript = null;
	}

	private String readFile(String aFilename)
	{
		BufferedReader in = null;
		StringBuffer content = null;
		try
		{
			File f = new File(aFilename);
			content = new StringBuffer((int)f.length());
			in = new BufferedReader(new FileReader(f));
			String line = in.readLine();
			while (line != null)
			{
				content.append(line);
				content.append('\n');
				line = in.readLine();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("BatchRunner.readFile()", "Error reading file " + aFilename, e);
			content = new StringBuffer();
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return content.toString();
	}
	public void execute()
		throws IOException
	{
		String file = null;
		boolean error = false;
		//WbStringTokenizer reader = new WbStringTokenizer(";",false, "\"'", true);

		for (int i=0; i < this.files.size(); i++)
		{
			file = (String)this.files.get(i);
			try
			{
				LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchProcessingFile") + " " + file);
				//reader.setSourceFile(file);
				//error = this.executeScript(reader);
				String script = this.readFile(file);
				error = this.executeScript(script);
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
					LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchExecutingErrorScript") + " " + this.errorScript);
					//reader.setSourceFile(this.errorScript);
					String errorScript = this.readFile(this.errorScript);
					this.executeScript(errorScript);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner.execute()", ResourceMgr.getString("MsgBatchScriptFileError") + " " + this.errorScript, e);
			}
		}
		else
		{
			try
			{
				if (this.successScript != null)
				{
					LogMgr.logInfo("BatchRunner", ResourceMgr.getString("MsgBatchExecutingSuccessScript") + " " + this.successScript);
					//reader.setSourceFile(this.successScript);
					String script = this.readFile(this.successScript);
					this.executeScript(script);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("BatchRunner.execute()", ResourceMgr.getString("MsgBatchScriptFileError") + " " + this.successScript, e);
			}
		}
	}


	//private boolean executeScript(WbStringTokenizer reader)
	private boolean executeScript(String aScript)
	{
		boolean error = false;
		StatementRunnerResult result = null;
		ScriptParser parser = new ScriptParser();
		parser.setAlternateDelimiter(WbManager.getSettings().getAlternateDelimiter());
		parser.setScript(aScript);
		List statements = parser.getCommands();
		String sql = null;
		int count = statements.size();
		for (int i=0; i < count; i++)
		{
			sql = (String)statements.get(i);
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
							System.out.println(msg[m]);
					}
				}
				if (this.showResultSets && result.isSuccess() && result.hasDataStores())
				{
					System.out.println();
					System.out.println(sql);
					System.out.println("---------------- " + ResourceMgr.getString("MsgResultLogStart") + " ----------------------------");
					DataStore[] data = result.getDataStores();
					for (int nr=0; nr < data.length; nr++)
					{
						System.out.println(data[nr].getDataString(StringUtil.LINE_TERMINATOR, true));
					}
					System.out.println("---------------- " + ResourceMgr.getString("MsgResultLogEnd") + " ----------------------------");
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

}