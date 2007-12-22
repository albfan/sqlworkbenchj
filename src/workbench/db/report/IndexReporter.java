/*
 * IndexReporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import workbench.db.IndexColumn;
import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.StrBuffer;

/**
 * Class to retrieve all index definitions for a table and
 * generate an XML string from that.
 *
 * @author  support@sql-workbench.net
 */
public class IndexReporter
{
	public static final String TAG_INDEX = "index-def";

	public static final String TAG_INDEX_NAME = "name";
	public static final String TAG_INDEX_UNIQUE = "unique";
	public static final String TAG_INDEX_PK = "primary-key";
	public static final String TAG_INDEX_TYPE = "type";
	public static final String TAG_INDEX_EXPR = "index-expression";
	public static final String TAG_INDEX_COLUMN_LIST = "column-list";
	public static final String TAG_INDEX_COLUMN_NAME = "column";

	private Collection<IndexDefinition> indexList;
	private TagWriter tagWriter = new TagWriter();

	public IndexReporter(TableIdentifier tbl, WbConnection conn)
	{
		indexList  = conn.getMetadata().getTableIndexList(tbl);
	}

	public IndexReporter(IndexDefinition index)
	{
		indexList  = new LinkedList<IndexDefinition>();
		indexList.add(index);
	}	
	
	public void appendXml(StrBuffer result, StrBuffer indent)
	{
		int numIndex = this.indexList.size();
		if (numIndex == 0) return;
		StrBuffer defIndent = new StrBuffer(indent);
		defIndent.append("  ");
		
		for (IndexDefinition index : indexList)
		{
			if (index == null) continue;
			tagWriter.appendOpenTag(result, indent, TAG_INDEX);
			result.append('\n');
			tagWriter.appendTag(result, defIndent, TAG_INDEX_NAME, index.getName());
			tagWriter.appendTag(result, defIndent, TAG_INDEX_EXPR, index.getExpression());
			tagWriter.appendTag(result, defIndent, TAG_INDEX_UNIQUE, index.isUnique());
			tagWriter.appendTag(result, defIndent, TAG_INDEX_PK, index.isPrimaryKeyIndex());
			tagWriter.appendTag(result, defIndent, TAG_INDEX_TYPE, index.getIndexType());
			List<IndexColumn> columns = index.getColumns();
			if (columns.size() > 0)
			{
				StrBuffer colIndent = new StrBuffer(defIndent);
				colIndent.append("  ");
				tagWriter.appendOpenTag(result, defIndent, TAG_INDEX_COLUMN_LIST);
				result.append('\n');
				for (IndexColumn col : columns)
				{
					
					List<TagAttribute> attrs = new ArrayList<TagAttribute>(2);
					attrs.add(new TagAttribute("name", col.getColumn()));
					
					if (col.getDirection() != null)
					{
						attrs.add(new TagAttribute("direction", col.getDirection()));
					}
					tagWriter.appendOpenTag(result, colIndent, TAG_INDEX_COLUMN_NAME, attrs, false);
					result.append("/>\n");
				}
				tagWriter.appendCloseTag(result, defIndent, TAG_INDEX_COLUMN_LIST);
			}
			tagWriter.appendCloseTag(result, indent, TAG_INDEX);
		}
		return;
	}

	public void setNamespace(String name)
	{
		this.tagWriter.setNamespace(name);
	}

	public Collection<IndexDefinition> getIndexList()
	{
		return this.indexList;
	}
	
	public void done()
	{
	}
}
