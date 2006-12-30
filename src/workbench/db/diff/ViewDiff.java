/*
 * ViewDiff.java
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

import workbench.db.TableIdentifier;
import workbench.db.report.ReportView;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

/**
 *
 * @author support@sql-workbench.net
 */
public class ViewDiff
{
	public static final String TAG_CREATE_VIEW = "create-view";
	public static final String TAG_UPDATE_VIEW = "update-view";
	
	private ReportView reference;
	private ReportView target;
	private TagWriter writer;
	private StrBuffer indent;
	
	public ViewDiff(ReportView ref, ReportView tar)
	{
		reference = ref;
		target = tar;
	}
	
	public StrBuffer getMigrateTargetXml()
	{
		StrBuffer result = new StrBuffer(500);
		if (this.writer == null) this.writer = new TagWriter();
		
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		boolean isDifferent = true;
		String tagToUse = TAG_CREATE_VIEW;

		String refSource = reference.getViewSource();
		String targetSource = (target == null ? null : target.getViewSource());
		if (targetSource != null)
		{
			isDifferent = !refSource.trim().equals(targetSource.trim());
			tagToUse = TAG_UPDATE_VIEW;
		}
		
		if (isDifferent)
		{
			writer.appendOpenTag(result, this.indent, tagToUse);
			result.append('\n');
			result.append(reference.getXml(myindent));
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
