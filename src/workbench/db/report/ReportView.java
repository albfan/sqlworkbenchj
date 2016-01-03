/*
 * ReportView.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.IndexDefinition;
import workbench.db.NoConfigException;
import workbench.db.TableCommentReader;
import workbench.db.TableDefinition;
import workbench.db.TableGrant;
import workbench.db.TableIdentifier;
import workbench.db.ViewGrantReader;
import workbench.db.ViewReader;
import workbench.db.WbConnection;

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
	public static final String TAG_VIEW_TYPE = "view-type";

	private TableIdentifier view;
	private ReportColumn[] columns;
	private String viewComment;
	private TagWriter tagWriter = new TagWriter();
	private IndexReporter index;

	private boolean includeGrants;
	private ReportTableGrants grants;

	/** The schema name to be used in the generated XML */
	private String schemaNameToUse = null;
	private boolean schemaAsCatalog;

	private CharSequence viewSource;

	/**
	 * Initialize this ReportView.
	 * This will read the following information for the table:
	 * <ul>
	 *	<li>columns for the table using {@link workbench.db.DbMetadata#getTableColumns(workbench.db.TableIdentifier) }</li>
	 *  <li>the comments for the view using {@link workbench.db.TableCommentReader#getTableComment(workbench.db.WbConnection, workbench.db.TableIdentifier)  }</li>
	 *  <li>the source for the view using {@link workbench.db.ViewReader#getViewSource(workbench.db.TableIdentifier)}</li>
	 *</ul>
	 */
	public ReportView(TableIdentifier tbl, WbConnection conn, boolean includeIndex, boolean includeViewGrants, boolean fullSource)
		throws SQLException
	{
		this.view = tbl;
		this.includeGrants = includeViewGrants;

		if (tbl.getSchema() == null)
		{
			// This is important for e.g. Oracle. Otherwise the table definition
			// will contain multiple columns if a table exists more then once in
			// different schemas with the same name
			tbl.setSchema(conn.getMetadata().getCurrentSchema());
		}
		List<ColumnIdentifier> cols = conn.getMetadata().getTableColumns(tbl);
		Collections.sort(cols);

		if (view.commentIsDefined())
		{
			this.viewComment = view.getComment();
		}
		else
		{
			TableCommentReader reader = new TableCommentReader();
			this.viewComment = reader.getTableComment(conn, this.view);
		}

		String schema = this.view.getSchema();
		if (schema == null || schema.length() == 0)
		{
			schema = conn.getMetadata().getSchemaToUse();
			if (schema != null) this.view.setSchema(schema);
		}
		try
		{
			ViewReader viewReader = conn.getMetadata().getViewReader();
      if (fullSource)
      {
        this.viewSource = viewReader.getFullViewSource(new TableDefinition(tbl, cols));
      }
			else
      {
        this.viewSource = viewReader.getViewSource(tbl);
      }
		}
		catch (NoConfigException no)
		{
			viewSource = StringUtil.EMPTY_STRING;
		}

		if (includeGrants)
		{
			ViewGrantReader grantReader = ViewGrantReader.createViewGrantReader(conn);
			if (grantReader != null)
			{
				Collection<TableGrant> viewGrants = grantReader.getViewGrants(conn, tbl);
				grants = new ReportTableGrants(viewGrants);
			}
		}
		this.setColumns(cols);
		if (includeIndex)
		{
			this.index = new IndexReporter(tbl, conn, false);
		}

		schemaAsCatalog = !conn.getDbSettings().supportsSchemas();
	}

	public ReportTableGrants getGrants()
	{
		return grants;
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
		StringBuilder line = this.getXml();
		out.append(line);
	}

	public StringBuilder getXml()
	{
		return getXml(new StringBuilder("  "));
	}

	public TableIdentifier getView()
	{
		return this.view;
	}

	public String getViewComment()
	{
		return this.viewComment;
	}

	public CharSequence getViewSource()
	{
		return this.viewSource;
	}

	public void appendTableNameXml(StringBuilder toAppend, StringBuilder indent)
	{
		if (schemaNameToUse != null)
		{
			if (schemaAsCatalog)
			{
				tagWriter.appendTag(toAppend, indent, TAG_VIEW_CATALOG, schemaNameToUse);
			}
			else
			{
				tagWriter.appendTag(toAppend, indent, TAG_VIEW_SCHEMA, this.schemaNameToUse);
			}
		}
		else
		{
			tagWriter.appendTag(toAppend, indent, TAG_VIEW_CATALOG, view.getCatalog());
			tagWriter.appendTag(toAppend, indent, TAG_VIEW_SCHEMA, view.getSchema());
		}
		tagWriter.appendTag(toAppend, indent, TAG_VIEW_NAME, this.view.getTableName());
	}

	public StringBuilder getXml(StringBuilder indent)
	{
		return getXml(indent, true);
	}

	/**
	 * Return an XML representation of this view information.
	 * The columns will be listed alphabetically not in the order
	 * they were retrieved from the database.
	 */
	public StringBuilder getXml(StringBuilder indent, boolean includeIndex)
	{
		StringBuilder line = new StringBuilder(this.viewSource.length() + 200);
		StringBuilder colindent = new StringBuilder(indent);
		colindent.append("  ");

		tagWriter.appendOpenTag(line, indent, TAG_VIEW_DEF, "name", this.view.getTableName());
		line.append('\n');
		appendTableNameXml(line, colindent);
		tagWriter.appendTag(line, colindent, TAG_VIEW_COMMENT, this.viewComment, true);
		if (!view.getType().equals("VIEW"))
		{
			tagWriter.appendTag(line, colindent, TAG_VIEW_TYPE, view.getType());
		}
		int cols = this.columns.length;
		for (int i=0; i < cols; i++)
		{
			this.columns[i].appendXml(line, colindent);
		}
		if (this.grants != null)
		{
			this.grants.appendXml(line, colindent);
		}
		writeSourceTag(tagWriter, line, colindent, viewSource);
		if (includeIndex && this.index != null) this.index.appendXml(line, colindent);
		tagWriter.appendCloseTag(line, indent, TAG_VIEW_DEF);
		return line;
	}

	public static void writeSourceTag(TagWriter tagWriter, StringBuilder target, StringBuilder indent, CharSequence source)
	{
		if (source == null) return;
		tagWriter.appendOpenTag(target, indent, TAG_VIEW_SOURCE);
		target.append(TagWriter.CDATA_START);
		target.append(source);
		target.append(TagWriter.CDATA_END);
		tagWriter.appendCloseTag(target, null, TAG_VIEW_SOURCE);

	}

}
