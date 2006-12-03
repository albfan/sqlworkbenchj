/*
 * WbDiff.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
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
	public static final String PARAM_SOURCEPROFILE = "referenceprofile";
	public static final String PARAM_SOURCEPROFILE_GROUP = "referencegroup";
	public static final String PARAM_TARGETPROFILE = "targetprofile";
	public static final String PARAM_TARGETPROFILE_GROUP = "targetgroup";
	
	public static final String PARAM_FILENAME = "file";
	public static final String PARAM_ENCODING = "encoding";
	public static final String PARAM_NAMESPACE = "namespace";

	public static final String PARAM_SOURCETABLES = "referencetables";
	public static final String PARAM_TARGETTABLES = "targettables";

	public static final String PARAM_SOURCESCHEMA = "referenceschema";
	public static final String PARAM_TARGETSCHEMA = "targetschema";

	public static final String PARAM_EXCLUDE_TABLES = "excludetables";

	public static final String PARAM_INCLUDE_INDEX = "includeindex";
	public static final String PARAM_INCLUDE_FK = "includeforeignkeys";
	public static final String PARAM_INCLUDE_PK = "includeprimarykeys";
	public static final String PARAM_INCLUDE_CONSTRAINTS = "includeconstraints";
	public static final String PARAM_INCLUDE_VIEWS = "includeviews";
	public static final String PARAM_INCLUDE_PROCS = "includeprocs";
	public static final String PARAM_DIFF_JDBC_TYPES = "usejdbctypes";
	
	private ArgumentParser cmdLine;
	private SchemaDiff diff;

	public WbSchemaDiff()
	{
		cmdLine = new ArgumentParser();
		cmdLine.addArgument(PARAM_SOURCEPROFILE);
		cmdLine.addArgument(PARAM_SOURCEPROFILE_GROUP);
		cmdLine.addArgument("sourceprofile"); // old name of the parameter
		cmdLine.addArgument(PARAM_TARGETPROFILE);
		cmdLine.addArgument(PARAM_TARGETPROFILE_GROUP);
		cmdLine.addArgument(PARAM_FILENAME);
		cmdLine.addArgument(PARAM_ENCODING);
		cmdLine.addArgument(PARAM_SOURCETABLES);
		cmdLine.addArgument(PARAM_TARGETTABLES);
		cmdLine.addArgument(PARAM_SOURCESCHEMA);
		cmdLine.addArgument(PARAM_TARGETSCHEMA);
		cmdLine.addArgument(PARAM_NAMESPACE);
		cmdLine.addArgument(PARAM_INCLUDE_FK);
		cmdLine.addArgument(PARAM_INCLUDE_PK);
		cmdLine.addArgument(PARAM_INCLUDE_INDEX);
		cmdLine.addArgument(PARAM_EXCLUDE_TABLES);
		cmdLine.addArgument(PARAM_INCLUDE_CONSTRAINTS);
		cmdLine.addArgument(PARAM_INCLUDE_VIEWS);
		cmdLine.addArgument(PARAM_INCLUDE_PROCS);
		cmdLine.addArgument(PARAM_DIFF_JDBC_TYPES);
		//cmdLine.addArgument(PARAM_INCLUDE_COMMENTS);
	}

	public String getVerb() { return VERB; }
	protected boolean isConnectionRequired() { return false; }

	public StatementRunnerResult execute(WbConnection aConnection, String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();

		String verb = SqlUtil.getSqlVerb(sql);
		if ("wbdiff".equalsIgnoreCase(verb))
		{
			result.addMessage("WbDiff has been renamed to WbSchemaDiff. Please use the new command instead.");
			result.addMessage("Support for 'WbDiff' will be removed in a future release.\n");
		}
		
		sql = stripVerb(SqlUtil.makeCleanSql(sql,false,false,'\''));
		
		try
		{
			cmdLine.parse(sql);
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrDiffWrongParameters"));
			result.setFailure();
			return result;
		}
		
		if (cmdLine.getArgumentCount() == 0)
		{
			result.addMessage(ResourceMgr.getString("ErrDiffWrongParameters"));
			result.setFailure();
			return result;
		}


		if (cmdLine.hasUnknownArguments())
		{
			List params = cmdLine.getUnknownArguments();
			StringBuilder msg = new StringBuilder(ResourceMgr.getString("ErrUnknownParameter") + " ");
			for (int i=0; i < params.size(); i++)
			{
				if (i > 0) msg.append(',');
				msg.append((String)params.get(i));
			}
			result.addMessage(msg.toString());
			result.addMessage("");
			result.addMessage(ResourceMgr.getString("ErrDiffWrongParameters"));
			result.setFailure();
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

		this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);

		if (targetProfile == null || (aConnection != null && aConnection.getProfile().isProfileForKey(targetKey)))
		{
			targetCon = aConnection;
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
				this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgDiffConnectingTarget"),-1,-1);
				targetCon = ConnectionMgr.getInstance().getConnection(targetKey, "Wb-Diff-Target");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrDiffCouldNotConnectTarget"));
				result.setFailure();
				return result;
			}
		}

		if (sourceProfile == null || (aConnection != null && aConnection.getProfile().isProfileForKey(sourceKey)))
		{
			sourceCon = aConnection;
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
				this.rowMonitor.setCurrentObject(ResourceMgr.getString("MsgDiffConnectingSource"),-1,-1);
				sourceCon = ConnectionMgr.getInstance().getConnection(sourceKey, "Wb-Diff-Source");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrDiffCouldNotConnectSource"));
				result.setFailure();
				// disconnect the target connection only if it was created by this command
				if (targetCon.getId().startsWith("Wb-Diff"))
				{
					try { targetCon.disconnect(); } catch (Throwable th) {}
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
		diff.setIncludeProcedures(cmdLine.getBoolean(PARAM_INCLUDE_PROCS, false));
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
						try { targetCon.disconnect(); } catch (Throwable th) {}
					}
					if (sourceCon.getId().startsWith("Wb-Diff"))
					{
						try { sourceCon.disconnect(); } catch (Throwable th) {}
					}
					return result;
				}
				if (excludeTables != null)
				{
					List l = StringUtil.stringToList(excludeTables, ",", true);
					diff.setExcludeTables(l);
				}
				diff.compareAll();
			}
			else
			{
				if (excludeTables != null)
				{
					List l = StringUtil.stringToList(excludeTables, ",", true);
					diff.setExcludeTables(l);
				}
				diff.setSchemas(refSchema, targetSchema);
			}
		}
		else if (tarTables == null)
		{
			List rl = StringUtil.stringToList(refTables, ",", true, true);
			for (int i = 0; i < rl.size(); i++)
			{
				String t = (String)rl.get(i);
				TableIdentifier tbl = new TableIdentifier(t);
				tbl.setType(this.currentConnection.getMetadata().getTableTypeName());
				rl.set(i, tbl);
			}
			diff.setTables(rl);
		}
		else
		{
			List rl = StringUtil.stringToList(refTables, ",", true, true);
			List tl = StringUtil.stringToList(tarTables, ",", true, true);
			if (rl.size() != tl.size())
			{
				result.addMessage(ResourceMgr.getString("ErrDiffTableListNoMatch"));
				result.setFailure();
				return result;
			}
			diff.setTables(rl, tl);
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
			if (!diff.isCancelled()) diff.writeXml(out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
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
