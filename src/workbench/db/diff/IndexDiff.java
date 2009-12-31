/*
 * IndexDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.util.Collection;
import java.util.Collections;
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
 * @author  Thomas Kellerer
 */
public class IndexDiff
{
	public static final String TAG_MODIFY_INDEX = "modify-index";
	public static final String TAG_ADD_INDEX = "add-index";
	public static final String TAG_DROP_INDEX = "drop-index";

	private Collection<IndexDefinition> reference = Collections.emptyList();
	private Collection<IndexDefinition> target = Collections.emptyList();
	private TagWriter writer;
	private StrBuffer indent;

	public IndexDiff(Collection<IndexDefinition> ref, Collection<IndexDefinition> targ)
	{
		if (ref != null) this.reference = ref;
		if (targ != null) this.target = targ;
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
		List<IndexDefinition> indexToDrop = new LinkedList<IndexDefinition>();

		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");

		StrBuffer idxIndent = new StrBuffer(myindent);
		idxIndent.append("  ");

		for (IndexDefinition refIndex : reference)
		{
			IndexDefinition ind = this.findIndexInTarget(refIndex.getExpression());
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

		for (IndexDefinition targetIndex : target)
		{
			String expr = targetIndex.getExpression();
			IndexDefinition ind = this.findIndexInReference(expr);
			if (ind == null)
			{
				indexToDrop.add(targetIndex);
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

		if (indexToDrop.size() > 0)
		{
			for (IndexDefinition idx : indexToDrop)
			{
				writer.appendTag(result, myindent, TAG_DROP_INDEX, idx.getName());
			}
		}
		return result;
	}

	private IndexDefinition findIndexInTarget(String expr)
	{
		return findIndex(target, expr);
	}

	private IndexDefinition findIndexInReference(String expr)
	{
		return findIndex(reference, expr);
	}

	private IndexDefinition findIndex(Collection<IndexDefinition> defs, String expr)
	{
		for (IndexDefinition idx : defs)
		{
			if (idx.equals(expr)) return idx;
		}
		return null;
	}
}
