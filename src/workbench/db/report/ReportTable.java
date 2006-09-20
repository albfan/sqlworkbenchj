/*
 * ReportTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;
import java.util.Collections;
import workbench.db.IndexDefinition;

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
 * @author  support@sql-workbench.net
 */
public class ReportTable
{
	public static final String TAG_TABLE_DEF = "table-def";
	public static final String TAG_TABLE_NAME = "table-name";
	public static final String TAG_TABLE_CATALOG = "table-catalog";
	public static final String TAG_TABLE_SCHEMA = "table-schema";
	public static final String TAG_TABLE_COMMENT = "table-comment";
	public static final String TAG_TABLE_CONSTRAINT = "table-constraint";

	private TableIdentifier table;
	private ReportColumn[] columns;
	private IndexReporter index;
	private String tableComment;
	private TagWriter tagWriter = new TagWriter();
	private String schemaNameToUse = null;
	private String namespace = null;
	private boolean includePrimaryKey = true;
	private String tableConstraints;
	
	public ReportTable(TableIdentifier tbl)
	{
		this(tbl, (String)null);
	}
	
	public ReportTable(TableIdentifier tbl, String nspace)
	{
		this.table = tbl;
		this.namespace = nspace;
		tagWriter.setNamespace(this.namespace);
	}
	
	public ReportTable(TableIdentifier tbl, WbConnection conn)
		throws SQLException
	{
		this(tbl, conn, null, true, true, true, true);
	}
	
	public ReportTable(TableIdentifier tbl, WbConnection conn, String nspace)
		throws SQLException
	{
		this(tbl, conn, nspace, true, true, true, true);
	}
	
	/**
	 * Initialize this ReportTable.
	 * This will read the following information for the table: 
	 * <ul>
	 *	<li>columns for the table using {@link workbench.db.DbMetadata#getTableColumns(TableIdentifier)}</li>
	 *  <li>the comments for the table using {@link workbench.db.DbMetadata#getTableComment(TableIdentifier)}</li>
	 *  <li>The defined indexes for the table if includeIndex == true using an {@link IndexReporter}</li>
	 *  <li>The defined foreign keys if includeFK == true</li>
	 *  <li>Table constraints if includeConstraints == true {@link workbench.db.DbMetadata#getTableConstraints(workbench.db.TableIdentifier, String)}</li>
	 *</ul>
	 */
	public ReportTable(TableIdentifier tbl, WbConnection conn, String nspace, boolean includeIndex, boolean includeFk, boolean includePk, boolean includeConstraints)
		throws SQLException
	{
		this.table = tbl;
		this.namespace = nspace;
		this.includePrimaryKey = includePk; 
		
		if (tbl.getSchema() == null)
		{
			// This is important for e.g. Oracle. Otherwise the table definition 
			// will contain multiple columns if a table exists more then once in 
			// different schemas with the same name
			tbl.setSchema(conn.getMetadata().getSchemaToUse());
		}
		List cols = conn.getMetadata().getTableColumns(tbl);
		Collections.sort(cols);

		this.tableComment = conn.getMetadata().getTableComment(this.table);
		String schema = this.table.getSchema();
		if (schema == null || schema.length() == 0)
		{
			schema = conn.getMetadata().getSchemaToUse();
			if (schema != null) this.table.setSchema(schema);
		}

		this.setColumns(cols);
		this.tagWriter.setNamespace(namespace);
		if (includeIndex)
		{
			this.index = new IndexReporter(tbl, conn);
			this.index.setNamespace(namespace);
		}
		if (includeFk) this.readForeignKeys(conn);
		if (includeConstraints)
		{
			this.tableConstraints = conn.getMetadata().getTableConstraints(tbl, "");
		}
	}

