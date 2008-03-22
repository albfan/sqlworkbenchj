/*
 * WbCopy.java
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

import java.sql.SQLException;
import java.util.List;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.profiles.ProfileKey;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;

/**
 * A command to copy data from one DBMS to another. This is the commandline
 * version of the DataPumper.
 * @author  support@sql-workbench.net
 */
public class WbCopy
	extends SqlCommand
{
	public static final String VERB = "WBCOPY";

	public static final String PARAM_SOURCETABLE = "sourceTable";
	public static final String PARAM_SOURCEQUERY = "sourceQuery";
	public static final String PARAM_TARGETTABLE = "targetTable";
	public static final String PARAM_SOURCEPROFILE = "sourceProfile";
	public static final String PARAM_SOURCEPROFILE_GROUP = "sourceGroup";
	public static final String PARAM_TARGETPROFILE = "targetProfile";
	public static final String PARAM_TARGETPROFILE_GROUP = "targetGroup";
	public static final String PARAM_COLUMNS = "columns";
	public static final String PARAM_SOURCEWHERE = "sourceWhere";
	public static final String PARAM_DELETETARGET = "deleteTarget";
	public static final String PARAM_KEYS = "keyColumns";
	public static final String PARAM_DROPTARGET = "dropTarget";
	public static final String PARAM_CREATETARGET = "createTarget";

	private static final String ID_PREFIX = "$Wb-Copy$";
	
	private CopyTask copier;

	public WbCopy()
	{
		cmdLine = new ArgumentParser();
		CommonArgs.addCommitParameter(cmdLine);
		CommonArgs.addImportModeParameter(cmdLine);
		CommonArgs.addContinueParameter(cmdLine);
		CommonArgs.addProgressParameter(cmdLine);
		CommonArgs.addCommitAndBatchParams(cmdLine);
		CommonArgs.addCheckDepsParameter(cmdLine);
		CommonArgs.addTableStatements(cmdLine);
		CommonArgs.addTransactionControL(cmdLine);
		
		cmdLine.addArgument(PARAM_SOURCETABLE);
		cmdLine.addArgument(PARAM_SOURCEQUERY);
		cmdLine.addArgument(PARAM_TARGETTABLE);
		cmdLine.addArgument(PARAM_SOURCEPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_TARGETPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_SOURCEPROFILE_GROUP);
		cmdLine.addArgument(PARAM_TARGETPROFILE_GROUP);
		cmdLine.addArgument(PARAM_COLUMNS);
		cmdLine.addArgument(PARAM_SOURCEWHERE);
		cmdLine.addArgument(PARAM_DELETETARGET, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_KEYS);
		cmdLine.addArgument(PARAM_DROPTARGET, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_CREATETARGET, ArgumentType.BoolArgument);
		cmdLine.addArgument(CommonArgs.ARG_BATCHSIZE);
	}

	public String getVerb() { return VERB; }

	protected boolean isConnectionRequired() { return false; }

	private void addWrongParams(StatementRunnerResult result)
	{
		if (!WbManager.getInstance().isBatchMode())
		{
			result.addMessageNewLine();
			result.addMessage(ResourceMgr.getString("ErrCopyWrongParameters"));
			result.setFailure();
		}
	}

	private ProfileKey getTargetProfile(ArgumentParser cmd)
	{
		String targetProfile = cmdLine.getValue(PARAM_TARGETPROFILE);
		String targetGroup = cmdLine.getValue(PARAM_TARGETPROFILE_GROUP);
		ProfileKey targetKey = null;
		if (targetProfile != null) targetKey = new ProfileKey(targetProfile, targetGroup);
		return targetKey;
	}
	
	private ProfileKey getSourceProfile(ArgumentParser cmd)
	{
		String sourceProfile = cmdLine.getValue(PARAM_SOURCEPROFILE);
		String sourceGroup = cmdLine.getValue(PARAM_SOURCEPROFILE_GROUP);
		ProfileKey sourceKey = null;
		if (sourceProfile != null) sourceKey = new ProfileKey(sourceProfile, sourceGroup);
		return sourceKey;
	}
	
	public StatementRunnerResult execute(final String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		
		cmdLine.parse(getCommandLine(sql));
		
		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrCopyWrongParameters"));
			return result;
		}
		
		ProfileKey sourceKey = getSourceProfile(cmdLine);
		ProfileKey targetKey = getTargetProfile(cmdLine);
		
		String sourcetable = cmdLine.getValue(PARAM_SOURCETABLE);
		String sourcequery = cmdLine.getValue(PARAM_SOURCEQUERY);

		if (sourcetable == null && sourcequery == null)
		{
			result.addMessage(ResourceMgr.getString("ErrCopyNoSourceSpecified"));
			addWrongParams(result);
			return result;
		}
		
		WbConnection targetCon = getConnection(result, targetKey, ID_PREFIX + "-Target$");
		if (targetCon == null || !result.isSuccess())
		{
			return result;
		}
		
		WbConnection sourceCon = getConnection(result, sourceKey, ID_PREFIX + "-Source$");
		if (sourceCon == null || !result.isSuccess())
		{
			return result;
		}

		List<TableIdentifier> tablesToExport = null;
		try
		{
			SourceTableArgument argParser = new SourceTableArgument(sourcetable, sourceCon);
			tablesToExport = argParser.getTables();
			if (tablesToExport.size() == 0 && argParser.wasWildCardArgument())
			{
				result.addMessage(ResourceMgr.getFormattedString("ErrExportNoTablesFound", sourcetable));
				result.setFailure();
				return result;
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbExport.runTableExports()", "Could not retrieve table list", e);
			result.addMessage(ExceptionUtil.getDisplay(e));
			result.setFailure();
			return result;
		}

		boolean schemaCopy = tablesToExport.size() > 1;
		
		String targettable = cmdLine.getValue(PARAM_TARGETTABLE);
		if (targettable == null && !schemaCopy)
		{
			result.addMessage(ResourceMgr.getString("ErrCopyNoTarget"));
			addWrongParams(result);
			return result;
		}
		
		if (schemaCopy)
		{
			this.copier = new SchemaCopy(tablesToExport);
		}
		else
		{
			this.copier = new TableCopy();
		}

		try
		{
			if (!copier.init(sourceCon, targetCon, result, cmdLine, rowMonitor))
			{
				return result;
			}
			
			copier.copyData();
			if (copier.isSuccess()) 
			{
				result.setSuccess();
			}
			else
			{
				result.setFailure();
			}
			result.addMessage(copier.getMessages());
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbCopy.execute()", "SQL Error when copying data", e);
			CharSequence msg = copier.getMessages();
			if (msg.length() == 0)
			{
				String err = ResourceMgr.getFormattedString("ErrCopy", ExceptionUtil.getDisplay(e, false));
				result.addMessage(err);
			}
			else
			{
				result.addMessage(msg);
			}
			result.setFailure();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.execute()", "Error when copying data", e);
			result.setFailure();
			addErrorInfo(result, sql, e);
			result.addMessage(copier.getMessages());
		}
		finally
		{
			closeConnections(sourceCon, targetCon);
		}
		
		return result;
	}

	public void done()
	{
		super.done();
		this.copier = null;
	}

	public void cancel()
		throws SQLException
	{
		super.cancel();
		if (this.copier != null)
		{
			this.copier.cancel();
		}
	}

	private void closeConnections(WbConnection sourceCon, WbConnection targetCon)
	{
		try
		{
			if (sourceCon != null && sourceCon.getId().startsWith(ID_PREFIX))
			{
				sourceCon.disconnect();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.execute()", "Error when disconnecting source connection", e);
		}

		try
		{
			if (targetCon != null && targetCon.getId().startsWith(ID_PREFIX))
			{
				targetCon.disconnect();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.execute()", "Error when disconnecting target connection", e);
		}
	}

	private WbConnection getConnection(StatementRunnerResult result, ProfileKey profileKey, String id)
	{
		if (profileKey == null || (currentConnection != null && currentConnection.getProfile().isProfileForKey(profileKey)))
		{
			return currentConnection;
		}
		else
		{
			ConnectionProfile tprof = ConnectionMgr.getInstance().getProfile(profileKey);
			if (tprof == null)
			{
				String msg = ResourceMgr.getFormattedString("ErrCopyProfileNotFound", profileKey.toString());
				result.addMessage(msg);
				result.setFailure();
				return null;
			}
			
			try
			{
				return ConnectionMgr.getInstance().getConnection(profileKey, id);
			}
			catch (Exception e)
			{
				LogMgr.logError("Wbcopy.getConnection()", "Error connecting to database", e);
				result.addMessage(ResourceMgr.getFormattedString("ErrCopyCouldNotConnect", profileKey.toString()));
				result.addMessage(ExceptionUtil.getDisplay(e));
				result.setFailure();
				return null;
			}
		}
	}

	public ConnectionProfile getModificationTarget(WbConnection con, String sql)
	{
		cmdLine.parse(getCommandLine(sql));
		ProfileKey target = getTargetProfile(cmdLine);
		ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(target);
		return prof;
	}
	
	
	/**
	 * This will check if the target profile is set to read only.
	 * If that is the case true will be returned, preventing a WbCopy
	 * command to run on a target profile that is marked as read-only
	 */
	public boolean isModificationAllowed(WbConnection con, String sql)
	{
		cmdLine.parse(getCommandLine(sql));
		ProfileKey target = getTargetProfile(cmdLine);
		ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(target);
		if (prof != null)
		{
			if (prof.getReadOnly()) return false;
		}
		return true;
	}
	
	
}
