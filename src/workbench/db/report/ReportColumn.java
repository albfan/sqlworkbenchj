/*
 * ReportColumn.java
 *
 * Created on September 9, 2004, 6:21 PM
 */

package workbench.db.report;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.XmlRowDataConverter;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ReportColumn
{
	public static final String TAG_COLUMN_DEFINITION = "column-def";
	public static final String TAG_COLUMN_NAME = "column-name";
	public static final String TAG_COLUMN_DBMS_TYPE = "dbms-data-type";
	public static final String TAG_COLUMN_JAVA_TYPE_NAME = "java-sql-type-name";
	public static final String TAG_COLUMN_JAVA_TYPE = "java-sql-type";
	public static final String TAG_COLUMN_JAVA_CLASS = "java-class";

	public static final String TAG_COLUMN_SIZE = "dbms-data-size";
	public static final String TAG_COLUMN_DIGITS = "dbms-data-digits";
	public static final String TAG_COLUMN_POSITION = "dbms-position";
	public static final String TAG_COLUMN_DEFAULT = "default-value";
	public static final String TAG_COLUMN_NULLABLE = "nullable";
	public static final String TAG_COLUMN_PK = "primary-key";
	public static final String TAG_COLUMN_COMMENT = "comment";

	private ColumnReference fk;
	private ColumnIdentifier column;
	private TagWriter tagWriter = new TagWriter();

	/** Creates a new instance of ReportColumn */
	public ReportColumn(ColumnIdentifier col)
	{
		this.column = col;
	}

	public ColumnIdentifier getColumn()
	{
		return this.column;
	}

	public void setForeignKeyReference(ColumnReference ref)
	{
		this.fk = ref;
		if (this.fk != null)
		{
			this.fk.setNamespace(this.tagWriter.getNamespace());
		}
	}

	public void appendXml(StrBuffer result, StrBuffer indent)
	{
		StrBuffer myindent = new StrBuffer(indent);

		myindent.append("  ");
		tagWriter.appendOpenTag(result, indent, TAG_COLUMN_DEFINITION);
		result.append('\n');

		tagWriter.appendTag(result, myindent, TAG_COLUMN_POSITION, this.column.getPosition());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_NAME, this.column.getColumnName());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_DBMS_TYPE, this.column.getDbmsType());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_PK, this.column.isPkColumn());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_NULLABLE, this.column.isNullable());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_DEFAULT, this.column.getDefaultValue());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_SIZE, this.column.getColumnSize());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_DIGITS, this.column.getDecimalDigits());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_JAVA_TYPE, this.column.getDataType());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_JAVA_TYPE_NAME, SqlUtil.getTypeName(this.column.getDataType()));

		tagWriter.appendOpenTag(result, myindent, TAG_COLUMN_COMMENT);
		String comment = this.column.getComment();
		if (comment != null && comment.trim().length() > 0)
		{
			result.append("<![CDATA[");
			result.append(comment);
			result.append("]]>");
		}
		tagWriter.appendCloseTag(result, null, TAG_COLUMN_COMMENT);

		if (this.fk != null)
		{
			result.append(fk.getXml(myindent));
		}
		tagWriter.appendCloseTag(result, indent, TAG_COLUMN_DEFINITION);
		return;
	}

	public void setNamespace(String namespace)
	{
		this.tagWriter.setNamespace(namespace);
		if (this.fk != null)
		{
			this.fk.setNamespace(namespace);
		}
	}

}