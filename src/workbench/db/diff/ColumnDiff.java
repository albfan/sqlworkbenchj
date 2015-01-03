/*
 * ColumnDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.diff;

import workbench.db.ColumnIdentifier;
import workbench.db.report.ColumnReference;
import workbench.db.report.ReportColumn;
import workbench.db.report.TagWriter;

import workbench.util.SqlUtil;
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
	public static final String TAG_CHANGED_ATTRIBUTES = "new-column-attributes";

	private ReportColumn referenceColumn;
	private ReportColumn targetColumn;
	private StringBuilder indent;
	private final TagWriter writer = new TagWriter();
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
	 *	Set an indent for generating the XML
	 */
	public void setIndent(StringBuilder ind)
	{
		if (ind == null)
		{
			this.indent = StringUtil.emptyBuilder();
		}
		else
		{
			this.indent = ind;
		}
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
	public StringBuilder getMigrateTargetXml()
	{
		ColumnIdentifier sId = this.referenceColumn.getColumn();
		ColumnIdentifier tId = this.targetColumn.getColumn();
		StringBuilder result = new StringBuilder();
		StringBuilder myindent = new StringBuilder(this.indent);
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
		boolean collationsDifferent = !StringUtil.equalString(sId.getCollation(), tId.getCollation());

		if (this.compareFK)
		{
			if (refFk != null && targetFk != null)
			{
				// when comparing only JDBC types, we should ignore FK rules as they differ extremely between DBMS
				refFk.setCompareFKRule(!this.compareJdbcTypes);
				targetFk.setCompareFKRule(!this.compareJdbcTypes);

				fkDefinitionDifferent = !(refFk.isFkDefinitionEqual(targetFk));
				fkNameDifferent = !(refFk.isFkNameEqual(targetFk));
			}
			else
			{
				fkDefinitionDifferent = (refFk != null || targetFk != null);
			}
		}

		String scomm = sId.getComment();
		String tcomm = tId.getComment();
		boolean commentDifferent = false;
		if (this.compareComments)
		{
			commentDifferent = !StringUtil.equalString(scomm, tcomm);
		}

		if (typeDifferent ||
			  nullableDifferent ||
			  defaultDifferent ||
			  commentDifferent ||
			  fkNameDifferent || fkDefinitionDifferent ||
			  computedColIsDifferent || collationsDifferent)
		{
			writer.appendOpenTag(result, this.indent, TAG_MODIFY_COLUMN, "name", SqlUtil.removeObjectQuotes(tId.getColumnName()));
			result.append('\n');

			// for some DBMS the full definition of the column must be used in order
			// to be able to generate an ALTER TABLE statement that changes the column definition
			// that's why the complete definition of the reference table is repeated in the output
			if (typeDifferent || nullableDifferent || defaultDifferent || commentDifferent || computedColIsDifferent)
			{
				referenceColumn.appendXml(result, myindent, false, "reference-column-definition", true);
			}

			StringBuilder attIndent = new StringBuilder(myindent);
			attIndent.append("  ");
			writer.appendOpenTag(result, myindent, TAG_CHANGED_ATTRIBUTES);
			result.append('\n');

			if (typeDifferent)
			{
				writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_DBMS_TYPE, sId.getDbmsType());
				writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_SIZE, sId.getColumnSize());
				if (SqlUtil.isNumberType(sId.getDataType()))
				{
					writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_DIGITS, sId.getDigitsDisplay());
				}
				String stype = sId.getColumnTypeName();
				String ttype = tId.getColumnTypeName();
				if (!stype.equals(ttype))
				{
					writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_JAVA_TYPE_NAME, sId.getColumnTypeName());
				}
			}

			if (nullableDifferent)
			{
				writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_NULLABLE, sId.isNullable());
			}

			if (defaultDifferent)
			{
				if (StringUtil.isBlank(sdef))
				{
					writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_DEFAULT, "", "remove", "true");
				}
				else
				{
					writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_DEFAULT, sdef);
				}
			}
			if (commentDifferent)
			{
				if (StringUtil.isBlank(scomm))
				{
					writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_COMMENT, "", "remove", "true");
				}
				else
				{
					writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_COMMENT, scomm);
				}
			}

			if (computedColIsDifferent)
			{
				writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_COMPUTED_COL, sId.getComputedColumnExpression());
			}

			if (collationsDifferent)
			{
				writer.appendTag(result, attIndent, ReportColumn.TAG_COLUMN_COLLATION, sId.getCollation());
			}
			writer.appendCloseTag(result, myindent, TAG_CHANGED_ATTRIBUTES);

			if (fkDefinitionDifferent)
			{
				StringBuilder refIndent = new StringBuilder(myindent);
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
				StringBuilder refIndent = new StringBuilder(myindent);
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
