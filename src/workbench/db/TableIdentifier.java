/*
 * TableIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.db.objectcache.DbObjectCacheFactory;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 * A class that represents a table (or view) in the database.
 *
 * @author  Thomas Kellerer
 */
public class TableIdentifier
	implements DbObject, Comparable<TableIdentifier>, Serializable
{
	private static final long serialVersionUID = DbObjectCacheFactory.CACHE_VERSION_UID;

	private String tablename;
	private String schema;
	private String catalog;
	private String server; // for SQL Server syntax
	private String owner;
	private boolean isNewTable;
	private boolean tableWasQuoted;
	private boolean serverWasQuoted;
	private boolean catalogWasQuoted;
	private boolean schemaWasQuoted;
	private PkDefinition primaryKey;
	private String type;
	private boolean neverAdjustCase;
	private boolean preserveQuotes;
	private boolean showOnlyTableName;
	private String tableComment;
	private boolean commentWasInitialized;
	private boolean retrieveFkSource;
	private boolean useInlinePK;
	private boolean useTableNameOnlyInExpression;

	// for Synonyms
	private TableIdentifier realTable;

	private ObjectSourceOptions sourceOptions = new ObjectSourceOptions();

	/**
	 * DBMS specific tablespace options
	 */
	private String tableSpace;

	public TableIdentifier(String aName)
	{
		this.isNewTable = false;
		this.parseTableIdentifier(aName);
	}

	public TableIdentifier(String aName, char catalogSeparator, char schemaSeparator)
	{
		this.isNewTable = false;
		this.parseTableIdentifier(aName, catalogSeparator, schemaSeparator, true, true);
	}

	public TableIdentifier(String aName, char catalogSeparator, char schemaSeparator, boolean supportsCatalogs, boolean supportsSchemas)
	{
		this.isNewTable = false;
		this.parseTableIdentifier(aName, catalogSeparator, schemaSeparator, supportsCatalogs, supportsSchemas);
	}

	public TableIdentifier(String aName, WbConnection conn)
	{
		this.isNewTable = false;
		DbSettings settings = (conn == null ? null : conn.getDbSettings());
		boolean supportsCatalogs = settings == null ? true : settings.supportsCatalogs();
		boolean supportsSchemas = settings == null ? true : settings.supportsSchemas();
		this.parseTableIdentifier(aName, SqlUtil.getCatalogSeparator(conn), SqlUtil.getSchemaSeparator(conn), supportsCatalogs, supportsSchemas);
		this.adjustCase(conn);
	}

	/**
	 * Initialize a TableIdentifier for a new (to be defined) table
	 * This is mainly used by the {@link workbench.db.datacopy.DataCopier}
	 * to flag the target table to be created on the fly
	 */
	public TableIdentifier()
	{
		this.schema = null;
		this.catalog = null;
		this.tablename = null;
		this.isNewTable = true;
	}

	public TableIdentifier(String aSchema, String aTable)
	{
		this.setCatalog(null);
		this.parseTableIdentifier(aTable);
		this.setSchema(aSchema);
	}

	public TableIdentifier(String aCatalog, String aSchema, String aTable)
	{
		this.parseTableIdentifier(aTable);
		this.setCatalog(aCatalog);
		this.setSchema(aSchema);
	}

	public TableIdentifier(String aCatalog, String aSchema, String aTable, boolean parseNames)
	{
		if (parseNames)
		{
			this.parseTableIdentifier(aTable);
		}
		else
		{
			this.setTablename(aTable);
		}
		this.setCatalog(aCatalog);
		this.setSchema(aSchema);
	}

	public boolean getUseNameOnly()
	{
		return useTableNameOnlyInExpression;
	}
	
	public void setUseNameOnly(boolean flag)
	{
		this.useTableNameOnlyInExpression = flag;
	}

	public TableIdentifier getRealTable()
	{
		return realTable;
	}

	public void setRealTable(TableIdentifier targetTable)
	{
		this.realTable = targetTable != null ? targetTable.createCopy() : null;
	}

	public String getOwner()
	{
		return owner;
	}

	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	/**
	 * Return the tablespace used for this table (if applicable)
	 */
	public String getTablespace()
	{
		return tableSpace;
	}

	public void setTablespace(String tableSpaceName)
	{
		this.tableSpace = tableSpaceName;
	}

	/**
	 * Return DBMS specific configuration options for this table.
	 */
	public String getAdditionalSql()
	{
		return sourceOptions.getAdditionalSql();
	}

	public void setAdditionalSql(String options)
	{
		this.sourceOptions.setAdditionalSql(options);
	}

	/**
	 * Define the source options to be used.
	 *
	 * @param options  the new options. If null, the call is ignored
	 */
	public void setSourceOptions(ObjectSourceOptions options)
	{
		if (options != null)
		{
			this.sourceOptions = options;
		}
	}

	/**
	 * Returns the source options to build the table's SQL
	 *
	 * @return the options. Never null
	 */
	public ObjectSourceOptions getSourceOptions()
	{
		return this.sourceOptions;
	}

	public boolean getUseInlinePK()
	{
		return useInlinePK;
	}

	public void setUseInlinePK(boolean flag)
	{
		this.useInlinePK = flag;
	}

	@Override
	public void setComment(String comment)
	{
		this.commentWasInitialized = true;
		this.tableComment = comment;
	}

	@Override
	public String getComment()
	{
		return tableComment;
	}

	public boolean commentIsDefined()
	{
		return commentWasInitialized;
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return null;
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return getTableExpression(con);
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		if (conn == null) return SqlUtil.quoteObjectname(tablename, tableWasQuoted);
		return conn.getMetadata().quoteObjectname(this.tablename);
	}

	@Override
	public String getFullyQualifiedName(WbConnection con)
	{
		return SqlUtil.fullyQualifiedName(con, this);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return getTableExpression(conn);
	}

	@Override
	public String getObjectType()
	{
		if (type == null) return "TABLE";
		return type.toUpperCase();
	}

	@Override
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
		if (meta == null) return;
		this.schemaWasQuoted = !meta.isDefaultCase(this.schema);
		this.catalogWasQuoted = !meta.isDefaultCase(this.catalog);
		this.tableWasQuoted = !meta.isDefaultCase(this.tablename);
		this.preserveQuotes = (this.schemaWasQuoted || this.catalogWasQuoted || this.tableWasQuoted );
	}

	public TableIdentifier createCopy()
	{
		TableIdentifier copy = new TableIdentifier();
		copy.isNewTable = this.isNewTable;
		copy.primaryKey = this.primaryKey == null ? null : this.primaryKey.createCopy();
		copy.schema = this.schema;
		copy.tablename = this.tablename;
		copy.catalog = this.catalog;
		copy.server = this.server;
		copy.neverAdjustCase = this.neverAdjustCase;
		copy.serverWasQuoted = this.serverWasQuoted;
		copy.tableWasQuoted = this.tableWasQuoted;
		copy.catalogWasQuoted = this.catalogWasQuoted;
		copy.schemaWasQuoted = this.schemaWasQuoted;
		copy.showOnlyTableName = this.showOnlyTableName;
		copy.preserveQuotes = this.preserveQuotes;
		copy.type = this.type;
		copy.retrieveFkSource = this.retrieveFkSource;
		copy.commentWasInitialized = this.commentWasInitialized;
		copy.tableComment = this.tableComment;
		copy.sourceOptions = this.sourceOptions == null ? null : sourceOptions.createCopy();
		copy.useInlinePK = this.useInlinePK;
		copy.owner = this.owner;
		copy.realTable = this.realTable == null ? null : realTable.createCopy();
		copy.useTableNameOnlyInExpression = this.useTableNameOnlyInExpression;
		return copy;
	}

	public String getTableExpression()
	{
		return buildTableExpression(null, '.', '.');
	}

	public String getTableExpression(char catalogSeparator, char schemaSeparator)
	{
		return this.buildTableExpression(null, catalogSeparator, schemaSeparator);
	}

	@Override
	public int hashCode()
	{
		return getTableExpression().hashCode();
	}

	public String getTableExpression(WbConnection conn)
	{
		char catalogSeparator = SqlUtil.getCatalogSeparator(conn);
		char schemaSeparator = SqlUtil.getSchemaSeparator(conn);
		return this.buildTableExpression(conn, catalogSeparator, schemaSeparator);
	}

	private String buildTableExpression(WbConnection conn, char catalogSeparator, char schemaSeparator)
	{
		if (this.isNewTable && this.tablename == null)
		{
			return ResourceMgr.getString("TxtNewTableIdentifier");
		}

		StringBuilder result = new StringBuilder(30);
		if (this.server != null)
		{
			result.append(SqlUtil.quoteObjectname(this.server, false));
			result.append('.');
		}
		if (conn == null)
		{
			if (this.catalog != null && !useTableNameOnlyInExpression)
			{
				result.append(SqlUtil.quoteObjectname(this.catalog, preserveQuotes && catalogWasQuoted, true, '"'));
				result.append(catalogSeparator);
			}
			if (this.schema != null && !useTableNameOnlyInExpression)
			{
				result.append(SqlUtil.quoteObjectname(this.schema, preserveQuotes && schemaWasQuoted, true, '"'));
				result.append(schemaSeparator);
			}
			result.append(SqlUtil.quoteObjectname(this.tablename, preserveQuotes && tableWasQuoted, true, '"'));
		}
		else
		{
			DbMetadata meta = conn.getMetadata();
			this.adjustCase(conn);
			String catalogToUse = getCatalogToUse(conn);
			boolean hasCatalog = false;
			if (StringUtil.isNonBlank(catalogToUse))
			{
				hasCatalog = true;
				result.append(meta.quoteObjectname(catalogToUse, preserveQuotes && catalogWasQuoted));
				result.append(catalogSeparator);
			}

			String schemaToUse = getSchemaToUse(conn);

			// if a catalog is present we always need the schema as the combination catalog.tablename is not valid
			// (this is mainly needed for SQL Server)
			if (hasCatalog && StringUtil.isBlank(schemaToUse))
			{
				schemaToUse = conn.getCurrentSchema();
			}

			if (StringUtil.isNonBlank(schemaToUse))
			{
				result.append(meta.quoteObjectname(schemaToUse, preserveQuotes && schemaWasQuoted));
				result.append(schemaSeparator);
			}
			result.append(meta.quoteObjectname(this.tablename, preserveQuotes && tableWasQuoted));
		}
		return result.toString();
	}

	public String getSchemaToUse(WbConnection conn)
	{
		if (useTableNameOnlyInExpression) return null;

		DbMetadata meta = conn.getMetadata();
		if (meta.needSchemaInDML(this))
		{
			String schemaToUse = this.schema;
			String currentSchema = null;
			if (this.schema == null)
			{
				currentSchema = meta.getCurrentSchema();
				schemaToUse = currentSchema;
			}

			if (meta.ignoreSchema(schemaToUse, currentSchema)) return null;

			return StringUtil.trim(schemaToUse);
		}
		return null;
	}

	public String getCatalogToUse(WbConnection conn)
	{
		if (useTableNameOnlyInExpression) return null;

		DbMetadata meta = conn.getMetadata();
		if (meta.needCatalogInDML(this))
		{
			String catalogToUse = this.catalog;
			if (catalogToUse == null)
			{
				catalogToUse = meta.getCurrentCatalog();
			}

			if (meta.ignoreCatalog(catalogToUse)) return null;

			return StringUtil.trim(catalogToUse);
		}
		return null;
	}

	public final void adjustCase(WbConnection conn)
	{
		if (this.neverAdjustCase) return;
		if (conn == null) return;
		DbMetadata meta = conn.getMetadata();

		if (this.tablename != null && !tableWasQuoted) this.tablename = meta.adjustObjectnameCase(this.tablename);
		if (this.schema != null && !schemaWasQuoted) this.schema = meta.adjustSchemaNameCase(this.schema);
		if (this.catalog != null && !catalogWasQuoted) this.catalog = meta.adjustObjectnameCase(this.catalog);
	}

	/**
	 * Return the fully qualified name of the table
	 * (including catalog and schema) but not quoted
	 * even if it needed quotes
	 */
	public String getQualifiedName()
	{
		StringBuilder result = new StringBuilder(32);
		if (this.server != null)
		{
			result.append(server);
			result.append('.');
		}
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

	public String getRawCatalog()
	{
		return this.catalog;
	}

	public String getRawTableName()
	{
		return this.tablename;
	}

	public String getRawSchema()
	{
		return this.schema;
	}

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
	 * @param meta the DbMetadata object used for checking if a name component is quoted
	 * @see workbench.db.DbMetadata#isQuoted(java.lang.String)
	 */
	public void checkIsQuoted(DbMetadata meta)
	{
		if (meta.isQuoted(server))
		{
			this.serverWasQuoted = true;
			this.server = server.substring(1, server.length() - 1);
		}
		if (meta.isQuoted(this.tablename))
		{
			this.tableWasQuoted = true;
			this.tablename = this.tablename.substring(1, tablename.length() - 1);
		}
		if (meta.isQuoted(schema))
		{
			this.schemaWasQuoted = true;
			this.schema = this.schema.substring(1, schema.length() - 1);
		}
		if (meta.isQuoted(catalog))
		{
			this.catalogWasQuoted = true;
			this.catalog= this.catalog.substring(1, catalog.length() - 1);
		}
	}

	public final void parseTableIdentifier(String aTable)
	{
		parseTableIdentifier(aTable, '.', '.', true, true);
	}

	/**
	 * Parse a table identifier into the different parts.
	 *
	 * If a name with two elements (e.g. foo.bar) is passed, the flags for schema/catalogs determine what the
	 * first part is used for. If both are supported or only schemas, the first part is a schema. If only catalogs
	 * are supported the first part is a catalog.
	 *
	 * @param tableId  the identifier (name) e.g. used by the user
	 * @param catalogSeparator  the catalog separator
	 * @param schemaSeparator   the schema separator
	 * @param supportsCatalogs  flag if catalogs are supported
	 * @param supportsSchemas   flag if schemas are supported
	 */
	public final void parseTableIdentifier(String tableId, char catalogSeparator, char schemaSeparator, boolean supportsCatalogs, boolean supportsSchemas)
	{
		if (!this.isNewTable && (StringUtil.isBlank(tableId)))
		{
			throw new IllegalArgumentException("Table name may not be empty");
		}

		if (tableId == null)
		{
			// this is a "new table"
			this.tablename = null;
			this.schema = null;
			this.catalog = null;
			return;
		}

		WbStringTokenizer tok = new WbStringTokenizer(schemaSeparator, "\"", true);
		tok.setSourceString(tableId);
		List<String> elements = tok.getAllTokens();

		if (elements.size() == 1)
		{
			// if only one element is found it could still be a two element identifier
			// in case the catalog separator is different from the schema separator (e.g. DB2 for iSeries)
			setCatalog(getCatalogPart(tableId, catalogSeparator));
			setTablename(getNamePart(tableId, catalogSeparator));
		}
		else if (elements.size() == 2)
		{
			if (supportsSchemas && supportsCatalogs)
			{
				setCatalog(getCatalogPart(elements.get(0), catalogSeparator));
				setSchema(getNamePart(elements.get(0), catalogSeparator));
			}
			if (supportsSchemas && !supportsCatalogs)
			{
				// no catalog supported, the first element must be the schema
				setSchema(elements.get(0));
			}
			if (supportsCatalogs && !supportsSchemas)
			{
				// e.g. MySQL qualifier: database.tablename
				setCatalog(elements.get(0));
			}
			setTablename(elements.get(1));
		}
		else if (elements.size() == 3)
		{
			// no ambiguity if three elements are used
			setCatalog(elements.get(0));
			setSchema(elements.get(1));
			setTablename(elements.get(2));
		}
		else if (elements.size() == 4)
		{
			// support for SQL Server syntax with a linked server
			setServerPart(elements.get(0));
			setCatalog(elements.get(1));
			setSchema(elements.get(2));
			setTablename(elements.get(3));
		}
	}

	protected static String getCatalogPart(String identifier, char catalogSeparator)
	{
		if (identifier == null) return identifier;
		WbStringTokenizer tok = new WbStringTokenizer(catalogSeparator, "\"", true);
		tok.setSourceString(identifier);
		List<String> tokens = tok.getAllTokens();
		if (tokens.size() == 2)
		{
			return tokens.get(0);
		}
		return null;
	}

	protected static String getNamePart(String identifier, char catalogSeparator)
	{
		if (identifier == null) return identifier;
		WbStringTokenizer tok = new WbStringTokenizer(catalogSeparator, "\"", true);
		tok.setSourceString(identifier);
		List<String> tokens = tok.getAllTokens();
		if (tokens.size() == 2)
		{
			return tokens.get(1);
		}
		return tokens.get(0);
	}

	/**
	 * For a table specified using SQL Server's extended syntax
	 * that allows 4 elements, return the first element. Usually this is the
	 * name of the linked server.
	 *
	 * @return the name of the server part if specified, null otherwise
	 */
	public String getServerPart()
	{
		return server;
	}

	public void setServerPart(String name)
	{
		if (StringUtil.isBlank(name))
		{
			server = null;
		}
		else
		{
			server = name.trim();
		}
	}

	private void setTablename(String name)
	{
		if (name == null) return;
		tableWasQuoted = SqlUtil.isQuotedIdentifier(name.trim());
		tablename = SqlUtil.removeObjectQuotes(name).trim();
	}

	@Override
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

	public final void setSchema(String aSchema)
	{
		if (StringUtil.isBlank(aSchema))
		{
			this.schema = null;
		}
		else
		{
			schemaWasQuoted = SqlUtil.isQuotedIdentifier(aSchema.trim());
			schema = SqlUtil.removeObjectQuotes(aSchema).trim();
		}
	}

	@Override
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

	public final void setCatalog(String aCatalog)
	{
		if (this.isNewTable) return;

		if (StringUtil.isBlank(aCatalog))
		{
			this.catalog = null;
		}
		else
		{
			catalogWasQuoted = SqlUtil.isQuotedIdentifier(aCatalog.trim());
			catalog = SqlUtil.removeObjectQuotes(aCatalog).trim();
		}
	}

	@Override
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

	public boolean isNewTable()
	{
		return this.isNewTable;
	}

	public void setNewTable(boolean flag)
	{
		this.isNewTable = flag;
	}

	public void setShowTablenameOnly(boolean flag)
	{
		this.showOnlyTableName = flag;
	}

	@Override
	public int compareTo(TableIdentifier other)
	{
		return this.getTableExpression().compareTo(other.getTableExpression());
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TableIdentifier)
		{
			boolean result;
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
	 * Compare this TableIdentifier to another.
	 *
	 * The schema and catalog fields are only compared if both identifiers have them.
	 *
	 * This is different to the equals() method, which returns false if one TableIdentifier has
	 * a schema and/or catalog and the other doesn't
	 *
	 * @return true if both tables have the same name and/or schema/catalog
	 */
	public boolean compareNames(TableIdentifier other)
	{
		boolean result;
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
		if (this.primaryKey == null) return null;
		return this.primaryKey.getPkName();
	}

	public PkDefinition getPrimaryKey()
	{
		return this.primaryKey;
	}

	public void setPrimaryKey(PkDefinition pk)
	{
		this.primaryKey = pk;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		return getSource(con, retrieveFkSource, con.getDbSettings().getGenerateTableGrants());
	}

	public CharSequence getSource(WbConnection con, boolean includeFk, boolean includeGrants)
		throws SQLException
	{
		CharSequence source;
		DbMetadata meta = con.getMetadata();
		if (meta.isExtendedObject(this))
		{
			return meta.getObjectSource(this);
		}
		else if (DbMetadata.MVIEW_NAME.equalsIgnoreCase(type))
		{
			source = meta.getViewReader().getExtendedViewSource(new TableDefinition(this, null), false, false);
		}
		else if ("SYNONYM".equalsIgnoreCase(type))
		{
			SynonymDDLHandler synHandler = new SynonymDDLHandler();
			source = synHandler.getSynonymSource(con, this, false, false);
		}
		else if ("VIEW".equalsIgnoreCase(type))
		{
			source = meta.getViewReader().getExtendedViewSource(new TableDefinition(this, null), false, false);
		}
		else if (con.getMetadata().isSequenceType(type))
		{
			SequenceReader reader = meta.getSequenceReader();
			source = (reader != null ? reader.getSequenceSource(getCatalog(), getSchema(), getTableName()) : null);
		}
		else
		{
			TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(con);
			source = builder.getTableSource(this, false, includeFk, includeGrants);
		}
		return source;
	}

	/**
	 * Controls if getSource() will return the source for the FK constraints as well.
	 *
	 * @param flag true, getSource() will include FK source, false: FK source will not be included
	 */
	public void setRetrieveFkSource(boolean flag)
	{
		retrieveFkSource = flag;
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

	public static TableIdentifier findTableByNameAndSchema(List<TableIdentifier> tables, TableIdentifier toFind)
	{
		if (tables == null) return null;
		String nameToFind = getQualifiedName(toFind);

		for (TableIdentifier table : tables)
		{
			String name = getQualifiedName(table);
			if (name.equalsIgnoreCase(nameToFind)) return table;
		}
		return null;
	}

	private static String getQualifiedName(TableIdentifier table)
	{
		String name = table.getTableName();
		String schema = table.getSchema();
		if (StringUtil.isEmptyString(schema) && StringUtil.isNonEmpty(table.getCatalog()))
		{
			schema = table.getCatalog();
		}
		if (StringUtil.isEmptyString(schema)) return name;
		return schema + "." + name;
	}

	public static boolean tablesAreEqual(TableIdentifier one, TableIdentifier other, WbConnection con)
	{
		if (one == null || other == null) return false;
		if (con == null)
		{
			return one.equals(other);
		}
		TableIdentifier tbl1 = one.createCopy();
		if (tbl1.getSchema() == null)
		{
			tbl1.setSchema(con.getCurrentSchema());
		}
		if (tbl1.getCatalog() == null)
		{
			tbl1.setCatalog(con.getCurrentCatalog());
		}

		TableIdentifier tbl2 = other.createCopy();
		if (tbl2.getSchema() == null)
		{
			tbl2.setSchema(con.getCurrentSchema());
		}
		if (tbl2.getCatalog() == null)
		{
			tbl2.setCatalog(con.getCurrentCatalog());
		}

		String expr1 = tbl1.getTableExpression(con);
		String expr2 = tbl2.getTableExpression(con);
		return expr1.equalsIgnoreCase(expr2);
	}
}
