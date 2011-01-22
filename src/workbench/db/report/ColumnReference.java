/*
 * ColumnReference.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import workbench.util.StrBuffer;
import workbench.util.NumberStringCache;

/**
 *
 * @author  Thomas Kellerer
 */
public class ColumnReference
{
	public static final String TAG_REFERENCE = "references";
	public static final String TAG_CONSTRAINT_NAME = "constraint-name";
	public static final String TAG_UPDATE_RULE = "update-rule";
	public static final String TAG_DELETE_RULE = "delete-rule";
	public static final String TAG_DEFER_RULE = "deferrable";

	private ForeignKeyDefinition fkDefinition;
	private String foreignColumn;
	private TagWriter tagWriter = new TagWriter();
	
	public ColumnReference(ForeignKeyDefinition fk)
	{
		this.fkDefinition = fk;
	}
	
	public String getFkName()
	{
		return fkDefinition.getFkName();
	}

	public void setCompareFKRule(boolean flag)
	{
		if (this.fkDefinition != null) fkDefinition.setCompareFKRules(flag);
	}

	public ReportTable getForeignTable()
	{
		return fkDefinition.getForeignTable();
	}
	
	public String getDeleteRule()
	{
		return fkDefinition.getDeleteRule();
	}
	
	public String getUpdateRule()
	{
		return fkDefinition.getUpdateRule();
	}

	public void setForeignColumn(String col)
	{
		this.foreignColumn = col;
	}
	
	public String getForeignColumn()
	{
		return foreignColumn;
	}
	
	public String toString()
	{
		return this.getFkName();
	}
	
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
		this.fkDefinition.getForeignTable().appendTableNameXml(result, indent);
		tagWriter.appendTag(result, indent, ReportColumn.TAG_COLUMN_NAME, this.foreignColumn);
		tagWriter.appendTag(result, indent, TAG_CONSTRAINT_NAME, this.getFkName());

		tagWriter.appendTag(result, indent, TAG_DELETE_RULE, this.fkDefinition.getDeleteRule(), "jdbcValue", NumberStringCache.getNumberString(this.fkDefinition.getDeleteRuleValue()));
		tagWriter.appendTag(result, indent, TAG_UPDATE_RULE, this.fkDefinition.getUpdateRule(), "jdbcValue", NumberStringCache.getNumberString(this.fkDefinition.getUpdateRuleValue()));
		tagWriter.appendTag(result, indent, TAG_DEFER_RULE, this.fkDefinition.getDeferRule(), "jdbcValue", NumberStringCache.getNumberString(this.fkDefinition.getDeferrableRuleValue()));
		return result;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 83 * hash + (this.fkDefinition != null ? this.fkDefinition.hashCode() : 0);
		hash = 83 * hash + (this.foreignColumn != null ? this.foreignColumn.hashCode() : 0);
		return hash;
	}
	
	public boolean equals(Object o)
	{
		if (o == null) return false;
		if (o instanceof ColumnReference) return equals((ColumnReference)o);
		return false;
	}

	public boolean isColumnEqual(ColumnReference ref)
	{
		return this.foreignColumn.equalsIgnoreCase(ref.foreignColumn);
	}
	
	public boolean isFkNameEqual(ColumnReference ref)
	{
		return this.fkDefinition.isNameEqual(ref.fkDefinition);
	}
	
	public boolean isFkDefinitionEqual(ColumnReference ref)
	{
		return this.fkDefinition.isDefinitionEqual(ref.fkDefinition);
	}

}
