/*
 * ReportIndex.java
 *
 * Created on September 9, 2004, 10:38 PM
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
 * @author  workbench@kellerer.org
 */
public class IndexReporter
{
	public static final String TAG_INDEX_DEFS = "index-definitions";
	public static final String TAG_INDEX_START = "index";
	
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
	
	public StrBuffer getXml(StrBuffer indent)
	{
		int numIndex = this.indexList.getRowCount();
		if (numIndex == 0) return null;
		StrBuffer result = new StrBuffer(numIndex * 100);
		StrBuffer idxIndent = new StrBuffer(indent);
		idxIndent.append(indent);
		StrBuffer defIndent = new StrBuffer(indent);
		defIndent.append(indent);
		defIndent.append(indent);
		
		tagWriter.appendOpenTag(result, indent, TAG_INDEX_DEFS);
		result.append('\n');
		for (int i=0; i < numIndex; i ++)
		{
			tagWriter.appendOpenTag(result, idxIndent, TAG_INDEX_START);
			result.append('\n');
			
			String value = this.indexList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
			tagWriter.appendTag(result, defIndent, TAG_INDEX_NAME, value);

			value = this.indexList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_COL_DEF);
			tagWriter.appendTag(result, defIndent, TAG_INDEX_EXPR, value);
			
			value = this.indexList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG);
			tagWriter.appendTag(result, defIndent, TAG_INDEX_UNIQUE, String.valueOf("YES".equals(value)));
			
			value = this.indexList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG);
			tagWriter.appendTag(result, defIndent, TAG_INDEX_PK, String.valueOf("YES".equals(value)));
			
			tagWriter.appendCloseTag(result, idxIndent, TAG_INDEX_START);
		}
		tagWriter.appendCloseTag(result, indent, TAG_INDEX_DEFS);
		return result;
	}

	public void setNamespace(String name)
	{
		this.tagWriter.setNamespace(name);
	}
	
}
