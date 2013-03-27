/*
 * TableSourceBuilder.java
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
package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import workbench.db.sqltemplates.ColumnDefinitionTemplate;
import workbench.db.sqltemplates.ConstraintNameTester;
import workbench.db.sqltemplates.FkTemplate;
import workbench.db.sqltemplates.PkTemplate;
import workbench.db.sqltemplates.TemplateHandler;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Re-create the source SQL for a given TableIdentifier.
 *
 * This class should not be instantiated directly. Use
 * TableSourceBuilderFactory.getBuilder() instead
 *
 * @author Thomas Kellerer
 * @see TableSourceBuilderFactory#getBuilder(workbench.db.WbConnection)
 */
public class TableSourceBuilder
{
	protected WbConnection dbConnection;
	private ConstraintNameTester nameTester;

	/**
	 * This class should not be instantiated directly.
	 *
	 * Use TableSourceBuilderFactory.getBuilder() instead.
	 *
	 * @param con the connection to be used
	 * @see TableSourceBuilderFactory#getBuilder(workbench.db.WbConnection)
	 */
	protected TableSourceBuilder(WbConnection con)
	{
		dbConnection = con;
		nameTester = new ConstraintNameTester(con.getDbId());
	}

	protected ViewReader getViewReader()
	{
		return dbConnection.getMetadata().getViewReader();
	}

	protected IndexReader getIndexReader()
	{
		return dbConnection.getMetadata().getIndexReader();
	}

	/**
   * Return the SQL statement to re-create the given table. (in the dialect for the
	 * current DBMS)
   *
	 * @return the SQL statement to create the given table.
	 * @param table the table for which the source should be retrieved
	 * @param includeDrop If true, a DROP TABLE statement will be included in the generated SQL script.
	 * @param includeFk if true, the foreign key constraints will be added after the CREATE TABLE
	 * @throws SQLException
	 */
	public String getTableSource(TableIdentifier table, boolean includeDrop, boolean includeFk)
		throws SQLException
	{
		if (dbConnection.getDbSettings().isViewType(table.getType()))
		{
			CharSequence s = getViewReader().getExtendedViewSource(table, includeDrop);
			if (s == null) return null;
			return s.toString();
		}
		DbMetadata meta = dbConnection.getMetadata();

		TableIdentifier tbl = table.createCopy();
		tbl.adjustCase(this.dbConnection);

		TableDefinition def = meta.getTableDefinition(tbl);
		List<ColumnIdentifier> cols = def.getColumns();
		List<IndexDefinition> indexDef = getIndexReader().getTableIndexList(def.getTable());
		DataStore fkDef = null;
		if (includeFk)
		{
			FKHandler fk = FKHandlerFactory.createInstance(dbConnection);
			fkDef = fk.getForeignKeys(def.getTable(), false);
		}
		String source = this.getTableSource(def.getTable(), cols, indexDef, fkDef, includeDrop, includeFk);
		return source;
	}

	public String getTableSource(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		List<IndexDefinition> indexInfo = getIndexReader().getTableIndexList(table);
		return getTableSource(table, columns, indexInfo, null, false, true);
	}

	private boolean isFKName(String name, DataStore foreignKeys)
	{
		if (StringUtil.isEmptyString(name)) return false;
		for (int row=0; row < foreignKeys.getRowCount(); row ++)
		{
			String fkname = foreignKeys.getValueAsString(row, FKHandler.COLUMN_IDX_FK_DEF_FK_NAME);
			if (name.equalsIgnoreCase(fkname)) return true;
		}
		return false;
	}

	/**
	 * Returns the indexes that should really be re-created.
	 *
	 * If the DBMS automatically creates an index when a FK constraint is defined (e.g. MySQL)
	 * the corresponding CREATE INDEX should not be part of the generated table source.
	 * 
	 * @see DbSettings#supportsAutomaticFkIndexes()
	 */
	private List<IndexDefinition> getIndexesToCreate(List<IndexDefinition> indexList, DataStore foreignKeys)
	{
		if (!dbConnection.getDbSettings().supportsAutomaticFkIndexes()) return indexList;
		if (CollectionUtil.isEmpty(indexList)) return indexList;
		if (foreignKeys.getRowCount() == 0) return indexList;

		List<IndexDefinition> result = new ArrayList<IndexDefinition>(indexList.size());
		for (IndexDefinition idx : indexList)
		{
			if (!isFKName(idx.getName(), foreignKeys))
			{
				result.add(idx);
			}
		}
		return result;
	}

