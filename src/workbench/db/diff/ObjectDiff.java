/*
 * ObjectDiff.java
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
package workbench.db.diff;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.BaseObjectType;
import workbench.db.ColumnIdentifier;
import workbench.db.ComparableDbObject;
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.db.report.GenericReportObject;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportObjectType;
import workbench.db.report.ReportTable;
import workbench.db.report.TagWriter;

import workbench.util.StringUtil;

import static workbench.db.diff.TableDiff.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectDiff
{
	public static final String TAG_ADD_OBJECT = "add-object";
	public static final String TAG_DROP_OBJECT = "drop-object";
	public static final String TAG_ALTER_OBJECT = "modify-object";
	public static final String TAG_REF_OBJECT_DEF = "reference-object";
	public static final String TAG_TARG_OBJECT_DEF = "target-object";

	public static final String TAG_ADD_TYPE = "add-type";
	public static final String TAG_DROP_TYPE = "drop-type";
	public static final String TAG_ALTER_TYPE = "modify-type";

	private ComparableDbObject referenceObject;
	private ComparableDbObject targetObject;
	private StringBuilder indent = StringUtil.emptyBuilder();
	private String targetSchema;

	public ObjectDiff(ComparableDbObject reference, ComparableDbObject target, String targetSchema)
	{
		this.referenceObject = reference;
		this.targetObject = target;
		this.targetSchema = targetSchema;
	}

	public boolean isDifferent(WbConnection referenceDb, WbConnection targetDb)
	{
		if (targetObject == null && referenceObject == null) return false;
		if (targetObject == null || referenceObject == null) return true;
		if (!targetObject.isEqualTo(referenceObject)) return true;

		try
		{
			CharSequence refSource = referenceObject.getSource(referenceDb);
			CharSequence targSource = targetObject.getSource(targetDb);
			if (refSource == null && targSource == null) return false;
			if (refSource == null || targSource == null) return false;
			return (!refSource.equals(targSource));
		}
		catch (SQLException sql)
		{
			LogMgr.logError("ObjectDiff.isDifferent()", "Could not compare source", sql);
		}
		return false;
	}

	/**
	 * Return the XML that describes how the object table needs to
	 * modified in order to get the same definition as the reference object.
	 *
	 * An empty string means that there are no differences
	 */
	public StringBuilder getMigrateTargetXml(WbConnection referenceDb, WbConnection targetDb)
	{
		if (isObjectType(targetObject) && isObjectType(referenceObject))
		{
			return getObjectTypeDiff(referenceDb, targetDb);
		}
		StringBuilder result = new StringBuilder(200);

		StringBuilder myIndent = new StringBuilder(indent);
		myIndent.append("  ");
		TagWriter writer = new TagWriter();

		if (targetObject == null && referenceObject != null)
		{
			// create a new object
			GenericReportObject reportObject = new GenericReportObject(referenceDb, referenceObject);
			reportObject.setSchemaNameToUse(targetSchema);
			StringBuilder xml = reportObject.getXml(myIndent);
			writer.appendOpenTag(result, indent, TAG_ADD_OBJECT);
			result.append('\n');
			result.append(xml);
			result.append('\n');
			writer.appendCloseTag(result, indent, TAG_ADD_OBJECT);
			result.append('\n');
		}
		else if (targetObject != null && referenceObject == null)
		{
			GenericReportObject reportObject = new GenericReportObject(targetDb, targetObject);
			StringBuilder xml = reportObject.getXml(myIndent);
			writer.appendOpenTag(result, indent, TAG_DROP_OBJECT);
			result.append('\n');
			result.append(xml);
			result.append('\n');
			writer.appendCloseTag(result, indent, TAG_DROP_OBJECT);
			result.append('\n');
		}
		else if (!targetObject.isEqualTo(referenceObject))
		{
			GenericReportObject refObj = new GenericReportObject(referenceDb, referenceObject);
			GenericReportObject tgObj = new GenericReportObject(targetDb, targetObject);
			StringBuilder indent2 = new StringBuilder(myIndent);
			indent2.append("  ");
			writer.appendOpenTag(result, indent, TAG_ALTER_OBJECT);
			result.append('\n');

			writer.appendOpenTag(result, myIndent, TAG_REF_OBJECT_DEF);
			result.append('\n');
			result.append(refObj.getXml(indent2));
			writer.appendCloseTag(result, myIndent, TAG_REF_OBJECT_DEF);

			writer.appendOpenTag(result, myIndent, TAG_TARG_OBJECT_DEF);
			result.append('\n');
			result.append(tgObj.getXml(indent2));
			writer.appendCloseTag(result, myIndent, TAG_TARG_OBJECT_DEF);

			writer.appendCloseTag(result, indent, TAG_ALTER_OBJECT);
			result.append('\n');
		}
		return result;
	}

	private boolean isObjectType(DbObject dbo)
	{
		if (dbo == null) return true;
		return (dbo instanceof BaseObjectType);
	}

	public StringBuilder getObjectTypeDiff(WbConnection referenceDb, WbConnection targetDb)
	{
		String tag = null;
		ReportObjectType reportObject = null;

		if (targetObject == null && referenceObject != null)
		{
			reportObject = new ReportObjectType((BaseObjectType)referenceObject);
			tag = TAG_ADD_TYPE;
			reportObject.setSchemaToUse(targetSchema);
		}
		else if (targetObject != null && referenceObject == null)
		{
			reportObject = new ReportObjectType((BaseObjectType)targetObject);
			tag = TAG_DROP_TYPE;
		}

		if (tag != null)
		{
			StringBuilder result = new StringBuilder(200);
			StringBuilder xml = reportObject.getXml(tag, indent);
			result.append(xml);
			return result;
		}

		return getDiff((BaseObjectType)referenceObject, (BaseObjectType)targetObject);
	}

	private StringBuilder getDiff(BaseObjectType ref, BaseObjectType target)
	{
		if (ref.equals(target)) return StringUtil.emptyBuilder();

		StringBuilder result = new StringBuilder(200);

		StringBuilder myIndent = new StringBuilder(indent);
		myIndent.append("  ");
		TagWriter writer = new TagWriter();
		writer.appendOpenTag(result, indent, TAG_ALTER_TYPE, "name", target.getObjectName());
		result.append('\n');
		ReportObjectType rep = new ReportObjectType(target);
		rep.appendDefinitionXml(result, myIndent);

		ArrayList<ReportColumn> colsToBeAdded = new ArrayList<>();
		List<ReportColumn> refCols = getColumns(ref);
		List<ReportColumn> tCols = getColumns(target);

		StringBuilder myindent = new StringBuilder(indent);
		myindent.append("  ");

		StringBuilder colDiff = new StringBuilder(100);

		for (ReportColumn refCol : refCols)
		{
			ReportColumn tcol = ReportTable.findColumn(tCols, refCol.getColumn().getColumnName());
			if (tcol == null)
			{
				colsToBeAdded.add(refCol);
			}
			else
			{
				ColumnDiff d = new ColumnDiff(refCol, tcol);
				d.setCompareForeignKeys(false);
				d.setCompareJdbcTypes(false);
				d.setIndent(myindent);
				StringBuilder diffXml = d.getMigrateTargetXml();
				if (diffXml.length() > 0)
				{
					colDiff.append(diffXml);
				}
			}
		}
		ArrayList<ReportColumn> colsToBeRemoved = new ArrayList<>();

		for (ReportColumn tcol : tCols)
		{
			if (ReportTable.findColumn(refCols, tcol.getColumn().getColumnName()) == null)
			{
				colsToBeRemoved.add(tcol);
			}
		}

		if (colDiff.length() > 0)
		{
			result.append(colDiff);
		}

		appendAddColumns(writer, result, colsToBeAdded, indent);
		appendRemoveColumns(writer, result, colsToBeRemoved, indent);

		writer.appendCloseTag(result, indent, TAG_ALTER_TYPE);
		result.append('\n');

		return result;
	}

	private List<ReportColumn> getColumns(BaseObjectType type)
	{
		List<ReportColumn> result = new ArrayList<>();
		List<ColumnIdentifier> atts = new ArrayList<>(type.getAttributes());
		Collections.sort(atts);
		for (ColumnIdentifier col : atts)
		{
			result.add(new ReportColumn(col));
		}
		return result;
	}
	/**
	 *	Set an indent for generated the XML.
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
}
