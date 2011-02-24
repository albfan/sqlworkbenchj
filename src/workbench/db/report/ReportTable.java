/*
 * ReportTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import workbench.db.FKHandler;
import workbench.db.IndexDefinition;
import workbench.db.TableCommentReader;
import workbench.db.TableConstraint;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.oracle.OracleTablePartition;
import workbench.log.LogMgr;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * A class to hold information about a database table that
 * will eventually be stored in an XML report.
 * It uses a {@link workbench.db.TableIdentifier} to store the
 * table's name, and {@link workbench.db.ColumnIdentifier} to
 * store the table's columns.
 * When initialized with a connection, it tries to find the primary
 * and foreign key constraints as well.
 *
 * Primary feature of this class is that it can create an XML
 * representation of itself.
 *
 * @author  Thomas Kellerer
 */
public class ReportTable
{
	public static final String TAG_TABLE_DEF = "table-def";
	public static final String TAG_TABLE_NAME = "table-name";
	public static final String TAG_TABLE_CATALOG = "table-catalog";
	public static final String TAG_TABLE_SCHEMA = "table-schema";
	public static final String TAG_TABLE_COMMENT = "table-comment";
	public static final String TAG_TABLE_PK_NAME = "primary-key-name";
	public static final String TAG_TABLE_CONSTRAINTS = "table-constraints";
	public static final String TAG_CONSTRAINT_DEF = "constraint-definition";
	public static final String TAG_CONSTRAINT_COMMENT = "constraint-comment";

	private TableIdentifier table;
	private Map<String, ForeignKeyDefinition> foreignKeys = new HashMap<String, ForeignKeyDefinition>();
	private ReportColumn[] columns;
	private IndexReporter reporter;
	private String tableComment;
	private TagWriter tagWriter = new TagWriter();
	private String schemaNameToUse = null;
	private boolean includePrimaryKey = true;
	private List<TableConstraint> tableConstraints;
	private List<TriggerDefinition> triggers;
	private ReportTableGrants grants;
	private List<ObjectOption> dbmsOptions;

	public ReportTable(TableIdentifier tbl)
	{
		this.table = tbl;
	}

	/**
	 * Initialize this ReportTable.
	 * This will read the following information for the table:
	 * <ul>
	 *	<li>columns for the table using {@link workbench.db.DbMetadata#getTableColumns(TableIdentifier)}</li>
	 *  <li>the comments for the table using {@link workbench.db.TableCommentReader#getTableComment(WbConnection, TableIdentifier)}</li>
	 *  <li>The defined indexes for the table if includeIndex == true using an {@link IndexReporter}</li>
	 *  <li>The defined foreign keys if includeFK == true</li>
	 *  <li>Table constraints if includeConstraints == true {@link workbench.db.ConstraintReader#getTableConstraints(workbench.db.WbConnection, workbench.db.TableIdentifier)}</li>
	 *</ul>
	 */
	public ReportTable(TableIdentifier tbl, WbConnection conn,
			boolean includeIndex,
			boolean includeFk,
			boolean includePk,
			boolean includeConstraints,
			boolean includeGrants,
			boolean includeTriggers)
		throws SQLException
	{
		this.includePrimaryKey = includePk;

		// By using getTableDefinition() the TableIdentifier is completely initialized
		// (mainly it will contain the primary key name, which it doesn't when the TableIdentifier
		// was created using getTableList()
		TableDefinition def = conn.getMetadata().getTableDefinition(tbl);
		this.table = def.getTable();
		this.table.checkQuotesNeeded(conn);

		List<ColumnIdentifier> cols = def.getColumns();
		Collections.sort(cols);

		TableCommentReader reader = new TableCommentReader();
		this.tableComment = reader.getTableComment(conn, this.table);
		String schema = this.table.getSchema();
		if (schema == null || schema.length() == 0)
		{
			// This is important for e.g. Oracle. Otherwise the table definition
			// will contain multiple columns if a table exists more then once in
			// different schemas with the same name
			schema = conn.getMetadata().getSchemaToUse();
			if (schema != null) this.table.setSchema(schema);
		}

		this.setColumns(cols);

		if (includeIndex)
		{
			this.reporter = new IndexReporter(tbl, conn);
		}

		if (includeFk)
		{
			this.readForeignKeys(conn);
		}

		if (includeConstraints)
		{
			this.tableConstraints = conn.getMetadata().getTableConstraints(tbl);
		}

		if (includeGrants)
		{
			grants = new ReportTableGrants(conn, this.table);
		}

		if (includeTriggers)
		{
			TriggerReader trgReader = TriggerReaderFactory.createReader(conn);
			try
			{
				triggers = trgReader.getTriggerList(table.getCatalog(), table.getSchema(), table.getTableName());
				if (triggers != null)
				{
					for (TriggerDefinition trg : triggers)
					{
						trg.setSource(trgReader.getTriggerSource(trg, false));
					}
				}
			}
			catch (SQLException e)
			{
				LogMgr.logError("ReportTable.<init>", "Could not retrieve table triggers", e);
				triggers = null;
			}
		}
		retrieveOptions(conn);
	}

