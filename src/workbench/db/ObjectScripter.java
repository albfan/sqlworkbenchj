/*
 * ObjectScripter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Collection;
import java.util.List;
import workbench.resource.ResourceMgr;
import workbench.util.ExceptionUtil;

import workbench.interfaces.ScriptGenerationMonitor;
import workbench.interfaces.Scripter;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectScripter
	implements Scripter
{
	public static final String TYPE_SEQUENCE = "sequence";
	public static final String TYPE_TABLE = "table";
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

	private List<? extends DbObject> objectList;
	private StringBuilder script;
	private ScriptGenerationMonitor progressMonitor;
	private WbConnection dbConnection;
	private boolean cancel;
	private String nl = Settings.getInstance().getInternalEditorLineEnding();
	private Collection<String> commitTypes;
	private boolean appendCommit;

	private Collection<String> typesWithoutSeparator;

	public ObjectScripter(List<? extends DbObject> objects, WbConnection aConnection)
	{
		this.objectList = objects;
		this.dbConnection = aConnection;

		commitTypes = CollectionUtil.caseInsensitiveSet(TYPE_SEQUENCE,
			TYPE_TABLE, TYPE_VIEW, TYPE_SYNONYM, TYPE_PROC, TYPE_FUNC, TYPE_TRG,
			TYPE_DOMAIN, TYPE_ENUM);

		typesWithoutSeparator = CollectionUtil.caseInsensitiveSet(TYPE_SELECT, TYPE_INSERT, TYPE_UPDATE);
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
			if (!cancel) this.appendObjectType(TYPE_SEQUENCE);
			if (!cancel) this.appendObjectType(TYPE_ENUM);
			if (!cancel) this.appendObjectType(TYPE_DOMAIN);
			if (!cancel) this.appendObjectType(TYPE_TABLE);
			if (!cancel) this.appendForeignKeys();
			if (!cancel) this.appendObjectType(TYPE_VIEW);
			if (!cancel) this.appendObjectType(TYPE_SYNONYM);
			if (!cancel) this.appendObjectType(TYPE_MVIEW);
			if (!cancel) this.appendObjectType(TYPE_INSERT);
			if (!cancel) this.appendObjectType(TYPE_UPDATE);
			if (!cancel) this.appendObjectType(TYPE_SELECT);
			if (!cancel) this.appendObjectType(TYPE_FUNC);
			if (!cancel) this.appendObjectType(TYPE_PROC);
			if (!cancel) this.appendObjectType(TYPE_PKG);
			if (!cancel) this.appendObjectType(TYPE_TRG);
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
			this.progressMonitor.setCurrentObject(ResourceMgr.getString("TxtScriptProcessFk"));
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
					this.script.append("-- BEGIN FOREIGN KEYS --");
					this.script.append(nl);
					first = false;
				}
				script.append(source);
			}
		}
		if (!first)
		{
			// no table was found, so no FK was added --> do not add separator
			this.script.append("-- END FOREIGN KEYS --");
			this.script.append(nl);
			this.script.append(nl);
		}
	}

	private void appendObjectType(String typeFilter)
	{
		for (DbObject dbo : objectList)
		{
			if (cancel) break;
			String type = dbo.getObjectType();

			if (!type.equalsIgnoreCase(typeFilter)) continue;

			CharSequence source = null;

			if (this.progressMonitor != null)
			{
				this.progressMonitor.setCurrentObject(dbo.getObjectName());
			}

			try
			{
				source = dbo.getSource(dbConnection);
				if (!appendCommit)
				{
					appendCommit = commitTypes.contains(type.toLowerCase());
				}
			}
			catch (Exception e)
			{
				this.script.append("\nError creating script for " + dbo.getObjectName() + " " + ExceptionUtil.getDisplay(e));
			}

			if (source != null && source.length() > 0)
			{
				boolean useSeparator = !typesWithoutSeparator.contains(type);
				if (useSeparator) this.script.append("-- BEGIN " + type + " " + dbo.getObjectName() + nl);
				this.script.append(source);
				if (useSeparator) this.script.append("-- END " + type + " " + dbo.getObjectName() + nl);
				this.script.append(nl);
			}
		}
	}

}
