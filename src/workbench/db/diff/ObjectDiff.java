/*
 * ObjectDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.diff;

import java.sql.SQLException;
import workbench.db.ComparableDbObject;
import workbench.log.LogMgr;

import workbench.db.WbConnection;
import workbench.db.report.GenericReportObject;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

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

	private ComparableDbObject referenceObject;
	private ComparableDbObject targetObject;
	private StrBuffer indent;

	public ObjectDiff(ComparableDbObject reference, ComparableDbObject target)
	{
		this.referenceObject = reference;
		this.targetObject = target;
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
	public StrBuffer getMigrateTargetXml(WbConnection referenceDb, WbConnection targetDb)
	{
		StrBuffer result = new StrBuffer(200);

		StrBuffer myIndent = new StrBuffer(indent);
		myIndent.append("  ");
		TagWriter writer = new TagWriter();

		if (targetObject == null && referenceObject != null)
		{
			// create a new object
			GenericReportObject reportObject = new GenericReportObject(referenceDb, referenceObject);
			StrBuffer xml = reportObject.getXml(myIndent);
			writer.appendOpenTag(result, indent, TAG_ADD_OBJECT);
			result.append('\n');
			result.append(xml);
			result.append('\n');
			writer.appendCloseTag(result, indent, TAG_ADD_OBJECT);
			result.append('\n');
		}
		else if (targetObject != null && referenceObject == null)
		{
			// create a new object
			GenericReportObject reportObject = new GenericReportObject(targetDb, targetObject);
			StrBuffer xml = reportObject.getXml(myIndent);
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
			StrBuffer indent2 = new StrBuffer(myIndent);
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

	/**
	 *	Set an indent for generated the XML.
	 */
	public void setIndent(StrBuffer ind)
	{
		if (ind == null) this.indent = null;
		this.indent = ind;
	}
}
