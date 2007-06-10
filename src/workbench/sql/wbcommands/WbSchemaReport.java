/*
 * WbSchemaReport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import workbench.db.ConnectionProfile;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.report.SchemaReporter;
import workbench.db.report.Workbench2Designer;
import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbSchemaReport
	extends SqlCommand
	implements RowActionMonitor
{
	public static final String PARAM_INCLUDE_TABLES = "includeTables";
	public static final String PARAM_INCLUDE_PROCS = "includeProcedures";
	public static final String PARAM_INCLUDE_GRANTS = "includeTableGrants";
	
	public static final String VERB = "WBREPORT";
	private SchemaReporter reporter;
	private int currentTable = 0;

	public WbSchemaReport()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("types");
		cmdLine.addArgument("file");
		cmdLine.addArgument("namespace");
		cmdLine.addArgument("tables", ArgumentType.TableArgument);
		cmdLine.addArgument("schemas");
		cmdLine.addArgument("format", StringUtil.stringToList("wb,dbdesigner"));
		cmdLine.addArgument("useSchemaName", ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_PROCS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_TABLES, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_GRANTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT);
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		boolean dbDesigner = false;
		StatementRunnerResult result = new StatementRunnerResult();
		
		sql = SqlUtil.stripVerb(SqlUtil.makeCleanSql(sql,false,false,'\''));

		cmdLine.parse(sql);

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			return result;
		}

		String file = evaluateFileArgument(cmdLine.getValue("file"));

		if (file == null)
		{
			result.addMessage(ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			result.setFailure();
			return result;
		}

		file = StringUtil.trimQuotes(file);
		String format = cmdLine.getValue("format");
		if (StringUtil.isEmptyString(format)) format = "xml";

		if ("dbdesigner".equalsIgnoreCase(format))
		{
			dbDesigner = true;
		}
		else if (!"xml".equalsIgnoreCase(format) &&
		         !"wb".equalsIgnoreCase(format) &&
						 !"wbxml".equalsIgnoreCase(format))
		{
			result.addMessage(ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			result.setFailure();
			return result;
		}
		String namespace = cmdLine.getValue("namespace");
		this.reporter = new SchemaReporter(currentConnection);
		this.reporter.setNamespace(namespace);

		TableIdentifier[] tables = this.parseTables();
		if (tables != null)
		{
			this.reporter.setTableList(tables);
		}
		else
		{
			String arg = cmdLine.getValue("schemas");
			List schemas = StringUtil.stringToList(arg, ",");
			this.reporter.setSchemas(schemas);
		}

		String alternateSchema = cmdLine.getValue("useschemaname");
		this.reporter.setSchemaNameToUse(alternateSchema);

		if (this.rowMonitor != null)
		{
			this.reporter.setProgressMonitor(this);
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
		}

		this.reporter.setIncludeTables(cmdLine.getBoolean(PARAM_INCLUDE_TABLES, true));
		this.reporter.setIncludeProcedures(cmdLine.getBoolean(PARAM_INCLUDE_PROCS, false));
		this.reporter.setIncludeGrants(cmdLine.getBoolean(PARAM_INCLUDE_GRANTS, false));
		if (currentConnection.getMetadata().isOracle())
		{
			// check if remarksReporting is turned on for Oracle, if not issue a warning.
			ConnectionProfile prof = currentConnection.getProfile();
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
		String wbFile = file;
		if (dbDesigner)
		{
			File f = new File(file);
			String dir = f.getParent();
			String fname = f.getName();
			File nf = new File(dir, "__wb_" + fname);
			wbFile = nf.getAbsolutePath();
		}

		this.reporter.setOutputFilename(wbFile);

		try
		{
			this.reporter.writeXml();
			String msg = ResourceMgr.getString("MsgSchemaReportTablesWritten");
			msg = msg.replaceAll("%numtables%", Integer.toString(this.currentTable));
			if (!dbDesigner)
			{
				File f = new File(file);
				msg = StringUtil.replace(msg, "%filename%", f.getAbsolutePath());
				result.addMessage(msg);
			}
			result.setSuccess();
		}
		catch (IOException e)
		{
			result.setFailure();
			result.addMessage(e.getMessage());
		}

		if (dbDesigner && result.isSuccess())
		{
			File f = new File(wbFile);
			try
			{
				if (this.rowMonitor != null)
				{
					this.setCurrentObject(ResourceMgr.getString("MsgConvertReport2Designer"), -1, -1);
				}
				Workbench2Designer converter = new Workbench2Designer(f);
				converter.transformWorkbench2Designer();
				File output = new File(file);
				converter.writeOutputFile(output);
			}
			catch (Exception e)
			{
				result.setFailure();
				LogMgr.logError("WbSchemaReport.execute()", "Error generating DBDesigner file", e);
				String msg = ResourceMgr.getString("ErrGeneratingDbDesigner");
				msg = StringUtil.replace(msg, "%wbfile%", f.getAbsolutePath());
				msg = StringUtil.replace(msg, "%error%", ExceptionUtil.getDisplay(e));
				result.addMessage(msg);
			}

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
			table = this.currentConnection.getMetadata().adjustObjectnameCase(table);
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

	public void setCurrentObject(String anObject, long number, long total)
	{
		if (anObject == null)
		{
			this.currentTable = 0;
			return;
		}
		else
		{
			this.currentTable ++;
			if (this.rowMonitor != null)
			{
				if (number > 0)
				{
					this.rowMonitor.setCurrentObject(anObject, number, total);
				}
				else if (rowMonitor instanceof ScriptGenerationMonitor)
				{
					((ScriptGenerationMonitor)this.rowMonitor).setCurrentObject(anObject);
				}

			}
		}
	}

	public void setCurrentRow(long number, long total) {}
	public int getMonitorType() { return RowActionMonitor.MONITOR_PLAIN; }
	public void setMonitorType(int aType) {}
	public void jobFinished() {}
	public void saveCurrentType(String type) {}
	public void restoreType(String type) {}
	
}
