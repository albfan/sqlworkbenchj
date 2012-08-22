/*
 * PackageDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.diff;

import workbench.db.report.ReportPackage;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

/**
 *
 * @author Thomas Kellerer
 */
public class PackageDiff
{
	public static final String TAG_CREATE_PKG = "create-package";
	public static final String TAG_UPDATE_PKG = "update-package";

	private ReportPackage referencePckg;
	private ReportPackage targetPckg;

	private TagWriter writer;
	private StrBuffer indent;

	public PackageDiff(ReportPackage reference, ReportPackage target)
	{
		this.referencePckg = reference;
		this.targetPckg = target;
	}

	public StrBuffer getMigrateTargetXml()
	{
		StrBuffer result = new StrBuffer(500);
		if (this.writer == null) this.writer = new TagWriter();

		boolean isDifferent = true;
		String tagToUse = TAG_CREATE_PKG;

		CharSequence refSource = referencePckg.getSource();
		CharSequence targetSource = targetPckg == null ? null : targetPckg.getSource();

		if (targetSource != null)
		{
			isDifferent = !refSource.toString().trim().equals(targetSource.toString().trim());
			tagToUse = TAG_UPDATE_PKG;
		}

		StrBuffer myIndent = new StrBuffer(indent);
		myIndent.append("  ");
		if (isDifferent)
		{
			writer.appendOpenTag(result, this.indent, tagToUse);
			result.append('\n');
			referencePckg.setIndent(myIndent);
			result.append(referencePckg.getXml(true));
			writer.appendCloseTag(result, this.indent, tagToUse);
		}
		return result;
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
