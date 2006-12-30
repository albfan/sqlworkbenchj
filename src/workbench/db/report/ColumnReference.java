/*
 * ColumnReference.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import workbench.util.StrBuffer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ColumnReference
{
	public static final String TAG_REFERENCE = "references";
	public static final String TAG_CONSTRAINT_NAME = "constraint-name";
	public static final String TAG_UPDATE_RULE = "update-rule";
	public static final String TAG_DELETE_RULE = "delete-rule";
	
	private String fkName;
	private String foreignColumn;
	private ReportTable foreignTable;
	private String updateRule;
	private String deleteRule;
	private TagWriter tagWriter = new TagWriter();
	private int updateRuleValue;
	private int deleteRuleValue;
	
	public ColumnReference()
	{
	}
	
	public void setNamespace(String namespace)
	{
		this.tagWriter.setNamespace(namespace);
	}
	public void setUpdateRuleValue(int value) { this.updateRuleValue = value; }
	public void setDeleteRuleValue(int value) { this.deleteRuleValue = value; }
	public void setConstraintName(String name) { this.fkName = name; }
	public void setForeignColumn(String col) { this.foreignColumn = col; }
	public void setForeignTable(ReportTable tbl) { this.foreignTable = tbl; }
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
		this.foreignTable.appendTableNameXml(result, indent);
		//tagWriter.appendTag(result, indent, ReportTable.TAG_TABLE_NAME, this.foreignTable);
		tagWriter.appendTag(result, indent, ReportColumn.TAG_COLUMN_NAME, this.foreignColumn);
		tagWriter.appendTag(result, indent, TAG_CONSTRAINT_NAME, this.fkName);
		tagWriter.appendTag(result, indent, TAG_DELETE_RULE, this.deleteRule, "jdbcValue", Integer.toString(this.deleteRuleValue));
		tagWriter.appendTag(result, indent, TAG_UPDATE_RULE, this.updateRule, "jdbcValue", Integer.toString(this.updateRuleValue));
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
