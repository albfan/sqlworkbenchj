/*
 * ReportTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
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
 *
 * @author  info@sql-workbench.net
 */
public class ReportTable
{
	public static final String TAG_TABLE_DEF = "table-def";
	public static final String TAG_TABLE_NAME = "table-name";
	public static final String TAG_TABLE_CATALOG = "table-catalog";
	public static final String TAG_TABLE_SCHEMA = "table-schema";
	public static final String TAG_TABLE_COMMENT = "table-comment";

	private List referencedTables;
	private TableIdentifier table;
	private ReportColumn[] columns;
	private IndexReporter index;
	private String tableComment;
	private TagWriter tagWriter = new TagWriter();
	private String schemaNameToUse = null;
	private String namespace = null;

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
		this(tbl, conn, null, true, true);
	}
	
	public ReportTable(TableIdentifier tbl, WbConnection conn, String nspace)
		throws SQLException
	{
		this(tbl, conn, nspace, true, true);
	}
	
	public ReportTable(TableIdentifier tbl, WbConnection conn, String nspace, boolean includeIndex, boolean includeFk)
		throws SQLException
	{
		this.table = tbl;
		this.namespace = nspace;
		
		List cols = conn.getMetadata().getTableColumns(tbl);
		Collections.sort(cols);

		this.tableComment = conn.getMetadata().getTableComment(this.table);
		String schema = this.table.getSchema();
		if (schema == null || schema.length() == 0)
		{
			schema = conn.getMetadata().getSchemaForTable(this.table.getTable());
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
	}

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
		DataStore ds = conn.getMetadata().getForeignKeys(this.table.getCatalog(), this.table.getSchema(), this.table.getTable());
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
				String colExpr = ds.getValueAsString(i, DbMetadata.COLUMN_IDX_FK_DEF_REFERENCE_COLUMN_NAME);
				String table = null;
				String column = null;
				int pos = colExpr.indexOf(".");
				if (pos  > -1)
				{
					table = colExpr.substring(0, pos);
					column = colExpr.substring(pos + 1);
				}
				ref.setForeignTable(table);
				ref.setForeignColumn(column);
				rcol.setForeignKeyReference(ref);
			}
		}
	}

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
	
	public StrBuffer getXml(StrBuffer indent)
	{
		StrBuffer line = new StrBuffer(500);
		StrBuffer colindent = new StrBuffer(indent);
		colindent.append(indent);

		tagWriter.appendOpenTag(line, indent, TAG_TABLE_DEF);
		line.append('\n');

		tagWriter.appendTag(line, colindent, TAG_TABLE_CATALOG, this.table.getCatalog());
		tagWriter.appendTag(line, colindent, TAG_TABLE_SCHEMA, (this.schemaNameToUse == null ? this.table.getSchema() : this.schemaNameToUse));
		tagWriter.appendTag(line, colindent, TAG_TABLE_NAME, this.table.getTable());
		tagWriter.appendTag(line, colindent, TAG_TABLE_COMMENT, this.tableComment);

		int cols = this.columns.length;
		for (int i=0; i < cols; i++)
		{
			this.columns[i].appendXml(line, colindent);
		}
		if (this.index != null) this.index.appendXml(line, colindent);

		tagWriter.appendCloseTag(line, indent, TAG_TABLE_DEF);
		return line;
	}
	
	public void setNamespace(String namespace)
	{
		this.tagWriter.setNamespace(namespace);
	}

	public void done()
	{
		this.columns = null;
		this.index.done();
	}
	
	
}
