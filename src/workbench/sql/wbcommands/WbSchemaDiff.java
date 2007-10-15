/*
 * WbSchemaDiff.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.diff.SchemaDiff;
import workbench.gui.profiles.ProfileKey;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.SqlUtil;
import workbench.util.StrWriter;
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class WbSchemaDiff
	extends SqlCommand
{
	public static final String VERB = "WBSCHEMADIFF";
	public static final String PARAM_SOURCEPROFILE = "referenceProfile";
	public static final String PARAM_SOURCEPROFILE_GROUP = "referenceGroup";
	public static final String PARAM_TARGETPROFILE = "targetProfile";
	public static final String PARAM_TARGETPROFILE_GROUP = "targetGroup";
	
	public static final String PARAM_FILENAME = "file";
	public static final String PARAM_ENCODING = "encoding";
	public static final String PARAM_NAMESPACE = "namespace";

	public static final String PARAM_SOURCETABLES = "referenceTables";
	public static final String PARAM_TARGETTABLES = "targetTables";

	public static final String PARAM_SOURCESCHEMA = "referenceSchema";
	public static final String PARAM_TARGETSCHEMA = "targetSchema";

	public static final String PARAM_EXCLUDE_TABLES = "excludeTables";

	public static final String PARAM_INCLUDE_INDEX = "includeIndex";
	public static final String PARAM_INCLUDE_FK = "includeForeignKeys";
	public static final String PARAM_INCLUDE_PK = "includePrimaryKeys";
	public static final String PARAM_INCLUDE_CONSTRAINTS = "includeConstraints";
	public static final String PARAM_INCLUDE_VIEWS = "includeViews";
	public static final String PARAM_DIFF_JDBC_TYPES = "useJdbcTypes";
	
	private SchemaDiff diff;

	public WbSchemaDiff()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_SOURCEPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_SOURCEPROFILE_GROUP);
		cmdLine.addArgument(PARAM_TARGETPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_TARGETPROFILE_GROUP);
		cmdLine.addArgument(PARAM_FILENAME);
		cmdLine.addArgument(PARAM_ENCODING);
		cmdLine.addArgument(PARAM_SOURCETABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_TARGETTABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_SOURCESCHEMA);
		cmdLine.addArgument(PARAM_TARGETSCHEMA);
		cmdLine.addArgument(PARAM_NAMESPACE);
		cmdLine.addArgument(PARAM_INCLUDE_FK, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_SEQUENCES, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_PK, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_INDEX, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_EXCLUDE_TABLES, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_CONSTRAINTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_INCLUDE_VIEWS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_PROCS, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbSchemaReport.PARAM_INCLUDE_GRANTS, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_DIFF_JDBC_TYPES, ArgumentType.BoolArgument);
	}

	public String getVerb() { return VERB; }
	protected boolean isConnectionRequired() { return false; }

	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		sql = SqlUtil.stripVerb(sql);
		
		cmdLine.parse(sql);
		
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

		String filename = evaluateFileArgument(cmdLine.getValue(PARAM_FILENAME));

		String sourceProfile = cmdLine.getValue(PARAM_SOURCEPROFILE);
		if (sourceProfile == null) sourceProfile = cmdLine.getValue("sourceprofile"); // support old name
		
		String sourceGroup = cmdLine.getValue(PARAM_SOURCEPROFILE_GROUP);
		ProfileKey sourceKey = null;
		if (sourceProfile != null) sourceKey = new ProfileKey(sourceProfile, sourceGroup);
		
		String targetProfile = cmdLine.getValue(PARAM_TARGETPROFILE);
		String targetGroup = cmdLine.getValue(PARAM_TARGETPROFILE_GROUP);
		ProfileKey targetKey = null;
		if (targetProfile != null) targetKey = new ProfileKey(targetProfile, targetGroup);
		
		WbConnection targetCon = null;
		WbConnection sourceCon = null;

		if (this.rowMonitor != null) this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);

		if (targetProfile == null || (currentConnection != null && currentConnection.getProfile().isProfileForKey(targetKey)))
		{
			targetCon = currentConnection;
		}
		else
		{
			ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(targetKey);
			if (prof == null)
			{
				String msg = ResourceMgr.getString("ErrProfileNotFound");
				msg = StringUtil.replace(msg, "%profile%", targetKey.toString());
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
			try
			{
				if (this.rowMonitor != null) this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgDiffConnectingTarget"),-1,-1);
				targetCon = ConnectionMgr.getInstance().getConnection(targetKey, "Wb-Diff-Target");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrDiffCouldNotConnectTarget"));
				result.setFailure();
				return result;
			}
		}

		if (sourceProfile == null || (currentConnection != null && currentConnection.getProfile().isProfileForKey(sourceKey)))
		{
			sourceCon = currentConnection;
		}
		else
		{
			ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(sourceKey);
			if (prof == null)
			{
				String msg = ResourceMgr.getString("ErrProfileNotFound");
				msg = StringUtil.replace(msg, "%profile%", sourceKey.toString());
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
			
			try
			{
				if (this.rowMonitor != null) this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgDiffConnectingSource"),-1,-1);
				sourceCon = ConnectionMgr.getInstance().getConnection(sourceKey, "Wb-Diff-Source");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrDiffCouldNotConnectSource"));
				result.setFailure();
				// disconnect the target connection only if it was created by this command
				if (targetCon.getId().startsWith("Wb-Diff"))
				{
					try { targetCon.disconnect(); } catch (Exception th) {}
				}
				return result;
			}
		}

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

		String refTables = cmdLine.getValue(PARAM_SOURCETABLES);
		String tarTables = cmdLine.getValue(PARAM_TARGETTABLES);

		if (refTables == null)
		{
			String refSchema = cmdLine.getValue(PARAM_SOURCESCHEMA);
			String targetSchema = cmdLine.getValue(PARAM_TARGETSCHEMA);
			String excludeTables = cmdLine.getValue(PARAM_EXCLUDE_TABLES);

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
				if (excludeTables != null)
				{
					List<String> l = StringUtil.stringToList(excludeTables, ",", true);
					diff.setExcludeTables(l);
				}
				diff.compareAll();
			}
			else
			{
				if (excludeTables != null)
				{
					List<String> l = StringUtil.stringToList(excludeTables, ",", true);
					diff.setExcludeTables(l);
				}
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
		try
		{
			if (filename == null || filename.trim().length() == 0)
			{
				out = new StrWriter(5000);
				outputToConsole = true;
			}
			else
			{
				String encoding = cmdLine.getValue(PARAM_ENCODING);
				if (encoding == null)
				{
					encoding =  diff.getEncoding();
				}
				else
				{
					diff.setEncoding(encoding);
				}
				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), encoding), 256*1024);
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
				File f = new File(filename);
				String msg = ResourceMgr.getString("MsgDiffFileWritten") + " " + f.getAbsolutePath();
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
