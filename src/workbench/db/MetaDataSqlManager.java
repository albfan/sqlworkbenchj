/*
 * MetaDataSqlManager.java
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

import java.io.BufferedInputStream;
import java.io.File;
import java.util.HashMap;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.WbPersistence;

/**
 * @author  Thomas Kellerer
 */
public class MetaDataSqlManager
{
	public static final String FQ_TABLE_NAME_PLACEHOLDER = "%fq_table_name%";
	public static final String TABLE_NAME_PLACEHOLDER = "%table_name%";
	public static final String INDEX_TYPE_PLACEHOLDER = "%indextype%";
	public static final String INDEX_NAME_PLACEHOLDER = "%indexname%";
	public static final String PK_NAME_PLACEHOLDER = "%pk_name%";
	public static final String PK_INLINE_DEFINITION = "%pk_definition%";
	public static final String UNIQUE_PLACEHOLDER = "%unique_key%";
	public static final String COLUMN_NAME_PLACEHOLDER = "%column_name%";
	public static final String COLUMN_LIST_PLACEHOLDER = "%columnlist%";
	public static final String FK_NAME_PLACEHOLDER = "%constraintname%";
	public static final String FK_TARGET_TABLE_PLACEHOLDER = "%targettable%";
	public static final String FK_TARGET_COLUMNS_PLACEHOLDER = "%targetcolumnlist%";
	public static final String FK_UPDATE_RULE = "%fk_update_rule%";
	public static final String FK_DELETE_RULE = "%fk_delete_rule%";
	public static final String DEFERRABLE = "%deferrable%";
	public static final String GENERAL_SQL = "All";

	private String productName;
	private static final GetMetaDataSql MARKER = new GetMetaDataSql();
	private static final String NO_STRING = "";

	private GetMetaDataSql procedureSource = MARKER;
	private GetMetaDataSql viewSource = MARKER;
	private GetMetaDataSql listTrigger = MARKER;
	private GetMetaDataSql triggerSource = MARKER;

	private String primaryKeyTemplate = NO_STRING;
	private String foreignKeyTemplate = NO_STRING;
//	private String columnCommentTemplate = NO_STRING;
//	private String tableCommentTemplate = NO_STRING;
	private final Object LOCK = new Object();

	public MetaDataSqlManager(String product)
	{
		this.productName = product;
	}

	public GetMetaDataSql getProcedureSourceSql()
	{
		if (this.procedureSource == MARKER)
		{
			synchronized (LOCK)
			{
				HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ProcSourceStatements.xml");
				this.procedureSource = sql.get(this.productName);
			}
		}
		return this.procedureSource;
	}

	public GetMetaDataSql getViewSourceSql()
	{
		if (this.viewSource == MARKER)
		{
			synchronized (LOCK)
			{
				HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ViewSourceStatements.xml");
				this.viewSource = sql.get(this.productName);
			}
		}
		return this.viewSource;
	}


	public GetMetaDataSql getListTriggerSql()
	{
		if (this.listTrigger == MARKER)
		{
			synchronized (LOCK)
			{
				HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ListTriggersStatements.xml");
				this.listTrigger = sql.get(this.productName);
			}
		}
		return this.listTrigger;
	}

	public GetMetaDataSql getTriggerSourceSql()
	{
		if (this.triggerSource == MARKER)
		{
			synchronized (LOCK)
			{
				HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("TriggerSourceStatements.xml");
				this.triggerSource = sql.get(this.productName);
			}
		}
		return this.triggerSource;
	}

	public String getPrimaryKeyTemplate()
	{
		if (this.primaryKeyTemplate == NO_STRING)
		{
			synchronized (LOCK)
			{
				HashMap<String, String> sql = this.readStatementTemplates("CreatePkStatements.xml");
				this.primaryKeyTemplate = sql.get(this.productName);
				if (this.primaryKeyTemplate == null)
				{
					this.primaryKeyTemplate = sql.get(GENERAL_SQL);
				}
			}
		}
		return this.primaryKeyTemplate;
	}

	public String getForeignKeyTemplate(boolean createInline)
	{
		if (this.foreignKeyTemplate == NO_STRING)
		{
			synchronized (LOCK)
			{
				HashMap<String, String> sql = this.readStatementTemplates("CreateFkStatements.xml");
				String template = sql.get(this.productName);
				if (template == null)
				{
					if (createInline)
					{
						template = sql.get("All-Inline");
					}
					else
					{
						template = sql.get(GENERAL_SQL);
					}
				}
				this.foreignKeyTemplate = template;
			}
		}
		return this.foreignKeyTemplate;
	}

	public static String removePlaceholder(String sql, String placeholder, boolean withNL)
	{
		String s = null;
		if (withNL)
		{
			StringBuilder b = new StringBuilder(placeholder.length() + 10);
			b.append("[ \\t]*");
			b.append(StringUtil.quoteRegexMeta(placeholder));
			b.append("[\n|\r\n]?");
			s = b.toString();
		}
		else
		{
			s = StringUtil.quoteRegexMeta(placeholder);
		}
		return sql.replaceAll(s, StringUtil.EMPTY_STRING);
	}

	@SuppressWarnings("unchecked")
	private HashMap readStatementTemplates(String aFilename)
	{
		HashMap result = null;

		BufferedInputStream in = new BufferedInputStream(DbMetadata.class.getResourceAsStream(aFilename));
		Object value;
		try
		{
			WbPersistence reader = new WbPersistence();
			value = reader.readObject(in);
		}
		catch (Exception e)
		{
			LogMgr.logError("MetaDataSqlManager.readStatementTemplate()", "Error reading internal templates file " + aFilename,e);
			value = null;
		}

		if (value instanceof HashMap)
		{
			result = (HashMap)value;
		}

		// Try to read the file in the current directory.
		File f = new File(aFilename);
		if (!f.exists())
		{
			// Not in the current directory, check the config-dir
			f = new File(Settings.getInstance().getConfigDir(), aFilename);
		}

		if (f.exists())
		{
			try
			{
				WbPersistence reader = new WbPersistence(f.getAbsolutePath());
				value = reader.readObject();
				LogMgr.logInfo("DbMetadata.readStatementTemplates()", "Retrieved user defined template file " + f.getAbsolutePath());
			}
			catch (Exception e)
			{
				LogMgr.logWarning("MetaDataSqlManager.readStatementTemplate()", "Error reading template file " + f.getAbsolutePath(), e);
			}
			if (value instanceof HashMap)
			{
				HashMap m = (HashMap<String, GetMetaDataSql>)value;
				if (result != null)
				{
					result.putAll(m);
				}
				else
				{
					result = m;
				}
			}
		}
		else if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("MetaDataSqlManager.readStatementTemplates()", "No user defined template file found for " + aFilename);
		}
		return result;
	}

}
