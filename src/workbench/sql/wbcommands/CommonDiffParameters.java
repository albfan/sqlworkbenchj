/*
 * CommonDiffParameters.java
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.RowActionMonitor;

import workbench.sql.StatementRunnerResult;

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
	public static final String PARAM_SOURCE_CONN = "referenceConnection";

	public static final String PARAM_TARGETPROFILE = "targetProfile";
	public static final String PARAM_TARGETPROFILE_GROUP = "targetGroup";
	public static final String PARAM_TARGET_CONN = "targetConnection";

	public static final String PARAM_FILENAME = "file";

	public static final String PARAM_REFERENCETABLES = "referenceTables";
	public static final String PARAM_TARGETTABLES = "targetTables";

	public static final String PARAM_REFERENCESCHEMA = "referenceSchema";
	public static final String PARAM_TARGETSCHEMA = "targetSchema";

	public static final String PARAM_EXCLUDE_TABLES = "excludeTables";
	public static final String PARAM_INCLUDE_TABLES = "includeTables";

	private RowActionMonitor monitor;
	private ArgumentParser cmdLine;
	private List<String> missingRefTables = new ArrayList<>();
	private List<String> missingTargetTables = new ArrayList<>();
	private String baseDir;

	public CommonDiffParameters(ArgumentParser args)
	{
		this(args, null);
	}
	
	public CommonDiffParameters(ArgumentParser args, String directory)
	{
		cmdLine = args;
		baseDir = directory;
		cmdLine.addArgument(PARAM_SOURCEPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_SOURCEPROFILE_GROUP);
		cmdLine.addArgument(PARAM_SOURCE_CONN);
		cmdLine.addArgument(PARAM_TARGETPROFILE, ArgumentType.ProfileArgument);
		cmdLine.addArgument(PARAM_TARGETPROFILE_GROUP);
		cmdLine.addArgument(PARAM_TARGET_CONN);

		cmdLine.addArgument(PARAM_FILENAME);
		CommonArgs.addEncodingParameter(cmdLine);
		cmdLine.addArgument(PARAM_REFERENCETABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_TARGETTABLES, ArgumentType.TableArgument);
		cmdLine.addArgument(PARAM_REFERENCESCHEMA);
		cmdLine.addArgument(PARAM_TARGETSCHEMA);
		cmdLine.addArgument(PARAM_EXCLUDE_TABLES);
		cmdLine.addArgument(PARAM_INCLUDE_TABLES);
	}

	public void setMonitor(RowActionMonitor rowMonitor)
	{
		this.monitor = rowMonitor;
	}

	public WbConnection getTargetConnection(WbConnection current, StatementRunnerResult result)
	{
		return getConnection(PARAM_TARGETPROFILE, PARAM_TARGETPROFILE_GROUP, PARAM_TARGET_CONN, current, "Target", result);
	}

	public WbConnection getSourceConnection(WbConnection current, StatementRunnerResult result)
	{
		return getConnection(PARAM_SOURCEPROFILE, PARAM_SOURCEPROFILE_GROUP, PARAM_SOURCE_CONN, current, "Source", result);
	}

	protected WbConnection getConnection(String nameArg, String groupArg, String connArg, WbConnection current, String connType, StatementRunnerResult result)
	{
		CommandLineConnectionHandler handler = new CommandLineConnectionHandler(cmdLine, nameArg, groupArg, connArg);

		WbConnection connection = null;
		try
		{
			if (this.monitor != null)
			{
				this.monitor.setCurrentObject(ResourceMgr.getString("MsgDiffConnecting" + connType),-1,-1);
			}
			connection = handler.getConnection(result, current, baseDir, "Wb-Diff-" + connType);
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

		this.missingRefTables.clear();
		this.missingTargetTables.clear();

		String excludedNames = cmdLine.getValue(PARAM_EXCLUDE_TABLES);
		String includeNames = cmdLine.getValue(PARAM_INCLUDE_TABLES);
		String refTableNames = cmdLine.getValue(PARAM_REFERENCETABLES);
		String targetTableNames = cmdLine.getValue(PARAM_TARGETTABLES);
		String refSchema = cmdLine.getValue(PARAM_REFERENCESCHEMA);
		String targetSchema = cmdLine.getValue(PARAM_TARGETSCHEMA);

		if (StringUtil.isEmptyString(refTableNames) && StringUtil.isEmptyString(refSchema))
		{
			refSchema = referenceConn.getCurrentSchema();
		}
		else if (StringUtil.isNonEmpty(refSchema))
		{
			refSchema = referenceConn.getMetadata().adjustSchemaNameCase(refSchema);
		}

		SourceTableArgument refArg = new SourceTableArgument(refTableNames, excludedNames, refSchema, referenceConn);
		refTables = refArg.getTables();
		missingRefTables.addAll(refArg.getMissingTables());

		if (StringUtil.isNonBlank(includeNames))
		{
			SourceTableArgument include = new SourceTableArgument(includeNames, null, refSchema, referenceConn);
			refTables.addAll(include.getTables());
		}

		boolean multipleSchema = getSchemas(refTables).size() > 1 || getCatalogs(targetTables).size() > 1;
		if (multipleSchema && StringUtil.isEmptyString(targetTableNames) && StringUtil.isEmptyString(targetSchema))
		{
			// multiples source schemas, this can only be achieved by specifying one.*, two.* for the reference tables
			targetTableNames = refTableNames;
			targetSchema = null;
		}
		else if (StringUtil.isEmptyString(targetTableNames) && StringUtil.isEmptyString(targetSchema))
		{
			if (targetCon.getDbSettings().supportsSchemas())
			{
				targetSchema = targetCon.getCurrentSchema();
			}
			else
			{
				targetSchema = targetCon.getCurrentCatalog();
			}
		}
		else if (StringUtil.isNonEmpty(targetSchema))
		{
			targetSchema = targetCon.getMetadata().adjustSchemaNameCase(targetSchema);
		}

		if (StringUtil.isEmptyString(targetSchema) && StringUtil.isEmptyString(targetTableNames))
		{
			// assume the reference tables are the targettables
			// if neither target tables were specified nor a schema could be detected
			targetTableNames = refTableNames;
		}

		SourceTableArgument targetArg = new SourceTableArgument(targetTableNames, null, targetSchema, targetCon);
		targetTables = targetArg.getTables();
		missingTargetTables.addAll(targetArg.getMissingTables());

		boolean matchNames = true;
		if (StringUtil.isNonEmpty(refTableNames) && StringUtil.isNonEmpty(targetTableNames) && !refArg.wasWildcardArgument())
		{
			matchNames = false;
		}

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
		if (CollectionUtil.isEmpty(tables)) return schemas;
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
		if (CollectionUtil.isEmpty(tables)) return schemas;
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
		public List<TableIdentifier> referenceTables = new ArrayList<>();
		public List<TableIdentifier> targetTables = new ArrayList<>();

		@Override
		public String toString()
		{
			return
				"reference: " + referenceTables.toString() + "\n" +
				"target: " + targetTables.toString();
		}
	}
}
