/*
 * WbSchemaDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StrWriter;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * @author  support@sql-workbench.net
 */
public class WbSchemaDiff
	extends SqlCommand
{
	public static final String VERB = "WBSCHEMADIFF";
	
	public static final String PARAM_NAMESPACE = "namespace";

	public static final String PARAM_INCLUDE_INDEX = "includeIndex";
	public static final String PARAM_INCLUDE_FK = "includeForeignKeys";
	public static final String PARAM_INCLUDE_PK = "includePrimaryKeys";
	public static final String PARAM_INCLUDE_CONSTRAINTS = "includeConstraints";
	public static final String PARAM_INCLUDE_VIEWS = "includeViews";
	public static final String PARAM_DIFF_JDBC_TYPES = "useJdbcTypes";
	
	private SchemaDiff diff;
	private CommonDiffParameters params; 
	
	public WbSchemaDiff()
	{
		cmdLine = new ArgumentParser();
		params = new CommonDiffParameters(cmdLine);
		cmdLine.addArgument(PARAM_NAMESPACE);
		cmdLine.addArgument(PARAM_INCLUDE_FK, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_SEQUENCES, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_PK, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_INDEX, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_CONSTRAINTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_VIEWS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_PROCS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_GRANTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_DIFF_JDBC_TYPES, ArgumentType.BoolArgument);
	}

	public String getVerb() { return VERB; }
	protected boolean isConnectionRequired() { return false; }

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
		
		WbConnection sourceCon = params.getSourceConnection(currentConnection, result);

		if (sourceCon == null && targetCon != null && targetCon != currentConnection)
		{
			try
			{
				targetCon.disconnect();
			}
			catch (Exception th)
			{
			}
			return result;
		}
		if (!result.isSuccess()) return result;
		
		this.diff = new SchemaDiff(sourceCon, targetCon);
		diff.setMonitor(this.rowMonitor);

		// this needs to be set before the tables are defined!
		diff.setIncludeForeignKeys(cmdLine.getBoolean(PARAM_INCLUDE_FK, true));
		diff.setIncludeIndex(cmdLine.getBoolean(PARAM_INCLUDE_INDEX, true));
		diff.setIncludePrimaryKeys(cmdLine.getBoolean(PARAM_INCLUDE_PK, true));
		diff.setIncludeTableConstraints(cmdLine.getBoolean(PARAM_INCLUDE_CONSTRAINTS, true));
		diff.setIncludeViews(cmdLine.getBoolean(PARAM_INCLUDE_VIEWS, true));
		diff.setCompareJdbcTypes(cmdLine.getBoolean(PARAM_DIFF_JDBC_TYPES, false));
		diff.setIncludeProcedures(cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_PROCS, false));
		diff.setIncludeTableGrants(cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_GRANTS, false));
		diff.setIncludeSequences(cmdLine.getBoolean(WbSchemaReport.PARAM_INCLUDE_SEQUENCES, false));
		//diff.setIncludeComments(cmdLine.getBoolean(PARAM_INCLUDE_COMMENTS, false));

		String refTables = cmdLine.getValue(CommonDiffParameters.PARAM_REFERENCETABLES);
		String tarTables = cmdLine.getValue(CommonDiffParameters.PARAM_TARGETTABLES);

		if (refTables == null)
		{
			String refSchema = cmdLine.getValue(CommonDiffParameters.PARAM_REFERENCESCHEMA);
			String targetSchema = cmdLine.getValue(CommonDiffParameters.PARAM_TARGETSCHEMA);
			String excludeTables = cmdLine.getValue(CommonDiffParameters.PARAM_EXCLUDE_TABLES);
			if (excludeTables != null)
			{
				List<String> l = StringUtil.stringToList(excludeTables, ",", true, true);
				diff.setExcludeTables(l);
			}

			if (refSchema == null && targetSchema == null)
			{
				if (sourceCon == targetCon)
				{
					result.addMessage(ResourceMgr.getString("ErrDiffSameConnectionNoTableSelection"));
					result.setFailure();
					if (targetCon.getId().startsWith("Wb-Diff"))
					{
						try { targetCon.disconnect(); } catch (Exception th) {}
					}
					if (sourceCon.getId().startsWith("Wb-Diff"))
					{
						try { sourceCon.disconnect(); } catch (Exception th) {}
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
			List<String> rl = StringUtil.stringToList(refTables, ",", true, true);
			List<TableIdentifier> tables = new ArrayList<TableIdentifier>(rl.size());
			String ttype = this.currentConnection.getMetadata().getTableTypeName();
			for (String tname : rl)
			{
				TableIdentifier tbl = new TableIdentifier(tname);
				tbl.setType(ttype);
				tables.add(tbl);
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
			e.printStackTrace();
		}
		finally
		{
			try { out.close(); } catch (Exception th) {}
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
			}
		}
		return result;
	}

	public void cancel()
	{
		if (this.diff != null) this.diff.cancel();
	}
}
