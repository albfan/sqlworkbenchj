/*
 * IndexReporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.report;

import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;

/**
 * Class to retrieve all index definitions for a table and
 * generate an XML string from that.
 *
 * @author  info@sql-workbench.net
 */
public class IndexReporter
{
	public static final String TAG_INDEX = "index-def";

	public static final String TAG_INDEX_NAME = "name";
	public static final String TAG_INDEX_UNIQUE = "unique";
	public static final String TAG_INDEX_PK = "primary-key";
	public static final String TAG_INDEX_EXPR = "index-expression";

	private DataStore indexList;
	private TagWriter tagWriter = new TagWriter();

	public IndexReporter(TableIdentifier tbl, WbConnection conn)
	{
		this.indexList = conn.getMetadata().getTableIndexInformation(tbl.getCatalog(), tbl.getSchema(), tbl.getTable());
	}

	public void appendXml(StrBuffer result, StrBuffer indent)
	{
		int numIndex = this.indexList.getRowCount();
		if (numIndex == 0) return;
		StrBuffer defIndent = new StrBuffer(indent);
		defIndent.append("    ");

		for (int i=0; i < numIndex; i ++)
		{
			tagWriter.appendOpenTag(result, indent, TAG_INDEX);
			result.append('\n');

			String value = this.indexList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
			tagWriter.appendTag(result, defIndent, TAG_INDEX_NAME, value);

			value = this.indexList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_COL_DEF);
			tagWriter.appendTag(result, defIndent, TAG_INDEX_EXPR, value);

			value = this.indexList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG);
			tagWriter.appendTag(result, defIndent, TAG_INDEX_UNIQUE, String.valueOf("YES".equals(value)));

			value = this.indexList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG);
			tagWriter.appendTag(result, defIndent, TAG_INDEX_PK, String.valueOf("YES".equals(value)));

			tagWriter.appendCloseTag(result, indent, TAG_INDEX);
		}
		return;
	}

	public void setNamespace(String name)
	{
		this.tagWriter.setNamespace(name);
	}

	public void done()
	{
		this.indexList.reset();
	}
}
