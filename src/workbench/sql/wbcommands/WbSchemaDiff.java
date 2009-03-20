/*
 * WbSchemaDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.FileUtil;
import workbench.util.StrWriter;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.XsltTransformer;

/**
 * @author  support@sql-workbench.net
 */
public class WbSchemaDiff
	extends SqlCommand
{
	public static final String VERB = "WBSCHEMADIFF";

	public static final String ARG_NAMESPACE = "namespace";
	public static final String ARG_INCLUDE_INDEX = "includeIndex";
	public static final String ARG_INCLUDE_FK = "includeForeignKeys";
	public static final String ARG_INCLUDE_PK = "includePrimaryKeys";
	public static final String ARG_INCLUDE_CONSTRAINTS = "includeConstraints";
	public static final String ARG_INCLUDE_VIEWS = "includeViews";
	public static final String ARG_DIFF_JDBC_TYPES = "useJdbcTypes";
	public static final String ARG_VIEWS_AS_TABLES = "viewAsTable";
	public static final String ARG_COMPARE_CHK_CONS_BY_NAME = "useConstraintNames";

	private SchemaDiff diff;
	private CommonDiffParameters params;

	public WbSchemaDiff()
	{
		super();
		cmdLine = new ArgumentParser();
		params = new CommonDiffParameters(cmdLine);
		cmdLine.addArgument(ARG_NAMESPACE);
		cmdLine.addArgument(ARG_INCLUDE_FK, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_SEQUENCES, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_PK, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_INDEX, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_CONSTRAINTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_INCLUDE_VIEWS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_PROCS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_GRANTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_DIFF_JDBC_TYPES, ArgumentType.BoolArgument);
		cmdLine.addArgument(ARG_VIEWS_AS_TABLES, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbXslt.ARG_STYLESHEET);
		cmdLine.addArgument(WbXslt.ARG_OUTPUT);
		cmdLine.addArgument(ARG_COMPARE_CHK_CONS_BY_NAME, ArgumentType.BoolArgument);
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
		params.setMonitor(rowMonitor);

		WbConnection targetCon = params.getTargetConnection(currentConnection, result);
		if (!result.isSuccess()) return result;

		WbConnection referenceConnection = params.getSourceConnection(currentConnection, result);

		if (referenceConnection == null && targetCon != null && targetCon != currentConnection)
		{
			targetCon.disconnect();
			return result;
		}
		if (!result.isSuccess()) return result;

		this.diff = new SchemaDiff(referenceConnection, targetCon);
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
		//diff.setIncludeComments(cmdLine.getBoolean(PARAM_INCLUDE_COMMENTS, false));

		String refTables = cmdLine.getValue(CommonDiffParameters.PARAM_REFERENCETABLES);
		String tarTables = cmdLine.getValue(CommonDiffParameters.PARAM_TARGETTABLES);

		// Setting the tables to be excluded must be done before setting any other table selection
		String excludeTables = cmdLine.getValue(CommonDiffParameters.PARAM_EXCLUDE_TABLES);
		if (excludeTables != null)
		{
			List<String> l = StringUtil.stringToList(excludeTables, ",", true, true);
			diff.setExcludeTables(l);
		}


		if (refTables == null)
		{
			String refSchema = cmdLine.getValue(CommonDiffParameters.PARAM_REFERENCESCHEMA);
			String targetSchema = cmdLine.getValue(CommonDiffParameters.PARAM_TARGETSCHEMA);
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
			List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
			for (TableIdentifier tbl : parms.getTables())
			{
				TableIdentifier realTable = referenceConnection.getMetadata().findTable(tbl);
				if (realTable != null)
				{
					tables.add(realTable);
				}
			}
			diff.setTables(tables);
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
		}

		Writer out = null;
		boolean outputToConsole = false;
		WbFile output = evaluateFileArgument(cmdLine.getValue(CommonDiffParameters.PARAM_FILENAME));

		try
		{
			if (output == null)
			{
				out = new StrWriter(5000);
				outputToConsole = true;
			}
			else
			{
				String encoding = cmdLine.getValue(CommonDiffParameters.PARAM_ENCODING);
				if (encoding == null)
				{
					encoding = diff.getEncoding();
				}
				else
				{
					diff.setEncoding(encoding);
				}
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
			FileUtil.closeQuitely(out);
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

				String xslt = cmdLine.getValue(WbXslt.ARG_STYLESHEET);
				String xsltOutput = cmdLine.getValue(WbXslt.ARG_OUTPUT);

				if (!StringUtil.isEmptyString(xslt) && !StringUtil.isEmptyString(xsltOutput))
				{
					try
					{
						XsltTransformer transfomer = new XsltTransformer();
						transfomer.transform(output.getFullPath(), xsltOutput, xslt);
						result.addMessage(ResourceMgr.getFormattedString("MsgXsltSuccessful", xsltOutput));
						result.setSuccess();
					}
					catch (Exception e)
					{
						LogMgr.logError("WbSchemaReport.execute()", "Error when transforming '" + output.getFullPath() + "' to '" + xsltOutput + "' using " + xslt, e);
						result.addMessage(e.getMessage());
					}
				}
			}
		}
		return result;
	}

	@Override
	public void cancel()
	{
		if (this.diff != null) this.diff.cancel();
	}
}
