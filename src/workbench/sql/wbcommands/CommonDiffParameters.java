/*
 * CommonDiffParameters.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

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

	public static final String PARAM_REFERENCETABLES = "referenceTables";
	public static final String PARAM_TARGETTABLES = "targetTables";

	public static final String PARAM_REFERENCESCHEMA = "referenceSchema";
	public static final String PARAM_TARGETSCHEMA = "targetSchema";

	public static final String PARAM_EXCLUDE_TABLES = "excludeTables";

	private RowActionMonitor monitor;
	private ArgumentParser cmdLine;
	private List<String> missingRefTables = new ArrayList<String>();
	private List<String> missingTargetTables = new ArrayList<String>();

	public CommonDiffParameters(ArgumentParser args)
	{
		cmdLine = args;
		cmdLine.addArgument(PARAM_SOURCEPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_SOURCEPROFILE_GROUP);
		cmdLine.addArgument(PARAM_TARGETPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_TARGETPROFILE_GROUP);
		cmdLine.addArgument(PARAM_FILENAME);
		CommonArgs.addEncodingParameter(cmdLine);
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

		this.missingRefTables.clear();
		this.missingTargetTables.clear();

		boolean multipleSchema = false;

		String excludedNames = cmdLine.getValue(PARAM_EXCLUDE_TABLES);
		if (cmdLine.isArgPresent(PARAM_REFERENCESCHEMA) || cmdLine.isArgPresent(PARAM_TARGETSCHEMA))
		{
			String refSchema = cmdLine.getValue(PARAM_REFERENCESCHEMA);
			String targetSchema = cmdLine.getValue(PARAM_TARGETSCHEMA);

			if (refSchema == null)
			{
				refSchema = referenceConn.getCurrentSchema();
			}
			else
			{
				refSchema = referenceConn.getMetadata().adjustSchemaNameCase(refSchema);
			}

			if (targetSchema == null)
			{
				targetSchema = targetCon.getCurrentSchema();
			}
			else
			{
				targetSchema = targetCon.getMetadata().adjustSchemaNameCase(targetSchema);
			}
			String refTableNames = cmdLine.getValue(PARAM_REFERENCETABLES);
			String targetTableNames = cmdLine.getValue(PARAM_TARGETTABLES);

			if (StringUtil.isNonBlank(refTableNames))
			{
				SourceTableArgument refArg = new SourceTableArgument(refTableNames, null, refSchema, referenceConn);
				refTables = refArg.getTables();
				missingRefTables.addAll(refArg.getMissingTables());
			}
			else
			{
				refTables = referenceConn.getMetadata().getTableList(null, refSchema);
			}

			if (StringUtil.isNonBlank(targetTableNames))
			{
				SourceTableArgument targetArg = new SourceTableArgument(targetTableNames, null, targetSchema, targetCon);
				targetTables = targetArg.getTables();
				missingTargetTables.addAll(targetArg.getMissingTables());
			}
			else
			{
				targetTables = targetCon.getMetadata().getTableList(null, targetSchema);
			}
			matchNames = true;
		}
		else
		{
			String tableNames = cmdLine.getValue(PARAM_REFERENCETABLES);
			String refSchema = null;
			if (StringUtil.isBlank(tableNames))
			{
				refSchema = referenceConn.getCurrentSchema();
			}
			SourceTableArgument refTableArgs = new SourceTableArgument(tableNames, excludedNames, refSchema, referenceConn);
			refTables = refTableArgs.getTables();
			missingRefTables.addAll(refTableArgs.getMissingTables());
			matchNames = refTableArgs.wasWildCardArgument();

			boolean refUseCatalog = !referenceConn.getDbSettings().supportsSchemas();
			tableNames = cmdLine.getValue(PARAM_TARGETTABLES);
			if (StringUtil.isBlank(tableNames))
			{
				targetTables = new ArrayList<TableIdentifier>(refTables.size());
				Set<String> schemas = refUseCatalog ? getCatalogs(refTables) : getSchemas(refTables);
				if (schemas.size() > 0)
				{
					for (String schema : schemas)
					{
						if (refUseCatalog)
						{
							targetTables.addAll(targetCon.getMetadata().getTableList(null, schema, null));
						}
						else
						{
							targetTables.addAll(targetCon.getMetadata().getTableList(null, schema));
						}
					}
				}
				else
				{
					targetTables.addAll(targetCon.getMetadata().getTableList());
				}
				matchNames = true;
			}
			else
			{
				SourceTableArgument tableArgs = new SourceTableArgument(tableNames, excludedNames, null, targetCon);
				targetTables = tableArgs.getTables();
				missingTargetTables.addAll(tableArgs.getMissingTables());
				matchNames = matchNames || tableArgs.wasWildCardArgument();
			}
		}

		multipleSchema = getSchemas(refTables).size() > 1 || getSchemas(targetTables).size() > 1;
		matchNames = matchNames || multipleSchema;

		int index = 0;
		for (TableIdentifier refTable : refTables)
		{
			if (matchNames)
			{
				TableIdentifier other = null;
				if (multipleSchema)
				{
					other = TableIdentifier.findTableByNameAndSchema(targetTables, refTable);
				}
				else
				{
					other = TableIdentifier.findTableByName(targetTables, refTable);
				}
				if (other != null)
				{
					mapping.referenceTables.add(refTable);
					mapping.targetTables.add(other);
				}
			}
			else
			{
				if (index < targetTables.size())
				{
					mapping.referenceTables.add(refTable);
					mapping.targetTables.add(targetTables.get(index));
					index ++;
				}
			}
		}

		return mapping;
	}

	private Set<String> getCatalogs(List<TableIdentifier> tables)
	{
		Set<String> schemas = CollectionUtil.caseInsensitiveSet();
		for (TableIdentifier tbl : tables)
		{
			if (tbl.getCatalog() != null)
			{
				schemas.add(tbl.getCatalog());
			}
		}
		return schemas;
	}

	private Set<String> getSchemas(List<TableIdentifier> tables)
	{
		Set<String> schemas = CollectionUtil.caseInsensitiveSet();
		for (TableIdentifier tbl : tables)
		{
			if (tbl.getSchema() != null)
			{
				schemas.add(tbl.getSchema());
			}
		}
		return schemas;
	}

	public List<String> getMissingReferenceTables()
	{
		return missingRefTables;
	}

	public List<String> getMissingTargetTables()
	{
		return missingTargetTables;
	}

	public static class TableMapping
	{
		public List<TableIdentifier> referenceTables = new ArrayList<TableIdentifier>();
		public List<TableIdentifier> targetTables = new ArrayList<TableIdentifier>();
	}
}
