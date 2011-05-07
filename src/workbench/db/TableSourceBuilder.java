/*
 * TableSourceBuilder.java
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Re-Create the source SQL for a given TableIdentifier.
 *
 *
 * This class should not be instantiated directly. Use
 * TableSourceBuilderFactory.getBuilder() instead
 *
 * @author Thomas Kellerer
 */
public class TableSourceBuilder
{
	protected WbConnection dbConnection;
	private boolean createInlineConstraints;

	/**
	 * This class should not be instantiated directly. Use
   * TableSourceBuilderFactory.getBuilder() instead.
	 *
	 * @param con the connection to be used
	 *
	 * @see TableSourceBuilderFactory#getBuilder(workbench.db.WbConnection)
	 */
	protected TableSourceBuilder(WbConnection con)
	{
		dbConnection = con;
		String productName = dbConnection.getMetadata().getProductName();
		this.createInlineConstraints = Settings.getInstance().getServersWithInlineConstraints().contains(productName);
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
		DataStore indexDef = meta.getIndexReader().getTableIndexInformation(def.getTable());
		DataStore fkDef = null;
		if (includeFk)
		{
			FKHandler fk = new FKHandler(dbConnection);
			fkDef = fk.getForeignKeys(def.getTable(), false);
		}

		// getTableDefinition() has already retrieved the necessary PK information
		// there is no need to retrieve the index definition to get the PK information
		String source = this.getTableSource(def.getTable(), cols, indexDef, fkDef, includeDrop, null, includeFk);
		return source;
	}

	public String getTableSource(TableIdentifier table, List<ColumnIdentifier> columns, String tableNameToUse)
	{
		DataStore indexInfo = getIndexReader().getTableIndexInformation(table);
		return getTableSource(table, columns, indexInfo, null, false, tableNameToUse, true);
	}

