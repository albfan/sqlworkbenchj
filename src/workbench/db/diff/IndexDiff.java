/*
 * IndexDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import workbench.db.IndexDefinition;
import workbench.db.report.IndexReporter;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

/**
 * Compare two index definitions and create an XML 
 * representation of the differences.
 *
 * @author  support@sql-workbench.net
 */
public class IndexDiff
{
	public static final String TAG_MODIFY_INDEX = "modify-index";
	public static final String TAG_ADD_INDEX = "add-index";
	private IndexDefinition[] reference;
	private IndexDefinition[] target;
	private TagWriter writer;
	private StrBuffer indent;
	
	public IndexDiff(IndexDefinition[] ref, IndexDefinition[] targ)
	{
		this.reference = (ref == null ? new IndexDefinition[0] : ref);
		this.target = (targ == null ? new IndexDefinition[0] : targ);
	}
	
	public void setTagWriter(TagWriter w) { this.writer = w; }
	
	public void setIndent(StrBuffer ind)
	{
		this.indent = ind;
	}
	
	public StrBuffer getMigrateTargetXml()
	{
		if (this.writer == null) this.writer = new TagWriter();
		StrBuffer result = new StrBuffer();
		List indexToAdd = new ArrayList();
		int count = this.reference.length;
		
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		
		StrBuffer idxIndent = new StrBuffer(myindent);
		idxIndent.append("  ");
		
		for (int i=0; i < count; i++)
		{
			IndexDefinition ind = this.findIndex(reference[i].getExpression());
			if (ind == null)
			{
				indexToAdd.add(reference[i]);
			}
			else
			{
				boolean uniqueDiff = ind.isUnique() != reference[i].isUnique();
				boolean pkDiff = ind.isPrimaryKeyIndex() != reference[i].isPrimaryKeyIndex();
				if (uniqueDiff || pkDiff)
				{
					writer.appendOpenTag(result, myindent, TAG_MODIFY_INDEX, "name", ind.getName());
					result.append('\n');
					if (uniqueDiff)
					{
						writer.appendTag(result, idxIndent, IndexReporter.TAG_INDEX_UNIQUE, reference[i].isUnique());
					}
					if (pkDiff)
					{
						writer.appendTag(result, idxIndent, IndexReporter.TAG_INDEX_PK, reference[i].isPrimaryKeyIndex());
					}	
					writer.appendCloseTag(result, myindent, TAG_MODIFY_INDEX);
				}
			}
		}
		
		if (indexToAdd.size() > 0)
		{
			Iterator itr = indexToAdd.iterator();
			writer.appendOpenTag(result, myindent, TAG_ADD_INDEX);
			result.append('\n');
			while (itr.hasNext())
			{
				IndexReporter rep = new IndexReporter((IndexDefinition)itr.next());
				rep.appendXml(result, idxIndent);
			}
			writer.appendCloseTag(result, myindent, TAG_ADD_INDEX);
		}
		return result;
	}
	
	private IndexDefinition findIndex(String expr)
	{
		for (int i=0; i < this.target.length; i++)
		{
			if (target[i].equals(expr)) return target[i];
		}
		return null;
	}
}
