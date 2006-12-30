/*
 * IndexDiff.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
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
	private Collection<IndexDefinition> reference;
	private Collection<IndexDefinition> target;
	private TagWriter writer;
	private StrBuffer indent;
	
	public IndexDiff(Collection<IndexDefinition> ref, Collection<IndexDefinition> targ)
	{
		this.reference = (ref == null ? Collections.EMPTY_LIST : ref);
		this.target = (targ == null ? Collections.EMPTY_LIST : targ);
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
		List<IndexDefinition> indexToAdd = new LinkedList<IndexDefinition>();
		int count = this.reference.size();
		
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		
		StrBuffer idxIndent = new StrBuffer(myindent);
		idxIndent.append("  ");
		
		for (IndexDefinition refIndex : reference)
		{
			IndexDefinition ind = this.findIndex(refIndex.getExpression());
			if (ind == null)
			{
				indexToAdd.add(refIndex);
			}
			else
			{
				boolean uniqueDiff = ind.isUnique() != refIndex.isUnique();
				boolean pkDiff = ind.isPrimaryKeyIndex() != refIndex.isPrimaryKeyIndex();
				boolean typeDiff = !(ind.getIndexType().equals(refIndex.getIndexType()));
				
				if (uniqueDiff || pkDiff || typeDiff)
				{
					writer.appendOpenTag(result, myindent, TAG_MODIFY_INDEX, "name", ind.getName());
					result.append('\n');
					if (uniqueDiff)
					{
						writer.appendTag(result, idxIndent, IndexReporter.TAG_INDEX_UNIQUE, refIndex.isUnique());
					}
					if (pkDiff)
					{
						writer.appendTag(result, idxIndent, IndexReporter.TAG_INDEX_PK, refIndex.isPrimaryKeyIndex());
					}	
					if (pkDiff)
					{
						writer.appendTag(result, idxIndent, IndexReporter.TAG_INDEX_TYPE, refIndex.getIndexType());
					}	
					writer.appendCloseTag(result, myindent, TAG_MODIFY_INDEX);
				}
			}
		}
		
		if (indexToAdd.size() > 0)
		{
			writer.appendOpenTag(result, myindent, TAG_ADD_INDEX);
			result.append('\n');
			for (IndexDefinition idx : indexToAdd)
			{
				IndexReporter rep = new IndexReporter(idx);
				rep.appendXml(result, idxIndent);
			}
			writer.appendCloseTag(result, myindent, TAG_ADD_INDEX);
		}
		return result;
	}
	
	private IndexDefinition findIndex(String expr)
	{
		for (IndexDefinition idx : target)
		{
			if (idx.equals(expr)) return idx;
		}
		return null;
	}
}
