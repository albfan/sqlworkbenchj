/*
 * ObjectScripter.java
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
package workbench.db;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.resource.DbExplorerSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectScripter
	implements Scripter
{
	public static final String TYPE_TABLE = "table";
	public static final String TYPE_TYPE = "type";
	public static final String TYPE_RULE = "rule";
	public static final String TYPE_VIEW = "view";
	public static final String TYPE_SYNONYM = "synonym";
	public static final String TYPE_INSERT = "insert";
	public static final String TYPE_UPDATE = "update";
	public static final String TYPE_SELECT = "select";
	public static final String TYPE_PROC = "procedure";
	public static final String TYPE_PKG = "package";
	public static final String TYPE_FUNC = "function";
	public static final String TYPE_TRG = "trigger";
	public static final String TYPE_DOMAIN = "domain";
	public static final String TYPE_ENUM = "enum";
	public static final String TYPE_MVIEW = DbMetadata.MVIEW_NAME.toLowerCase();

	private Set<String> knownTypes = CollectionUtil.caseInsensitiveSet(TYPE_DOMAIN, TYPE_ENUM, TYPE_FUNC, TYPE_INSERT, TYPE_MVIEW, TYPE_MVIEW, TYPE_PKG, TYPE_PROC, TYPE_RULE, TYPE_SELECT, TYPE_SYNONYM, TYPE_TABLE, TYPE_TRG, TYPE_TYPE, TYPE_UPDATE, TYPE_VIEW);
	private List<? extends DbObject> objectList;
	private StringBuilder script;
	private ScriptGenerationMonitor progressMonitor;
	private WbConnection dbConnection;
	private boolean cancel;
	private String nl = Settings.getInstance().getInternalEditorLineEnding();
	private Collection<String> commitTypes;
	private boolean appendCommit;
	private boolean useSeparator;
	private boolean includeDrop;
	private boolean includeGrants = true;
	private Collection<String> typesWithoutSeparator;
	private String sequenceType;
	private String synonymType = TYPE_SYNONYM;
	private GenericObjectDropper dropper;

	public ObjectScripter(List<? extends DbObject> objects, WbConnection aConnection)
	{
		this.objectList = objects;
		this.dbConnection = aConnection;

		SequenceReader reader = aConnection.getMetadata().getSequenceReader();
		if (reader != null)
		{
			sequenceType = reader.getSequenceTypeName();
		}

    SynonymReader synReader = aConnection.getMetadata().getSynonymReader();
    if (synReader != null)
    {
      synonymType = synReader.getSynonymTypeName();
    }

		commitTypes = CollectionUtil.caseInsensitiveSet(TYPE_TABLE, TYPE_VIEW, TYPE_PROC, TYPE_FUNC, TYPE_TRG, TYPE_DOMAIN, TYPE_ENUM, TYPE_TYPE, TYPE_RULE);

		if (sequenceType != null)
		{
			commitTypes.add(sequenceType.toLowerCase());
      knownTypes.add(sequenceType);
		}

    if (synonymType != null)
		{
			commitTypes.add(synonymType.toLowerCase());
      knownTypes.add(synonymType);
		}

    useSeparator = DbExplorerSettings.getGenerateScriptSeparator();

		typesWithoutSeparator = CollectionUtil.caseInsensitiveSet(TYPE_SELECT, TYPE_INSERT, TYPE_UPDATE);
		dropper = new GenericObjectDropper();
		dropper.setConnection(dbConnection);
		dropper.setCascade(true);
	}

	@Override
	public WbConnection getCurrentConnection()
	{
		return dbConnection;
	}

	public void setIncludeGrants(boolean flag)
	{
		this.includeGrants = flag;
	}

	public void setIncludeDrop(boolean flag)
	{
		includeDrop = flag;
	}

	public void setUseSeparator(boolean flag)
	{
		this.useSeparator = flag;
	}

	@Override
	public void setProgressMonitor(ScriptGenerationMonitor aMonitor)
	{
		this.progressMonitor = aMonitor;
	}

	@Override
	public String getScript()
	{
		if (this.script == null) this.generateScript();
		return this.script.toString();
	}

	@Override
	public boolean isCancelled()
	{
		return this.cancel;
	}

	@Override
	public void generateScript()
	{
		try
		{
			this.dbConnection.setBusy(true);
			this.cancel = false;
			this.script = new StringBuilder(this.objectList.size() * 500);
			if (!cancel && sequenceType != null) this.appendObjectType(sequenceType);
			if (!cancel) this.appendObjectType(TYPE_ENUM);
			if (!cancel) this.appendObjectType(TYPE_TYPE);
			if (!cancel) this.appendObjectType(TYPE_DOMAIN);
			if (!cancel) this.appendObjectType(TYPE_TABLE);
			if (!cancel) this.appendForeignKeys();
			if (!cancel) this.appendObjectType(TYPE_VIEW);
			if (!cancel && synonymType != null) this.appendObjectType(synonymType);
			if (!cancel) this.appendObjectType(TYPE_MVIEW);
			if (!cancel) this.appendObjectType(TYPE_INSERT);
			if (!cancel) this.appendObjectType(TYPE_UPDATE);
			if (!cancel) this.appendObjectType(TYPE_SELECT);
			if (!cancel) this.appendObjectType(TYPE_FUNC);
			if (!cancel) this.appendObjectType(TYPE_PROC);
			if (!cancel) this.appendObjectType(TYPE_PKG);
			if (!cancel) this.appendObjectType(TYPE_TRG);
			if (!cancel) this.appendObjectType(TYPE_RULE);
			if (!cancel) this.appendObjectType(null); // everything else
		}
		finally
		{
			this.dbConnection.setBusy(false);
		}
		if (appendCommit && this.dbConnection.getDbSettings().ddlNeedsCommit())
		{
			script.append("\nCOMMIT;\n");
		}
	}

	@Override
	public void cancel()
	{
		this.cancel = true;
	}

	private void appendForeignKeys()
	{
		if (this.progressMonitor != null)
		{
			this.progressMonitor.setCurrentObject(ResourceMgr.getString("TxtScriptProcessFk"), -1, -1);
		}
		boolean first = true;
		TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(dbConnection);

		for (DbObject dbo : objectList)
		{
			if (cancel) break;
			String type = dbo.getObjectType();
			if (!type.equalsIgnoreCase(TYPE_TABLE)) continue;

			TableIdentifier tbl = (TableIdentifier)dbo;
			tbl.adjustCase(this.dbConnection);
			StringBuilder source = builder.getFkSource(tbl);
			if (source != null && source.length() > 0)
			{
				if (first)
				{
					if (useSeparator)
					{
						this.script.append("-- BEGIN FOREIGN KEYS --");
					}
					first = false;
				}
				script.append(nl);
				script.append(source);
			}
		}
		if (!first)
		{
			// no table was found, so no FK was added --> do not add separator
			if (useSeparator)
			{
				this.script.append("-- END FOREIGN KEYS --");
				this.script.append(nl);
			}
			this.script.append(nl);
		}
	}

	private void appendObjectType(String typeFilter)
	{
		int current = 1;
		int count = objectList.size();

		for (DbObject dbo : objectList)
		{
			if (cancel) break;
			String type = dbo.getObjectType();

			if (typeFilter == null)
			{
				// only process unknown types if filter is null
				if (knownTypes.contains(type)) continue;
			}
			else
			{
				if (!type.equalsIgnoreCase(typeFilter)) continue;
			}

			CharSequence source = null;

			if (this.progressMonitor != null)
			{
				this.progressMonitor.setCurrentObject(dbo.getObjectName(), current++, count);
			}

			try
			{
				if (dbo instanceof TableIdentifier)
				{
					// do not generate foreign keys now, they should be generated at the end after all tables
					source = ((TableIdentifier)dbo).getSource(dbConnection, false, includeGrants);
				}
				else
				{
					source = dbo.getSource(dbConnection);
				}
				if (!appendCommit)
				{
					appendCommit = commitTypes.contains(type.toLowerCase());
				}
			}
			catch (Exception e)
			{
				this.script.append("\nError creating script for ");
				this.script.append(dbo.getObjectName());
				this.script.append(' ');
				this.script.append(ExceptionUtil.getDisplay(e));
			}

			if (source != null && source.length() > 0)
			{
				boolean writeSeparator = useSeparator && !typesWithoutSeparator.contains(type) && this.objectList.size() > 1;
				if (writeSeparator)
				{
					this.script.append("-- BEGIN ").append(type).append(' ').append(dbo.getObjectName()).append(nl);
				}

				if (includeDrop)
				{
					CharSequence drop = dropper.getDropForObject(dbo);
					if (drop != null && drop.length() > 0)
					{
						this.script.append(drop);
						this.script.append(';');
						this.script.append(nl);
					}
				}
				this.script.append(source);

				if (!StringUtil.endsWith(source, nl))
				{
					script.append(nl);
				}

				if (writeSeparator)
				{
					this.script.append("-- END ").append(type).append(' ').append(dbo.getObjectName()).append(nl);
				}
				this.script.append(nl);
			}
		}
	}
}
