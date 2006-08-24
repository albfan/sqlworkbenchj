/*
 * WbSchemaReport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbSchemaReport
	extends SqlCommand
	implements RowActionMonitor
{
	public static final String VERB = "WBREPORT";
	private SchemaReporter reporter;
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
		cmdLine.addArgument("format");
		cmdLine.addArgument("useschemaname");
		cmdLine.addArgument("includeprocedures");
		cmdLine.addArgument("includetables");
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT);
	}

	public String getVerb() { return VERB; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		boolean dbDesigner = false;
		StatementRunnerResult result = new StatementRunnerResult();
		this.currentConnection = aConnection;
		aSql = stripVerb(aSql);
//		aSql = SqlUtil.makeCleanSql(aSql, false, '"');
//		int pos = aSql.indexOf(' ');
//		if (pos > -1)
//			aSql = aSql.substring(pos);
//		else
//			aSql = "";

		try
		{
			cmdLine.parse(aSql);
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			result.setFailure();
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			List params = cmdLine.getUnknownArguments();
			StringBuffer msg = new StringBuffer(ResourceMgr.getString("ErrUnknownParameter"));
			for (int i=0; i < params.size(); i++)
			{
				msg.append((String)params.get(i));
				if (i > 0) msg.append(',');
			}
			result.addMessage(msg.toString());
			result.addMessage(ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			result.setFailure();
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
		if (format == null || format.length() == 0) format = "xml";

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
		this.reporter = new SchemaReporter(aConnection);
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

		this.reporter.setIncludeTables(cmdLine.getBoolean("includetables", true));
		this.reporter.setIncludeProcedures(cmdLine.getBoolean("includeprocedures", false));
		
		if (aConnection.getMetadata().isOracle())
		{
			// check if remarksReporting is turned on for Oracle, if not issue a warning.
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