	public String getTableSource(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList, DataStore fkList, boolean includeDrop, boolean includeFk)
	{
		CharSequence createSql = getCreateTable(table, columns, indexList, fkList, includeDrop, includeFk);

		StringBuilder result = new StringBuilder(createSql.length() + 50);
		result.append(createSql);

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();

		boolean inlineFK = getCreateInlineFKConstraints();

		if (!inlineFK && includeFk && dbConnection.getDbSettings().getGenerateTableFKSource())
		{
			CharSequence fk = getFkSource(table, fkList, false);
			if (StringUtil.isNonBlank(fk))
			{
				result.append(lineEnding);
				result.append(fk);
			}
		}

		if (dbConnection.getDbSettings().getGenerateTableIndexSource())
		{
			List<IndexDefinition> toCreate = getIndexesToCreate(indexList, fkList);
			StringBuilder indexSource = getIndexReader().getIndexSource(table, toCreate);
			if (StringUtil.isNonBlank(indexSource))
			{
				result.append(lineEnding);
				result.append(indexSource);
			}
		}

		if (dbConnection.getDbSettings().getGenerateTableComments())
		{
			TableCommentReader commentReader = new TableCommentReader();
			String tableComment = commentReader.getTableCommentSql(dbConnection, table);
			if (StringUtil.isNonBlank(tableComment))
			{
				result.append(lineEnding);
				result.append(tableComment);
			}

			if (!dbConnection.getDbSettings().useInlineColumnComments())
			{
				StringBuilder colComments = commentReader.getTableColumnCommentsSql(this.dbConnection, table, columns);
				if (StringUtil.isNonBlank(colComments))
				{
					result.append(lineEnding);
					result.append(colComments);
				}
			}
		}

		if (dbConnection.getDbSettings().getGenerateTableGrants())
		{
			TableGrantReader grantReader = new TableGrantReader();
			StringBuilder grants = grantReader.getTableGrantSource(this.dbConnection, table);
			if (grants.length() > 0)
			{
				result.append(lineEnding);
				result.append(grants);
			}
		}

		CharSequence extendedSQL = getAdditionalTableSql(table, columns);
		if (extendedSQL != null)
		{
			result.append(lineEnding);
			result.append(extendedSQL);
		}
		return result.toString();
	}

	public CharSequence getAdditionalTableSql(TableIdentifier table, List<ColumnIdentifier> columns)
	{
		return null;
	}

	/**
	 * Generate the pure CREATE TABLE statement for the passed table definition.
	 *
	 * Any table constraints will be retrieved if needed.
	 *
	 * @param table the table name
	 * @param columns the columns of the table
	 * @param indexList defined indexes for the table (may be null)
	 * @param fkDefinitions defined foreign keys for the table (may be null)
	 * @param includeDrop if true, a DROP TABLE will be added before the CREATE TABLE
	 * @param tableNameToUse an alternate name to use (instead of the one in the table parameter)
	 * @param includeFk if true, foreign key definitions (if present) will be included
	 *
	 * @return the CREATE TABLE statement for the table
	 */
	public CharSequence getCreateTable(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList, DataStore fkDefinitions, boolean includeDrop, boolean includeFk)
	{
		return getCreateTable(table, columns, indexList, fkDefinitions, includeDrop, includeFk, true);
	}