	public String getTableSource(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop, String tableNameToUse, boolean includeFk)
	{
		StringBuilder result = new StringBuilder(250);
		result.append(getCreateTable(table, columns, aIndexDef, aFkDef, includeDrop, tableNameToUse, includeFk));

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();

		if (!this.createInlineConstraints && includeFk && dbConnection.getDbSettings().getGenerateTableFKSource())
		{
			CharSequence fk = getFkSource(table, aFkDef, tableNameToUse, createInlineConstraints);
			if (StringUtil.isNonBlank(fk))
			{
				result.append(lineEnding);
				result.append(fk);
			}
		}

		if (dbConnection.getDbSettings().getGenerateTableIndexSource())
		{
			StringBuilder indexSource = getIndexReader().getIndexSource(table, aIndexDef, tableNameToUse);
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

			StringBuilder colComments = commentReader.getTableColumnCommentsSql(this.dbConnection, table, columns);
			if (StringUtil.isNonBlank(colComments))
			{
				result.append(lineEnding);
				result.append(colComments);
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
	 * @param indexDefinition defined indexes for the table (may be null)
	 * @param fkDefinitions defined foreign keys for the table (may be null)
	 * @param includeDrop if true, a DROP TABLE will be added before the CREATE TABLE
	 * @param tableNameToUse an alternate name to use (instead of the one in the table parameter)
	 * @param includeFk if true, foreign key definitions (if present) will be included
	 *
	 * @return the CREATE TABLE statement for the table
	 */
	public CharSequence getCreateTable(TableIdentifier table, List<ColumnIdentifier> columns, DataStore indexDefinition, DataStore fkDefinitions, boolean includeDrop, String tableNameToUse, boolean includeFk)
	{
		if (table == null) return StringUtil.EMPTY_STRING;

		String nativeSql = getNativeTableSource(table, includeDrop);
		if (nativeSql != null) return nativeSql;

		if (CollectionUtil.isEmpty(columns)) return StringUtil.EMPTY_STRING;

		StringBuilder result = new StringBuilder(250);
		DbMetadata meta = dbConnection.getMetadata();

		Map<String, String> columnConstraints = meta.getColumnConstraints(table);

		String typeToUse = "TABLE";
		if (meta.isExtendedObject(table))
		{
			typeToUse = table.getType();
		}

		String name = table.getTableExpression(dbConnection);
		if (tableNameToUse != null)
		{
			name = dbConnection.getMetadata().quoteObjectname(tableNameToUse);
		}

		if (table.getTableTypeOption() == null)
		{
			readTableTypeOptions(table);
		}

		result.append(meta.generateCreateObject(includeDrop, typeToUse, name, table.getTableTypeOption()));
		result.append("\n(\n");

		List<String> pkCols = new LinkedList<String>();
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

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();

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

			boolean isFirstSql = meta.isFirstSql();
			if (column.isPkColumn() || (isFirstSql && "sequence".equals(type)))
			{
				pkCols.add(colName.trim());
			}

			for (int k=0; k < maxColLength - quotedColName.length(); k++) result.append(' ');
			String coldef = getColumnSQL(column, maxTypeLength, columnConstraints.get(column.getColumnName()));

			result.append(coldef);
			if (itr.hasNext()) result.append(',');
			result.append(lineEnding);
		}


		String cons = meta.getTableConstraintSource(table, "   ");
		if (StringUtil.isNonBlank(cons))
		{
			result.append("   ,");
			result.append(cons);
			result.append(lineEnding);
		}

		String pkname = table.getPrimaryKeyName() != null ? table.getPrimaryKeyName() : getPKName(indexDefinition);

		if (this.createInlineConstraints && pkCols.size() > 0)
		{
			result.append(lineEnding);
			result.append("   ,");
			if (StringUtil.isNonBlank(pkname))
			{
				result.append("CONSTRAINT ");
				result.append(pkname);
			}
			result.append(' ');
			result.append(dbConnection.getDbSettings().getInlinePKKeyword());
			result.append(" (");

			result.append(StringUtil.listToString(pkCols, ", ", false));
			result.append(")");
			result.append(lineEnding);

			if (includeFk)
			{
				StringBuilder fk = getFkSource(table, fkDefinitions, tableNameToUse, createInlineConstraints);
				if (fk.length() > 0)
				{
					result.append(fk);
				}
			}
		}

		result.append(")");
		String options = getAdditionalTableOptions(table, columns, indexDefinition);
		if (options != null)
		{
			result.append(lineEnding);
			result.append(options);
			result.append(lineEnding);
		}
		result.append(';');
		result.append(lineEnding);
		// end of CREATE TABLE

		// Add additional column information provided by any specialized descendant class
		String colInfo = getAdditionalColumnSql(table, columns, indexDefinition);
		if (StringUtil.isNonBlank(colInfo))
		{
			result.append(colInfo);
			result.append(lineEnding);
			result.append(lineEnding);
		}

		if (!this.createInlineConstraints && pkCols.size() > 0)
		{
			CharSequence pkSource = getPkSource( (tableNameToUse == null ? table : new TableIdentifier(tableNameToUse)), pkCols, pkname);
			result.append(pkSource);
		}

		return result;
	}

	/**
	 * Read additional options for the CREATE TABLE part
	 * @param tbl
	 */
	public void readTableTypeOptions(TableIdentifier tbl)
	{
		// nothing here
	}

	protected String getColumnSQL(ColumnIdentifier column, int maxTypeLength, String columnConstraint)
	{
		DbMetadata meta = dbConnection.getMetadata();
		boolean includeCommentInTableSource = Settings.getInstance().getBoolProperty("workbench.db.colcommentinline." + meta.getDbId(), false);

		StringBuilder result = new StringBuilder(50);

		ColumnIdentifier toUse = column;
		String type = column.getDbmsType();

		if (meta.isFirstSql() && "sequence".equals(type))
		{
			toUse = column.createCopy();
			// with FirstSQL a column of type "sequence" is always the primary key
			toUse.setDbmsType(type + " PRIMARY KEY");
		}

		ColumnDefinitionTemplate tmpl = new ColumnDefinitionTemplate(meta.getDbId());
		tmpl.setFixDefaultValues(!dbConnection.getDbSettings().returnsValidDefaultExpressions());
		result.append(tmpl.getColumnDefinitionSQL(toUse, columnConstraint, maxTypeLength));

		if (includeCommentInTableSource && StringUtil.isNonBlank(column.getComment()))
		{
			result.append(" COMMENT '");
			result.append(SqlUtil.escapeQuotes(column.getComment()));
			result.append('\'');
		}
		return result.toString();
	}

	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		return null;
	}

	protected String getAdditionalColumnSql(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		return null;
	}

	private String getPKName(DataStore anIndexDef)
	{
		if (anIndexDef == null) return null;
		int count = anIndexDef.getRowCount();

		String name = null;
		for (int row = 0; row < count; row ++)
		{
			String is_pk = anIndexDef.getValue(row, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG).toString();
			if ("YES".equalsIgnoreCase(is_pk))
			{
				name = anIndexDef.getValue(row, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME).toString();
				break;
			}
		}
		return name;
	}

	public String getNativeTableSource(TableIdentifier table, boolean includeDrop)
	{
		String sql = dbConnection.getDbSettings().getRetrieveTableSourceSql();
		if (sql == null) return null;

		StringBuilder result = new StringBuilder(250);

		int colIndex = dbConnection.getDbSettings().getRetrieveTableSourceCol();

		if (includeDrop)
		{
			CharSequence drop = dbConnection.getMetadata().generateDrop(table.getType(), table.getTableExpression(dbConnection));
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
	 * @param table
	 * @param pkCols
	 * @param pkName
	 * @return an SQL statement to add a PK constraint on the given table.
	 */
	public CharSequence getPkSource(TableIdentifier table, List<String> pkCols, String pkName)
	{
		DbMetadata meta = dbConnection.getMetadata();
		String template = meta.metaSqlMgr.getPrimaryKeyTemplate();

		if (StringUtil.isEmptyString(template)) return "";

		StringBuilder result = new StringBuilder(100);
		String tablename = table.getTableExpression(this.dbConnection);

		template = StringUtil.replace(template, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, tablename);
		template = StringUtil.replace(template, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, StringUtil.listToString(pkCols, ", ", false));

		if (meta.isSystemConstraintName(pkName))
		{
			pkName = null;
		}

		if (pkName == null && Settings.getInstance().getAutoGeneratePKName())
		{
			pkName = "pk_" + SqlUtil.cleanupIdentifier(table.getTableName().toLowerCase());
		}

		if (meta.isKeyword(pkName)) pkName = meta.getQuoteCharacter() + pkName + meta.getQuoteCharacter() ;

		if (StringUtil.isEmptyString(pkName))
		{
			pkName = ""; // remove placeholder if no name is available
			template = StringUtil.replace(template, " CONSTRAINT ", ""); // remove CONSTRAINT KEYWORD if no name is available
		}

		template = StringUtil.replace(template, MetaDataSqlManager.PK_NAME_PLACEHOLDER, pkName);
		result.append(template);
		result.append(";\n");

		return result;
	}

	public StringBuilder getFkSource(TableIdentifier table)
	{
		FKHandler fk = new FKHandler(dbConnection);
		DataStore fkDef = fk.getForeignKeys(table, false);
		return getFkSource(table, fkDef, null, createInlineConstraints);
	}

	/**
	 *	Return a SQL script to re-create the Foreign key definition for the given table.
	 *
	 *	@param table the tablename for which the foreign keys should be created
	 *  @param aFkDef a DataStore with the FK definition as returned by #getForeignKeys()
	 *
	 *	@return a SQL statement to add the foreign key definitions to the given table
	 */
	public StringBuilder getFkSource(TableIdentifier table, DataStore aFkDef, String tableNameToUse, boolean forInlineUse)
	{
		DbMetadata meta = dbConnection.getMetadata();

		if (aFkDef == null) return StringUtil.emptyBuffer();
		int count = aFkDef.getRowCount();
		if (count == 0) return StringUtil.emptyBuffer();

		String template = meta.metaSqlMgr.getForeignKeyTemplate(forInlineUse);

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
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, (tableNameToUse == null ? table.getTableExpression(dbConnection) : tableNameToUse));

			if (meta.isSystemConstraintName(fkname))
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, "");
				stmt = StringUtil.replace(stmt, " CONSTRAINT ", "");
			}
			else
			{
				if (dbConnection.getDbSettings().useFQConstraintName())
				{
					String fqName = SqlUtil.buildExpression(dbConnection, table.getCatalog(), table.getSchema(), fkname);
					stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, fqName);
				}
				else
				{
					stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, fkname);
				}
			}

			String entry = StringUtil.listToString(colList, ", ", false);
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, entry);
			String rule = updateRules.get(fkname);
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_UPDATE_RULE, " ON UPDATE " + rule);
			rule = deleteRules.get(fkname);
			if (meta.isOracle())
			{
				// Oracle does not allow ON DELETE RESTRICT, so we'll have to
				// remove the placeholder completely
				if ("restrict".equalsIgnoreCase(rule))
				{
					stmt = MetaDataSqlManager.removePlaceholder(stmt, MetaDataSqlManager.FK_DELETE_RULE, true);
				}
				else
				{
					stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_DELETE_RULE, " ON DELETE " + rule);
				}
			}
			else
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_DELETE_RULE, " ON DELETE " + rule);
			}

			rule = getDeferrableVerb(deferrable.get(fkname));
			if (StringUtil.isEmptyString(rule))
			{
				stmt = MetaDataSqlManager.removePlaceholder(stmt, MetaDataSqlManager.DEFERRABLE, true);
			}
			else
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.DEFERRABLE, rule.trim());
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
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_TARGET_TABLE_PLACEHOLDER, targetTable);
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_TARGET_COLUMNS_PLACEHOLDER, colListBuffer.toString());
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
			fk.append(nl);
		}

		return fk;
	}

	private String getDeferrableVerb(String type)
	{
		if (dbConnection.getDbSettings().isNotDeferrable(type)) return StringUtil.EMPTY_STRING;
		return " DEFERRABLE " + type;
	}

}