	/**
	 *	Return the list of column names (String) 
	 *  that make up the primary key of this table
	 *  If the table has no primary key, an empty list
	 *  is returned.
	 */
	public List getPrimaryKeyColumns()
	{
		if (!includePrimaryKey) return Collections.EMPTY_LIST;
		List result = new ArrayList();
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
	 *	Return the name of the primary 
	 */
	public String getPrimaryKeyName()
	{
		if (!includePrimaryKey) return null;
		if (this.index == null) return null;
		List pk = this.getPrimaryKeyColumns();
		if (pk.size() == 0) return null;
		IndexDefinition[] idx = this.index.getIndexList();
		int count = idx.length;
		for (int i=0; i < count; i++)
		{
			if (idx[i].isPrimaryKeyIndex())
			{
				return idx[i].getName();
			}
		}
		return null;
	}
	
	/**
	 * Define the columns that belong to this table
	 */
	public void setColumns(List cols)
	{
		if (cols == null) return;
		int numCols = cols.size();
		this.columns = new ReportColumn[numCols];
		for (int i=0; i < numCols; i++)
		{
			ColumnIdentifier col = (ColumnIdentifier)cols.get(i);
			this.columns[i] = new ReportColumn(col);
			this.columns[i].setNamespace(this.namespace);
		}
	}
	
	private void readForeignKeys(WbConnection conn)
	{
		DataStore ds = conn.getMetadata().getForeignKeys(this.table.getCatalog(), this.table.getSchema(), this.table.getTableName(), true);
		int keys = ds.getRowCount();
		if (keys == 0) return;

		for (int i=0; i < keys; i++)
		{
			String col = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_FK_DEF_COLUMN_NAME);
			ReportColumn rcol = this.findColumn(col);
			if (rcol != null)
			{
				ColumnReference ref = new ColumnReference();
				ref.setConstraintName(ds.getValueAsString(i, DbMetadata.COLUMN_IDX_FK_DEF_FK_NAME));
				ref.setDeleteRule(ds.getValueAsString(i, DbMetadata.COLUMN_IDX_FK_DEF_DELETE_RULE));
				ref.setUpdateRule(ds.getValueAsString(i, DbMetadata.COLUMN_IDX_FK_DEF_UPDATE_RULE));
				ref.setDeleteRuleValue(ds.getValueAsInt(i, DbMetadata.COLUMN_IDX_FK_DEF_DELETE_RULE_VALUE, -1));
				ref.setUpdateRuleValue(ds.getValueAsInt(i, DbMetadata.COLUMN_IDX_FK_DEF_UPDATE_RULE_VALUE, -1));
				String colExpr = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME);
				String reftable = null;
				String column = null;
				int pos = colExpr.lastIndexOf(".");
				if (pos  > -1)
				{
					reftable = colExpr.substring(0, pos);
					column = colExpr.substring(pos + 1);
				}
				ref.setForeignTable(new ReportTable(new TableIdentifier(reftable)));
				ref.setForeignColumn(column);
				rcol.setForeignKeyReference(ref);
			}
		}
	}

	/**
	 * Find a column witht the given name.
	 */
	public ReportColumn findColumn(String col)
	{
		if (col == null) return null;

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

	public IndexDefinition[] getIndexList()
	{
		if (this.index == null) return null;
		return this.index.getIndexList();
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
	
	public String getTableComment() { return this.tableComment; }
	public String getTableConstraints() { return this.tableConstraints; }
	
	public void appendTableNameXml(StrBuffer toAppend, StrBuffer indent)
	{
		tagWriter.appendTag(toAppend, indent, TAG_TABLE_CATALOG, this.table.getCatalog());
		tagWriter.appendTag(toAppend, indent, TAG_TABLE_SCHEMA, (this.schemaNameToUse == null ? this.table.getSchema() : this.schemaNameToUse));
		tagWriter.appendTag(toAppend, indent, TAG_TABLE_NAME, this.table.getTableName());
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
		colindent.append(indent);

		tagWriter.appendOpenTag(line, indent, TAG_TABLE_DEF, "name", this.table.getTableName());
		line.append('\n');
		appendTableNameXml(line, colindent);
		tagWriter.appendTag(line, colindent, TAG_TABLE_COMMENT, this.tableComment, true);
		int cols = this.columns.length;
		for (int i=0; i < cols; i++)
		{
			this.columns[i].appendXml(line, colindent);
		}
		if (this.index != null) this.index.appendXml(line, colindent);
		if (this.tableConstraints != null && this.tableConstraints.length() > 0)
		{
			tagWriter.appendTag(line, colindent, TAG_TABLE_CONSTRAINT, this.tableConstraints, true);
		}
		tagWriter.appendCloseTag(line, indent, TAG_TABLE_DEF);
		return line;
	}

	/**
	 * The namespace to be used for the XML representation
	 */
	public void setNamespace(String namespace)
	{
		this.tagWriter.setNamespace(namespace);
	}

	public void done()
	{
		this.columns = null;
		this.index.done();
	}

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
