/*
 * TableDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportTable;
import workbench.db.report.ReportTableGrants;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Compares and evaluates the difference between a reference table
 * and a target table.
 * Comparing the columns is delegated to {@link ColumnDiff}
 * 
 * @author  support@sql-workbench.net
 */
public class TableDiff
{
	public static final String TAG_RENAME_TABLE = "rename";
	public static final String TAG_MODIFY_TABLE = "modify-table";
	public static final String TAG_ADD_COLUMN = "add-column";
	public static final String TAG_REMOVE_COLUMN = "remove-column";
	public static final String TAG_ADD_PK = "add-primary-key";
	public static final String TAG_MODIFY_PK = "modify-primary-key";
	public static final String TAG_REMOVE_PK = "remove-primary-key";
	public static final String TAG_TABLE_CONS = "table-constraint";
	
	private ReportTable referenceTable;
	private ReportTable targetTable;
	private StrBuffer indent;
	private TagWriter writer;
	private SchemaDiff diff;
	
	public TableDiff(ReportTable reference, ReportTable target, SchemaDiff factory)
	{
		if (reference == null) throw new NullPointerException("Reference table may not be null");
		if (target == null) throw new NullPointerException("Target table may not be null");
		this.referenceTable = reference;
		this.targetTable = target;
		this.diff = factory;
	}

/**
	 * Return the XML that describes how the target table needs to 
	 * modified in order to get the same structure as the reference table.
	 * An empty string means that there are no differences
	 */
	public StrBuffer getMigrateTargetXml()
	{
		StrBuffer result = new StrBuffer(500);
		TableIdentifier ref = this.referenceTable.getTable();
		TableIdentifier target = this.targetTable.getTable();
		if (this.writer == null) this.writer = new TagWriter();
		StrBuffer colDiff = new StrBuffer(500);
		ArrayList<ReportColumn> colsToBeAdded = new ArrayList<ReportColumn>();
		ReportColumn[] refCols = this.referenceTable.getColumns();
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		
		for (int i=0; i < refCols.length; i++)
		{
			ReportColumn tcol = targetTable.findColumn(refCols[i].getColumn().getColumnName());
			if (tcol == null)
			{
				colsToBeAdded.add(refCols[i]);
			}
			else
			{
				ColumnDiff d = new ColumnDiff(refCols[i], tcol);
				//d.setCompareComments(this.compareComments);
				d.setCompareForeignKeys(this.diff.getIncludeForeignKeys());
				d.setCompareJdbcTypes(diff.getCompareJdbcTypes());
				d.setTagWriter(this.writer);
				d.setIndent(myindent);
				StrBuffer diffXml = d.getMigrateTargetXml();
				if (diffXml.length() > 0)
				{
					colDiff.append(diffXml);
					//colDiff.append('\n');
				}
			}
		}
		ArrayList<ReportColumn> colsToBeRemoved = new ArrayList<ReportColumn>();
		ReportColumn[] tcols = this.targetTable.getColumns();
		for (int i=0; i < tcols.length; i++)
		{
			if (this.referenceTable.findColumn(tcols[i].getColumn().getColumnName()) == null)
			{
				colsToBeRemoved.add(tcols[i]);
			}
		}

//		boolean commentsEqual = true;
//		if (this.compareComments)
//		{
//			commentsEqual = StringUtil.equalString(this.referenceTable.getTableComment(), this.targetTable.getTableComment());
//		}
		
		String refname = ref.getTableName();
		String tname = target.getTableName();
		boolean rename = false;
		
		// If either one of the table names is quoted
		// we have to do a case-sensitiv comparison
		if (refname.charAt(0) == '\"' || tname.charAt(0) == '\"')
		{
			refname = StringUtil.trimQuotes(refname);
			tname = StringUtil.trimQuotes(tname);
			rename = !refname.equals(tname);
		}
		else
		{
			rename = !refname.equalsIgnoreCase(tname);
		}
		
		String rc = this.referenceTable.getTableConstraints();
		String tc = this.targetTable.getTableConstraints();
		
		boolean constraintsEqual = StringUtil.equalString(rc, tc);
		
		List<String> refPk = this.referenceTable.getPrimaryKeyColumns();
		List<String> tPk = this.targetTable.getPrimaryKeyColumns();
		
		StrBuffer indexDiff = getIndexDiff();
		StrBuffer grantDiff = getGrantDiff();
		
		boolean grantDifferent = grantDiff != null && grantDiff.length() > 0;
		
		boolean indexDifferent = indexDiff != null && indexDiff.length() > 0;
		
		if (colDiff.length() == 0 && !rename && colsToBeAdded.size() == 0 
			  && colsToBeRemoved.size() == 0 && refPk.equals(tPk) && constraintsEqual
				&& !indexDifferent && !grantDifferent) 
		{
			return result;
		}

		writer.appendOpenTag(result, this.indent, TAG_MODIFY_TABLE, "name", target.getTableName());
		result.append('\n');
		if (rename)
		{
			writer.appendOpenTag(result, myindent, TAG_RENAME_TABLE);
			result.append('\n');
			myindent.append("  ");
			writer.appendTag(result, myindent, ReportTable.TAG_TABLE_NAME, this.referenceTable.getTable().getTableName());
			myindent.removeFromEnd(2);
			writer.appendCloseTag(result, myindent, TAG_RENAME_TABLE);
		}
		
		appendAddColumns(result, colsToBeAdded);
		appendRemoveColumns(result, colsToBeRemoved);
		
		String pkTagToUse = null;
		String[] attr = new String[] { "name" };
		String[] value = new String[1];
		List pkcols = null;

		if (refPk.size() == 0 && tPk.size() > 0)
		{
			value[0] = this.targetTable.getPrimaryKeyName();
			pkTagToUse = TAG_REMOVE_PK;
			pkcols = this.targetTable.getPrimaryKeyColumns();
		}
		else if (refPk.size() > 0 && tPk.size() == 0)
		{
			value[0] = this.referenceTable.getPrimaryKeyName();
			pkTagToUse = TAG_ADD_PK;
			pkcols = this.referenceTable.getPrimaryKeyColumns();
		}
		else if (!refPk.equals(tPk))
		{
			value[0] = this.targetTable.getPrimaryKeyName();
			pkTagToUse = TAG_MODIFY_PK;
			pkcols = this.referenceTable.getPrimaryKeyColumns();
		}
		
		if (pkcols != null)
		{
			writer.appendOpenTag(result, myindent, pkTagToUse, attr, value);
			result.append('\n');
			myindent.append("  ");
			Iterator itr = pkcols.iterator();
			while (itr.hasNext())
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_NAME, (String)itr.next());
			}
			myindent.removeFromEnd(2);
			writer.appendCloseTag(result, myindent, pkTagToUse);
		}
		
		if (colDiff.length() > 0)
		{
			result.append(colDiff);
		}
		
		if (!constraintsEqual) 
		{
			writer.appendTag(result, myindent, TAG_TABLE_CONS, this.referenceTable.getTableConstraints(), true);
		}
		
		if (indexDifferent)
		{
			result.append(indexDiff);
		}
		if (grantDifferent)
		{
			result.append(grantDiff);
		}
		writer.appendCloseTag(result, this.indent, TAG_MODIFY_TABLE);
		return result;
	}
	
	private void appendAddColumns(StrBuffer result, List colsToAdd)
	{
		Iterator itr = colsToAdd.iterator();
		if (!itr.hasNext()) return;
		StrBuffer myindent = new StrBuffer(this.indent);
		myindent.append("  ");
		writer.appendOpenTag(result, myindent, TAG_ADD_COLUMN);
		result.append('\n');
		myindent.append("  ");
		while (itr.hasNext())
		{
			ReportColumn col = (ReportColumn)itr.next();
			col.setNamespace(this.writer.getNamespace());
			col.appendXml(result, myindent, false);
		}
		myindent.removeFromEnd(2);
		writer.appendCloseTag(result, myindent, TAG_ADD_COLUMN);
		result.append('\n');
	}
	
	private void appendRemoveColumns(StrBuffer result, List colsToRemove)
	{
		Iterator itr = colsToRemove.iterator();
		if (!itr.hasNext()) return;
		StrBuffer myindent = new StrBuffer(this.indent);
		myindent.append(indent);
		while (itr.hasNext())
		{
			ReportColumn col = (ReportColumn)itr.next();
			writer.appendEmptyTag(result, myindent, TAG_REMOVE_COLUMN, "name", col.getColumn().getColumnName());
			result.append('\n');
		}
	}
	
	private StrBuffer getGrantDiff()
	{
		if (!this.diff.getIncludeTableGrants()) return null;
		ReportTableGrants reference = this.referenceTable.getGrants();
		ReportTableGrants target = this.targetTable.getGrants();
		if (reference == null && target == null) return null;
		
		TableGrantDiff td = new TableGrantDiff(reference, target);
		StrBuffer diffXml = td.getMigrateTargetXml(writer, indent);
		return diffXml;
	}
	
	private StrBuffer getIndexDiff()
	{
		if (!this.diff.getIncludeIndex()) return null;
		
		Collection<IndexDefinition> ref = this.referenceTable.getIndexList();
		Collection<IndexDefinition> targ = this.targetTable.getIndexList();
		if (ref == null && targ == null) return null;
		IndexDiff id = new IndexDiff(ref, targ);
		id.setTagWriter(this.writer);
		id.setIndent(indent);
		StrBuffer diffXml = id.getMigrateTargetXml();
		return diffXml;
	}
	
	/**
	 *	Set the {@link workbench.db.report.TagWriter} to 
	 *  be used for writing the XML tags
	 */
	public void setTagWriter(TagWriter tagWriter)
	{
		this.writer = tagWriter;
	}
	
	/**
	 *	Set an indent for generating the XML
	 */
	public void setIndent(String ind)
	{
		if (ind == null) this.indent = null;
		this.indent = new StrBuffer(ind);
	}
	
	/**
	 *	Set an indent for generating the XML
	 */
	public void setIndent(StrBuffer ind)
	{
		this.indent = ind;
	}
	
}
