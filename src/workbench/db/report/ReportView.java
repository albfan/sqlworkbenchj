/*
 * ReportView.java
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
import workbench.util.StringUtil;

/**
 * A class to hold information about a database view that 
 * will eventually be stored in an XML report. 
 * It uses a {@link workbench.db.TableIdentifier} to store the 
 * view's name, and {@link workbench.db.ColumnIdentifier} to 
 * store the view's columns.
 *
 * @author  support@sql-workbench.net
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
	
	/** The schema name to be used in the generated XML */
	private String schemaNameToUse = null;
	
	private String namespace = null;
	private String viewSource;
	
	public ReportView(TableIdentifier tbl)
	{
		this(tbl, (String)null);
	}
	
	public ReportView(TableIdentifier tbl, String nspace)
	{
		this.view = tbl;
		this.namespace = nspace;
		tagWriter.setNamespace(this.namespace);
	}
	
	/**
	 * Initialize this ReportView.
	 * This will read the following information for the table: 
	 * <ul>
	 *	<li>columns for the table using {@link workbench.db.DbMetadata#getTableColumns(workbench.db.TableIdentifier)}</li>
	 *  <li>the comments for the view using {@link workbench.db.DbMetadata#getTableComment(workbench.db.TableIdentifier)}</li>
	 *  <li>the source for the view using{@link workbench.db.DbMetadata#getViewSource(workbench.db.TableIdentifier)}</li>
	 *</ul>
	 */
	public ReportView(TableIdentifier tbl, WbConnection conn, String nspace)
		throws SQLException
	{
		this.view = tbl;
		this.namespace = nspace;
		
		if (tbl.getSchema() == null)
		{
			// This is important for e.g. Oracle. Otherwise the table definition 
			// will contain multiple columns if a table exists more then once in 
			// different schemas with the same name
			tbl.setSchema(conn.getMetadata().getSchemaToUse());
		}
		List cols = conn.getMetadata().getTableColumns(tbl);
		Collections.sort(cols);

		this.viewComment = conn.getMetadata().getTableComment(this.view);
		String schema = this.view.getSchema();
		if (schema == null || schema.length() == 0)
		{
			schema = conn.getMetadata().findSchemaForTable(this.view.getTableName());
			if (schema != null) this.view.setSchema(schema);
		}
		this.viewSource = conn.getMetadata().getViewSource(tbl);
		if (viewSource == null) viewSource = StringUtil.EMPTY_STRING;
		this.setColumns(cols);
		this.tagWriter.setNamespace(namespace);
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
			this.columns[i].setRealColumn(false);
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
	public String getViewSource() { return this.viewSource; }
	
	public void appendTableNameXml(StrBuffer toAppend, StrBuffer indent)
	{
		tagWriter.appendTag(toAppend, indent, TAG_VIEW_CATALOG, this.view.getCatalog());
		tagWriter.appendTag(toAppend, indent, TAG_VIEW_SCHEMA, (this.schemaNameToUse == null ? this.view.getSchema() : this.schemaNameToUse));
		tagWriter.appendTag(toAppend, indent, TAG_VIEW_NAME, this.view.getTableName());
	}

	/**
	 * Return an XML representation of this view information.
	 * The columns will be listed alphabetically not in the order
	 * they were retrieved from the database.
	 */
	public StrBuffer getXml(StrBuffer indent)
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
		tagWriter.appendOpenTag(line, colindent, TAG_VIEW_SOURCE);
		line.append(TagWriter.CDATA_START);
		line.append(this.viewSource);
		line.append(TagWriter.CDATA_END);
		line.append('\n');
		tagWriter.appendCloseTag(line, colindent, TAG_VIEW_SOURCE);
		tagWriter.appendCloseTag(line, indent, TAG_VIEW_DEF);
		return line;
	}

	/**
	 * The namespace to be used for the XML representation
	 */
	public void setNamespace(String namespace)
	{
		this.tagWriter.setNamespace(namespace);
	}

}
