/*
 * WbSchemaReport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
	public static final String ARG_EXCLUDE_TABLES = "excludeTableNames";
	public static final String ARG_EXCLUDE_OBJECTS = "excludeObjectNames";
	public static final String ARG_INCLUDE_TABLES = "includeTables";
	public static final String ARG_INCLUDE_PROCS = "includeProcedures";
	public static final String ARG_INCLUDE_PARTITIONS = "includePartitions";
	public static final String ARG_INCLUDE_GRANTS = "includeTableGrants";
	public static final String ARG_INCLUDE_SEQUENCES = "includeSequences";
	public static final String ARG_INCLUDE_TRIGGERS = "includeTriggers";
	public static final String ARG_INCLUDE_VIEWS = "includeViews";
	public static final String ARG_OBJECT_NAMES = "objects";
	public static final String ARG_OBJECT_TYPE_NAMES = "objectTypeNames";
	public static final String ARG_FULL_SOURCE = "writeFullSource";
	public static final String ARG_GENERATE_CONSTRAINT_NAMES = "generateConstraintNames";

	public static final String ALTERNATE_VERB = "WbReport";
	public static final String VERB = "WbSchemaReport";

	private SchemaReporter reporter;
	private int currentTable = 0;

	public WbSchemaReport()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(CommonArgs.ARG_TYPES, ArgumentType.ObjectTypeArgument);
		cmdLine.addArgument(CommonArgs.ARG_FILE, ArgumentType.Filename);
		cmdLine.addDeprecatedArgument(CommonArgs.ARG_TABLES, ArgumentType.StringArgument);
		cmdLine.addArgument(ARG_OBJECT_NAMES, ArgumentType.TableArgument);
		cmdLine.addArgument(ARG_EXCLUDE_OBJECTS, ArgumentType.TableArgument);
		cmdLine.addDeprecatedArgument(ARG_EXCLUDE_TABLES, ArgumentType.StringArgument);
		cmdLine.addArgument(ARG_OBJECT_TYPE_NAMES, ArgumentType.Repeatable);
		cmdLine.addArgument(CommonArgs.ARG_SCHEMAS);
		cmdLine.addArgument("reportTitle");
		cmdLine.addArgument("useSchemaName", ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_GENERATE_CONSTRAINT_NAMES, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_VIEWS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_PROCS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_TABLES, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_PARTITIONS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_GRANTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_SEQUENCES, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_TRIGGERS, ArgumentType.BoolArgument);
    WbXslt.addCommonXsltParameters(cmdLine);
		cmdLine.addArgument(ARG_FULL_SOURCE, ArgumentType.BoolSwitch);
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
    if (displayHelp(result))
    {
      return result;
    }

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			return result;
		}

		WbFile output = evaluateFileArgument(cmdLine.getValue(CommonArgs.ARG_FILE));

		if (output == null)
		{
			result.addErrorMessage(ResourceMgr.getString("ErrSchemaReportWrongParameters"));
			return result;
		}

		this.reporter = new SchemaReporter(currentConnection);
		boolean includeViews = cmdLine.getBoolean(ARG_INCLUDE_VIEWS, true);
		boolean includeSequences = cmdLine.getBoolean(ARG_INCLUDE_SEQUENCES, true);
		boolean includeTables = cmdLine.getBoolean(ARG_INCLUDE_TABLES, true);

		String title = cmdLine.getValue("reportTitle");
		this.reporter.setReportTitle(title);
    this.reporter.setGenerateConstraintNames(cmdLine.getBoolean(ARG_GENERATE_CONSTRAINT_NAMES, false));
		this.reporter.setIncludeProcedures(cmdLine.getBoolean(ARG_INCLUDE_PROCS, false));
    reporter.setCreateFullObjectSource(cmdLine.getBoolean(ARG_FULL_SOURCE, false));
		Set<String> types = CollectionUtil.caseInsensitiveSet();
		types.addAll(cmdLine.getListValue(CommonArgs.ARG_TYPES));

		String tableNames = this.cmdLine.getValue(ARG_OBJECT_NAMES, this.cmdLine.getValue(CommonArgs.ARG_TABLES));
		String exclude = cmdLine.getValue(ARG_EXCLUDE_OBJECTS, cmdLine.getValue(ARG_EXCLUDE_TABLES));
		String schemaNames = cmdLine.getValue(CommonArgs.ARG_SCHEMAS);
		List<String> typeFilter = cmdLine.getList(ARG_OBJECT_TYPE_NAMES);

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

		reporter.setIncludeTriggers(cmdLine.getBoolean(ARG_INCLUDE_TRIGGERS, true));
		reporter.setIncludePartitions(cmdLine.getBoolean(ARG_INCLUDE_PARTITIONS, false));

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
        if (isCancelled == false)
        {
          List<TableIdentifier> tables = tableArg.getTables();
          if (tables != null && tables.size() > 0)
          {
            reporter.setObjectList(tables);
          }
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
              if (isCancelled == false)
              {
                List<TableIdentifier> tables = tableArg.getTables();
                if (tables != null && tables.size() > 0)
                {
                  reporter.setObjectList(tables);
                }
              }
						}
					}
				}
				else
				{
					result.addWarning(ResourceMgr.getFormattedString("ErrIgnoringArg", filter, ARG_OBJECT_TYPE_NAMES));
				}
			}
		}

    if (isCancelled)
    {
      result.addWarningByKey("MsgStatementCancelled");
      return result;
    }

		// this is important for the retrieval of the stored procedures
		// which is the only thing the SchemaReporter retrieves
		reporter.setSchemas(schemas);

		String alternateSchema = cmdLine.getValue("useschemaname");
		reporter.setSchemaNameToUse(alternateSchema);

		this.reporter.setProgressMonitor(this);

		if (this.rowMonitor != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
		}

		this.reporter.setIncludeGrants(cmdLine.getBoolean(ARG_INCLUDE_GRANTS, false));

		if (currentConnection != null)
		{
			if (currentConnection.getMetadata().isOracle() && !OracleUtils.remarksEnabled(currentConnection))
			{
				result.addMessageByKey("MsgSchemaReporterOracleRemarksWarning");
				result.addMessageNewLine();
			}
			if (currentConnection.getMetadata().isMySql() && !OracleUtils.remarksEnabledMySQL(currentConnection))
			{
				result.addMessageByKey("MsgSchemaReporterMySQLRemarksWarning");
				result.addMessageNewLine();
			}
		}

		reporter.retrieveProcedures();

		if (reporter.getObjectCount() == 0)
		{
			result.addErrorMessageByKey("ErrNoTablesFound");
			return result;
		}

		// currentTable will be incremented as we have registered
		// this object as the RowActionMonitor of the SchemaReporter
		// see setCurrentObject()
		this.currentTable = 0;
		this.reporter.setOutputFilename(output.getFullPath());

		try
		{
			reporter.writeXml();
		}
		catch (IOException e)
		{
			result.addErrorMessage(e.getMessage());
		}

		WbFile xslt = evaluateFileArgument(cmdLine.getValue(WbXslt.ARG_STYLESHEET));
		WbFile xsltOutput = evaluateFileArgument(cmdLine.getValue(WbXslt.ARG_OUTPUT));

		if (result.isSuccess())
		{
			result.addMessageByKey("MsgSchemaReportTablesWritten", currentTable, output.getFullPath());
			result.setSuccess();
		}

		if (xslt != null && xsltOutput != null)
		{
			XsltTransformer transformer = new XsltTransformer();
      Map<String, String> params = WbXslt.getParameters(cmdLine);

			try
			{
				transformer.setXsltBaseDir(getXsltBaseDir());
				transformer.transform(output, xsltOutput, xslt, params);
				String msg = transformer.getAllOutputs();
				if (msg.length() != 0)
				{
					result.addMessage(msg);
					result.addMessageNewLine(); // create newline
				}
				result.addMessageByKey("MsgXsltSuccessful", xsltOutput);
				result.setSuccess();
			}
			catch (FileNotFoundException fnf)
			{
				LogMgr.logError("WbSchemaReport.execute()", "Stylesheet " + xslt + " not found!", fnf);
				result.addErrorMessageByKey("ErrXsltNotFound", xslt.toString());
			}
			catch (Exception e)
			{
				String msg = transformer.getAllOutputs(e);
				LogMgr.logError("WbSchemaReport.execute()", "Error when transforming '" + output.getFullPath() + "' to '" + xsltOutput + "' using " + xslt, e);
				LogMgr.logError("WbSchemaReport.execute()", msg, null);
				result.addErrorMessage(msg);
			}
		}
		return result;
	}

	@Override
	public void cancel()
		throws SQLException
	{
    super.cancel();
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
