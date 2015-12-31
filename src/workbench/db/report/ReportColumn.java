/*
 * ReportColumn.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.report;

import workbench.db.ColumnIdentifier;
import workbench.db.sqltemplates.ColumnDefinitionTemplate;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
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
	public static final String TAG_COLUMN_AUTO_INC = "auto-increment";
	public static final String TAG_COLUMN_GENERATED = "generated-column";
	public static final String TAG_COLUMN_DEFAULT = "default-value";
	public static final String TAG_COLUMN_COLLATION = "collation";
	public static final String TAG_COLUMN_NULLABLE = "nullable";
	public static final String TAG_COLUMN_PK = "primary-key";
	public static final String TAG_COLUMN_COMMENT = "comment";
	public static final String TAG_COLUMN_COMPUTED_COL = "computed-column-expression";

	private ColumnReference fk;
	private ColumnIdentifier column;
	private TagWriter tagWriter = new TagWriter();
	private boolean isRealColumn = true;
	private boolean isReferenced = false;
  private ColumnDefinitionTemplate template = new ColumnDefinitionTemplate();

	public ReportColumn(ColumnIdentifier col)
	{
		this.column = col;
	}

  public void setFixDefaultValue(boolean flag)
  {
    template.setFixDefaultValues(flag);
  }

	public ColumnIdentifier getColumn()
	{
		return this.column;
	}

	public ColumnReference getForeignKey()
	{
		return this.fk;
	}

	public void setIsReferenced(boolean flag)
	{
		isReferenced = flag;
	}
	public boolean isReferenced()
	{
		return isReferenced;
	}

	public void setForeignKeyReference(ColumnReference ref)
	{
		this.fk = ref;
	}

	public void appendXml(StringBuilder result, StringBuilder indent)
	{
		appendXml(result, indent, true);
	}

	public void appendXml(StringBuilder result, StringBuilder indent, boolean includePosition)
	{
		appendXml(result, indent, includePosition, TAG_COLUMN_DEFINITION, false);
	}

	public void appendXml(StringBuilder result, StringBuilder indent, boolean includePosition, String mainTagToUse, boolean shortInfo)
	{
		StringBuilder myindent = new StringBuilder(indent);

		myindent.append("  ");
		if (shortInfo)
		{
			tagWriter.appendOpenTag(result, indent, mainTagToUse);
		}
		else
		{
			tagWriter.appendOpenTag(result, indent, mainTagToUse, "name", SqlUtil.removeObjectQuotes(this.column.getColumnName()));
		}

		result.append('\n');

		if (includePosition) tagWriter.appendTag(result, myindent, TAG_COLUMN_POSITION, this.column.getPosition());
		if (!shortInfo) tagWriter.appendTag(result, myindent, TAG_COLUMN_NAME, SqlUtil.removeObjectQuotes(this.column.getColumnName()));
		tagWriter.appendTag(result, myindent, TAG_COLUMN_DBMS_TYPE, this.column.getDbmsType());
		if (isRealColumn && !shortInfo) tagWriter.appendTag(result, myindent, TAG_COLUMN_PK, this.column.isPkColumn());
		if (isRealColumn) tagWriter.appendTag(result, myindent, TAG_COLUMN_NULLABLE, this.column.isNullable());
		if (isRealColumn) tagWriter.appendTag(result, myindent, TAG_COLUMN_DEFAULT, getDefaultValue(), true);
		if (isRealColumn) tagWriter.appendTag(result, myindent, TAG_COLUMN_AUTO_INC, this.column.isAutoincrement());
		if (isRealColumn && this.column.isGenerated() != null)
		{
			tagWriter.appendTag(result, myindent, TAG_COLUMN_GENERATED, this.column.isGenerated());
		}
		tagWriter.appendTag(result, myindent, TAG_COLUMN_SIZE, this.column.getColumnSize());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_DIGITS, this.column.getDigitsDisplay());
		if (!shortInfo) tagWriter.appendTag(result, myindent, TAG_COLUMN_JAVA_TYPE, this.column.getDataType());
		tagWriter.appendTag(result, myindent, TAG_COLUMN_JAVA_TYPE_NAME, SqlUtil.getTypeName(this.column.getDataType()));
		if (isRealColumn)
		{
			if (StringUtil.isNonBlank(column.getComputedColumnExpression()))
			{
				tagWriter.appendTag(result, myindent, TAG_COLUMN_COMPUTED_COL, column.getComputedColumnExpression(), true);
			}
			if (StringUtil.isNonBlank(column.getCollation()))
			{
				tagWriter.appendTag(result, myindent, TAG_COLUMN_COLLATION, column.getCollation());
			}
		}
		if (!shortInfo) tagWriter.appendTag(result, myindent, TAG_COLUMN_COMMENT, this.column.getComment(), true);

		if (this.fk != null)
		{
			result.append(fk.getXml(myindent));
		}
		tagWriter.appendCloseTag(result, indent, mainTagToUse);
	}

  private String getDefaultValue()
  {
    if (column.getDefaultValue() == null) return "";
    return template.getDefaultExpression(column);
  }

	/**
	 * Marks this column as being a "real" column or not.
	 * If this ReportColumn is used for a VIEW definition,
	 * the columns should be marked as "not real" in order to
	 * surpress certain column attributes that only make sense for
	 * real table columns.
	 */
	public void setIsRealColumn(boolean flag)
	{
		this.isRealColumn = flag;
	}

	@Override
	public String toString()
	{
		return column.getColumnName();
	}
}
