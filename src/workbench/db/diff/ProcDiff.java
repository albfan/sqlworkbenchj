/*
 * ProcDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import workbench.db.report.ReportProcedure;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

/**
 *
 * @author support@sql-workbench.net
 */
public class ProcDiff
{
	public static final String TAG_CREATE_PROC = "create-proc";
	public static final String TAG_UPDATE_PROC = "update-proc";
	
	private ReportProcedure reference;
	private ReportProcedure target;
	private TagWriter writer;
	private StrBuffer indent;
	
	public ProcDiff(ReportProcedure ref, ReportProcedure tar)
	{
		reference = ref;
		target = tar;
	}
	
	public StrBuffer getMigrateTargetXml()
	{
		StrBuffer result = new StrBuffer(500);
		if (this.writer == null) this.writer = new TagWriter();
		
		boolean isDifferent = true;
		String tagToUse = TAG_CREATE_PROC;

		String refSource = reference.getSource();
		String targetSource = target.getSource();
		
		if (targetSource != null)
		{
			isDifferent = !refSource.equals(targetSource);
			tagToUse = TAG_UPDATE_PROC;
		}
		
		StrBuffer myIndent = new StrBuffer(indent);
		myIndent.append("  ");
		if (isDifferent)
		{
			writer.appendOpenTag(result, this.indent, tagToUse);
			result.append('\n');
			reference.setIndent(myIndent);
			result.append(reference.getXml());
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
