/*
 * TableDiff.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import workbench.resource.Settings;

import workbench.db.IndexDefinition;
import workbench.db.TableConstraint;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.report.ForeignKeyDefinition;
import workbench.db.report.ObjectOption;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportTable;
import workbench.db.report.ReportTableGrants;
import workbench.db.report.TagWriter;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
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

	private ReportTable referenceTable;
	private ReportTable targetTable;
	private StringBuilder indent = StringUtil.emptyBuilder();
	private final TagWriter writer = new TagWriter();
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
	public StringBuilder getMigrateTargetXml()
	{
		StringBuilder result = new StringBuilder(500);
		TableIdentifier ref = this.referenceTable.getTable();
		TableIdentifier target = this.targetTable.getTable();
		StringBuilder colDiff = new StringBuilder(500);
		ArrayList<ReportColumn> colsToBeAdded = new ArrayList<>();
		List<ReportColumn> refCols = this.referenceTable.getColumns();
		StringBuilder myindent = new StringBuilder(indent);
		myindent.append("  ");

		for (ReportColumn refCol : refCols)
		{
			ReportColumn tcol = targetTable.findColumn(refCol.getColumn().getColumnName());
			if (tcol == null)
			{
				colsToBeAdded.add(refCol);
			}
			else
			{
				ColumnDiff d = new ColumnDiff(refCol, tcol);
				boolean oldFormat = Settings.getInstance().getBoolProperty("workbench.tablediff.columnfk", false);
				d.setCompareForeignKeys(oldFormat && this.diff.getIncludeForeignKeys());
				d.setCompareJdbcTypes(diff.getCompareJdbcTypes());
				d.setIndent(myindent);
				StringBuilder diffXml = d.getMigrateTargetXml();
				if (diffXml.length() > 0)
				{
					colDiff.append(diffXml);
				}
			}
		}
		ArrayList<ReportColumn> colsToBeRemoved = new ArrayList<>();
		List<ReportColumn> tcols = this.targetTable.getColumns();

		for (ReportColumn tcol : tcols)
		{
			if (this.referenceTable.findColumn(tcol.getColumn().getColumnName()) == null)
			{
				colsToBeRemoved.add(tcol);
			}
		}

		String refname = ref.getTableName();
		String tname = target.getTableName();
		boolean rename = false;

		// If either one of the table names is quoted
		// we have to do a case-sensitiv comparison
		if (refname.charAt(0) == '\"' || tname.charAt(0) == '\"')
		{
			refname = SqlUtil.removeObjectQuotes(refname);
			tname = SqlUtil.removeObjectQuotes(tname);
			rename = !refname.equals(tname);
		}
		else
		{
			rename = !refname.equalsIgnoreCase(tname);
		}

		List<TableConstraint> missingConstraints = getMissingConstraints();
		List<TableConstraint> modifiedConstraints = getModifiedConstraints();
		List<TableConstraint> constraintsToDelete = getConstraintsToDelete();

		List<ForeignKeyDefinition> missingFK = getMissingForeignKeys();
		List<ForeignKeyDefinition> fkToDelete = getFKsToDelete();

		List<TriggerDefinition> refTriggers = referenceTable.getTriggers();
		List<TriggerDefinition> tarTriggers = targetTable.getTriggers();

		boolean triggersDifferent = false;

		TriggerListDiff trgDiff = null;
		if (CollectionUtil.isNonEmpty(refTriggers) || CollectionUtil.isNonEmpty(tarTriggers))
		{
			trgDiff = new TriggerListDiff(refTriggers, tarTriggers);
			triggersDifferent = trgDiff.hasChanges();
		}

		boolean fksAreEqual = missingFK.isEmpty() && fkToDelete.isEmpty();
		boolean constraintsAreEqual = missingConstraints.isEmpty() && modifiedConstraints.isEmpty() && constraintsToDelete.isEmpty();

		List<String> refPk = this.referenceTable.getPrimaryKeyColumns();
		List<String> tPk = this.targetTable.getPrimaryKeyColumns();

		StringBuilder indexDiff = getIndexDiff();
		StringBuilder grantDiff = getGrantDiff();

		boolean grantDifferent = grantDiff != null && grantDiff.length() > 0;

		boolean indexDifferent = indexDiff != null && indexDiff.length() > 0;

		String refOptionType = referenceTable.getTable().getSourceOptions().getTypeModifier();
		String tgOptionType = targetTable.getTable().getSourceOptions().getTypeModifier();
		boolean typesAreEquals = StringUtil.equalStringOrEmpty(refOptionType, tgOptionType, false);

		String refTblSpace = referenceTable.getTable().getTablespace();
		String tgTblSpace = targetTable.getTable().getTablespace();
		boolean tblSpacesAreEquals = StringUtil.equalStringOrEmpty(refTblSpace, tgTblSpace, false);

		boolean optionsAreDifferent = optionsAreDifferent();

		if (colDiff.length() == 0 && !rename && colsToBeAdded.isEmpty()
			  && colsToBeRemoved.isEmpty() && refPk.equals(tPk) && constraintsAreEqual && fksAreEqual
				&& !indexDifferent && !grantDifferent && !triggersDifferent && !optionsAreDifferent && typesAreEquals && tblSpacesAreEquals)
		{
			return result;
		}

		writer.appendOpenTag(result, this.indent, TAG_MODIFY_TABLE, "name", SqlUtil.removeObjectQuotes(target.getTableName()));
		result.append('\n');
		targetTable.appendTableNameXml(result, myindent);
		result.append('\n');
		if (rename)
		{
			writer.appendOpenTag(result, myindent, TAG_RENAME_TABLE);
			result.append('\n');
			myindent.append("  ");
			writer.appendTag(result, myindent, ReportTable.TAG_TABLE_NAME, SqlUtil.removeObjectQuotes(this.referenceTable.getTable().getTableName()));
			StringUtil.removeFromEnd(myindent, 2);
			writer.appendCloseTag(result, myindent, TAG_RENAME_TABLE);
		}

		appendAddColumns(writer, result, colsToBeAdded, indent);
		appendRemoveColumns(writer, result, colsToBeRemoved, indent);

		String pkTagToUse = null;
		String[] attr = new String[] { "name" };
		String[] value = new String[1];
		List<String> pkcols = null;

		if (refPk.isEmpty() && tPk.size() > 0)
		{
			value[0] = this.targetTable.getPrimaryKeyName();
			pkTagToUse = TAG_REMOVE_PK;
			pkcols = this.targetTable.getPrimaryKeyColumns();
		}
		else if (refPk.size() > 0 && tPk.isEmpty())
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

		if (CollectionUtil.isNonEmpty(pkcols))
		{
			writer.appendOpenTag(result, myindent, pkTagToUse, attr, value);
			result.append('\n');
			myindent.append("  ");
			for (String col : pkcols)
			{
				writer.appendTag(result, myindent, ReportColumn.TAG_COLUMN_NAME, SqlUtil.removeObjectQuotes(col));
			}
			StringUtil.removeFromEnd(myindent, 2);
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
			StringBuilder consIndent = new StringBuilder(myindent).append("  ");
			writeConstraints(constraintsToDelete, result, "drop-constraint", consIndent);
			writeConstraints(missingConstraints, result, "add-constraint", consIndent);
			writeConstraints(modifiedConstraints, result, "modify-constraint", consIndent);
			writer.appendCloseTag(result, myindent, ReportTable.TAG_TABLE_CONSTRAINTS);
		}

		if (!fksAreEqual)
		{
			writeFKs(fkToDelete, result, "drop-foreign-keys", myindent);
			writeFKs(missingFK, result, "add-foreign-keys", myindent);
		}

		if (!typesAreEquals)
		{
			writer.appendTag(result, myindent, ReportTable.TAG_TABLE_TYPE, referenceTable.getTable().getSourceOptions().getTypeModifier());
		}

		if (!tblSpacesAreEquals)
		{
			writer.appendTag(result, myindent, ReportTable.TAG_TABLESPACE, referenceTable.getTable().getTablespace());
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
		writeOptionsDiff(result, myindent);
		writer.appendCloseTag(result, this.indent, TAG_MODIFY_TABLE);
		return result;
	}

	private void writeOptionsDiff(StringBuilder result, StringBuilder indent)
	{
		List<ObjectOption> refOptions = referenceTable.getDbmsOptions();
		List<ObjectOption> targetOptions = targetTable.getDbmsOptions();
		StringBuilder myindent = new StringBuilder(indent);
		myindent.append("  ");
		boolean firstOption = true;
		boolean optionWritten = false;
		for (ObjectOption refOption : refOptions)
		{
			if (!targetOptions.contains(refOption))
			{
				if (firstOption)
				{
					writer.appendOpenTag(result, indent, "add-options");
					result.append('\n');
					firstOption = false;
				}
				optionWritten = true;
				result.append(refOption.getXml(myindent));
			}
		}
		if (optionWritten)
		{
			writer.appendCloseTag(result, indent, "add-options");
		}

		firstOption = true;
		optionWritten = false;
		for (ObjectOption targetOption : targetOptions)
		{
			if (!refOptions.contains(targetOption))
			{
				if (firstOption)
				{
					writer.appendOpenTag(result, indent, "remove-options");
					result.append('\n');
					firstOption = false;
				}
				optionWritten = true;
				result.append(targetOption.getXml(myindent));
			}
		}
		if (optionWritten)
		{
			writer.appendCloseTag(result, indent, "remove-options");
		}
	}

	private boolean optionsAreDifferent()
	{
		List<ObjectOption> refOptions = referenceTable.getDbmsOptions();
		List<ObjectOption> targetOptions = targetTable.getDbmsOptions();
		return !refOptions.equals(targetOptions);
	}

	private ForeignKeyDefinition findFKByDefinition(Collection<ForeignKeyDefinition> fkDefs, ForeignKeyDefinition toFind)
	{
		for (ForeignKeyDefinition fk : fkDefs)
		{
			if (fk.isDefinitionEqual(toFind)) return fk;
		}
		return null;
	}

	private List<ForeignKeyDefinition> getMissingForeignKeys()
	{
		if (!diff.getIncludeForeignKeys()) Collections.emptyList();
		Collection<ForeignKeyDefinition> sourceFK = referenceTable.getForeignKeys().values();
		Collection<ForeignKeyDefinition> targetFK = targetTable.getForeignKeys().values();

		List<ForeignKeyDefinition> missing = new ArrayList<>();
		for (ForeignKeyDefinition fk : sourceFK)
		{
			ForeignKeyDefinition other = findFKByDefinition(targetFK, fk);
			if (other == null)
			{
				missing.add(fk);
			}
		}
		return missing;
	}

	private List<ForeignKeyDefinition> getFKsToDelete()
	{
		if (!diff.getIncludeForeignKeys()) Collections.emptyList();
		Collection<ForeignKeyDefinition> sourceFK = referenceTable.getForeignKeys().values();
		Collection<ForeignKeyDefinition> targetFK = targetTable.getForeignKeys().values();

		List<ForeignKeyDefinition> toDelete = new ArrayList<>();
		for (ForeignKeyDefinition fk : targetFK)
		{
			ForeignKeyDefinition other = findFKByDefinition(sourceFK, fk);
			if (other == null)
			{
				toDelete.add(fk);
			}
		}
		return toDelete;
	}

	private void writeFKs(List<ForeignKeyDefinition> fks, StringBuilder result, String tag, StringBuilder mainIndent)
	{
		if (fks.isEmpty()) return;
		writer.appendOpenTag(result, mainIndent, tag);
		result.append('\n');
		StringBuilder fkIndent = new StringBuilder(mainIndent);
		fkIndent.append("  ");
		StringBuilder fkDefIndent = new StringBuilder(fkIndent);
		fkDefIndent.append("  ");

		for (ForeignKeyDefinition fk : fks)
		{
			if (fk == null) continue;
			StringBuilder xml = fk.getInnerXml(fkDefIndent);
			writer.appendOpenTag(result, fkIndent, ForeignKeyDefinition.TAG_FOREIGN_KEY);
			result.append('\n');
			result.append(xml);
			writer.appendCloseTag(result, fkIndent, ForeignKeyDefinition.TAG_FOREIGN_KEY);
		}
		writer.appendCloseTag(result, mainIndent, tag);
	}


	private void writeConstraints(List<TableConstraint> constraints, StringBuilder result, String tag, StringBuilder indent)
	{
		if (constraints.isEmpty()) return;
		StringBuilder consIndent = new StringBuilder(indent);
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

	public static void appendAddColumns(TagWriter tw, StringBuilder result, List<ReportColumn> colsToAdd, StringBuilder colIndent)
	{
		if (colsToAdd.isEmpty()) return;

		StringBuilder myindent = new StringBuilder(colIndent);
		myindent.append("  ");
		tw.appendOpenTag(result, myindent, TAG_ADD_COLUMN);
		result.append('\n');
		myindent.append("  ");
		for (ReportColumn col : colsToAdd)
		{
			col.appendXml(result, myindent, false);
		}
		StringUtil.removeFromEnd(myindent, 2);
		tw.appendCloseTag(result, myindent, TAG_ADD_COLUMN);
	}

	public static void appendRemoveColumns(TagWriter tw, StringBuilder result, List<ReportColumn> colsToRemove, StringBuilder colIndent)
	{
		if (colsToRemove.isEmpty()) return;

		StringBuilder myindent = new StringBuilder(colIndent);
		myindent.append("  ");
		for (ReportColumn col : colsToRemove)
		{
			tw.appendEmptyTag(result, myindent, TAG_REMOVE_COLUMN, "name", SqlUtil.removeObjectQuotes(col.getColumn().getColumnName()));
			result.append('\n');
		}
	}

	private StringBuilder getGrantDiff()
	{
		if (!this.diff.getIncludeTableGrants()) return null;
		ReportTableGrants reference = this.referenceTable.getGrants();
		ReportTableGrants target = this.targetTable.getGrants();
		if (reference == null && target == null) return null;

		TableGrantDiff td = new TableGrantDiff(reference, target);
		StringBuilder diffXml = td.getMigrateTargetXml(writer, indent);
		return diffXml;
	}

	private StringBuilder getIndexDiff()
	{
		if (!this.diff.getIncludeIndex()) return null;

		Collection<IndexDefinition> ref = this.referenceTable.getIndexList();
		Collection<IndexDefinition> targ = this.targetTable.getIndexList();
		if (ref == null && targ == null) return null;
		IndexDiff id = new IndexDiff(ref, targ);
		id.setIndent(indent);
		StringBuilder diffXml = id.getMigrateTargetXml();
		return diffXml;
	}

	/**
	 *	Set an indent for generating the XML
	 */
	public void setIndent(String ind)
	{
		if (ind == null)
		{
			this.indent = null;
		}
		else
		{
			this.indent = new StringBuilder(ind);
		}
	}

	/**
	 *	Set an indent for generating the XML
	 */
	public void setIndent(StringBuilder ind)
	{
		this.indent = ind;
	}

}
