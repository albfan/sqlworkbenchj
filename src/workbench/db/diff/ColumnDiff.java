/*
 * ColumnDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.sql.Types;
import workbench.db.ColumnIdentifier;
import workbench.db.report.ReportColumn;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Compare two table columns for possible differences.
 * Currently the following attributes are checked:
   <ul>
 * <li>Data type (as returned from the database)</li>
 * <li>Primary key</li>
 * <li>NULL value allowed</li>
 * <li>Default value</li>
 * <li>Comments (if returned by the JDBC driver)</li>
 * </ul>
 * @author  info@sql-workbench.net
 */
public class ColumnDiff
{
	public static final String TAG_MODIFY_COLUMN = "modify-column";

	// Use a ReportColumn for future FK reference diff...
	private ReportColumn referenceColumn;
	private ReportColumn targetColumn;
	private String namespace;
	private StrBuffer indent;
	private TagWriter writer;

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
		
		boolean typeDifferent = !sId.getDbmsType().equalsIgnoreCase(tId.getDbmsType());
//		boolean pkDifferent = (sId.isPkColumn() != tId.isPkColumn());
		boolean nullableDifferent = (sId.isNullable() != tId.isNullable());
		String sdef = sId.getDefaultValue();
		String tdef = tId.getDefaultValue();
		boolean defaultDifferent = !StringUtil.equalString(sdef, tdef);
		
		String scomm = sId.getComment();
		String tcomm = tId.getComment();
		boolean commentDifferent = !StringUtil.equalString(scomm, tcomm);
		
		if (writer == null) this.writer = new TagWriter();
		
		if (typeDifferent || nullableDifferent || defaultDifferent || commentDifferent)
		{
			writer.appendOpenTag(result, this.indent, TAG_MODIFY_COLUMN, "name", tId.getColumnName());
			result.append('\n');
			if (typeDifferent)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_DBMS_TYPE, sId.getDbmsType());
			}
//			if (pkDifferent)
//			{
//				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_PK, sId.isPkColumn());
//			}
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
			writer.appendCloseTag(result, this.indent, TAG_MODIFY_COLUMN);
		}
		return result;
	}

	public static void main(String args[])
	{
		try
		{
			ColumnIdentifier reference = new ColumnIdentifier("VORNAME");
			reference.setColumnSize(20);
			reference.setDbmsType("VARCHAR(25)");
			reference.setDataType(Types.VARCHAR);
			reference.setIsNullable(true);
			reference.setDefaultValue("test");
			ColumnIdentifier target = new ColumnIdentifier("VORNAME");
			target.setColumnSize(25);
			target.setDbmsType("VARCHAR(20)");
			target.setDataType(Types.VARCHAR);
			target.setIsNullable(false);
			ColumnDiff diff = new ColumnDiff(new ReportColumn(reference), new ReportColumn(target));
			System.out.println(diff.getMigrateTargetXml());
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
		System.out.println("Done.");
	}
}
