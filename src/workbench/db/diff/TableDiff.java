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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import workbench.db.IndexDefinition;
import workbench.db.TableConstraint;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportTable;
import workbench.db.report.ReportTableGrants;
import workbench.db.report.TagWriter;
import workbench.util.CollectionUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Compares and evaluates the difference between a reference table
 * and a target table.
 * Comparing the columns is delegated to {@link ColumnDiff}
 *
 * @author Thomas Kellerer
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
	private boolean checkConstraintNames;

	public TableDiff(ReportTable reference, ReportTable target, SchemaDiff factory)
	{
		if (reference == null) throw new NullPointerException("Reference table may not be null");
		if (target == null) throw new NullPointerException("Target table may not be null");
		this.referenceTable = reference;
		this.targetTable = target;
		this.diff = factory;
	}

	/**
	 * Controls how check constraints are compared. If this is set to
	 * true, name and expression will be compared. If this is false
	 * only the expressions will be compared
	 * @param flag
	 */
	public void setExactConstraintMatch(boolean flag)
	{
		this.checkConstraintNames = flag;
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

		List<TableConstraint> missingConstraints = getMissingConstraints();
		List<TableConstraint> modifiedConstraints = getModifiedConstraints();
		List<TableConstraint> constraintsToDelete = getConstraintsToDelete();

		List<TriggerDefinition> refTriggers = referenceTable.getTriggers();
		List<TriggerDefinition> tarTriggers = targetTable.getTriggers();

		boolean triggersDifferent = false;
		TriggerListDiff trgDiff = null;
		if (!CollectionUtil.isEmpty(refTriggers) || !CollectionUtil.isEmpty(tarTriggers))
		{
			trgDiff = new TriggerListDiff(refTriggers, tarTriggers);
			triggersDifferent = trgDiff.hasChanges();
		}

		boolean constraintsAreEqual =
			(missingConstraints.size() == 0 &&
			modifiedConstraints.size() == 0 &&
			constraintsToDelete.size() == 0);

		List<String> refPk = this.referenceTable.getPrimaryKeyColumns();
		List<String> tPk = this.targetTable.getPrimaryKeyColumns();

		StrBuffer indexDiff = getIndexDiff();
		StrBuffer grantDiff = getGrantDiff();

		boolean grantDifferent = grantDiff != null && grantDiff.length() > 0;

		boolean indexDifferent = indexDiff != null && indexDiff.length() > 0;

		if (colDiff.length() == 0 && !rename && colsToBeAdded.size() == 0
			  && colsToBeRemoved.size() == 0 && refPk.equals(tPk) && constraintsAreEqual
				&& !indexDifferent && !grantDifferent && !triggersDifferent)
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

		if (!constraintsAreEqual)
		{
			writer.appendOpenTag(result, myindent, ReportTable.TAG_TABLE_CONSTRAINTS);
			result.append('\n');
			StrBuffer consIndent = new StrBuffer(myindent).append("  ");
			writeConstraints(constraintsToDelete, result, "drop-constraint", consIndent);
			writeConstraints(missingConstraints, result, "add-constraint", consIndent);
			writeConstraints(modifiedConstraints, result, "modify-constraint", consIndent);
			writer.appendCloseTag(result, myindent, ReportTable.TAG_TABLE_CONSTRAINTS);
		}

		if (indexDifferent)
		{
			result.append(indexDiff);
		}

		if (triggersDifferent && trgDiff != null)
		{
			trgDiff.writeXml(myindent, result);
		}
		
		if (grantDifferent)
		{
			result.append(grantDiff);
		}

		writer.appendCloseTag(result, this.indent, TAG_MODIFY_TABLE);
		return result;
	}


	private void writeConstraints(List<TableConstraint> constraints, StrBuffer result, String tag, StrBuffer indent)
	{
		if (constraints.size() == 0) return;
		StrBuffer consIndent = new StrBuffer(indent);
		consIndent.append("  ");
		writer.appendOpenTag(result, indent, tag);
		result.append('\n');
		for (TableConstraint c : constraints)
		{
			if (c == null) continue;
			ReportTable.writeConstraint(c, writer, result, consIndent);
		}
		writer.appendCloseTag(result, indent, tag);
	}

	private List<TableConstraint> getConstraintsToDelete()
	{
		List<TableConstraint> targConstraints = targetTable.getTableConstraints();
		List<TableConstraint> refConstraints = referenceTable.getTableConstraints();
		return processDeltas(targConstraints, refConstraints);
	}

	private List<TableConstraint> getMissingConstraints()
	{
		List<TableConstraint> targConstraints = targetTable.getTableConstraints();
		List<TableConstraint> refConstraints = referenceTable.getTableConstraints();
		return processDeltas(refConstraints, targConstraints);
	}

	private List<TableConstraint> processDeltas(List<TableConstraint> refConstraints, List<TableConstraint> targConstraints)
	{
		if (refConstraints == null) return Collections.emptyList();

		List<TableConstraint> result = CollectionUtil.arrayList();

		for (TableConstraint ref : refConstraints)
		{
			if (ref == null) continue;
			if (checkConstraintNames)
			{
				if (!findByName(targConstraints, ref))
				{
					result.add(ref);
				}
			}
			else
			{
				if (!findByExpression(targConstraints, ref))
				{
					result.add(ref);
				}
			}
		}
		return result;
	}

	private boolean findByExpression(List<TableConstraint> toSearch, TableConstraint cons)
	{
		if (cons == null) return false;
		if (toSearch == null) return false;

		for (TableConstraint c : toSearch)
		{
			if (c.expressionIsEqual(cons)) return true;
		}
		return false;
	}

	private boolean findByName(List<TableConstraint> toSearch, TableConstraint cons)
	{
		if (cons == null) return false;
		if (toSearch == null) return false;

		for (TableConstraint c : toSearch)
		{
			if (StringUtil.equalStringIgnoreCase(c.getConstraintName(), cons.getConstraintName())) return true;
		}
		return false;
	}

	private List<TableConstraint> getModifiedConstraints()
	{
		if (!checkConstraintNames) return Collections.emptyList();
		
		List<TableConstraint> targConstraints = targetTable.getTableConstraints();
		if (targConstraints == null) return Collections.emptyList();

		List<TableConstraint> refConstraints = referenceTable.getTableConstraints();
		if (refConstraints == null) return Collections.emptyList();

		List<TableConstraint> result = CollectionUtil.arrayList();

		for (TableConstraint ref : refConstraints)
		{
			if (isModified(targConstraints, ref))
			{
				result.add(ref);
			}
		}
		return result;
	}

	private boolean isModified(List<TableConstraint> toSearch, TableConstraint cons)
	{
		if (cons == null) return false;
		if (toSearch == null) return false;
		for (TableConstraint c : toSearch)
		{
			if (StringUtil.equalStringIgnoreCase(cons.getConstraintName(), c.getConstraintName()))
			{
				if (!cons.expressionIsEqual(c)) return true;
			}
		}
		return false;
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
