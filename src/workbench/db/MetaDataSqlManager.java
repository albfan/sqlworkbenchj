/*
 * MetaDataSqlManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.HashMap;
import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.Settings;
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
	public static final String IDX_DIRECTION_PLACEHOLDER = "%idx_dir%";
	public static final String COLUMN_NAME_PLACEHOLDER = "%column_name%";
	public static final String COLUMN_LIST_PLACEHOLDER = "%columnlist%";
	public static final String CONSTRAINT_NAME_PLACEHOLDER = "%constraint_name%";
	public static final String FK_TARGET_TABLE_PLACEHOLDER = "%targettable%";
	public static final String FK_TARGET_COLUMNS_PLACEHOLDER = "%targetcolumnlist%";
	public static final String FK_UPDATE_RULE = "%fk_update_rule%";
	public static final String FK_DELETE_RULE = "%fk_delete_rule%";
	public static final String DEFERRABLE = "%deferrable%";
	public static final String DEFERRED = "%deferred%";
	public static final String GENERAL_SQL = "All";

	private String productName;

	private GetMetaDataSql procedureSource;
	private GetMetaDataSql viewSource;
	private GetMetaDataSql listTrigger;
	private GetMetaDataSql triggerSource;

	private final Object LOCK = new Object();

	public MetaDataSqlManager(String product)
	{
		this.productName = product;
	}

	public GetMetaDataSql getProcedureSourceSql()
	{
		synchronized (LOCK)
		{
			if (this.procedureSource == null)
			{
				HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ProcSourceStatements.xml");
				this.procedureSource = sql.get(this.productName);
			}
			return this.procedureSource;
		}
	}

	public GetMetaDataSql getViewSourceSql()
	{
		synchronized (LOCK)
		{
			if (this.viewSource == null)
			{
				HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ViewSourceStatements.xml");
				this.viewSource = sql.get(this.productName);
			}
			return this.viewSource;
		}
	}


	public GetMetaDataSql getListTriggerSql()
	{
		synchronized (LOCK)
		{
			if (this.listTrigger == null)
			{
				HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("ListTriggersStatements.xml");
				this.listTrigger = sql.get(this.productName);
			}
			return this.listTrigger;
		}
	}

	public GetMetaDataSql getTriggerSourceSql()
	{
		synchronized (LOCK)
		{
			if (this.triggerSource == null)
			{
				HashMap<String, GetMetaDataSql> sql = this.readStatementTemplates("TriggerSourceStatements.xml");
				this.triggerSource = sql.get(this.productName);
			}
			return this.triggerSource;
		}
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
		catch (Throwable e)
		{
			LogMgr.logError("MetaDataSqlManager.readStatementTemplate()", "Error reading internal templates file " + aFilename,e);
			value = null;
		}

		if (value instanceof HashMap)
		{
			result = (HashMap)value;
		}


		File f = new File(Settings.getInstance().getConfigDir(), aFilename);
		if (!f.exists())
		{
			// not in the config directory, try the directory where the sqlworkbench.jar is located
			f = new File(WbManager.getInstance().getJarPath(), aFilename);
		}

		if (!f.exists())
		{
			// not in the config directory, not in the jar file's directry, try the current directory
			f = new File(aFilename);
		}

		if (f.exists())
		{
			try
			{
				WbPersistence reader = new WbPersistence(f.getAbsolutePath());
				value = reader.readObject();
				LogMgr.logInfo("DbMetadata.readStatementTemplates()", "Retrieved user defined template file " + f.getAbsolutePath());
			}
			catch (Throwable e)
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
					// no built-in file found. Should not happen.
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
