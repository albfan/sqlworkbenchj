/*
 * GetMetaDataSql.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

/**
 *
 * @author Thomas Kellerer
 */
public class GetMetaDataSql
{
	private String baseSql;
	private String schema;
	private String schemaField;
	private String catalog;
	private String catalogField;
	private String objectName;
	private String objectNameField;
	private String orderBy;
	private boolean useUpperCase;
	private boolean useLowerCase;
	private boolean isProcedureCall;
	private int schemaArgumentPos;
	private int catalogArgumentPos;
	private int objectNameArgumentPos;

	private String baseObjectName;
	private String baseObjectCatalog;
	private String baseObjectSchema;

	private String baseObjectNameField;
	private String baseObjectCatalogField;
	private String baseObjectSchemaField;

	public String getSql()
	{
		if (this.isProcedureCall) return this.getProcedureCallSql();
		else return this.getSelectSql();
	}

	private String getSelectSql()
	{
		boolean needsAnd = false;
		boolean needsWhere = false;
		StringBuilder sql = new StringBuilder(baseSql);
		if (baseSql.toLowerCase().indexOf("where") == -1 || baseSql.trim().endsWith(")"))
		{
			needsWhere = true;
		}
		else
		{
			needsAnd = true;
		}

		if (schema != null && schemaField != null)
		{
			if (needsWhere)
			{
				sql.append(" WHERE ");
				needsWhere = false;
			}

			if (needsAnd) sql.append(" AND ");
			sql.append(schemaField + " = '" + getNameValue(schema) + "'");
			needsAnd = true;
		}

		if (catalog != null && catalogField != null)
		{
			if (needsWhere)
			{
				sql.append(" WHERE ");
				needsWhere = false;
			}
			if (needsAnd) sql.append(" AND ");
			sql.append(catalogField + " = '" + getNameValue(catalog) + "'");
			needsAnd = true;
		}

		if (objectName != null && objectNameField != null)
		{
			if (needsWhere)
			{
				sql.append(" WHERE ");
				needsWhere = false;
			}
			if (needsAnd) sql.append(" AND ");
			sql.append(objectNameField + " = '" + getNameValue(objectName) + "'");
			needsAnd = true;
		}

		if (baseObjectName != null && baseObjectNameField != null)
		{
			sql.append(" AND ");
			sql.append(baseObjectNameField + " = '" + getNameValue(baseObjectName ) + "'");
		}

		if (baseObjectCatalog != null && baseObjectCatalogField != null)
		{
			sql.append(" AND ");
			sql.append(baseObjectCatalogField + " = '" + getNameValue(baseObjectCatalog) + "'");
		}

		if (baseObjectSchema != null && baseObjectSchemaField != null)
		{
			sql.append(" AND ");
			sql.append(baseObjectSchemaField + " = '" + getNameValue(baseObjectSchema) + "'");
		}

		if (this.orderBy != null)
		{
			sql.append(" " + this.orderBy);
		}
		return sql.toString();
	}

	private String getNameValue(String value)
	{
		if (value == null) return null;
		if (useLowerCase) return value.toLowerCase();
		if (useUpperCase) return value.toUpperCase();
		return value;
	}

	private String getProcedureCallSql()
	{
		StringBuilder sql = new StringBuilder(this.baseSql);
		sql.append(' ');
		for (int i = 1; i < 4; i++)
		{
			if (schemaArgumentPos == i && this.schema != null)
			{
				if (i > 1) sql.append(',');
				sql.append(this.schema);
			}
			else if (catalogArgumentPos == i && this.catalog != null)
			{
				if (i > 1) sql.append(',');
				sql.append(this.catalog);
			}
			else if (this.objectNameArgumentPos == i && this.objectName != null)
			{
				if (i > 1) sql.append(',');
				sql.append(this.objectName);
			}
		}
		return sql.toString();
	}

	public String getBaseSql()
	{
		return baseSql;
	}

	public void setBaseSql(String sql)
	{
		this.baseSql = sql;
	}

	public String getSchema()
	{
		return schema;
	}

	public void setSchema(String schem)
	{
		this.schema = schem;
	}

	public String getCatalog()
	{
		return catalog;
	}

	public void setCatalog(String cat)
	{
		this.catalog = cat;
	}

	public String getObjectName()
	{
		return objectName;
	}

	public void setObjectName(String name)
	{
		this.objectName = name;
	}

	public String toString()
	{
		return getSql();
	}

	public String getSchemaField()
	{
		return schemaField;
	}

	public void setSchemaField(String field)
	{
		this.schemaField = field;
	}

	public String getCatalogField()
	{
		return catalogField;
	}

	public void setCatalogField(String field)
	{
		this.catalogField = field;
	}

	public String getObjectNameField()
	{
		return objectNameField;
	}

	public void setObjectNameField(String field)
	{
		this.objectNameField = field;
	}

	public String getOrderBy()
	{
		return orderBy;
	}

	public void setOrderBy(String order)
	{
		this.orderBy = order;
	}

	public boolean getUseUpperCase()
	{
		return useUpperCase;
	}

	public void setUseUpperCase(boolean upperCase)
	{
		this.useUpperCase = upperCase;
	}

	public boolean getUseLowerCase()
	{
		return useLowerCase;
	}

	public void setUseLowerCase(boolean lowerCase)
	{
		this.useLowerCase = lowerCase;
	}

	public boolean isIsProcedureCall()
	{
		return isProcedureCall;
	}

	public void setIsProcedureCall(boolean isCall)
	{
		this.isProcedureCall = isCall;
	}

	public int getSchemaArgumentPos()
	{
		return schemaArgumentPos;
	}

	public void setSchemaArgumentPos(int pos)
	{
		this.schemaArgumentPos = pos;
	}

	public int getCatalogArgumentPos()
	{
		return catalogArgumentPos;
	}

	public void setCatalogArgumentPos(int pos)
	{
		this.catalogArgumentPos = pos;
	}

	public int getObjectNameArgumentPos()
	{
		return objectNameArgumentPos;
	}

	public void setObjectNameArgumentPos(int pos)
	{
		this.objectNameArgumentPos = pos;
	}

	public String getBaseObjectCatalog()
	{
		return baseObjectCatalog;
	}

	public void setBaseObjectCatalog(String baseObjectCatalog)
	{
		this.baseObjectCatalog = baseObjectCatalog;
	}

	public String getBaseObjectCatalogField()
	{
		return baseObjectCatalogField;
	}

	public void setBaseObjectCatalogField(String baseObjectCatalogField)
	{
		this.baseObjectCatalogField = baseObjectCatalogField;
	}

	public String getBaseObjectName()
	{
		return baseObjectName;
	}

	public void setBaseObjectName(String baseObjectName)
	{
		this.baseObjectName = baseObjectName;
	}

	public String getBaseObjectNameField()
	{
		return baseObjectNameField;
	}

	public void setBaseObjectNameField(String baseObjectNameField)
	{
		this.baseObjectNameField = baseObjectNameField;
	}

	public String getBaseObjectSchema()
	{
		return baseObjectSchema;
	}

	public void setBaseObjectSchema(String baseObjectSchema)
	{
		this.baseObjectSchema = baseObjectSchema;
	}

	public String getBaseObjectSchemaField()
	{
		return baseObjectSchemaField;
	}

	public void setBaseObjectSchemaField(String baseObjectSchemaField)
	{
		this.baseObjectSchemaField = baseObjectSchemaField;
	}

}
