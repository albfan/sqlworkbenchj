/*
 * ReportTable.java
 *
 * Created on September 9, 2004, 6:21 PM
 */

package workbench.db.report;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.storage.DataStore;
import workbench.util.StrBuffer;
import workbench.util.StrWriter;

/**
 *
 * @author  workbench@kellerer.org
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
	
	/** Creates a new instance of ReportTable */
	public ReportTable(TableIdentifier tbl, WbConnection conn, String namespace)
		throws SQLException
	{
		this.table = tbl;
		List cols = conn.getMetadata().getTableColumns(tbl);
		this.tableComment = conn.getMetadata().getTableComment(this.table);
		
		int numCols = cols.size();
		this.columns = new ReportColumn[numCols];
		for (int i=0; i < numCols; i++)
		{
			ColumnIdentifier col = (ColumnIdentifier)cols.get(i);
			this.columns[i] = new ReportColumn(col);
			this.columns[i].setNamespace(namespace);
		}
		this.index = new IndexReporter(tbl, conn);
		this.index.setNamespace(namespace);
		this.tagWriter.setNamespace(namespace);
		this.readForeignKeys(conn);
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
	
	private ReportColumn findColumn(String col)
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
	
	public String getXml()
	{
		StrWriter out = new StrWriter(4000);
		try
		{
			this.writeXml(out);
		}
		catch (IOException e)
		{
		}
		return out.toString();
	}
	
	public void writeXml(Writer out)
		throws IOException
	{
		StrBuffer line = new StrBuffer();
		StrBuffer indent = new StrBuffer("  ");
		StrBuffer colindent = new StrBuffer(indent);
		colindent.append(indent);
		
		tagWriter.appendOpenTag(line, indent, TAG_TABLE_DEF);
		line.append('\n');
		
		tagWriter.appendTag(line, colindent, TAG_TABLE_CATALOG, this.table.getCatalog());
		tagWriter.appendTag(line, colindent, TAG_TABLE_SCHEMA, this.table.getSchema());
		tagWriter.appendTag(line, colindent, TAG_TABLE_NAME, this.table.getTable());
		tagWriter.appendTag(line, colindent, TAG_TABLE_COMMENT, this.tableComment);
		
		line.writeTo(out);
		
		int cols = this.columns.length;
		for (int i=0; i < cols; i++)
		{
			StrBuffer col = this.columns[i].getXml(colindent);
			col.writeTo(out);
		}
		StrBuffer idx = this.index.getXml(colindent);
		if (idx != null)
		{
			line = new StrBuffer();
			line.append(idx);
			line.writeTo(out);
		}
		line = new StrBuffer();
		tagWriter.appendCloseTag(line, indent, TAG_TABLE_DEF);
		line.writeTo(out);
	}
	
	/**
	 * Setter for property xmlNamespace.
	 * @param xmlNamespace New value of property xmlNamespace.
	 */
	public void setNamespace(String namespace)
	{
		this.tagWriter.setNamespace(namespace);
	}
	
}
