/*
 * ColumnReference.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.report;

import workbench.util.StrBuffer;

/**
 *
 * @author  info@sql-workbench.net
 */
public class ColumnReference
{
	public static final String TAG_REFERENCE = "references";
	public static final String TAG_CONSTRAINT_NAME = "constraint-name";
	public static final String TAG_UPDATE_RULE = "update-rule";
	public static final String TAG_DELETE_RULE = "delete-rule";
	
	private String fkName;
	private String foreignColumn;
	private String foreignTable;
	private String updateRule;
	private String deleteRule;
	private TagWriter tagWriter = new TagWriter();
	
	public ColumnReference()
	{
	}
	
	public void setNamespace(String namespace)
	{
		this.tagWriter.setNamespace(namespace);
	}
	public void setConstraintName(String name) { this.fkName = name; }
	public void setForeignColumn(String col) { this.foreignColumn = col; }
	public void setForeignTable(String tbl) { this.foreignTable = tbl; }
	public void setUpdateRule(String rule) { this.updateRule = rule; }
	public void setDeleteRule(String rule) { this.deleteRule = rule; }
	
	public StrBuffer getXml(StrBuffer indent)
	{
		StrBuffer result = new StrBuffer(250);
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		tagWriter.appendOpenTag(result, indent, TAG_REFERENCE);
		result.append('\n');

		result.append(getInnerXml(myindent));
		
		tagWriter.appendCloseTag(result, indent, TAG_REFERENCE);
		
		return result;
	}

	public StrBuffer getInnerXml(StrBuffer indent)
	{
		StrBuffer result = new StrBuffer(250);
		tagWriter.appendTag(result, indent, ReportTable.TAG_TABLE_NAME, this.foreignTable);
		tagWriter.appendTag(result, indent, ReportColumn.TAG_COLUMN_NAME, this.foreignColumn);
		tagWriter.appendTag(result, indent, TAG_CONSTRAINT_NAME, this.fkName);
		tagWriter.appendTag(result, indent, TAG_DELETE_RULE, this.deleteRule);
		tagWriter.appendTag(result, indent, TAG_UPDATE_RULE, this.updateRule);
		return result;
	}
	
	public boolean equals(Object o)
	{
		if (o == null) return false;
		if (o instanceof ColumnReference) return equals((ColumnReference)o);
		return false;
	}
	
	public boolean equals(ColumnReference ref)
	{
		try
		{
			return (this.foreignColumn.equals(ref.foreignColumn) &&
			        this.foreignTable.equals(ref.foreignTable) &&
							this.updateRule.equals(ref.updateRule) &&
							this.deleteRule.equals(ref.deleteRule)
			        );
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