	public CharSequence getCreateTable(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList, DataStore fkDefinitions, boolean includeDrop, boolean includeFk, boolean includePK)
	{
		if (table == null) return StringUtil.EMPTY_STRING;

		String nativeSql = getNativeTableSource(table, includeDrop);
		if (nativeSql != null) return nativeSql;

		if (CollectionUtil.isEmpty(columns)) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(250);
		DbMetadata meta = dbConnection.getMetadata();

		ConstraintReader consReader = ReaderFactory.getConstraintReader(meta);
		Map<String, String> columnConstraints = consReader.getColumnConstraints(dbConnection, table);

		readTableConfigOptions(table);

		result.append(generateCreateObject(includeDrop, table, table.getTableTypeOption()));
		result.append("\n(\n");

		appendColumnDefinitions(result, columns, meta, columnConstraints);

		List<TableConstraint> constraints = consReader.getTableConstraints(dbConnection, table);
		String cons = consReader.getConstraintSource(constraints, "   ");
		if (StringUtil.isNonEmpty(cons))
		{
			result.append("\n   ,");
			result.append(cons);
		}

		String pkIndexName = getPKName(indexList);
		String pkname = table.getPrimaryKeyName() != null ? table.getPrimaryKeyName() : pkIndexName;

		List<String> pkCols = findPkColumns(columns);

		if (pkname != null && pkCols.isEmpty())
		{
			// this can happen in DB2 iSeries. Apparently the columns are not always marked as PK
			// but the PK index is detected by SQL Workbench
			pkCols = getPKColsFromIndex(indexList, pkname);
		}

		PkDefinition pk = table.getPrimaryKey();
		if (includePK && pk == null)
		{
			pk = getIndexReader().getPrimaryKey(table);
			if (pk == null && pkCols.size() > 0)
			{
				pk = new PkDefinition(pkCols);
			}
			else
			{
				table.setPrimaryKey(pk);
			}
		}
		syncPkIndexType(pk, indexList);

		boolean inlinePK = getCreateInlinePKConstraints();
		if (includePK && inlinePK && pk != null)
		{
			result.append("\n   ,");
			CharSequence pkSql = getPkSource(table, pk, true);
			result.append(pkSql);
		}

		if (includeFk && getCreateInlineFKConstraints())
		{
			StringBuilder fk = getFkSource(table, fkDefinitions, true);
			if (fk.length() > 0)
			{
				result.append('\n');
				result.append(fk);
			}
		}

		result.append('\n');
		result.append(")");
		String options = getAdditionalTableOptions(table, columns, indexList);
		if (StringUtil.isNonEmpty(options))
		{
			result.append('\n');
			result.append(options);
			result.append('\n');
		}
		StringUtil.trimTrailingWhitespace(result);
		result.append(";\n");
		// end of CREATE TABLE

		// Add additional information provided by any specialized descendant class
		String info = getAdditionalTableInfo(table, columns, indexList);
		if (StringUtil.isNonBlank(info))
		{
			result.append(info);
			result.append("\n\n");
		}

		if (includePK && !inlinePK && pk != null)
		{
			CharSequence pkSource = getPkSource(table, pk, false);
			result.append('\n');
			result.append(pkSource);
		}

		return result;
	}

	private void syncPkIndexType(PkDefinition pk, List<IndexDefinition> indexList)
	{
		if (pk == null) return;
		if (CollectionUtil.isEmpty(indexList)) return;

		for (IndexDefinition index : indexList)
		{
			if (index.isPrimaryKeyIndex())
			{
				pk.setIndexType(index.getIndexType());
				pk.setEnabled(index.isEnabled());
				pk.setValidated(index.isValidated());
			}
		}
	}

	public CharSequence generateDrop(DbObject toDrop, boolean cascadeConstraints)
	{
		String type = toDrop.getObjectType();
		type = type.replace("SYSTEM ", "");
		String objectName = toDrop.getObjectNameForDrop(dbConnection);
		StringBuilder result = new StringBuilder(type.length() + objectName.length() + 15);

		String drop = dbConnection.getDbSettings().getDropDDL(type, cascadeConstraints);
		if (drop == null)
		{
			// Fallback, just in case no DROP statement was configured
			result.append("DROP ");
			result.append(type.toUpperCase());
			result.append(' ');
			result.append(objectName);
			String cascade = dbConnection.getDbSettings().getCascadeConstraintsVerb(type);
			if (cascade != null)
			{
				result.append(' ');
				result.append(cascade);
			}
			result.append(";\n");
		}
		else
		{
			drop = StringUtil.replace(drop, "%name%", objectName);
			result.append(SqlUtil.addSemicolon(drop));
		}
		return result;
	}

