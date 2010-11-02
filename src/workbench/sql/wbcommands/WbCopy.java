/*
 * WbCopy.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.List;
import workbench.AppArguments;
import workbench.WbManager;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.TableNotFoundException;
import workbench.db.WbConnection;
import workbench.gui.profiles.ProfileKey;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;
import workbench.util.StringUtil;

/**
 * A command to copy data from one DBMS to another. This is the commandline
 * version of the DataPumper.
 * @author  Thomas Kellerer
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
	public static final String PARAM_KEYS = "keyColumns";
	public static final String PARAM_DROPTARGET = "dropTarget";
	public static final String PARAM_CREATETARGET = "createTarget";


	/**
	 * If PARAM_CREATETARGET is set to true, this parameter defines
	 * the table type (template) to be used when creating the table.
	 *
	 * @see workbench.db.DbSettings#getCreateTableTemplate(java.lang.String)
	 */
	public static final String PARAM_TABLE_TYPE = "tableType";
	public static final String PARAM_USE_SOURCE_DEF = "useSourceTableDefinition";

	public static final String PARAM_DELETE_SYNC = "syncDelete";

	private static final String ID_PREFIX = "$Wb-Copy$";

	private CopyTask copier;

	public WbCopy()
	{
		super();
		this.isUpdatingCommand = true;
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
		cmdLine.addArgument(AppArguments.ARG_IGNORE_DROP, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_SOURCEQUERY);
		cmdLine.addArgument(PARAM_TARGETTABLE);
		cmdLine.addArgument(PARAM_SOURCEPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_TARGETPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_SOURCEPROFILE_GROUP);
		cmdLine.addArgument(PARAM_TARGETPROFILE_GROUP);
		cmdLine.addArgument(PARAM_COLUMNS);
		cmdLine.addArgument(PARAM_SOURCEWHERE);
		cmdLine.addArgument(CommonArgs.ARG_DELETE_TARGET, ArgumentType.BoolArgument);
		cmdLine.addArgument(CommonArgs.ARG_TRUNCATE_TABLE, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_KEYS);
		cmdLine.addArgument(PARAM_DROPTARGET, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_USE_SOURCE_DEF, ArgumentType.BoolArgument);

		cmdLine.addArgument(PARAM_CREATETARGET, ArgumentType.BoolArgument);
		cmdLine.addArgument(PARAM_DELETE_SYNC, ArgumentType.BoolArgument);
		cmdLine.addArgument(WbImport.ARG_USE_SAVEPOINT, ArgumentType.BoolArgument);
		cmdLine.addArgumentWithValues(PARAM_TABLE_TYPE, DbSettings.getCreateTableTypes());
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

	private ProfileKey getTargetProfile()
	{
		String targetProfile = cmdLine.getValue(PARAM_TARGETPROFILE);
		String targetGroup = cmdLine.getValue(PARAM_TARGETPROFILE_GROUP);
		ProfileKey targetKey = null;
		if (targetProfile != null) targetKey = new ProfileKey(targetProfile, targetGroup);
		return targetKey;
	}

	private ProfileKey getSourceProfile()
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

		ProfileKey sourceKey = getSourceProfile();
		ProfileKey targetKey = getTargetProfile();

		String sourcetable = cmdLine.getValue(PARAM_SOURCETABLE);
		String sourcequery = cmdLine.getValue(PARAM_SOURCEQUERY);

		if (StringUtil.isBlank(sourcetable) && StringUtil.isBlank(sourcequery))
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
		SourceTableArgument sourceTables = null;
		try
		{
			sourceTables = new SourceTableArgument(sourcetable, sourceCon);
			tablesToExport = sourceTables.getTables();
			if (tablesToExport.isEmpty() && sourceTables.wasWildCardArgument())
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
				result.addMessage(copier.getMessages());
				result.setFailure();
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
		catch (TableNotFoundException tnf)
		{
			String err = ResourceMgr.getFormattedString("ErrImportTableNotFound", tnf.getTableName());
			result.addMessage(err);
			result.setFailure();
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
				String msg = ResourceMgr.getFormattedString("ErrProfileNotFound", profileKey.toString());
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

	/**
	 * Extracts the target profile from the passed SQL statement.
	 */
	public ConnectionProfile getModificationTarget(WbConnection con, String sql)
	{
		cmdLine.parse(getCommandLine(sql));
		ProfileKey target = getTargetProfile();
		ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(target);
		return prof;
	}

}
