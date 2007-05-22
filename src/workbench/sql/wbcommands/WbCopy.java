/*
 * WbCopy.java
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.WbManager;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.datacopy.DataCopier;
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
import workbench.util.SqlUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.StringUtil;

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

	private DataCopier copier;

	public WbCopy()
	{
		cmdLine = new ArgumentParser();
		CommonArgs.addCommitParameter(cmdLine);
		CommonArgs.addImportModeParameter(cmdLine);
		CommonArgs.addContinueParameter(cmdLine);
		CommonArgs.addProgressParameter(cmdLine);
		CommonArgs.addCommitAndBatchParams(cmdLine);
		
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
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		
		aSql = SqlUtil.stripVerb(aSql);

		cmdLine.parse(aSql);
		
		if (cmdLine.hasUnknownArguments())
		{
			setUnknownMessage(result, cmdLine, ResourceMgr.getString("ErrCopyWrongParameters"));
			return result;
		}
		
		String sourceProfile = cmdLine.getValue(PARAM_SOURCEPROFILE);
		String sourceGroup = cmdLine.getValue(PARAM_SOURCEPROFILE_GROUP);
		ProfileKey sourceKey = null;
		if (sourceProfile != null) sourceKey = new ProfileKey(sourceProfile, sourceGroup);
		
		String targetProfile = cmdLine.getValue(PARAM_TARGETPROFILE);
		String targetGroup = cmdLine.getValue(PARAM_TARGETPROFILE_GROUP);
		ProfileKey targetKey = null;
		if (targetProfile != null) targetKey = new ProfileKey(targetProfile, targetGroup);
		
		String sourcetable = cmdLine.getValue(PARAM_SOURCETABLE);
		String sourcequery = cmdLine.getValue(PARAM_SOURCEQUERY);
		if (sourcetable == null && sourcequery == null)
		{
			result.addMessage(ResourceMgr.getString("ErrCopyNoSourceSpecified"));
			addWrongParams(result);
			return result;
		}

		String targettable = cmdLine.getValue(PARAM_TARGETTABLE);
		if (targettable == null)
		{
			result.addMessage(ResourceMgr.getString("ErrCopyNoTarget"));
			addWrongParams(result);
			return result;
		}

		WbConnection targetCon = null;
		WbConnection sourceCon = null;
		if (targetProfile == null || (aConnection != null && aConnection.getProfile().isProfileForKey(targetKey)))
		{
			targetCon = aConnection;
		}
		else
		{
			ConnectionProfile tprof = ConnectionMgr.getInstance().getProfile(targetKey);
			if (tprof == null)
			{
				String msg = ResourceMgr.getString("ErrCopyProfileNotFound");
				msg = StringUtil.replace(msg, "%profile%", targetKey.toString());
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
			
			try
			{
				targetCon = ConnectionMgr.getInstance().getConnection(targetKey, "Wb-Copy-Target");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrCopyCouldNotConnectTarget"));
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
			ConnectionProfile tprof = ConnectionMgr.getInstance().getProfile(sourceKey);
			if (tprof == null)
			{
				String msg = ResourceMgr.getString("ErrCopyProfileNotFound");
				msg = StringUtil.replace(msg, "%profile%", sourceKey.toString());
				result.addMessage(msg);
				result.setFailure();
				return result;
			}
			try
			{
				sourceCon = ConnectionMgr.getInstance().getConnection(sourceKey, "Wb-Copy-Source");
			}
			catch (Exception e)
			{
				result.addMessage(ResourceMgr.getString("ErrCopyCouldNotConnectSource"));
				result.setFailure();
				// disconnect the target connection only if it was created by this command
				if (targetCon.getId().startsWith("Wb-Copy"))
				{
					try { targetCon.disconnect(); } catch (Throwable th) {}
				}
				return result;
			}
		}
		boolean delete = cmdLine.getBoolean(PARAM_DELETETARGET);
		boolean cont = cmdLine.getBoolean(CommonArgs.ARG_CONTINUE);
		
		boolean createTable = cmdLine.getBoolean(PARAM_CREATETARGET);
		boolean dropTable = cmdLine.getBoolean(PARAM_DROPTARGET);
		String keys = cmdLine.getValue(PARAM_KEYS);

		this.copier = new DataCopier();
		copier.setKeyColumns(keys);

		String mode = cmdLine.getValue(CommonArgs.ARG_IMPORT_MODE);
		if (mode != null)
		{
			if (!this.copier.setMode(mode))
			{
				result.addMessage(ResourceMgr.getString("ErrInvalidModeIgnored").replaceAll("%mode%", mode));
			}
		}

		CommonArgs.setProgressInterval(copier, cmdLine);

		copier.setRowActionMonitor(this.rowMonitor);
		copier.setContinueOnError(cont);
		
		CommonArgs.setCommitAndBatchParams(copier, cmdLine);
		
		copier.setDeleteTarget(delete);
		
		TableIdentifier targetId = new TableIdentifier(targettable);
		targetId.setNewTable(createTable);

		try
		{
			if (sourcetable != null)
			{
				TableIdentifier srcTable = new TableIdentifier(sourcetable);
				String where = cmdLine.getValue(PARAM_SOURCEWHERE);
				String columns = cmdLine.getValue(PARAM_COLUMNS);
				boolean hasColumns = columns != null;
				boolean containsMapping = hasColumns && (columns.indexOf('/') > -1);

				if ((containsMapping || !hasColumns) && !createTable)
				{
					Map mapping = this.parseMapping();
					copier.copyFromTable(sourceCon, targetCon, srcTable, targetId, mapping, where, createTable, dropTable);
				}
				else if (createTable)
				{
					ColumnIdentifier[] cols = this.parseColumns();
					copier.copyToNewTable(sourceCon, targetCon, srcTable, targetId, cols, where, dropTable);
				}
				else
				{
					result.addMessage(ResourceMgr.getString("ErrCopyWrongParameters"));
					result.setFailure();
					return result;
				}
			}
			else
			{
				ColumnIdentifier[] cols = this.parseColumns(sourcequery, sourceCon);
				copier.copyFromQuery(sourceCon, targetCon, sourcequery, targetId, cols);
			}

			copier.startCopy();
			if (copier.isSuccess()) 
			{
				result.setSuccess();
			}
			else
			{
				result.setFailure();
			}
			result.addMessage(copier.getAllMessages());
		}
		catch (SQLException e)
		{
			LogMgr.logError("WbCopy.execute()", "SQL Error when copying data", e);
			result.addMessage(ResourceMgr.getString("ErrOnCopy"));
			result.addMessage(copier.getAllMessages());
			result.setFailure();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.execute()", "Error when copying data", e);
			result.setFailure();
			addErrorInfo(result, aSql, e);
			result.addMessage(copier.getAllMessages());
		}
		finally
		{
			try
			{
				if (sourceCon.getId().startsWith("Wb-Copy"))
				{
					sourceCon.disconnect();
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("WbCopy.execute()", "Error when disconnecting source connection",e);
				result.addMessage(ExceptionUtil.getDisplay(e));
			}

			try
			{
				if (targetCon.getId().startsWith("Wb-Copy"))
				{
					targetCon.disconnect();
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("WbCopy.execute()", "Error when disconnecting target connection",e);
				result.addMessage(ExceptionUtil.getDisplay(e));
			}
		}

		return result;
	}

	private ColumnIdentifier[] parseColumns(String sourceQuery, WbConnection sourceCon)
	{
		// First read the defined columns from the passed parameter
		String cols = cmdLine.getValue(PARAM_COLUMNS);
		List l = StringUtil.stringToList(cols, ",");
		int count = l.size();
		ColumnIdentifier[] result = new ColumnIdentifier[count];
		for (int i=0; i < count; i++)
		{
			String c = (String)l.get(i);
			result[i] = new ColumnIdentifier(c);
		}

		// now try to read the column definitions from the query
		// if a matching column is found, the definition from the query
		// is used (because it will/should contain the correct datatype information
		try
		{
			List colsFromQuery = SqlUtil.getResultSetColumns(sourceQuery, sourceCon);
			for (int i=0; i < count; i++)
			{
				int idx = colsFromQuery.indexOf(result[i]);
				if (idx > -1)
				{
					ColumnIdentifier c = (ColumnIdentifier)colsFromQuery.get(idx);
					result[i] = c;
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("WbCopy.parseColumns()", "Error retrieving column definition from source query", e);
		}
		return result;
	}

	private ColumnIdentifier[] parseColumns()
	{
		String cols = cmdLine.getValue(PARAM_COLUMNS);
		if (cols == null) return null;
		List l = StringUtil.stringToList(cols, ",");
		int count = l.size();
		ColumnIdentifier[] result = new ColumnIdentifier[count];
		for (int i=0; i < count; i++)
		{
			String c = (String)l.get(i);
			result[i] = new ColumnIdentifier(c);
		}
		return result;
	}

	private Map parseMapping()
	{
		String cols = cmdLine.getValue(PARAM_COLUMNS);
		if (cols == null || cols.length() == 0) return null;

		List l = StringUtil.stringToList(cols, ",");
		int count = l.size();
		HashMap mapping = new HashMap(count);
		for (int i=0; i < count; i++)
		{
			String s = (String)l.get(i);
			int pos = s.indexOf('/');
			String scol = s.substring(0, pos).trim();
			String tcol = s.substring(pos + 1).trim();
			mapping.put(scol, tcol);
		}
		return mapping;
	}

	public void done()
	{
		super.done();
		this.copier = null;;
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

}