	/**
	 * Generate a CREATE statement for the given object type
	 * @param includeDrop if true, a DROP ... will be included in the SQL
	 * @param objectType the object type (TABLE, VIEW, ...)
	 * @param name the name of the object to create
	 * @param typeOption an option for the CREATE statement. This is only
	 * @return
	 */
	public StringBuilder generateCreateObject(boolean includeDrop, DbObject toCreate, String typeOption)
	{
		StringBuilder result = new StringBuilder();
		boolean replaceAvailable = false;

		String objectType = toCreate.getObjectType();
		objectType = objectType.replace("SYSTEM ", "");

		String prefix = "workbench.db.";
		String suffix = "." + DbSettings.getKeyValue(objectType) + ".sql." + dbConnection.getDbId();

		String name = toCreate.getObjectExpression(dbConnection);

		String replace = Settings.getInstance().getProperty(prefix + "replace" + suffix, null);
		if (replace != null)
		{
			result.append(StringUtil.replace(replace, "%name%", name));
			replaceAvailable = true;
		}

		if (includeDrop && !replaceAvailable)
		{
			result.append(generateDrop(toCreate, true));
			result.append('\n');
		}

		if (!replaceAvailable)
		{
			String create = Settings.getInstance().getProperty(prefix + "create" + suffix, null);
			if (create == null)
			{
				result.append("CREATE ");
				result.append(objectType.toUpperCase());
				result.append(' ');
				result.append(name);
			}
			else
			{
				create = StringUtil.replace(create, "%name%", name);
				create = StringUtil.replace(create, "%fq_name%", SqlUtil.fullyQualifiedName(dbConnection, toCreate));
				if (StringUtil.isNonBlank(typeOption))
				{
					create = StringUtil.replace(create, "%typeoption%", typeOption);
				}
				else
				{
					create = StringUtil.replace(create, "%typeoption% ", "");
				}
				result.append(create);
			}
		}
		return result;
	}

	private List<String> findPkColumns(List<ColumnIdentifier> columns)
	{
		List<String> result = new ArrayList<String>(2);
		for (ColumnIdentifier column : columns)
		{
			if (column.isPkColumn())
			{
				result.add(column.getColumnName());
			}
		}
		return result;
	}

	public void appendColumnDefinitions(StringBuilder result, List<ColumnIdentifier> columns, DbMetadata meta)
	{
		appendColumnDefinitions(result, columns, meta, new HashMap<String, String>());
	}

	protected void appendColumnDefinitions(StringBuilder result, List<ColumnIdentifier> columns, DbMetadata meta, Map<String, String> constraints)
	{
		int maxColLength = 0;
		int maxTypeLength = 0;

		// calculate the longest column name, so that the display can be formatted
		for (ColumnIdentifier column : columns)
		{
			String colName = meta.quoteObjectname(column.getColumnName());
			String type = column.getDbmsType();
			maxColLength = Math.max(maxColLength, colName.length());
			maxTypeLength = Math.max(maxTypeLength, (type != null ? type.length() : 0));
		}
		maxColLength += 2;
		maxTypeLength += 2;

		Iterator<ColumnIdentifier> itr = columns.iterator();
		while (itr.hasNext())
		{
			ColumnIdentifier column = itr.next();
			String colName = column.getColumnName();
			String quotedColName = meta.quoteObjectname(colName);
			String type = column.getDbmsType();
			if (type == null) type = "";

			result.append("   ");
			result.append(quotedColName);

			for (int k=0; k < maxColLength - quotedColName.length(); k++)
			{
				result.append(' ');
			}
			String coldef = getColumnSQL(column, maxTypeLength, constraints.get(column.getColumnName()));

			result.append(coldef);
			if (itr.hasNext())
			{
				result.append(",\n");
			}
		}

	}

	private List<String> getPKColsFromIndex(List<IndexDefinition> indexList, String pkname)
	{
		List<String> columns = new ArrayList<String>();
		for (IndexDefinition index : indexList)
		{
			if (index != null && index.isPrimaryKeyIndex())
			{
				for (IndexColumn col : index.getColumns())
				{
					columns.add(col.getColumn());
				}
			}
		}
		return columns;
	}
	/**
	 * Read additional options for the CREATE TABLE part.
	 *
	 * This could be tablespace definitions or other options that are valid for the CREATE TABLE
	 *
	 * @param tbl the table for which to read the options
	 */
	public void readTableConfigOptions(TableIdentifier tbl)
	{
		// nothing here
	}

