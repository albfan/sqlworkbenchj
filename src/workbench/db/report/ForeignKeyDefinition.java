/*
 * ForeignKeyDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.util.Map;
import java.util.TreeMap;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.StrBuffer;
import workbench.util.NumberStringCache;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ForeignKeyDefinition
{
	public static final String TAG_FOREIGN_KEY = "foreign-key";
	public static final String TAG_CONSTRAINT_NAME = "constraint-name";
	public static final String TAG_SOURCE_COLS = "source-columns";
	public static final String TAG_TARGET_COLS = "referenced-columns";
	public static final String TAG_UPDATE_RULE = "update-rule";
	public static final String TAG_DELETE_RULE = "delete-rule";
	public static final String TAG_DEFER_RULE = "deferrable";
	
	private String fkName;
	
	// Stores which column in the source table 
	// references which column in the foreignTabl
	private Map<String, String> columnMap = new TreeMap<String, String>(new CaseInsensitiveComparator());
	private ReportTable foreignTable;
	private String updateRule;
	private String deleteRule;
	private String deferRule;
	private TagWriter tagWriter = new TagWriter();
	private int updateRuleValue;
	private int deleteRuleValue;
	private int deferrableRuleValue;
	private boolean compareFKRules = false;
	
	public ForeignKeyDefinition(String name)
	{
		fkName = name;
	}

	public void setCompareFKRules(boolean flag)
	{
		compareFKRules = flag;
	}
	
	public void setNamespace(String namespace)
	{
		this.tagWriter.setNamespace(namespace);
	}
	
	public void addReferenceColumn(String myColumn, String foreignColumn)
	{
		this.columnMap.put(myColumn, foreignColumn);
	}
	
	public void setUpdateRuleValue(int value) { this.updateRuleValue = value; }
	public int getUpdateRuleValue() { return this.updateRuleValue; }
	public void setDeleteRuleValue(int value) { this.deleteRuleValue = value; }
	public int getDeleteRuleValue() { return this.deleteRuleValue; }
	public void setDeferrableRuleValue(int value) { this.deferrableRuleValue = value; }
	public int getDeferrableRuleValue() { return this.deferrableRuleValue; }
	
	public void setForeignTable(ReportTable tbl) 
	{ 
		if (this.foreignTable == null)
		{
			this.foreignTable = tbl;
		} 
	}
	
	public ColumnReference getColumnReference(String sourceCol)
	{
		String targetCol = this.columnMap.get(sourceCol);
		ColumnReference ref = null;
		if (targetCol != null)
		{
			ref = new ColumnReference(this);
			ref.setForeignColumn(targetCol);
		}
		return ref;
	}
	
	public void setUpdateRule(String rule) { this.updateRule = rule; }
	public String getUpdateRule() { return this.updateRule; }
	public void setDeleteRule(String rule) { this.deleteRule = rule; }
	public String getDeleteRule() { return deleteRule; }
	public void setDeferRule(String rule) { this.deferRule= rule; }
	public String getDeferRule() { return this.deferRule; }

	
	public ReportTable getForeignTable()
	{
		return this.foreignTable;
	}
	
	public String getFkName()
	{
		return fkName;
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
		tagWriter.appendOpenTag(result, indent, TAG_FOREIGN_KEY);
		result.append('\n');

		result.append(getInnerXml(myindent));
		
		tagWriter.appendCloseTag(result, indent, TAG_FOREIGN_KEY);
		
		return result;
	}

	public StrBuffer getInnerXml(StrBuffer indent)
	{
		StrBuffer result = new StrBuffer(250);
		StrBuffer colIndent = new StrBuffer(indent);
		colIndent.append("  ");
		tagWriter.appendTag(result, indent, TAG_CONSTRAINT_NAME, this.fkName);
		tagWriter.appendOpenTag(result, indent, "references");
		result.append('\n');
		this.foreignTable.appendTableNameXml(result, colIndent);
		tagWriter.appendCloseTag(result, indent, "references");

		tagWriter.appendOpenTag(result, indent, TAG_SOURCE_COLS);
		result.append('\n');
		
		for (String col : columnMap.keySet())
		{
			tagWriter.appendTag(result, colIndent, "column", col);
		}
		tagWriter.appendCloseTag(result, indent, TAG_SOURCE_COLS);

		tagWriter.appendOpenTag(result, indent, TAG_TARGET_COLS);
		result.append('\n');
		for (String col : columnMap.keySet())
		{
			String target = columnMap.get(col);
			tagWriter.appendTag(result, colIndent, "column", target);
		}
		tagWriter.appendCloseTag(result, indent, TAG_TARGET_COLS);
		
		tagWriter.appendTag(result, indent, TAG_DELETE_RULE, this.deleteRule, "jdbcValue", NumberStringCache.getNumberString(this.deleteRuleValue));
		tagWriter.appendTag(result, indent, TAG_UPDATE_RULE, this.updateRule, "jdbcValue", NumberStringCache.getNumberString(this.updateRuleValue));
		tagWriter.appendTag(result, indent, TAG_DEFER_RULE, this.deferRule, "jdbcValue", NumberStringCache.getNumberString(this.deferrableRuleValue));
		
		return result;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + (this.fkName != null ? this.fkName.hashCode() : 0);
		hash = 53 * hash + (this.foreignTable != null ? this.foreignTable.hashCode() : 0);
		if (compareFKRules)
		{
			hash = 53 * hash + this.updateRuleValue;
			hash = 53 * hash + this.deleteRuleValue;
			hash = 53 * hash + this.deferrableRuleValue;
		}
		return hash;
	}
	
	public boolean equals(Object o)
	{
		if (o == null) return false;
		if (o instanceof ForeignKeyDefinition) return equals((ForeignKeyDefinition)o);
		return false;
	}

	private boolean compareColumns(ForeignKeyDefinition other)
	{
		if (other == null) return false;
		for (Map.Entry<String, String> entry : columnMap.entrySet())
		{
			String mappedTo = other.columnMap.get(entry.getKey().toLowerCase());
			if (!mappedTo.equalsIgnoreCase(entry.getValue())) return false;
		}
		return true;
	}

	public boolean equals(ForeignKeyDefinition ref)
	{
		try
		{
			boolean columnsAreEqual = compareColumns(ref);
			boolean namesAreEqual = this.fkName.equalsIgnoreCase(ref.fkName);
			boolean tablesAreEqual = this.foreignTable.equals(ref.foreignTable);

			boolean baseEquals = columnsAreEqual && namesAreEqual && tablesAreEqual;
			
			if (baseEquals && compareFKRules)
			{
				baseEquals = baseEquals &&
							(this.updateRuleValue == ref.updateRuleValue) &&
							(this.deleteRuleValue == ref.deleteRuleValue) &&
							(this.deferrableRuleValue == ref.deferrableRuleValue);
			}
			return baseEquals;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
