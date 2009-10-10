/*
 * TableSourceBuilder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * Re-Create the source SQL for a given TableIdentifier.
 *
 *
 * This class should not be instantiated directly. Use
 * TableSourceBuilderFactory.getBuilder() instead
 *
 * @author support@sql-workbench.net
 */
public class TableSourceBuilder
{
	protected WbConnection dbConnection;
	private boolean createInlineConstraints;
	private boolean useNullKeyword;

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
		this.useNullKeyword = dbConnection.getDbSettings().useNullKeyword();
	}

	private ViewReader getViewReader()
	{
		return dbConnection.getMetadata().getViewReader();
	}

	private IndexReader getIndexReader()
	{
		return dbConnection.getMetadata().getIndexReader();
	}

	/**
   * Return the SQL statement to re-create the given table. (in the dialect for the
	 * current DBMS)
   *
	 * @return the SQL statement to create the given table.
	 * @param table the table for which the source should be retrievedcatalog The catalog in which the table is defined. This should be null if the DBMS does not support catalogs
	 * @param includeDrop If true, a DROP TABLE statement will be included in the generated SQL script.
	 * @param includeFk if true, the foreign key constraints will be added after the CREATE TABLE
	 * @throws SQLException
	 */
	public String getTableSource(TableIdentifier table, boolean includeDrop, boolean includeFk)
		throws SQLException
	{
		if (dbConnection.getMetadata().getViewTypeName().equalsIgnoreCase(table.getType()))
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

	public String getTableSource(TableIdentifier table, DataStore columns, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop, String tableNameToUse)
	{
		List<ColumnIdentifier> cols = TableColumnsDatastore.createColumnIdentifiers(dbConnection.getMetadata(), columns);
		return getTableSource(table, cols, aIndexDef, aFkDef, includeDrop, tableNameToUse, true);
	}

	public String getTableSource(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef, DataStore aFkDef, boolean includeDrop, String tableNameToUse, boolean includeFk)
	{
		if (table == null) return StringUtil.EMPTY_STRING;

		String nativeSql = getNativeTableSource(table, includeDrop);
		if (nativeSql != null) return nativeSql;

		if (columns == null || columns.size() == 0) return StringUtil.EMPTY_STRING;

		if ("MVIEW_NAME".equals(table.getType()))
		{
			return dbConnection.getMetadata().getMViewSource(table, columns, aIndexDef, includeDrop);
		}

		StringBuilder result = new StringBuilder(250);
		DbMetadata meta = dbConnection.getMetadata();

		Map<String, String> columnConstraints = meta.getColumnConstraints(table);

		result.append(meta.generateCreateObject(includeDrop, "TABLE", (tableNameToUse == null ? table.getTableName() : tableNameToUse)));
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
		maxColLength++;
		maxTypeLength++;

		// Some RDBMS require the "DEFAULT" clause before the [NOT] NULL clause
		boolean defaultBeforeNull = meta.getDbSettings().getDefaultBeforeNull();
		String nullKeyword = Settings.getInstance().getProperty("workbench.db." + meta.getDbId() + ".nullkeyword", "NULL");
		boolean includeCommentInTableSource = Settings.getInstance().getBoolProperty("workbench.db.colcommentinline." + meta.getDbId(), false);

		String lineEnding = Settings.getInstance().getInternalEditorLineEnding();

		Iterator<ColumnIdentifier> itr = columns.iterator();
		while (itr.hasNext())
		{
			ColumnIdentifier column = itr.next();
			String colName = column.getColumnName();
			String quotedColName = meta.quoteObjectname(colName);
			String type = column.getDbmsType();
			if (type == null) type = "";
			String def = column.getDefaultValue();
			int typeLength = type.length();
			result.append("   ");
			result.append(quotedColName);

			boolean isFirstSql = meta.isFirstSql();
			if (column.isPkColumn() || (isFirstSql && "sequence".equals(type)))
			{
				pkCols.add(colName.trim());
			}

			for (int k=0; k < maxColLength - quotedColName.length(); k++) result.append(' ');
			result.append(type);

			// Check if any additional keywords are coming after
			// the datatype. If yes, we fill the line with spaces
			// to align the keywords properly
			if ( StringUtil.isNonBlank(def) ||
				   (!column.isNullable()) ||
				   (column.isNullable() && this.useNullKeyword)
					)
			{
				for (int k=0; k < maxTypeLength - typeLength; k++) result.append(' ');
			}

			if (defaultBeforeNull && StringUtil.isNonBlank(def))
			{
				result.append(" DEFAULT ");
				result.append(def.trim());
			}

			if (isFirstSql && "sequence".equals(type))
			{
				// with FirstSQL a column of type "sequence" is always the primary key
				result.append(" PRIMARY KEY");
			}
			else if (column.isNullable())
			{
				if (this.useNullKeyword)
				{
					result.append(' ');
					result.append(nullKeyword);
				}
			}
			else
			{
				result.append(" NOT NULL");
			}

			if (!defaultBeforeNull && !StringUtil.isEmptyString(def))
			{
				result.append(" DEFAULT ");
				result.append(def.trim());
			}

			String constraint = columnConstraints.get(colName);
			if (constraint != null && constraint.length() > 0)
			{
				result.append(' ');
				result.append(constraint);
			}

			if (includeCommentInTableSource && StringUtil.isNonBlank(column.getComment()))
			{
				result.append(" COMMENT '");
				result.append(SqlUtil.escapeQuotes(column.getComment()));
				result.append('\'');
			}

			if (itr.hasNext()) result.append(',');
			result.append(lineEnding);
		}

		String cons = meta.getTableConstraintSource(table, "   ");
		if (cons != null && cons.length() > 0)
		{
			result.append("   ,");
			result.append(cons);
			result.append(lineEnding);
		}

		String pkname = table.getPrimaryKeyName() != null ? table.getPrimaryKeyName() : getPKName(aIndexDef);

		if (this.createInlineConstraints && pkCols.size() > 0)
		{
			result.append(lineEnding + "   ,");
			if (StringUtil.isNonBlank(pkname))
			{
				result.append("CONSTRAINT " + pkname);
			}
			result.append(" PRIMARY KEY (");

			result.append(StringUtil.listToString(pkCols, ", ", false));
			result.append(")" + lineEnding);

			if (includeFk)
			{
				StringBuilder fk = getFkSource(table, aFkDef, tableNameToUse, createInlineConstraints);
				if (fk.length() > 0)
				{
					result.append(fk);
				}
			}
		}

		result.append(")");
		String options = getAdditionalTableOptions(table, columns, aIndexDef);
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
		String colInfo = getAdditionalColumnInformation(table, columns, aIndexDef);
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

		StringBuilder indexSource = getIndexReader().getIndexSource(table, aIndexDef, tableNameToUse);
		if (StringUtil.isNonBlank(indexSource))
		{
			result.append(lineEnding);
			result.append(indexSource);
		}

		if (!this.createInlineConstraints && includeFk)
		{
			CharSequence fk = getFkSource(table, aFkDef, tableNameToUse, createInlineConstraints);
			if (StringUtil.isNonBlank(fk))
			{
				result.append(lineEnding);
				result.append(fk);
			}
		}

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

		TableGrantReader grantReader = new TableGrantReader();
		StringBuilder grants = grantReader.getTableGrantSource(this.dbConnection, table);
		if (grants.length() > 0)
		{
			result.append(lineEnding);
			result.append(grants);
		}

		return result.toString();
	}

	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		return null;
	}
	
	protected String getAdditionalColumnInformation(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
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

		StringBuilder result = new StringBuilder(100);

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
		catch (SQLException se)
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
	public CharSequence getPkSource(TableIdentifier table, List pkCols, String pkName)
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
			pkName = "pk_" + StringUtil.trimQuotes(tablename.toLowerCase());
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

		// collects all columns from the base table mapped to the
		// defining foreign key constraing.
		// The fk name is the key.
		// to the hashtable. The entry will be a HashSet containing the column names
		// this ensures that each column will only be used once per fk definition
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
			//"FK_NAME", "COLUMN_NAME", "REFERENCES"};
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
			stmt = StringUtil.replace(stmt, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, (tableNameToUse == null ? table.getTableName() : tableNameToUse));

			if (meta.isSystemConstraintName(fkname))
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, "");
				stmt = StringUtil.replace(stmt, " CONSTRAINT ", "");
			}
			else
			{
				stmt = StringUtil.replace(stmt, MetaDataSqlManager.FK_NAME_PLACEHOLDER, fkname);
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
				col = (String)itr.next();//tok.nextToken();
				int pos = col.lastIndexOf('.');
				if (targetTable == null)
				{
					// The last element has to be the column name!
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