	protected String getColumnSQL(ColumnIdentifier column, int maxTypeLength, String columnConstraint)
	{
		DbMetadata meta = dbConnection.getMetadata();
		boolean inlineColumnComments = dbConnection.getDbSettings().useInlineColumnComments();

		StringBuilder result = new StringBuilder(50);

		ColumnIdentifier toUse = column;

		ColumnDefinitionTemplate tmpl = new ColumnDefinitionTemplate(meta.getDbId());
		tmpl.setFixDefaultValues(!dbConnection.getDbSettings().returnsValidDefaultExpressions());
		result.append(tmpl.getColumnDefinitionSQL(toUse, columnConstraint, maxTypeLength));

		if (inlineColumnComments && StringUtil.isNonBlank(column.getComment()))
		{
			result.append(" COMMENT '");
			result.append(SqlUtil.escapeQuotes(column.getComment()));
			result.append('\'');
		}
		return result.toString();
	}

	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList)
	{
		return null;
	}

	protected String getAdditionalTableInfo(TableIdentifier table, List<ColumnIdentifier> columns, List<IndexDefinition> indexList)
	{
		return null;
	}

	private String getPKName(List<IndexDefinition> indexList)
	{
		if (indexList == null) return null;
		for (IndexDefinition index : indexList)
		{
			if (index.isPrimaryKeyIndex())
			{
				return index.getName();
			}
		}
		return null;
	}

	public String getNativeTableSource(TableIdentifier table, boolean includeDrop)
	{
		String sql = dbConnection.getDbSettings().getRetrieveTableSourceSql();
		if (sql == null) return null;

		StringBuilder result = new StringBuilder(250);

		int colIndex = dbConnection.getDbSettings().getRetrieveTableSourceCol();

		if (includeDrop)
		{
			CharSequence drop = generateDrop(table, true);
			result.append(drop);
			result.append("\n\n");
		}

		boolean needQuotes = dbConnection.getDbSettings().getRetrieveTableSourceNeedsQuotes();

		if (StringUtil.isBlank(table.getCatalog()))
		{
			sql = sql.replace("%catalog%.", ""); // in case the sql contains %catalog%.%tablename%
			sql = sql.replace("%catalog%", ""); // in case no trailing dot is present (e.g. for a procedure call)
		}
		else
		{
			String cat = (needQuotes ? dbConnection.getMetadata().quoteObjectname(table.getCatalog()) : table.getCatalog());
			sql = sql.replace("%catalog%", cat);
		}

		if (StringUtil.isBlank(table.getSchema()))
		{
			sql = sql.replace("%schema%.", "");
			sql = sql.replace("%schema%", "");
		}
		else
		{
			String schema = (needQuotes ? dbConnection.getMetadata().quoteObjectname(table.getSchema()) : table.getSchema());
			sql = sql.replace("%schema%", schema);
		}

		String tname = (needQuotes ? dbConnection.getMetadata().quoteObjectname(table.getTableName()) : table.getTableName());
		sql = sql.replace(MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, tname);

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("TableSourceBuilder.getNativeTableSource()", "Using SQL=" + sql);
		}
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = dbConnection.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				result.append(rs.getString(colIndex));
			}
			result.append('\n');
		}
		catch (Exception se)
		{
			LogMgr.logError("TableSourceBuilder.getNativeTableSource()", "Error retrieving table source", se);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		StringUtil.trimTrailingWhitespace(result);
		if (result.charAt(result.length() -1 ) != ';')
		{
			result.append(";\n");
		}

		return result.toString();
	}

	/**
	 * Builds an ALTER TABLE to add a primary key definition for the given tablename.
	 *
	 * @param table         the table for which the PK statement should be created.
	 * @param pk            the PK definition, if null the PK from the table is used
	 * @param forInlineUse  if true, the SQL is useable "inline" for a CREATE TABLE statement.
	 * @return an SQL statement to add a PK constraint on the given table.
	 */
	public CharSequence getPkSource(TableIdentifier table, PkDefinition pk, boolean forInlineUse)
	{
		if (pk == null) return StringUtil.EMPTY_STRING;

		DbMetadata meta = dbConnection.getMetadata();

		PkTemplate pkTmpl = new PkTemplate(dbConnection, forInlineUse);
		String template = pkTmpl.getSQLTemplate();

		if (StringUtil.isEmptyString(template)) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(100);
		String tablename = table.getTableExpression(this.dbConnection);

		List<String> pkCols = pk.getColumns();
		String pkName = pk.getPkName();

		template = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, tablename);
		template = StringUtil.replace(template, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, StringUtil.listToString(pkCols, ", ", false));

		if (nameTester.isSystemConstraintName(pkName))
		{
			pkName = null;
		}

		if (pkName == null && Settings.getInstance().getAutoGeneratePKName())
		{
			pkName = "pk_" + SqlUtil.cleanupIdentifier(table.getTableName().toLowerCase());
		}

		if (StringUtil.isEmptyString(pkName))
		{
			template = TemplateHandler.removePlaceholder(template, "CONSTRAINT " + MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, true);
			template = TemplateHandler.removePlaceholder(template, MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, true);
			template = StringUtil.replace(template, " CONSTRAINT ", ""); // remove CONSTRAINT KEYWORD if no name is available
		}
		else
		{
			pkName = SqlUtil.quoteObjectname(pkName, false, true, meta.getQuoteCharacter().charAt(0));
			template = StringUtil.replace(template, MetaDataSqlManager.PK_NAME_PLACEHOLDER, pkName);  // old templates
			template = TemplateHandler.replacePlaceholder(template, MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, pkName);  // new templates through DbSettings.getAddPk()
		}

		template = template.replaceAll("ADD\\s+PRIMARY", "ADD PRIMARY"); // removing the constraint name leaves two spaces which I find ugly :)
		result.append(template);
		if (!forInlineUse)
		{
			result.append(";\n");
		}

		return result;
	}

	private boolean getCreateInlinePKConstraints()
	{
		if (dbConnection == null) return false;
		return dbConnection.getDbSettings().createInlinePKConstraints();
	}

	private boolean getCreateInlineFKConstraints()
	{
		if (dbConnection == null) return false;
		return dbConnection.getDbSettings().createInlineFKConstraints();
	}


	public StringBuilder getFkSource(TableIdentifier table)
	{
		FKHandler fk = FKHandlerFactory.createInstance(dbConnection);
		DataStore fkDef = fk.getForeignKeys(table, false);
		return getFkSource(table, fkDef, getCreateInlineFKConstraints());
	}

	/**
	 *	Return a SQL script to re-create the Foreign key definition for the given table.
	 *
	 *	@param table the tablename for which the foreign keys should be created
	 *  @param aFkDef a DataStore with the FK definition as returned by #getForeignKeys()
	 *
	 *	@return a SQL statement to add the foreign key definitions to the given table, never null
	 */
	public StringBuilder getFkSource(TableIdentifier table, DataStore aFkDef, boolean forInlineUse)
	{
		if (aFkDef == null) return StringUtil.emptyBuffer();
		int count = aFkDef.getRowCount();
		if (count == 0) return StringUtil.emptyBuffer();

		FkTemplate tmpl = new FkTemplate(dbConnection.getDbId(), forInlineUse);
		String template = tmpl.getSQLTemplate();

		// fkCols collects all columns from the base table mapped to the
		// defining foreign key constraint.
		// The fk name is the key to the hashtable.
		// The entry will be a LinkedList containing the column names.
		// This ensures that each column will only be used once per fk definition
		// (the postgres driver returns some columns twice!)
		HashMap<String, List<String>> fkCols = new HashMap<String, List<String>>();

		// this hashmap contains the columns of the referenced table
		HashMap<String, List<String>> fkTarget = new HashMap<String, List<String>>();

		HashMap<String, String> fks = new HashMap<String, String>();
		HashMap<String, String> updateRules = new HashMap<String, String>();
		HashMap<String, String> deleteRules = new HashMap<String, String>();
		HashMap<String, String> deferrable = new HashMap<String, String>();

		String fkname;
		String col;
		String fkCol;
		String updateRule;
		String deleteRule;
		String deferRule;

		for (int i=0; i < count; i++)
		{
			fkname = aFkDef.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_FK_NAME);
			col = aFkDef.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_COLUMN_NAME);
			fkCol = aFkDef.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME);
			updateRule = aFkDef.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_UPDATE_RULE);
			deleteRule = aFkDef.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_DELETE_RULE);
			deferRule = aFkDef.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_DEFERRABLE);

			List<String> colList = fkCols.get(fkname);
			if (colList == null)
			{
				colList = new LinkedList<String>();
				fkCols.put(fkname, colList);
			}
			colList.add(col);
			updateRules.put(fkname, updateRule);
			deleteRules.put(fkname, deleteRule);
			deferrable.put(fkname, deferRule);

			colList = fkTarget.get(fkname);
			if (colList == null)
			{
				colList = new LinkedList<String>();
				fkTarget.put(fkname, colList);
			}
			colList.add(fkCol);
		}

		// now put the real statements together
		Iterator<Map.Entry<String, List<String>>> names = fkCols.entrySet().iterator();
		while (names.hasNext())
		{
			Map.Entry<String, List<String>> mapentry = names.next();
			fkname = mapentry.getKey();
			List<String> colList = mapentry.getValue();

			String stmt = fks.get(fkname);
			if (stmt == null)
			{
				// first time we hit this FK definition in this loop
				stmt = template;
			}
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableExpression(dbConnection));

			if (nameTester.isSystemConstraintName(fkname))
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, "");
				stmt = StringUtil.replace(stmt, " CONSTRAINT ", "");
			}
			else
			{
				if (dbConnection.getDbSettings().useFQConstraintName())
				{
					String fqName = SqlUtil.buildExpression(dbConnection, table.getCatalog(), table.getSchema(), fkname);
					stmt = StringUtil.replace(stmt, MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, fqName);
				}
				else
				{
					stmt = StringUtil.replace(stmt, MetaDataSqlManager.CONSTRAINT_NAME_PLACEHOLDER, fkname);
				}
			}

			String entry = StringUtil.listToString(colList, ", ", false);
			stmt = TemplateHandler.replacePlaceholder(stmt, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, entry);

			String rule = updateRules.get(fkname);
			if (dbConnection.getDbSettings().supportsFkOption("update", rule))
			{
				stmt = TemplateHandler.replacePlaceholder(stmt, MetaDataSqlManager.FK_UPDATE_RULE, "ON UPDATE " + rule);
			}
			else
			{
				stmt = TemplateHandler.removePlaceholder(stmt, MetaDataSqlManager.FK_UPDATE_RULE, true);
			}

			rule = deleteRules.get(fkname);
			if (dbConnection.getDbSettings().supportsFkOption("delete", rule))
			{
				stmt = TemplateHandler.replacePlaceholder(stmt, MetaDataSqlManager.FK_DELETE_RULE, "ON DELETE " + rule);
			}
			else
			{
				stmt = TemplateHandler.removePlaceholder(stmt, MetaDataSqlManager.FK_DELETE_RULE, true);
			}

			rule = getDeferrableVerb(deferrable.get(fkname));
			if (StringUtil.isEmptyString(rule))
			{
				stmt = TemplateHandler.removePlaceholder(stmt, MetaDataSqlManager.DEFERRABLE, true);
			}
			else
			{
				stmt = TemplateHandler.replacePlaceholder(stmt, MetaDataSqlManager.DEFERRABLE, rule.trim());
			}

			colList = fkTarget.get(fkname);
			if (colList == null)
			{
				LogMgr.logError("DbMetadata.getFkSource()", "Retrieved a null list for constraing [" + fkname + "] but should contain a list for table [" + table.getTableName() + "]",null);
				continue;
			}

			Iterator itr = colList.iterator();
			StringBuilder colListBuffer = new StringBuilder(30);
			String targetTable = null;
			boolean first = true;

			while (itr.hasNext())
			{
				col = (String)itr.next();
				int pos = col.lastIndexOf('.');
				if (targetTable == null)
				{
					String t = col.substring(0, pos);
					TableIdentifier tbl = new TableIdentifier(t);
					targetTable = tbl.getTableExpression(this.dbConnection);
				}
				if (!first)
				{
					colListBuffer.append(',');
				}
				else
				{
					first = false;
				}
				colListBuffer.append(col.substring(pos + 1));
			}
			stmt = TemplateHandler.replacePlaceholder(stmt, MetaDataSqlManager.FK_TARGET_TABLE_PLACEHOLDER, targetTable);
			stmt = TemplateHandler.replacePlaceholder(stmt, MetaDataSqlManager.FK_TARGET_COLUMNS_PLACEHOLDER, colListBuffer.toString());
			fks.put(fkname, stmt.trim());
		}
		StringBuilder fk = new StringBuilder();

		String nl = Settings.getInstance().getInternalEditorLineEnding();

		Iterator<String> values = fks.values().iterator();
		while (values.hasNext())
		{
			if (forInlineUse)
			{
				fk.append("   ,");
				fk.append(values.next());
			}
			else
			{
				fk.append(values.next());
				fk.append(';');
				fk.append(nl);
			}
			if (values.hasNext()) fk.append(nl);
		}

		return fk;
	}

	private String getDeferrableVerb(String type)
	{
		if (dbConnection.getDbSettings().isNotDeferrable(type)) return StringUtil.EMPTY_STRING;
		return "DEFERRABLE " + type;
	}

}
