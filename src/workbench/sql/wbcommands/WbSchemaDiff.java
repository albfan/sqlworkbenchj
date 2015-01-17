/*
 * WbSchemaDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;

import workbench.storage.RowActionMonitor;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.XsltTransformer;

import static workbench.sql.wbcommands.WbXslt.*;

/**
 * @author  Thomas Kellerer
 */
public class WbSchemaDiff
	extends SqlCommand
{
	public static final String VERB = "WbSchemaDiff";

	public static final String ARG_NAMESPACE = "namespace";
	public static final String ARG_INCLUDE_INDEX = "includeIndex";
	public static final String ARG_INCLUDE_FK = "includeForeignKeys";
	public static final String ARG_INCLUDE_PK = "includePrimaryKeys";
	public static final String ARG_INCLUDE_CONSTRAINTS = "includeConstraints";
	public static final String ARG_INCLUDE_VIEWS = "includeViews";
	public static final String ARG_DIFF_JDBC_TYPES = "useJdbcTypes";
	public static final String ARG_VIEWS_AS_TABLES = "viewAsTable";
	public static final String ARG_COMPARE_CHK_CONS_BY_NAME = "useConstraintNames";
	public static final String ARG_ADD_TYPES = "additionalTypes";

	private SchemaDiff diff;
	private CommonDiffParameters diffParams;

	public WbSchemaDiff()
	{
		super();
		cmdLine = new ArgumentParser();
		diffParams = new CommonDiffParameters(cmdLine, getBaseDir());
		cmdLine.addArgument(ARG_NAMESPACE);
		cmdLine.addArgument(ARG_INCLUDE_FK, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_SEQUENCES, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_PK, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_INDEX, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_CONSTRAINTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_VIEWS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_PARTITIONS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_PROCS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_GRANTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_TRIGGERS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_DIFF_JDBC_TYPES, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_VIEWS_AS_TABLES, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET, ArgumentType.Filename);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT, ArgumentType.Filename);
		cmdLine.addArgument(WbXslt.ARG_PARAMETERS, ArgumentType.Repeatable);
		cmdLine.addArgument(ARG_COMPARE_CHK_CONS_BY_NAME, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_ADD_TYPES, ArgumentType.ListArgument);
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	@Override
	public StatementRunnerResult execute(final String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		cmdLine.parse(getCommandLine(sql));

		if (cmdLine.getArgumentCount() == 0)
		{
			result.addMessage(ResourceMgr.getString("ErrDiffWrongParameters"));
			result.setFailure();
			return result;
		}

		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrDiffWrongParameters"));
			return result;
		}

		if (this.rowMonitor != null) this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
		diffParams.setMonitor(rowMonitor);

		WbConnection targetCon = diffParams.getTargetConnection(currentConnection, result);
		if (!result.isSuccess()) return result;

		WbConnection referenceConnection = diffParams.getSourceConnection(currentConnection, result);

		if (referenceConnection == null && targetCon != null && targetCon != currentConnection)
		{
			targetCon.disconnect();
			return result;
		}

		if (!result.isSuccess()) return result;

		if (isCancelled)
		{
			result.setWarning(true);
			result.addMessage(ResourceMgr.getString("MsgDiffCancelled"));
			return result;
		}

		diff = new SchemaDiff(referenceConnection, targetCon);
		diff.setMonitor(this.rowMonitor);

		// this needs to be set before the tables are defined!
		diff.setIncludeForeignKeys(cmdLine.getBoolean(ARG_INCLUDE_FK, true));
		diff.setIncludeIndex(cmdLine.getBoolean(ARG_INCLUDE_INDEX, true));
		diff.setIncludePrimaryKeys(cmdLine.getBoolean(ARG_INCLUDE_PK, true));
		diff.setIncludeTableConstraints(cmdLine.getBoolean(ARG_INCLUDE_CONSTRAINTS, true));
		diff.setIncludeViews(cmdLine.getBoolean(ARG_INCLUDE_VIEWS, true));
		diff.setCompareJdbcTypes(cmdLine.getBoolean(ARG_DIFF_JDBC_TYPES, false));
		diff.setIncludeProcedures(cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_PROCS, false));
		diff.setIncludeTableGrants(cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_GRANTS, false));
		diff.setIncludeSequences(cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_SEQUENCES, false));
		diff.setTreatViewAsTable(cmdLine.getBoolean(ARG_VIEWS_AS_TABLES, false));
		diff.setCompareConstraintsByName(cmdLine.getBoolean(ARG_COMPARE_CHK_CONS_BY_NAME, true));
		diff.setIncludeTriggers(cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_TRIGGERS, true));
		diff.setIncludePartitions(cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_PARTITIONS, false));
		List<String> types = cmdLine.getListValue(ARG_ADD_TYPES);
		diff.setAdditionalTypes(types);

		String refTables = cmdLine.getValue(CommonDiffParameters.PARAM_REFERENCETABLES);
		String tarTables = cmdLine.getValue(CommonDiffParameters.PARAM_TARGETTABLES);

		// Setting the tables to be excluded must be done before setting any other table selection
		String excludeTables = cmdLine.getValue(CommonDiffParameters.PARAM_EXCLUDE_TABLES);
		if (excludeTables != null)
		{
			List<String> l = StringUtil.stringToList(excludeTables, ",", true, true);
			diff.setExcludeTables(l);
		}

		String refSchema = cmdLine.getValue(CommonDiffParameters.PARAM_REFERENCESCHEMA);
		String targetSchema = cmdLine.getValue(CommonDiffParameters.PARAM_TARGETSCHEMA);

		if (refTables == null)
		{
			if (refSchema == null && targetSchema == null)
			{
				if (referenceConnection == targetCon)
				{
					result.addMessage(ResourceMgr.getString("ErrDiffSameConnectionNoTableSelection"));
					result.setFailure();
					if (targetCon.getId().startsWith("Wb-Diff"))
					{
						try { targetCon.disconnect(); } catch (Exception th) {}
					}
					if (referenceConnection.getId().startsWith("Wb-Diff"))
					{
						try { referenceConnection.disconnect(); } catch (Exception th) {}
					}
					return result;
				}
				diff.compareAll();
			}
			else
			{
				diff.setSchemas(refSchema, targetSchema);
			}
		}
		else if (tarTables == null)
		{
			SourceTableArgument parms = new SourceTableArgument(refTables, referenceConnection);
			List<TableIdentifier> tables = new ArrayList<>();
			for (TableIdentifier tbl : parms.getTables())
			{
				TableIdentifier realTable = referenceConnection.getMetadata().findTable(tbl, false);
				if (realTable != null)
				{
					tables.add(realTable);
				}
			}
			diff.setTables(tables);
			diff.setSchemaNames(refSchema, targetSchema);
		}
		else
		{
			List<String> rl = StringUtil.stringToList(refTables, ",", true, true);
			List<String> tl = StringUtil.stringToList(tarTables, ",", true, true);
			if (rl.size() != tl.size())
			{
				result.addMessage(ResourceMgr.getString("ErrDiffTableListNoMatch"));
				result.setFailure();
				return result;
			}
			diff.setTableNames(rl, tl);
			diff.setSchemaNames(refSchema, targetSchema);
		}

		if (isCancelled || diff.isCancelled())
		{
			result.setWarning(true);
			result.addMessage(ResourceMgr.getString("MsgDiffCancelled"));
			return result;
		}

		Writer out = null;
		boolean outputToConsole = false;
		WbFile output = evaluateFileArgument(cmdLine.getValue(CommonDiffParameters.PARAM_FILENAME));

		try
		{
			if (output == null)
			{
				out = new StringWriter(5000);
				outputToConsole = true;
			}
			else
			{
				String encoding = cmdLine.getValue(CommonArgs.ARG_ENCODING, "UTF-8");
				encoding = EncodingUtil.cleanupEncoding(encoding);
				diff.setEncoding(encoding);
				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), encoding), 256*1024);
			}

			// this will start the actual diff process
			if (!diff.isCancelled())
			{
				diff.writeXml(out);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbSchemaDiff.execute()", "Error writing output file", e);
		}
		finally
		{
			FileUtil.closeQuietely(out);
			if (referenceConnection.getId().startsWith("Wb-Diff"))
			{
				referenceConnection.disconnect();
			}
			if (targetCon.getId().startsWith("Wb-Diff"))
			{
				targetCon.disconnect();
			}
		}

		if (diff.isCancelled())
		{
			result.addMessage(ResourceMgr.getString("MsgDiffCancelled"));
		}
		else
		{
			if (outputToConsole)
			{
				result.addMessage(out.toString());
			}
			else
			{
				String msg = ResourceMgr.getString("MsgDiffFileWritten") + " " + output.getFullPath();
				result.addMessage(msg);

				File  xslt = evaluateFileArgument(cmdLine.getValue(WbXslt.ARG_STYLESHEET));
				File xsltOutput = evaluateFileArgument(cmdLine.getValue(WbXslt.ARG_OUTPUT));
				Map<String, String> xsltParams = cmdLine.getMapValue(ARG_PARAMETERS);

				if (xslt != null && xsltOutput != null)
				{
					XsltTransformer transformer = new XsltTransformer();
					try
					{
						transformer.setXsltBaseDir(new File(getBaseDir()));
						transformer.transform(output, xsltOutput, xslt, xsltParams);
						String xsltMsg = transformer.getAllOutputs();
						if (xsltMsg.length() != 0)
						{
							result.addMessage(xsltMsg);
							result.addMessage(""); // create newline
						}
						result.addMessage(ResourceMgr.getFormattedString("MsgXsltSuccessful", xsltOutput));
						result.setSuccess();
					}
					catch (FileNotFoundException fnf)
					{
						LogMgr.logError("WbSchemaDiff.execute()", "Stylesheet " + xslt + " not found!", fnf);
						result.addMessage(ResourceMgr.getFormattedString("ErrXsltNotFound", xslt));
						result.setFailure();
					}
					catch (Exception e)
					{
						LogMgr.logError("WbSchemaReport.execute()", "Error when transforming '" + output.getFullPath() + "' to '" + xsltOutput + "' using " + xslt, e);
						result.addMessage(transformer.getAllOutputs(e));
						result.setFailure();
					}
				}
			}
		}
		return result;
	}

	@Override
	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.diff != null) this.diff.cancel();
	}

	@Override
	public boolean isWbCommand()
	{
		return true;
	}
}
