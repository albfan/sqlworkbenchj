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
	public static final String TAG_TABLE_NAME = "table-name";
	public static final String TAG_TABLE_CATALOG = "table-catalog";
	public static final String TAG_TABLE_SCHEMA = "table-schema";
	public static final String TAG_TABLE_COMMENT = "table-comment";
	
	private List referencedTables;
	private TableIdentifier table;
	private ReportColumn[] columns;
	private String tableComment;
	private String xmlNamespace;
	
	/** Creates a new instance of ReportTable */
	public ReportTable(TableIdentifier tbl, WbConnection conn, String namespace)
		throws SQLException
	{
		this.table = tbl;
		this.xmlNamespace = namespace;
		List cols = conn.getMetadata().getTableColumns(tbl);
		this.tableComment = conn.getMetadata().getTableComment(this.table);
		
		int numCols = cols.size();
		this.columns = new ReportColumn[numCols];
		for (int i=0; i < numCols; i++)
		{
			ColumnIdentifier col = (ColumnIdentifier)cols.get(i);
			this.columns[i] = new ReportColumn(col, this.table);
			this.columns[i].setNamespace(this.xmlNamespace);
		}
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
		
		appendOpenTag(line, indent, "table-definition");
		line.append('\n');
		
		appendTag(line, colindent, TAG_TABLE_CATALOG, this.table.getCatalog());
		appendTag(line, colindent, TAG_TABLE_SCHEMA, this.table.getSchema());
		appendTag(line, colindent, TAG_TABLE_NAME, this.table.getTable());
		appendTag(line, colindent, TAG_TABLE_COMMENT, this.tableComment);
		line.append('\n');
		
		line.writeTo(out);
		
		int cols = this.columns.length;
		for (int i=0; i < cols; i++)
		{
			StrBuffer col = this.columns[i].getXml(colindent);
			col.writeTo(out);
		}
		line = new StrBuffer();
		appendCloseTag(line, indent, "table-definition");
		line.writeTo(out);
	}
	
	private void appendTag(StrBuffer target, StrBuffer indent, String tag, int value)
	{
		this.appendTag(target, indent, tag, String.valueOf(value));
	}
	
	private void appendTag(StrBuffer target, StrBuffer indent, String tag, String value)
	{
		appendOpenTag(target, indent, tag);
		target.append(value);
		appendCloseTag(target, null, tag);
	}
	
	private void appendOpenTag(StrBuffer target, StrBuffer indent, String tag)
	{
		if (indent != null) target.append(indent);
		target.append('<');
		if (this.xmlNamespace != null)
		{
			target.append(xmlNamespace);
			target.append(':');
		}
		target.append(tag);
		target.append('>');
	}

	private void appendCloseTag(StrBuffer target, StrBuffer indent, String tag)
	{
		if (indent != null) target.append(indent);
		target.append("</");
		if (this.xmlNamespace != null)
		{
			target.append(xmlNamespace);
			target.append(':');
		}
		target.append(tag);
		target.append(">\n");
	}
	
	/**
	 * Getter for property xmlNamespace.
	 * @return Value of property xmlNamespace.
	 */
	public String getNamespace()
	{
		return xmlNamespace;
	}
	
	/**
	 * Setter for property xmlNamespace.
	 * @param xmlNamespace New value of property xmlNamespace.
	 */
	public void setNamespace(String namespace)
	{
		this.xmlNamespace = namespace;
	}
	
}
