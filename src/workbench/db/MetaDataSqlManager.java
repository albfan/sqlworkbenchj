/*
 * MetaDataSqlManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
 * @author  support@sql-workbench.net
 */
public class MetaDataSqlManager
{
	private String productName;
	private static final GetMetaDataSql MARKER = new GetMetaDataSql();
	private static final String NO_STRING = "";
	
	private GetMetaDataSql procedureSource = MARKER;
	private GetMetaDataSql viewSource = MARKER;
	private GetMetaDataSql listTrigger = MARKER;
	private GetMetaDataSql triggerSource = MARKER;
	
	private String primaryKeyTemplate = NO_STRING;
	private String indexTemplate = NO_STRING;
	private String foreignKeyTemplate = NO_STRING;
	private String columnCommentTemplate = NO_STRING;
	private String tableCommentTemplate = NO_STRING;
	
	public MetaDataSqlManager(String product)
	{
		this.productName = product;
	}
	
	public GetMetaDataSql getProcedureSourceSql()
	{
		if (this.procedureSource == MARKER)
		{
			synchronized (MARKER)
			{
				HashMap sql = this.readStatementTemplates("ProcSourceStatements.xml");
				this.procedureSource = (GetMetaDataSql)sql.get(this.productName);
			}
		}
		return this.procedureSource;
	}

	public GetMetaDataSql getViewSourceSql()
	{
		if (this.viewSource == MARKER)
		{
			synchronized (MARKER)
			{
				HashMap sql = this.readStatementTemplates("ViewSourceStatements.xml");
				this.viewSource = (GetMetaDataSql)sql.get(this.productName);
			}
		}
		return this.viewSource;
	}
	
	
	public GetMetaDataSql getListTriggerSql()
	{
		if (this.listTrigger == MARKER)
		{
			synchronized (MARKER)
			{
				HashMap sql = this.readStatementTemplates("ListTriggersStatements.xml");
				this.listTrigger = (GetMetaDataSql)sql.get(this.productName);
			}
		}
		return this.listTrigger;
	}
	
	public GetMetaDataSql getTriggerSourceSql()
	{
		if (this.triggerSource == MARKER)
		{
			synchronized (MARKER)
			{
				HashMap sql = this.readStatementTemplates("TriggerSourceStatements.xml");
				this.triggerSource = (GetMetaDataSql)sql.get(this.productName);
			}
		}
		return this.triggerSource;
	}
	
	public String getPrimaryKeyTemplate()
	{
		if (this.primaryKeyTemplate == NO_STRING)
		{
			synchronized (NO_STRING)
			{
				HashMap sql = this.readStatementTemplates("CreatePkStatements.xml");
				this.primaryKeyTemplate = (String)sql.get(this.productName);
				if (this.primaryKeyTemplate == null)
				{
					this.primaryKeyTemplate = (String)sql.get(DbMetadata.GENERAL_SQL);
				}
			}
		}
		return this.primaryKeyTemplate;
	}
	
	public String getForeignKeyTemplate(boolean createInline)
	{
		if (this.foreignKeyTemplate == NO_STRING)
		{
			synchronized (MARKER)
			{
				HashMap sql = this.readStatementTemplates("CreateFkStatements.xml");
				String template = (String)sql.get(this.productName);
				if (template == null)
				{
					if (createInline)
					{
						template = (String)sql.get("All-Inline");
					}
					else
					{
						template = (String)sql.get(DbMetadata.GENERAL_SQL);
					}
				}
				this.foreignKeyTemplate = template;
			}
		}
		return this.foreignKeyTemplate;
	}
	
	public String getIndexTemplate()
	{
		if (this.indexTemplate == NO_STRING)
		{
			synchronized (NO_STRING)
			{
				HashMap sql = this.readStatementTemplates("CreateIndexStatements.xml");
				this.indexTemplate = (String)sql.get(this.productName);
				if (indexTemplate == null)
				{
					this.indexTemplate = (String)sql.get(DbMetadata.GENERAL_SQL);
				}
			}
		}
		return this.indexTemplate;
	}
	
	public String getColumnCommentSql()
	{
		if (this.columnCommentTemplate == NO_STRING)
		{
			synchronized (NO_STRING)
			{
				HashMap sql = this.readStatementTemplates("ColumnCommentStatements.xml");
				this.columnCommentTemplate = (String)sql.get(this.productName);
				if (columnCommentTemplate == null)
				{
					this.columnCommentTemplate = (String)sql.get(DbMetadata.GENERAL_SQL);
				}
			}
		}
		return this.columnCommentTemplate;
	}
	
	public String getTableCommentSql()
	{
		if (this.tableCommentTemplate == NO_STRING)
		{
			synchronized (NO_STRING)
			{
				HashMap sql = this.readStatementTemplates("TableCommentStatements.xml");
				this.tableCommentTemplate = (String)sql.get(this.productName);
				if (tableCommentTemplate == null)
				{
					this.tableCommentTemplate = (String)sql.get(DbMetadata.GENERAL_SQL);
				}
			}
		}
		return this.tableCommentTemplate;
	}
	
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
			LogMgr.logError("MetaDataSqlManager.readStatementTemplate()", "Error reading templates file " + aFilename,e);
			value = null;
		}

		if (value != null && value instanceof HashMap)
		{
			result = (HashMap)value;
		}

		// Try to read the file in the current directory.
		File f = new File(aFilename);
		if (!f.exists())
		{
			String dir = Settings.getInstance().getConfigDir();
			f = new File(dir, aFilename);
		}
		if (f.exists())
		{
			LogMgr.logInfo("DbMetadata.readStatementTemplates()", "Reading user defined template file " + f.getAbsolutePath());
			// try to read additional definitions from local file
			try
			{
				WbPersistence reader = new WbPersistence(aFilename);
				value = reader.readObject();
			}
			catch (Exception e)
			{
				LogMgr.logDebug("MetaDataSqlManager.readStatementTemplate()", "Error reading template file " + aFilename, e);
			}
			if (value != null && value instanceof HashMap)
			{
				HashMap m = (HashMap)value;
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
		else
		{
			LogMgr.logDebug("MetaDataSqlManager.readStatementTemplates()", "No user defined template file found for " + aFilename);		
		}
		return result;
	}
	
}
