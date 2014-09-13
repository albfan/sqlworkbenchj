/*
 * WbSchemaReport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.SequenceReader;
import workbench.db.TableIdentifier;
import workbench.db.oracle.OracleUtils;
import workbench.db.report.SchemaReporter;

import workbench.storage.RowActionMonitor;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.XsltTransformer;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbSchemaReport
	extends SqlCommand
	implements RowActionMonitor
{
	public static final String PARAM_EXCLUDE_TABLES = "excludeTableNames";
	public static final String PARAM_EXCLUDE_OBJECTS = "excludeObjectNames";
	public static final String PARAM_INCLUDE_TABLES = "includeTables";
	public static final String PARAM_INCLUDE_PROCS = "includeProcedures";
	public static final String PARAM_INCLUDE_PARTITIONS = "includePartitions";
	public static final String PARAM_INCLUDE_GRANTS = "includeTableGrants";
	public static final String PARAM_INCLUDE_SEQUENCES = "includeSequences";
	public static final String PARAM_INCLUDE_TRIGGERS = "includeTriggers";
	public static final String PARAM_INCLUDE_VIEWS = "includeViews";
	public static final String PARAM_TABLE_NAMES = "tables";
	public static final String PARAM_OBJECT_NAMES = "objects";
	public static final String PARAM_OBJECT_TYPE_NAMES = "objectTypeNames";

	public static final String ALTERNATE_VERB = "WbReport";
	public static final String VERB = "WbSchemaReport";

	private SchemaReporter reporter;
	private int currentTable = 0;

	public WbSchemaReport()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_TYPES, ArgumentType.ObjectTypeArgument);
		cmdLine.addArgument("file");
		cmdLine.addArgument(PARAM_TABLE_NAMES, ArgumentType.Deprecated);
		cmdLine.addArgument(PARAM_OBJECT_NAMES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_EXCLUDE_OBJECTS, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_EXCLUDE_TABLES, ArgumentType.Deprecated);
		cmdLine.addArgument(PARAM_OBJECT_TYPE_NAMES, ArgumentType.Repeatable);
		cmdLine.addArgument(CommonArgs.ARG_SCHEMAS);
		cmdLine.addArgument("reportTitle");
		cmdLine.addArgument("useSchemaName", ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_VIEWS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_PROCS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_TABLES, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_PARTITIONS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_GRANTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_SEQUENCES, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_TRIGGERS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET, ArgumentType.Filename);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT);
		cmdLine.addArgument(WbXslt.ARG_PARAMETERS, ArgumentType.Repeatable);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	public StatementRunnerResult execute(final String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		cmdLine.parse(getCommandLine(sql));

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			return result;
		}

		WbFile output = evaluateFileArgument(cmdLine.getValue("file"));

		if (output == null)
		{
			result.addMessage(ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			result.setFailure();
			return result;
		}

		this.reporter = new SchemaReporter(currentConnection);
		boolean includeViews = cmdLine.getBoolean(PARAM_INCLUDE_VIEWS, true);
		boolean includeSequences = cmdLine.getBoolean(PARAM_INCLUDE_SEQUENCES, true);
		boolean includeTables = cmdLine.getBoolean(PARAM_INCLUDE_TABLES, true);

		String title = cmdLine.getValue("reportTitle");
		this.reporter.setReportTitle(title);
		this.reporter.setIncludeProcedures(cmdLine.getBoolean(PARAM_INCLUDE_PROCS, false));

		Set<String> types = CollectionUtil.caseInsensitiveSet();
		types.addAll(cmdLine.getListValue(CommonArgs.ARG_TYPES));

		String tableNames = this.cmdLine.getValue(PARAM_OBJECT_NAMES, this.cmdLine.getValue(PARAM_TABLE_NAMES));
		String exclude = cmdLine.getValue(PARAM_EXCLUDE_OBJECTS, cmdLine.getValue(PARAM_EXCLUDE_TABLES));
		String schemaNames = cmdLine.getValue(CommonArgs.ARG_SCHEMAS);
		List<String> typeFilter = cmdLine.getList(PARAM_OBJECT_TYPE_NAMES);

		if (types.isEmpty())
		{
			if (includeTables)
			{
				types.addAll(currentConnection.getMetadata().getTableTypes());
			}

			if (includeViews)
			{
				types.add(currentConnection.getMetadata().getViewTypeName());
			}

			if (includeSequences)
			{
				SequenceReader reader = currentConnection.getMetadata().getSequenceReader();
				if (reader != null)
				{
					// sequences are also retrieved by SourceTableArgument if provided
					// as it uses DbMetadata.getObjects()
					// by adding the type here, the sequences will also be selected using
					// potential wildcards specified through -tables
					types.add(reader.getSequenceTypeName());
				}
			}
		}


		String[] typesArray = StringUtil.toArray(types, true);

		reporter.setIncludeTriggers(cmdLine.getBoolean(PARAM_INCLUDE_TRIGGERS, true));
		reporter.setIncludePartitions(cmdLine.getBoolean(PARAM_INCLUDE_PARTITIONS, false));

		List<String> schemas = StringUtil.stringToList(schemaNames, ",");
		if (schemas.isEmpty())
		{
			schemas.add(currentConnection.getCurrentSchema());
		}

		if (this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgRetrievingTables"), -1, -1);
		}

		if (CollectionUtil.isEmpty(typeFilter))
		{
			for (String schema : schemas)
			{
				SourceTableArgument tableArg = new SourceTableArgument(tableNames, exclude, schema, typesArray, this.currentConnection);
				List<TableIdentifier> tables = tableArg.getTables();
				if (tables != null && tables.size() > 0)
				{
					this.reporter.setObjectList(tables);
				}
			}
		}
		else
		{
			for (String filter : typeFilter)
			{
				String[] def = filter.split(":");
				if (def != null && def.length == 2)
				{
					String type = def[0].toUpperCase();
					String[] typeNames = new String[] { type };
					String names = def[1];

					for (String schema : schemas)
					{
						if (type.equalsIgnoreCase("procedure"))
						{
							this.reporter.setProcedureNames(names);
						}
						else
						{
							SourceTableArgument tableArg = new SourceTableArgument(names, exclude, schema, typeNames, this.currentConnection);
							List<TableIdentifier> tables = tableArg.getTables();
							if (tables != null && tables.size() > 0)
							{
								this.reporter.setObjectList(tables);
							}
						}
					}
				}
				else
				{
					result.addMessage(ResourceMgr.getFormattedString("ErrIgnoringArg", filter, PARAM_OBJECT_TYPE_NAMES));
					result.setWarning(true);
				}
			}
		}

		// this is important for the retrieval of the stored procedures
		// which is the only thing the SchemaReporter retrieves
		reporter.setSchemas(schemas);

		String alternateSchema = cmdLine.getValue("useschemaname");
		this.reporter.setSchemaNameToUse(alternateSchema);

		this.reporter.setProgressMonitor(this);

		if (this.rowMonitor != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
		}

		this.reporter.setIncludeGrants(cmdLine.getBoolean(PARAM_INCLUDE_GRANTS, false));

		if (currentConnection != null)
		{
			if (currentConnection.getMetadata().isOracle() && !OracleUtils.remarksEnabled(currentConnection))
			{
				result.addMessage(ResourceMgr.getString("MsgSchemaReporterOracleRemarksWarning"));
				result.addMessage("");
			}
			if (currentConnection.getMetadata().isMySql() && !OracleUtils.remarksEnabledMySQL(currentConnection))
			{
				result.addMessage(ResourceMgr.getString("MsgSchemaReporterMySQLRemarksWarning"));
				result.addMessage("");
			}
		}

		reporter.retrieveProcedures();

		if (reporter.getObjectCount() == 0)
		{
			result.setFailure();
			result.addMessageByKey("ErrNoTablesFound");
			return result;
		}

		// currentTable will be incremented as we have registered
		// this object as the RowActionMonitor of the SchemaReporter
		// see setCurrentObject()
		this.currentTable = 0;
		this.reporter.setOutputFilename(output.getFullPath());

		try
		{
			this.reporter.writeXml();
		}
		catch (IOException e)
		{
			result.setFailure();
			result.addMessage(e.getMessage());
		}

		String xslt = cmdLine.getValue(WbXslt.ARG_STYLESHEET);
		String xsltOutput = cmdLine.getValue(WbXslt.ARG_OUTPUT);

		if (result.isSuccess())
		{
			String msg = ResourceMgr.getFormattedString("MsgSchemaReportTablesWritten", currentTable, output.getFullPath());
			result.addMessage(msg);
			result.setSuccess();
		}

		if (!StringUtil.isEmptyString(xslt) && !StringUtil.isEmptyString(xsltOutput))
		{
			XsltTransformer transformer = new XsltTransformer();
			Map<String, String> params = cmdLine.getMapValue(WbXslt.ARG_PARAMETERS);

			try
			{
				transformer.setXsltBaseDir(new File(getBaseDir()));
				transformer.transform(output.getFullPath(), xsltOutput, xslt, params);
				String msg = transformer.getAllOutputs();
				if (msg.length() != 0)
				{
					result.addMessage(msg);
					result.addMessage(""); // create newline
				}
				result.addMessage(ResourceMgr.getFormattedString("MsgXsltSuccessful", xsltOutput));
				result.setSuccess();
			}
			catch (FileNotFoundException fnf)
			{
				LogMgr.logError("WbSchemaReport.execute()", "Stylesheet " + xslt + " not found!", fnf);
				result.addMessage(ResourceMgr.getFormattedString("ErrXsltNotFound", xslt));
				result.setFailure();
			}
			catch (Exception e)
			{
				LogMgr.logError("WbSchemaReport.execute()", "Error when transforming '" + output.getFullPath() + "' to '" + xsltOutput + "' using " + xslt, e);
				String msg = transformer.getAllOutputs(e);
				LogMgr.logError("WbSchemaReport.execute()", msg, null);
				result.addMessage(msg);
				result.setFailure();
			}
		}
		return result;
	}

	@Override
	public void cancel()
		throws SQLException
	{
		if (this.reporter != null)
		{
			this.reporter.cancelExecution();
		}
	}

	@Override
	public void setCurrentObject(String anObject, long number, long total)
	{
		if (anObject == null)
		{
			this.currentTable = 0;
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
					((ScriptGenerationMonitor)this.rowMonitor).setCurrentObject(anObject, (int)number, (int)total);
				}

			}
		}
	}

	@Override
	public String getAlternateVerb()
	{
		return ALTERNATE_VERB;
	}

	@Override
	public void setCurrentRow(long number, long total)
	{
	}

	@Override
	public int getMonitorType()
	{
		return RowActionMonitor.MONITOR_PLAIN;
	}

	@Override
	public void setMonitorType(int aType)
	{
	}

	@Override
	public void jobFinished()
	{
	}

	@Override
	public void saveCurrentType(String type)
	{
	}

	@Override
	public void restoreType(String type)
	{
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
