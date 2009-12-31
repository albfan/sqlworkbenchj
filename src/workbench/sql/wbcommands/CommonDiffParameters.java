/*
 * CommonDiffParameters.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.profiles.ProfileKey;
import workbench.resource.ResourceMgr;
import workbench.sql.StatementRunnerResult;
import workbench.storage.RowActionMonitor;
import workbench.util.ArgumentParser;
import workbench.util.ArgumentType;

/**
 * Arguments for diff-ing data that are used by several commands.
 *
 * @author Thomas Kellerer
 */
public class CommonDiffParameters 
{
	public static final String PARAM_SOURCEPROFILE = "referenceProfile";
	public static final String PARAM_SOURCEPROFILE_GROUP = "referenceGroup";
	public static final String PARAM_TARGETPROFILE = "targetProfile";
	public static final String PARAM_TARGETPROFILE_GROUP = "targetGroup";
	
	public static final String PARAM_FILENAME = "file";
	public static final String PARAM_ENCODING = "encoding";

	public static final String PARAM_REFERENCETABLES = "referenceTables";
	public static final String PARAM_TARGETTABLES = "targetTables";

	public static final String PARAM_REFERENCESCHEMA = "referenceSchema";
	public static final String PARAM_TARGETSCHEMA = "targetSchema";

	public static final String PARAM_EXCLUDE_TABLES = "excludeTables";
	
	private RowActionMonitor monitor;
	private ArgumentParser cmdLine;
	
	public CommonDiffParameters(ArgumentParser args)
	{
		cmdLine = args;
		cmdLine.addArgument(PARAM_SOURCEPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_SOURCEPROFILE_GROUP);
		cmdLine.addArgument(PARAM_TARGETPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_TARGETPROFILE_GROUP);
		cmdLine.addArgument(PARAM_FILENAME);
		cmdLine.addArgument(PARAM_ENCODING);
		cmdLine.addArgument(PARAM_REFERENCETABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_TARGETTABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_REFERENCESCHEMA);
		cmdLine.addArgument(PARAM_TARGETSCHEMA);
		cmdLine.addArgument(PARAM_EXCLUDE_TABLES);
	}

	public void setMonitor(RowActionMonitor rowMonitor)
	{
		this.monitor = rowMonitor;
	}
	
	public WbConnection getTargetConnection(WbConnection current, StatementRunnerResult result)
	{
		return getConnection(PARAM_TARGETPROFILE, PARAM_TARGETPROFILE_GROUP, current, "Target", result);
	}
	
	public WbConnection getSourceConnection(WbConnection current, StatementRunnerResult result)
	{
		return getConnection(PARAM_SOURCEPROFILE, PARAM_SOURCEPROFILE_GROUP, current, "Source", result);
	}
	
	protected WbConnection getConnection(String nameArg, String groupArg, WbConnection current, String connType, StatementRunnerResult result)
	{
		String profileName = cmdLine.getValue(nameArg);
		String profileGroup = cmdLine.getValue(groupArg);
		ProfileKey profileKey = null;
		if (profileName != null) profileKey = new ProfileKey(profileName, profileGroup);
		
		if (profileKey == null || (current != null && current.getProfile().isProfileForKey(profileKey)))
		{
			return current;
		}
		
		WbConnection connection = null;
		
		ConnectionProfile prof = ConnectionMgr.getInstance().getProfile(profileKey);
		if (prof == null)
		{
			String msg = ResourceMgr.getFormattedString("ErrProfileNotFound", profileKey.toString());
			result.addMessage(msg);
			result.setFailure();
		}
		try
		{
			if (this.monitor != null) this.monitor.setCurrentObject(ResourceMgr.getString("MsgDiffConnecting" + connType),-1,-1);
			connection = ConnectionMgr.getInstance().getConnection(profileKey, "Wb-Diff-" + connType);
		}
		catch (Exception e)
		{
			result.addMessage(ResourceMgr.getString("ErrDiffCouldNotConnect" + connType));
			result.setFailure();
		}

		return connection;
	}
	
	public TableMapping getTables(WbConnection referenceConn, WbConnection targetCon)
		throws SQLException
	{
		TableMapping mapping = new TableMapping();
		List<TableIdentifier> refTables = null;
		List<TableIdentifier> targetTables = null;
		boolean matchNames = true;
		
		if (cmdLine.isArgPresent(PARAM_REFERENCESCHEMA) || cmdLine.isArgPresent(PARAM_TARGETSCHEMA)
			|| !cmdLine.isArgPresent(PARAM_REFERENCETABLES))
		{
			String refSchema = cmdLine.getValue(PARAM_REFERENCESCHEMA);
			String targetSchema = cmdLine.getValue(PARAM_TARGETSCHEMA);

			if (refSchema == null) refSchema = referenceConn.getMetadata().getSchemaToUse();
			if (targetSchema == null) targetSchema = targetCon.getMetadata().getSchemaToUse();
			
			refTables = referenceConn.getMetadata().getObjectList(refSchema, new String[] { referenceConn.getMetadata().getTableTypeName() });
			targetTables = targetCon.getMetadata().getObjectList(targetSchema, new String[] { targetCon.getMetadata().getTableTypeName() });
		}
		else
		{
			String tableNames = cmdLine.getValue(PARAM_REFERENCETABLES);
			SourceTableArgument refTableArgs = new SourceTableArgument(tableNames, referenceConn);
			refTables = refTableArgs.getTables();

			tableNames = cmdLine.getValue(PARAM_TARGETTABLES);
			if (tableNames == null)
			{
				targetTables = targetCon.getMetadata().getTableList();
			}
			else
			{
				SourceTableArgument tableArgs = new SourceTableArgument(tableNames, targetCon);
				targetTables = tableArgs.getTables();
				matchNames = false;
			}
		}
		
		String exNames = cmdLine.getValue(PARAM_EXCLUDE_TABLES);
		List<TableIdentifier> excluded = null;
		if (exNames != null)
		{
			SourceTableArgument args = new SourceTableArgument(exNames, referenceConn);
			excluded = args.getTables();
		}
		
		int index = 0;
		for (TableIdentifier refTable : refTables)
		{
			if (TableIdentifier.findTableByName(excluded, refTable) != null) continue;
			mapping.referenceTables.add(refTable);
			if (matchNames)
			{
				TableIdentifier other = TableIdentifier.findTableByName(targetTables, refTable);
				if (other != null)
				{
					mapping.targetTables.add(other);
				}
			}
			else
			{
				if (index < targetTables.size())
				{
					mapping.targetTables.add(targetTables.get(index));
				}
			}
		}
		
		return mapping;
	}

	public static class TableMapping
	{
		public List<TableIdentifier> referenceTables = new ArrayList<TableIdentifier>();
		public List<TableIdentifier> targetTables = new ArrayList<TableIdentifier>();
	}
}