	public List<ObjectOption> getDbmsOptions()
	{
		if (dbmsOptions == null)
		{
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(dbmsOptions);
	}

	private void retrieveOptions(WbConnection conn)
		throws SQLException
	{
		if (!conn.getMetadata().isOracle()) return;
		OracleTablePartition partition = new OracleTablePartition(conn);
		partition.retrieve(this.table, conn);
		if (partition.isPartitioned())
		{
			String source = partition.getSourceForTableDefinition();
			dbmsOptions = new ArrayList<ObjectOption>();
			ObjectOption option = new ObjectOption("partition", source);
			dbmsOptions.add(option);
		}
	}

	public List<TriggerDefinition> getTriggers()
	{
		return triggers;
	}

	/**
	 * Returns the ReportTableGrants for this table. If table grants
	 * are not included, it will return null.
	 */
	public ReportTableGrants getGrants()
	{
		return grants;
	}

	/**
	 *	Return the list of column names (String)
	 *  that make up the primary key of this table
	 *  If the table has no primary key, an empty list
	 *  is returned.
	 */
	public List<String> getPrimaryKeyColumns()
	{
		if (!includePrimaryKey) return Collections.emptyList();
		List<String> result = new ArrayList<String>();
		int count = this.columns.length;
		for (int i=0; i < count; i++)
		{
			if (this.columns[i].getColumn().isPkColumn())
			{
				result.add(this.columns[i].getColumn().getColumnName().toUpperCase());
			}
		}
		Collections.sort(result);
		return result;
	}

	/**
	 *	Return the name of the primary key
	 */
	public String getPrimaryKeyName()
	{
		if (!includePrimaryKey) return null;
		if (this.reporter == null) return null;
		List pk = this.getPrimaryKeyColumns();
		if (pk.isEmpty()) return null;
		Collection<IndexDefinition> idxList = this.reporter.getIndexList();
		for (IndexDefinition idx : idxList)
		{
			if (idx.isPrimaryKeyIndex())
			{
				return idx.getName();
			}
		}
		return null;
	}

	/**
	 * Define the columns that belong to this table
	 */
	public final void setColumns(List<ColumnIdentifier> cols)
	{
		if (cols == null) return;
		int numCols = cols.size();
		this.columns = new ReportColumn[numCols];
		for (int i=0; i < numCols; i++)
		{
			ColumnIdentifier col = cols.get(i);
			this.columns[i] = new ReportColumn(col);
		}
	}

	private void readForeignKeys(WbConnection conn)
	{
		FKHandler	fk = new FKHandler(conn);
		DataStore ds = fk.getForeignKeys(this.table, true);
		int keys = ds.getRowCount();
		if (keys == 0) return;

		for (int i=0; i < keys; i++)
		{
			String col = ds.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_COLUMN_NAME);
			ReportColumn rcol = this.findColumn(col);
			if (rcol != null)
			{
				String fkname = ds.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_FK_NAME);
				ForeignKeyDefinition def = this.foreignKeys.get(fkname);
				if (def == null)
				{
					def = new ForeignKeyDefinition(fkname);
					def.setCompareFKRules(true);
					def.setDeleteRuleValue(ds.getValueAsInt(i, FKHandler.COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE, DatabaseMetaData.importedKeyNoAction));
					def.setUpdateRuleValue(ds.getValueAsInt(i, FKHandler.COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE, DatabaseMetaData.importedKeyNoAction));
					def.setDeleteRule(ds.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_DELETE_RULE));
					def.setUpdateRule(ds.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_UPDATE_RULE));
					def.setDeferrableRuleValue(ds.getValueAsInt(i, FKHandler.COLUMN_IDX_FK_DEF_DEFERRABLE_RULE_VALUE, DatabaseMetaData.importedKeyNotDeferrable));
					def.setDeferRule(ds.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_DEFERRABLE));
					foreignKeys.put(fkname, def);
				}
				String colExpr = ds.getValueAsString(i, FKHandler.COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME);
				String reftable = null;
				String refcolumn = null;
				int pos = colExpr.lastIndexOf('.');
				if (pos  > -1)
				{
					reftable = colExpr.substring(0, pos);
					refcolumn = colExpr.substring(pos + 1);
				}
				if (def.getForeignTable() == null)
				{
					def.setForeignTable(new ReportTable(new TableIdentifier(reftable)));
				}
				def.addReferenceColumn(col, refcolumn);
				rcol.setForeignKeyReference(def.getColumnReference(col));
			}
		}
	}

	public Map<String, ForeignKeyDefinition> getForeignKeys()
	{
		return foreignKeys;
	}

	/**
	 * Find a column witht the given name.
	 */
	public ReportColumn findColumn(String col)
	{
		if (col == null) return null;
		if (columns == null)
		{
			return null;
		}
		ReportColumn result = null;
		int numCols = this.columns.length;
		for (int i=0; i < numCols; i++)
		{
			if (col.equalsIgnoreCase(columns[i].getColumn().getColumnName()))
			{
				result = columns[i];
				break;
			}
		}
		return result;
	}

	public Collection<IndexDefinition> getIndexList()
	{
		if (this.reporter == null) return null;
		return this.reporter.getIndexList();
	}

	public List<ReportColumn> getColumnsSorted()
	{
		Comparator<ReportColumn> comp = new Comparator<ReportColumn>()
		{
			@Override
			public int compare(ReportColumn o1, ReportColumn o2)
			{
				int pos1 = o1.getColumn().getPosition();
				int pos2 = o2.getColumn().getPosition();
				return pos1 - pos2;
			}
		};
		List<ReportColumn> result = new ArrayList<ReportColumn>(columns.length);
		result.addAll(Arrays.asList(columns));
		Collections.sort(result, comp);
		return result;
	}

	public ReportColumn[] getColumns()
	{
		return this.columns;
	}

	public TableIdentifier getTable()
	{
		return this.table;
	}

	public void setSchemaNameToUse(String name)
	{
		this.schemaNameToUse = name;
	}

	public void writeXml(Writer out)
		throws IOException
	{
		StrBuffer line = this.getXml();
		line.writeTo(out);
	}

	public StrBuffer getXml()
	{
		return getXml(new StrBuffer("  "));
	}

	@Override
	public String toString()
	{
		return this.table.toString();
	}

	public String getTableComment() { return this.tableComment; }

	public List<TableConstraint> getTableConstraints()
	{
		return this.tableConstraints;
	}

	public void appendTableNameXml(StrBuffer toAppend, StrBuffer indent)
	{
		tagWriter.appendTag(toAppend, indent, TAG_TABLE_CATALOG, StringUtil.trimQuotes(this.table.getCatalog()));
		tagWriter.appendTag(toAppend, indent, TAG_TABLE_SCHEMA, (this.schemaNameToUse == null ? StringUtil.trimQuotes(this.table.getSchema()) : this.schemaNameToUse));
		tagWriter.appendTag(toAppend, indent, TAG_TABLE_NAME, StringUtil.trimQuotes(this.table.getTableName()));
	}

	/**
	 * Return an XML representation of this table information.
	 * The columns will be listed alphabetically not in the order
	 * they were retrieved from the database.
	 */
	public StrBuffer getXml(StrBuffer indent)
	{
		StrBuffer line = new StrBuffer(500);
		StrBuffer colindent = new StrBuffer(indent);
		colindent.append("  ");

		String type = this.table.getType();

		if (!"TABLE".equalsIgnoreCase(type))
		{
			String[] att = new String[] {"name", "type"};
			String[] val = new String[] { StringUtil.trimQuotes(this.table.getTableName()), type };
			tagWriter.appendOpenTag(line, indent, TAG_TABLE_DEF, att, val);
		}
		else
		{
			tagWriter.appendOpenTag(line, indent, TAG_TABLE_DEF, "name", StringUtil.trimQuotes(this.table.getTableName()));
		}
		line.append('\n');
		appendTableNameXml(line, colindent);
		tagWriter.appendTag(line, colindent, TAG_TABLE_PK_NAME, table.getPrimaryKeyName(), false);
		tagWriter.appendTag(line, colindent, TAG_TABLE_COMMENT, this.tableComment, true);
		int cols = this.columns.length;
		for (int i=0; i < cols; i++)
		{
			this.columns[i].appendXml(line, colindent);
		}
		if (this.reporter != null) this.reporter.appendXml(line, colindent);

		writeConstraints(tableConstraints, tagWriter, line, colindent);

		if (this.foreignKeys.size() > 0)
		{
			tagWriter.appendOpenTag(line, colindent, "foreign-keys");
			line.append('\n');
			StrBuffer fkindent = new StrBuffer(colindent);
			fkindent.append("  ");
			for (ForeignKeyDefinition def : foreignKeys.values())
			{
				line.append(def.getXml(fkindent));
			}
			tagWriter.appendCloseTag(line, colindent, "foreign-keys");
		}
		if (this.grants != null)
		{
			this.grants.appendXml(line, colindent);
		}
		if (triggers != null)
		{
			for (TriggerDefinition trg : triggers)
			{
				ReportTrigger rtrig = new ReportTrigger(trg);
				rtrig.setIndent(colindent);
				line.append(rtrig.getXml());
			}
		}
		writeDBMSOptions(line, indent);
		tagWriter.appendCloseTag(line, indent, TAG_TABLE_DEF);
		return line;
	}

	private void writeDBMSOptions(StrBuffer output, StrBuffer indent)
	{
		if (CollectionUtil.isEmpty(dbmsOptions)) return;

		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		output.append(myindent);
		output.append("<table-options>\n");
		StrBuffer nextindent = new StrBuffer(myindent);
		nextindent.append("  ");
		for (ObjectOption option : dbmsOptions)
		{
			StrBuffer result = option.getXml(nextindent);
			output.append(result);
		}
		output.append(myindent);
		output.append("</table-options>\n");
	}

	public static void writeConstraints(List<TableConstraint> constraints, TagWriter tagWriter, StrBuffer line, StrBuffer indent)
	{
		if (constraints != null && constraints.size() > 0)
		{
			tagWriter.appendOpenTag(line, indent, TAG_TABLE_CONSTRAINTS);
			line.append('\n');
			StrBuffer consIndent = new StrBuffer(indent);
			consIndent.append("  ");
			for (TableConstraint cons : constraints)
			{
				writeConstraint(cons, tagWriter, line, consIndent);
			}
			tagWriter.appendCloseTag(line, indent, TAG_TABLE_CONSTRAINTS);
		}
	}

	public static void writeConstraint(TableConstraint constraint, TagWriter tagWriter, StrBuffer line, StrBuffer indent)
	{
		if (constraint == null) return;
		String name = constraint.getConstraintName();
		String expr = constraint.getExpression();
		String systemName = Boolean.toString(constraint.isSystemName());

		TagAttribute type = new TagAttribute("type", constraint.getType());
		TagAttribute sysName = null;
		TagAttribute nameAttr = null;

		if (name != null)
		{
			nameAttr = new TagAttribute("name", name);
			sysName = new TagAttribute("generated-name", systemName);
		}
		tagWriter.appendCDATATag(line, indent, ReportTable.TAG_CONSTRAINT_DEF, expr, type, sysName, nameAttr);
	}

	public void done()
	{
		this.columns = null;
		this.reporter.done();
	}

	@Override
	public int hashCode()
	{
		int hash = 17 * 7 + (this.table != null ? this.table.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ReportTable)
		{
			return equals((ReportTable)other);
		}
		return false;
	}

	public boolean equals(ReportTable other)
	{
		return this.table.getTableName().equalsIgnoreCase(other.table.getTableName());
	}

}
