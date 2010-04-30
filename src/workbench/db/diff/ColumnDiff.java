/*
 * ColumnDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
 * @author  Thomas Kellerer
 */
public class ColumnDiff
{
	public static final String TAG_MODIFY_COLUMN = "modify-column";
	public static final String TAG_DROP_FK = "drop-reference";
	public static final String TAG_ADD_FK = "add-reference";
	public static final String TAG_RENAME_FK = "rename-reference";
	
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

	public void setCompareForeignKeys(boolean flag) 
	{
		this.compareFK = flag;
	}
	
	public void setCompareComments(boolean flag)
	{
		this.compareComments = flag;
	}
	
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

			if (SqlUtil.isClobType(targetType) && SqlUtil.isClobType(sourceType))
			{
				return true;
			}
			else if (SqlUtil.isBlobType(targetType) && SqlUtil.isBlobType(sourceType))
			{
				return true;
			}
			else if (SqlUtil.isCharacterType(sourceType) && SqlUtil.isCharacterType(targetType))
			{
				int sourceSize = sId.getColumnSize();
				int targetSize = tId.getColumnSize();
				return targetSize == sourceSize;
			}
			else if (SqlUtil.isIntegerType(sourceType) && SqlUtil.isIntegerType(targetType))
			{
				return true;
			}
			else if (SqlUtil.isNumberType(sourceType) && SqlUtil.isNumberType(targetType))
			{
				int sourceDigits = sId.getDecimalDigits();
				int targetDigits = tId.getDecimalDigits();
				return sourceDigits == targetDigits;
			}
			return (sourceType == targetType);
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
		boolean computedColIsDifferent = !StringUtil.equalString(sId.getComputedColumnExpression(), tId.getComputedColumnExpression());
		
		ColumnReference refFk = this.referenceColumn.getForeignKey();
		ColumnReference targetFk = this.targetColumn.getForeignKey();
		
		boolean fkDefinitionDifferent = false;
		boolean fkNameDifferent = false;

		if (this.compareFK)
		{
			if (refFk == null && targetFk == null)
			{
				fkDefinitionDifferent = false;
			}
			else if ((refFk == null && targetFk != null) || (refFk != null && targetFk == null))
			{
				fkDefinitionDifferent = true;
			}
			else 
			{
				// when comparing only JDBC types, we should ignore FK rule as they
				// differ extremely between DBMS
				refFk.setCompareFKRule(!this.compareJdbcTypes);
				targetFk.setCompareFKRule(!this.compareJdbcTypes);
				
				fkDefinitionDifferent = !(refFk.isFkDefinitionEqual(targetFk));
				fkNameDifferent = !(refFk.isFkNameEqual(targetFk));
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
		
		if (typeDifferent || nullableDifferent || defaultDifferent || commentDifferent || fkNameDifferent || fkDefinitionDifferent || computedColIsDifferent)
		{
			writer.appendOpenTag(result, this.indent, TAG_MODIFY_COLUMN, "name", tId.getColumnName());
			result.append('\n');

			// for some DBMS the full definition of the column must be used in order
			// to be able to generate an ALTER TABLE statement that changes the column definition
			// that's why the complete definition of the reference table is repeated in the output
			if (typeDifferent || nullableDifferent || defaultDifferent || commentDifferent || computedColIsDifferent)
			{
				referenceColumn.appendXml(result, myindent, false, "reference-column-definition", true);
			}

			if (typeDifferent)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_DBMS_TYPE, sId.getDbmsType());
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_SIZE, sId.getColumnSize());
				if (SqlUtil.isNumberType(sId.getDataType()))
				{
					writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_DIGITS, sId.getDigitsDisplay());
				}
				String stype = sId.getColumnTypeName();
				String ttype = tId.getColumnTypeName();
				if (!stype.equals(ttype))
				{
					writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_JAVA_TYPE_NAME, sId.getColumnTypeName());
				}
			}
			
			if (nullableDifferent)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_NULLABLE, sId.isNullable());
			}

			if (defaultDifferent)
			{
				if (StringUtil.isBlank(sdef))
				{
					writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_DEFAULT, "", "remove", "true");
				}
				else
				{
					writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_DEFAULT, sdef);
				}
			}
			if (commentDifferent)
			{
				if (StringUtil.isBlank(scomm))
				{
					writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_COMMENT, "", "remove", "true");
				}
				else
				{
					writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_COMMENT, scomm);
				}
				
			}

			if (computedColIsDifferent)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_COMPUTED_COL, sId.getComputedColumnExpression());
			}

			if (fkDefinitionDifferent)
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
			else if (fkNameDifferent)
			{
				StrBuffer refIndent = new StrBuffer(myindent);
				refIndent.append("  ");
				writer.appendOpenTag(result, myindent, TAG_RENAME_FK);
				result.append('\n');
				result.append(myindent);
				result.append("  <old-name>");
				result.append(targetFk.getFkName());
				result.append("</old-name>\n");
				
				result.append(myindent);
				result.append("  <new-name>");
				result.append(refFk.getFkName());
				result.append("</new-name>\n");
				writer.appendCloseTag(result, myindent, TAG_RENAME_FK);
			}
			writer.appendCloseTag(result, this.indent, TAG_MODIFY_COLUMN);
		}
		return result;
	}

	public boolean getCompareJdbcTypes()
	{
		return compareJdbcTypes;
	}

	public void setCompareJdbcTypes(boolean flag)
	{
		this.compareJdbcTypes = flag;
	}
}
