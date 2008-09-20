/*
 * TableIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import workbench.resource.ResourceMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class TableIdentifier
	implements DbObject, Comparable<TableIdentifier>
{
	private String tablename;
	private String schema;
	private String catalog;
	private String expression;
	private boolean isNewTable;
	private boolean tableWasQuoted; 
	private boolean catalogWasQuoted; 
	private boolean schemaWasQuoted; 
	private String pkName;
	private String type;
	private boolean neverAdjustCase;
	private boolean preserveQuotes;
	private boolean showOnlyTableName;
	
	public TableIdentifier(String aName)
	{
		this.expression = null;
		this.isNewTable = false;
		this.setTable(aName);
	}
	
	public TableIdentifier(String aName, WbConnection conn)
	{
		this.expression = null;
		this.isNewTable = false;
		this.setTable(aName);
		this.adjustCase(conn);
	}

	/**
	 * Initialize a TableIdentifier for a new (to be defined) table
	 * This is mainly used by the {@link workbench.db.datacopy.DataCopier}
	 * to flag the target table to be created on the fly
	 */
	public TableIdentifier()
	{
		this.expression = null;
		this.schema = null;
		this.catalog = null;
		this.tablename = null;
		this.isNewTable = true;
	}

	public TableIdentifier(String aSchema, String aTable)
	{
		this.setCatalog(null);
		this.setTable(aTable);
		this.setSchema(aSchema);
	}

	public TableIdentifier(String aCatalog, String aSchema, String aTable)
	{
		this.setTable(aTable);
		this.setCatalog(aCatalog);
		this.setSchema(aSchema);
	}

	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.tablename);
	}
	
	public String getObjectExpression(WbConnection conn)
	{
		return getTableExpression(conn);
	}
	
	public String getObjectType()
	{
		if (type == null) return "TABLE";
		return type.toUpperCase();
	}
	
	public String getObjectName()
	{
		return getTableName();
	}
	
	public void setPreserveQuotes(boolean flag)
	{
		this.preserveQuotes = flag;
	}
	
	public boolean getNeverAdjustCase() 
	{
		return this.neverAdjustCase;
	}
	
	public void setNeverAdjustCase(boolean flag)
	{
		this.neverAdjustCase = flag;
	}

	public void checkQuotesNeeded(WbConnection con)
	{
		if (con == null) return;
		DbMetadata meta = con.getMetadata();
		this.schemaWasQuoted = !meta.isDefaultCase(this.schema);
		this.catalogWasQuoted = !meta.isDefaultCase(this.catalog);
		this.tableWasQuoted = !meta.isDefaultCase(this.tablename);
		this.preserveQuotes = (this.schemaWasQuoted || this.catalogWasQuoted || this.tableWasQuoted );
//		if (!preserveQuotes)
//		{
//			this.setNeverAdjustCase(false);
//		}
	}
	
	public TableIdentifier createCopy()
	{
		TableIdentifier copy = new TableIdentifier();
		copy.isNewTable = this.isNewTable;
		copy.pkName = this.pkName;
		copy.schema = this.schema;
		copy.tablename = this.tablename;
		copy.catalog = this.catalog;
		copy.expression = null;
		copy.neverAdjustCase = this.neverAdjustCase;
		copy.tableWasQuoted = this.tableWasQuoted;
		copy.catalogWasQuoted = this.catalogWasQuoted;
		copy.schemaWasQuoted = this.schemaWasQuoted;
		copy.showOnlyTableName = this.showOnlyTableName;
		copy.preserveQuotes = this.preserveQuotes;
		copy.type = this.type;
		return copy;
	}
	
	public String getTableExpression()
	{
		if (this.expression == null) this.initExpression();
		return this.expression;
	}

	public int hashCode()
	{
		return getTableExpression().hashCode();
	}
	
	public String getTableExpression(WbConnection conn)
	{
		return this.buildTableExpression(conn);
	}

	private void initExpression()
	{
		this.expression = this.buildTableExpression(null);
	}
	
	private String buildTableExpression(WbConnection conn)
	{
		if (this.isNewTable)
		{
			if (this.tablename == null)
			{
				return ResourceMgr.getString("TxtNewTableIdentifier");
			}
			else
			{
				return this.tablename;
			}
		}

		StringBuilder result = new StringBuilder(30);
		if (conn == null)
		{
			if (this.catalog != null)
			{
				result.append(SqlUtil.quoteObjectname(this.catalog, preserveQuotes && catalogWasQuoted));
				result.append('.');
			}
			if (this.schema != null)
			{
				result.append(SqlUtil.quoteObjectname(this.schema, preserveQuotes && schemaWasQuoted));
				result.append('.');
			}
			result.append(SqlUtil.quoteObjectname(this.tablename, preserveQuotes && tableWasQuoted));
		}
		else
		{
			DbMetadata meta = conn.getMetadata();
			this.adjustCase(conn);
			if (meta.needCatalogInDML(this))
			{
				String catalogToUse = this.catalog;
				if (catalogToUse == null)
				{
					catalogToUse = meta.getCurrentCatalog();
				}
				
				if (catalogToUse != null)
				{
					result.append(meta.quoteObjectname(catalogToUse, preserveQuotes && catalogWasQuoted));
					result.append('.');
				}
			}
			
			if (meta.needSchemaInDML(this))
			{
				String schemaToUse = this.schema;
				if (schemaToUse == null)
				{
					schemaToUse = meta.getSchemaToUse();
				}
				
				if (schemaToUse != null)
				{
					result.append(meta.quoteObjectname(schemaToUse, preserveQuotes && schemaWasQuoted));
					result.append('.');
				}
			}
			
			result.append(meta.quoteObjectname(this.tablename, preserveQuotes && tableWasQuoted));
		}
		return result.toString();
	}

	public void adjustCase(WbConnection conn)
	{
		if (this.neverAdjustCase) return;
		if (conn == null) return;
		DbMetadata meta = conn.getMetadata();
		
		if (this.tablename != null && !tableWasQuoted) this.tablename = meta.adjustObjectnameCase(this.tablename);
		if (this.schema != null && !schemaWasQuoted) this.schema = meta.adjustSchemaNameCase(this.schema);
		if (this.catalog != null && !catalogWasQuoted) this.catalog = meta.adjustObjectnameCase(this.catalog);
		this.expression = null;
	}
	
	/**
	 * Return the fully qualified name of the table 
	 * (including catalog and schema) but not quoted
	 * even if it needed quotes
	 */
	public String getQualifiedName()
	{
		StringBuilder result = new StringBuilder(32);
		if (catalog != null)
		{
			result.append(catalog);
			result.append('.');
		}
		if (schema != null)
		{
			result.append(schema);
			result.append('.');
		}
		result.append(this.tablename);
		return result.toString();
	}
	
	String getRawCatalog() { return this.catalog; }
	String getRawTableName() { return this.tablename; }
	String getRawSchema() { return this.schema; }
	
	public String getTableName() 
	{ 
		if (tablename == null) return null;
		if (!tableWasQuoted || !preserveQuotes) return this.tablename; 
		
		StringBuilder result = new StringBuilder(tablename.length() + 2);
		result.append('\"');
		result.append(tablename);
		result.append('\"');
		return result.toString();
	}

	/**
	 * Check for non-standard quote characters in the table, schema or catalog name
	 * in order to support DBMS who don't even care about the most simple standards.
	 *
	 * This will only re-add the correct quotes later, if getTableExpression(WbConnection) is used!
	 * 
	 * @param quoteCharacter
	 */
	public void checkIsQuoted(DbMetadata meta)
	{

		if (meta.isQuoted(this.tablename))
		{
			this.tableWasQuoted = true;
			this.tablename = this.tablename.substring(1, tablename.length() - 1);
			this.expression = null;
		}
		if (meta.isQuoted(schema))
		{
			this.schemaWasQuoted = true;
			this.schema = this.schema.substring(1, schema.length() - 1);
			this.expression = null;
		}
		if (meta.isQuoted(catalog))
		{
			this.catalogWasQuoted = true;
			this.catalog= this.catalog.substring(1, catalog.length() - 1);
			this.expression = null;
		}
	}

	public void setTable(String aTable)
	{
		if (!this.isNewTable && (aTable == null || aTable.trim().length() == 0))
			throw new IllegalArgumentException("Table name may not be null");

		if (aTable == null)
		{
			this.tablename = null;
			this.schema = null;
			this.expression = null;
			return;
		}

		List<String> elements = new ArrayList<String>(4);
		WbStringTokenizer tok = new WbStringTokenizer('.', "\"", true);
		tok.setSourceString(aTable);
		while (tok.hasMoreTokens())
		{
			elements.add(tok.nextToken());
		}
		
		if (elements.size() == 1)
		{
			setTablename(aTable);
		}
		else if (elements.size() == 2)
		{
			setSchema(elements.get(0));
			setTablename(elements.get(1));
		}
		else if (elements.size() == 3)
		{
			setCatalog(elements.get(0));
			setSchema(elements.get(1));
			setTablename(elements.get(2));
		}

		this.expression = null;
	}

	private void setTablename(String name)
	{
		if (name == null) return;
		tableWasQuoted = name.trim().startsWith("\"");
		this.tablename = StringUtil.trimQuotes(name).trim();
	}
	
	public String getSchema() 
	{ 
		if (schema == null) return null;
		if (!schemaWasQuoted || !preserveQuotes) return schema;
		
		StringBuilder result = new StringBuilder(schema.length() + 2);
		result.append('\"');
		result.append(schema);
		result.append('\"');
		return result.toString();
	}
	
	public void setSchema(String aSchema)
	{
		if (this.isNewTable) return;

		if (aSchema == null || aSchema.trim().length() == 0)
		{
			this.schema = null;
		}
		else
		{
			schemaWasQuoted = aSchema.trim().startsWith("\"");
			this.schema = StringUtil.trimQuotes(aSchema).trim();
		}
		this.expression = null;
	}

	public String getCatalog() 
	{ 
		if (catalog == null) return null;
		if (!catalogWasQuoted || !preserveQuotes) return this.catalog; 
		
		StringBuilder result = new StringBuilder(catalog.length() + 2);
		result.append('\"');
		result.append(catalog);
		result.append('\"');
		return result.toString();
	}
	
	public void setCatalog(String aCatalog)
	{
		if (this.isNewTable) return;

		if (aCatalog == null || aCatalog.trim().length() == 0)
		{
			this.catalog = null;
		}
		else
		{
			catalogWasQuoted = aCatalog.trim().startsWith("\"");
			this.catalog = StringUtil.trimQuotes(aCatalog).trim();
		}
		this.expression = null;
	}

	public String toString()
	{
		if (this.isNewTable)
		{
			if (this.tablename == null)
			{
				return this.getTableExpression();
			}
			else
			{
				return "(+) " + this.tablename;
			}
		}
		else if (this.showOnlyTableName)
		{
			return this.getTableName();
		}
		else
		{
			return this.getTableExpression();
		}
	}
	public boolean isNewTable() { return this.isNewTable; }

	public void setNewTable(boolean flag)
	{
		this.expression = null;
		this.isNewTable = flag;
	}

	public void setShowTablenameOnly(boolean flag)
	{
		this.showOnlyTableName = flag;
	}
	
	public int compareTo(TableIdentifier other)
	{
		return this.getTableExpression().compareTo(other.getTableExpression());
	}
	
	public boolean equals(Object other)
	{
		if (other instanceof TableIdentifier)
		{
			boolean result = false;
			TableIdentifier t = (TableIdentifier)other;
			if (this.isNewTable && t.isNewTable)
			{
				result = true;
			}
			else if (this.isNewTable || t.isNewTable)
			{
				result = false;
			}
			else
			{
				result = this.getTableExpression().equals(t.getTableExpression());
			}
			return result;
		}
		return false;
	}

	/**
	 * Compare this TableIdentifier to another. The schema and catalog fields
	 * are only compared if bothe identifiers have them. This is different
	 * to the equals() method, which returns false if one TableIdentifier has
	 * a schema and/or catalog and the other doesn't
	 */
	public boolean compareNames(TableIdentifier other)
	{
		boolean result = false;
		if (this.isNewTable && other.isNewTable)
		{
			result = true;
		}
		else if (this.isNewTable || other.isNewTable)
		{
			result = false;
		}
		else
		{
			result = this.getTableName().equalsIgnoreCase(other.getTableName());
			if (result && this.schema != null && other.schema != null)
			{
				result = this.schema.equalsIgnoreCase(other.schema);
			}
			if (result && this.catalog != null && other.catalog != null)
			{
				result = this.catalog.equalsIgnoreCase(other.catalog);
			}
		}
		return result;
	}

	public String getPrimaryKeyName()
	{
		return this.pkName;
	}
	
	public void setPrimaryKeyName(String name)
	{
		this.pkName = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		CharSequence source = null;
		DbMetadata meta = con.getMetadata();
		if (DbMetadata.MVIEW_NAME.equalsIgnoreCase(type))
		{
			source = meta.getExtendedViewSource(this, false);
		}
		else if ("SYNONYM".equalsIgnoreCase(type))
		{
			source = meta.getSynonymSource(this);
		}
		else if ("VIEW".equalsIgnoreCase(type))
		{
			source = meta.getExtendedViewSource(this, false);
		}
		else if ("SEQUENCE".equalsIgnoreCase(type))
		{
			SequenceReader reader = meta.getSequenceReader();
			
			source = (reader != null ? reader.getSequenceSource(getSchema(), getTableName()) : null);
		}
		else
		{
			source = meta.getTableSource(this, false, false);
		}
		return source;
	}

	public static TableIdentifier findTableByName(List<TableIdentifier> tables, String toFind)
	{
		return findTableByName(tables, new TableIdentifier(toFind));
	}
	
	public static TableIdentifier findTableByName(List<TableIdentifier> tables, TableIdentifier toFind)
	{
		if (tables == null) return null;
		
		for (TableIdentifier table : tables)
		{
			if (table.getTableName().equalsIgnoreCase(toFind.getTableName())) return table;
		}
		return null;
	}
	
}
