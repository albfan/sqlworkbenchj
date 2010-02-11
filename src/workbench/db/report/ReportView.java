/*
 * ReportView.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.StrBuffer;
import java.util.Collections;
import workbench.db.IndexDefinition;
import workbench.db.TableCommentReader;
import workbench.util.StringUtil;

/**
 * A class to hold information about a database view that 
 * will eventually be stored in an XML report. 
 * It uses a {@link workbench.db.TableIdentifier} to store the 
 * view's name, and {@link workbench.db.ColumnIdentifier} to 
 * store the view's columns.
 *
 * @author  Thomas Kellerer
 */
public class ReportView
{
	public static final String TAG_VIEW_DEF = "view-def";
	public static final String TAG_VIEW_NAME = "view-name";
	public static final String TAG_VIEW_CATALOG = "view-catalog";
	public static final String TAG_VIEW_SCHEMA = "view-schema";
	public static final String TAG_VIEW_COMMENT = "view-comment";
	public static final String TAG_VIEW_SOURCE = "view-source";

	private TableIdentifier view;
	private ReportColumn[] columns;
	private String viewComment;
	private TagWriter tagWriter = new TagWriter();
	private IndexReporter index;
	
	/** The schema name to be used in the generated XML */
	private String schemaNameToUse = null;
	
	private CharSequence viewSource;
	
	public ReportView(TableIdentifier tbl)
	{
		this.view = tbl;
	}
	
	/**
	 * Initialize this ReportView.
	 * This will read the following information for the table: 
	 * <ul>
	 *	<li>columns for the table using {@link workbench.db.DbMetadata#getTableColumns(workbench.db.TableIdentifier) }</li>
	 *  <li>the comments for the view using {@link workbench.db.TableCommentReader#getTableComment(workbench.db.WbConnection, workbench.db.TableIdentifier)  }</li>
	 *  <li>the source for the view using {@link workbench.db.ViewReader#getViewSource(workbench.db.TableIdentifier)}</li>
	 *</ul>
	 */
	public ReportView(TableIdentifier tbl, WbConnection conn, boolean includeIndex)
		throws SQLException
	{
		this.view = tbl;
		
		if (tbl.getSchema() == null)
		{
			// This is important for e.g. Oracle. Otherwise the table definition 
			// will contain multiple columns if a table exists more then once in 
			// different schemas with the same name
			tbl.setSchema(conn.getMetadata().getSchemaToUse());
		}
		List<ColumnIdentifier> cols = conn.getMetadata().getTableColumns(tbl);
		Collections.sort(cols);

		TableCommentReader reader = new TableCommentReader();
		this.viewComment = reader.getTableComment(conn, this.view);
		String schema = this.view.getSchema();
		if (schema == null || schema.length() == 0)
		{
			schema = conn.getMetadata().getSchemaToUse();
			if (schema != null) this.view.setSchema(schema);
		}
		this.viewSource = conn.getMetadata().getViewReader().getViewSource(tbl);
		if (viewSource == null) viewSource = StringUtil.EMPTY_STRING;
		this.setColumns(cols);
		if (includeIndex)
		{
			this.index = new IndexReporter(tbl, conn);
		}
	}

	/**
	 * Return the list of IndexDefinitions for this view
	 * @return defined indexes, maybe null
	 */
	public Collection<IndexDefinition> getIndexList()
	{
		if (this.index == null) return null;
		return this.index.getIndexList();
	}
	
	/**
	 * Define the columns that belong to this view
	 */
	public void setColumns(List<ColumnIdentifier> cols)
	{
		if (cols == null) return;
		int numCols = cols.size();
		this.columns = new ReportColumn[numCols];
		int i = 0;
		for (ColumnIdentifier column : cols)
		{
			this.columns[i] = new ReportColumn(column);
			this.columns[i].setIsRealColumn(false);
			i++;
		}
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
	
	public TableIdentifier getView() { return this.view; }
	public String getViewComment() { return this.viewComment; }
	public CharSequence getViewSource() { return this.viewSource; }
	
	public void appendTableNameXml(StrBuffer toAppend, StrBuffer indent)
	{
		tagWriter.appendTag(toAppend, indent, TAG_VIEW_CATALOG, this.view.getCatalog());
		tagWriter.appendTag(toAppend, indent, TAG_VIEW_SCHEMA, (this.schemaNameToUse == null ? this.view.getSchema() : this.schemaNameToUse));
		tagWriter.appendTag(toAppend, indent, TAG_VIEW_NAME, this.view.getTableName());
	}

	public StrBuffer getXml(StrBuffer indent)
	{
		return getXml(indent, true);
	}
	
	/**
	 * Return an XML representation of this view information.
	 * The columns will be listed alphabetically not in the order
	 * they were retrieved from the database.
	 */
	public StrBuffer getXml(StrBuffer indent, boolean includeIndex)
	{
		StrBuffer line = new StrBuffer(this.viewSource.length() + 200);
		StrBuffer colindent = new StrBuffer(indent);
		colindent.append(indent);

		tagWriter.appendOpenTag(line, indent, TAG_VIEW_DEF, "name", this.view.getTableName());
		line.append('\n');
		appendTableNameXml(line, colindent);
		tagWriter.appendTag(line, colindent, TAG_VIEW_COMMENT, this.viewComment, true);
		int cols = this.columns.length;
		for (int i=0; i < cols; i++)
		{
			this.columns[i].appendXml(line, colindent);
		}
		writeSourceTag(tagWriter, line, colindent, viewSource);
		if (includeIndex && this.index != null) this.index.appendXml(line, colindent);
		tagWriter.appendCloseTag(line, indent, TAG_VIEW_DEF);
		return line;
	}

	public static final void writeSourceTag(TagWriter tagWriter, StrBuffer target, StrBuffer indent, CharSequence source)
	{
		if (source == null) return;
		tagWriter.appendOpenTag(target, indent, TAG_VIEW_SOURCE);
		target.append(TagWriter.CDATA_START);
		target.append(source);
		target.append(TagWriter.CDATA_END);
		target.append('\n');
		tagWriter.appendCloseTag(target, indent, TAG_VIEW_SOURCE);
		
	}

}
