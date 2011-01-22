/*
 * ViewDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import workbench.db.IndexDefinition;
import workbench.db.report.ReportView;
import workbench.db.report.TagAttribute;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

/**
 * Compares two database views for differences in their definition. 
 * The generating source of the views is compared using String.equals(), 
 * so any difference in Upper/Lowercase writing (even if not important
 * for the functionality of the view) qualify the two views as 
 * beeing different.
 * 
 * @author Thomas Kellerer
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
		boolean sourceDifferent = false;
		boolean indexDifferent = false;
		boolean createView = (target == null);
		
		CharSequence s = null;
		
		s = reference.getViewSource();
		String refSource = (s == null ? null : s.toString());
		s = (target == null ? null : target.getViewSource());
		String targetSource = (s == null ? null : s.toString());
		
		if (targetSource != null)
		{
			sourceDifferent = !refSource.trim().equals(targetSource.trim());
		}
		
		StrBuffer indexDiff = getIndexDiff();
		if (indexDiff != null && indexDiff.length() > 0)
		{
			indexDifferent = true;
		}
		
		if (!sourceDifferent && !createView && !indexDifferent) return result;
		
		List<TagAttribute> att = new ArrayList<TagAttribute>();
			
		String type = reference.getView().getType();
		if (!"VIEW".equals(type))
		{
			att.add(new TagAttribute("type", type));
		}
		
		if (indexDifferent && !sourceDifferent)
		{
			att.add(new TagAttribute("name", target.getView().getTableName()));
		}
		
		writer.appendOpenTag(result, this.indent, (createView ? TAG_CREATE_VIEW : TAG_UPDATE_VIEW), att, true);
		
		result.append('\n');
		if (createView)
		{
			result.append(reference.getXml(myindent, true));
		}
		else if (sourceDifferent)
		{
			result.append(reference.getXml(myindent, indexDifferent));
		}
		else
		{
			result.append(indexDiff);
		}
		writer.appendCloseTag(result, this.indent, (createView ? TAG_CREATE_VIEW : TAG_UPDATE_VIEW));

		return result;
	}	
	
	private StrBuffer getIndexDiff()
	{
		if (this.target == null) return null;
		
		Collection<IndexDefinition> ref = this.reference.getIndexList();
		Collection<IndexDefinition> targ = this.target.getIndexList();
		if (ref == null && targ == null) return null;
		IndexDiff id = new IndexDiff(ref, targ);
		id.setTagWriter(this.writer);
		id.setIndent(indent);
		StrBuffer diff = id.getMigrateTargetXml();
		return diff;
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
