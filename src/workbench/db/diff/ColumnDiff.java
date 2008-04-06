/*
 * ColumnDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import workbench.db.ColumnIdentifier;
import workbench.db.report.ColumnReference;
import workbench.db.report.ReportColumn;
import workbench.db.report.TagWriter;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Compare two table columns for possible differences.
 * Currently the following attributes are checked:
   <ul>
 * <li>Data type (as returned from the database)</li>
 * <li>NULL value allowed</li>
 * <li>Default value</li>
 * <li>Comments (if returned by the JDBC driver)</li>
 * </ul>
 * @author  support@sql-workbench.net
 */
public class ColumnDiff
{
	public static final String TAG_MODIFY_COLUMN = "modify-column";
	public static final String TAG_DROP_FK = "drop-reference";
	public static final String TAG_ADD_FK = "add-reference";
	
	// Use a ReportColumn for future FK reference diff...
	private ReportColumn referenceColumn;
	private ReportColumn targetColumn;
	private StrBuffer indent;
	private TagWriter writer;
	private boolean compareFK = true;
	private boolean compareComments = true;
	private boolean compareJdbcTypes = false;
	
	/**
	 *	Create a ColumnDiff object for the reference and target columns
	 */
	public ColumnDiff(ReportColumn reference, ReportColumn target)
	{
		if (reference == null) throw new NullPointerException("Reference column may not be null");
		if (target == null) throw new NullPointerException("Target column may not be null");
		this.referenceColumn = reference;
		this.targetColumn = target;
	}

	public void setCompareForeignKeys(boolean flag) { this.compareFK = flag; }
	public void setCompareComments(boolean flag) { this.compareComments = flag; }
	
	/**
	 *	Set the {@link workbench.db.report.TagWriter} to 
	 *  be used for writing the XML tags
	 */
	public void setTagWriter(TagWriter tw)
	{
		this.writer = tw;
	}
	
	/**
	 *	Set an indent for generating the XML
	 */
	public void setIndent(String ind)
	{
		if (ind == null) this.indent = null;
		else this.indent = new StrBuffer(ind);
	}

	/**
	 *	Set an indent for generating the XML
	 */
	public void setIndent(StrBuffer ind)
	{
		this.indent = ind;
	}
	
	private boolean typesAreEqual()
	{
		ColumnIdentifier sId = this.referenceColumn.getColumn();
		ColumnIdentifier tId = this.targetColumn.getColumn();
		if (this.getCompareJdbcTypes())
		{
			int sourceType = sId.getDataType();
			int targetType = tId.getDataType();
			if (sourceType != targetType) return false;
			
			if (SqlUtil.isCharacterType(sourceType))
			{
				int sourceSize = sId.getColumnSize();
				int targetSize = tId.getColumnSize();
				return targetSize == sourceSize;
			}
			else if (SqlUtil.isNumberType(sourceType))
			{
				int sourceDigits = sId.getDecimalDigits();
				int targetDigits = tId.getDecimalDigits();
				return sourceDigits == targetDigits;
			}
			return true;
		}
		else
		{
			return sId.getDbmsType().equalsIgnoreCase(tId.getDbmsType());
		}
	}
	
	/**
	 * Return the XML describing how to modify the reference column
	 * to get the same definition as the source column.
	 * An empty string means that there are no differences
	 * This does not include foreign key references.
	 */
	public StrBuffer getMigrateTargetXml()
	{
		ColumnIdentifier sId = this.referenceColumn.getColumn();
		ColumnIdentifier tId = this.targetColumn.getColumn();
		StrBuffer result = new StrBuffer();
		StrBuffer myindent = new StrBuffer(this.indent);
		myindent.append("  ");

		// the PK attribute is not checked, because this is already handled
		// by the TableDiff class
		boolean typeDifferent = !typesAreEqual();
		boolean nullableDifferent = (sId.isNullable() != tId.isNullable());
		String sdef = sId.getDefaultValue();
		String tdef = tId.getDefaultValue();
		boolean defaultDifferent = !StringUtil.equalString(sdef, tdef);
		
		ColumnReference refFk = this.referenceColumn.getForeignKey();
		ColumnReference targetFk = this.targetColumn.getForeignKey();
		boolean fkDifferent = false;
		if (this.compareFK)
		{
			if (refFk == null && targetFk == null)
			{
				fkDifferent = false;
			}
			else if ((refFk == null && targetFk != null) || (refFk != null && targetFk == null))
			{
				fkDifferent = true;
			}
			else 
			{
				fkDifferent = !(refFk.equals(targetFk));
			}
		}
		
		String scomm = sId.getComment();
		String tcomm = tId.getComment();
		boolean commentDifferent = false;
		if (this.compareComments)
		{
			commentDifferent = !StringUtil.equalString(scomm, tcomm);
		}
		
		if (writer == null) this.writer = new TagWriter();
		
		if (typeDifferent || nullableDifferent || defaultDifferent || commentDifferent || fkDifferent)
		{
			writer.appendOpenTag(result, this.indent, TAG_MODIFY_COLUMN, "name", tId.getColumnName());
			result.append('\n');
			if (typeDifferent)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_DBMS_TYPE, sId.getDbmsType());
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_SIZE, sId.getColumnSize());
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_DIGITS, sId.getDigitsDisplay());
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_JAVA_TYPE_NAME, sId.getColumnTypeName());
			}
			if (nullableDifferent)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_NULLABLE, sId.isNullable());
			}
			if (defaultDifferent)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_DEFAULT, (sdef == null ? "" : sdef));
			}
			if (commentDifferent)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_COMMENT, (scomm == null ? "" : scomm));
			}
			if (fkDifferent)
			{
				StrBuffer refIndent = new StrBuffer(myindent);
				refIndent.append("  ");
				if (refFk == null)
				{
					writer.appendOpenTag(result, myindent, TAG_DROP_FK);
					result.append('\n');
					result.append(targetFk.getInnerXml(refIndent));
					writer.appendCloseTag(result, myindent, TAG_DROP_FK);
				}
				else
				{
					writer.appendOpenTag(result, myindent, TAG_ADD_FK);
					result.append('\n');
					result.append(refFk.getInnerXml(refIndent));
					writer.appendCloseTag(result, myindent, TAG_ADD_FK);
				}
			}
			writer.appendCloseTag(result, this.indent, TAG_MODIFY_COLUMN);
		}
		return result;
	}

	public boolean getCompareJdbcTypes()
	{
		return compareJdbcTypes;
	}

	public void setCompareJdbcTypes(boolean compareJdbcTypes)
	{
		this.compareJdbcTypes = compareJdbcTypes;
	}
}
