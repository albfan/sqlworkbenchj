/*
 * ReportColumn.java
 *
 * Created on September 9, 2004, 6:21 PM
 */

package workbench.db.report;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.storage.XmlRowDataConverter;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ReportColumn
{
	public static final String TAG_COLUMN_SIZE = "dbms-data-size";
	public static final String TAG_COLUMN_DIGITS = "dbms-data-digits";
	public static final String TAG_COLUMN_POSITION = "dbms-position";
	public static final String TAG_COLUMN_DEFAULT = "default-value";
	public static final String TAG_COLUMN_NULLABLE = "nullable";
	public static final String TAG_COLUMN_PK = "primary-key";
	public static final String TAG_COLUMN_COMMENT = "comment";
	
	private TableIdentifier table;
	private ColumnIdentifier column;
	private String xmlNamespace = null;
	
	/** Creates a new instance of ReportColumn */
	public ReportColumn(ColumnIdentifier col, TableIdentifier tbl)
	{
		this.column = col;
		this.table = tbl;
	}

	public StrBuffer getXml(StrBuffer indent)
	{
		StrBuffer result = new StrBuffer(100);
		StrBuffer myindent = new StrBuffer(indent);
		
		myindent.append(indent);
		appendOpenTag(result, indent, XmlRowDataConverter.TAG_COLUMN_DEFINITION);
		result.append('\n');
		
		appendTag(result, myindent, TAG_COLUMN_POSITION, this.column.getPosition());
		appendTag(result, myindent, XmlRowDataConverter.TAG_COLUMN_NAME, this.column.getColumnName());
		appendTag(result, myindent, XmlRowDataConverter.TAG_COLUMN_DBMS_TYPE, this.column.getDbmsType());
		appendTag(result, myindent, TAG_COLUMN_PK, String.valueOf(this.column.isPkColumn()));
		appendTag(result, myindent, TAG_COLUMN_NULLABLE, String.valueOf(this.column.isNullable()));
		appendTag(result, myindent, TAG_COLUMN_DEFAULT, this.column.getDefaultValue());
		appendTag(result, myindent, TAG_COLUMN_SIZE, this.column.getColumnSize());
		appendTag(result, myindent, TAG_COLUMN_DIGITS, this.column.getDecimalDigits());
		appendTag(result, myindent, XmlRowDataConverter.TAG_COLUMN_JAVA_TYPE, this.column.getDataType());
		appendTag(result, myindent, XmlRowDataConverter.TAG_COLUMN_JAVA_TYPE_NAME, SqlUtil.getTypeName(this.column.getDataType()));
	
		appendOpenTag(result, myindent, TAG_COLUMN_COMMENT);
		String comment = this.column.getComment();
		if (comment != null && comment.trim().length() > 0)
		{
			result.append("<![CDATA[");
			result.append(comment);
			result.append("]]>");
		}
		appendCloseTag(result, null, TAG_COLUMN_COMMENT);
		appendCloseTag(result, indent, XmlRowDataConverter.TAG_COLUMN_DEFINITION);
		
		return result;
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
	 * Getter for property namespace.
	 * @return Value of property namespace.
	 */
	public java.lang.String getNamespace()
	{
		return xmlNamespace;
	}
	
	/**
	 * Setter for property namespace.
	 * @param namespace New value of property namespace.
	 */
	public void setNamespace(java.lang.String namespace)
	{
		this.xmlNamespace = namespace;
	}
	
}
