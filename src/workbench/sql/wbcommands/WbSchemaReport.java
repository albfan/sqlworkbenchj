package workbench.sql.wbcommands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import workbench.db.ConnectionProfile;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.report.SchemaReporter;
import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbSchemaReport 
	extends SqlCommand
	implements ScriptGenerationMonitor
{
	public static final String VERB = "WBREPORT";
	private SchemaReporter reporter;
	private ScriptGenerationMonitor scriptMonitor;
	private ArgumentParser cmdLine;
	private int currentTable = 0;
	
	public WbSchemaReport()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("types");
		cmdLine.addArgument("file");
		cmdLine.addArgument("namespace");
		cmdLine.addArgument("tables");
		cmdLine.addArgument("schemas");
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT);
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(aSql);
		this.currentConnection = aConnection;
		aSql = SqlUtil.makeCleanSql(aSql, false, '"');
		int pos = aSql.indexOf(' ');
		if (pos > -1)
			aSql = aSql.substring(pos);
		else
			aSql = "";

		try
		{
			cmdLine.parse(aSql);
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrorSchemaReportWrongParameters"));
			result.setFailure();
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			List params = cmdLine.getUnknownArguments();
			StringBuffer msg = new StringBuffer(ResourceMgr.getString("ErrorUnknownParameter"));
			for (int i=0; i < params.size(); i++)
			{
				msg.append((String)params.get(i));
				if (i > 0) msg.append(',');
			}
			result.addMessage(msg.toString());
			result.addMessage(ResourceMgr.getString("ErrorSchemaReportWrongParameters"));
			result.setFailure();
			return result;
		}

		String file = cmdLine.getValue("file");

		if (file == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorSchemaReportWrongParameters"));
			result.setFailure();
			return result;
		}

		file = StringUtil.trimQuotes(file);
		
		String namespace = cmdLine.getValue("namespace");
		this.reporter = new SchemaReporter(aConnection);
		this.reporter.setNamespace(namespace);
		TableIdentifier[] tables = this.parseTables();
		if (tables != null)
		{
			this.reporter.setTableList(tables);
		}
		
		if (this.rowMonitor != null)
		{
			if (this.rowMonitor instanceof ScriptGenerationMonitor)
			{
				this.scriptMonitor = (ScriptGenerationMonitor)this.rowMonitor;
			}
			this.reporter.setProgressMonitor(this);
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS_TABLE);
		}
		
		if (aConnection.getMetadata().isOracle())
		{
			// check if remarksReporting is turned on for Oracle, if not
			// issue a warning.
			ConnectionProfile prof = aConnection.getProfile();
			Properties props = prof.getConnectionProperties();
			String value = "false";
			if (props != null)
			{
				value = props.getProperty("remarksReporting", "false");
			}
			if (!"true".equals(value))
			{
				result.addMessage(ResourceMgr.getString("MsgSchemaReporterOracleRemarksWarning"));
				result.addMessage("");
			}
		}
		this.currentTable = 0;
		this.reporter.setOutputFilename(file);
		
		try
		{
			this.reporter.writeXml();
			String msg = ResourceMgr.getString("MsgSchemaReportTablesWritten");
			msg = msg.replaceAll("%numtables%", Integer.toString(this.currentTable));
			result.addMessage(msg);
			result.setSuccess();
		}
		catch (IOException e)
		{
			result.setFailure();
			result.addMessage(e.getMessage());
		}
		
		return result;
	}

	private TableIdentifier[] parseTables()
	{
		String tables = this.cmdLine.getValue("tables");
		if (tables == null) return null;
		List l = StringUtil.stringToList(tables, ",");
		int count = l.size();
		TableIdentifier[] result = new TableIdentifier[count];
		
		for (int i=0; i < count; i++)
		{
			String table = (String)l.get(i);
			if (table == null) continue;
			if (table.trim().length() == 0) continue;
			table = this.currentConnection.getMetadata().adjustObjectname(table);
			result[i] = new TableIdentifier(table);
		}
		return result;
	}
	
	public void cancel()
		throws SQLException
	{
		if (this.reporter != null)
		{
			this.reporter.cancelExecution();
		}
	}
	
	public void currentObject(String anObject)
	{
		this.currentTable ++;
		if (this.scriptMonitor != null)
		{
			this.scriptMonitor.currentObject(anObject);
		}
		else if (this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentRow(currentTable, -1);
		}
	}
	
}