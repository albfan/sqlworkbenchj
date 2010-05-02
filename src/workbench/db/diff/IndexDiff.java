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
import workbench.db.report.TagAttribute;
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
	public static final String TAG_RENAME_INDEX = "rename-index";
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
			IndexDefinition ind = this.findIndexInTarget(refIndex);
			if (ind == null)
			{
				indexToAdd.add(refIndex);
			}
			else
			{
				boolean uniqueDiff = ind.isUnique() != refIndex.isUnique();
				boolean pkDiff = ind.isPrimaryKeyIndex() != refIndex.isPrimaryKeyIndex();
				boolean typeDiff = !(ind.getIndexType().equals(refIndex.getIndexType()));
				boolean nameDiff = !(ind.getName().equalsIgnoreCase(refIndex.getName()));

				if (uniqueDiff || pkDiff || typeDiff || nameDiff)
				{
					writer.appendOpenTag(result, myindent, TAG_MODIFY_INDEX, "name", ind.getName());
					result.append('\n');

					// In order to completely create the correct SQL for the index change,
					// the full definition of the reference index needs to be included in the XML
					IndexReporter rep = new IndexReporter(refIndex);
					rep.setMainTagToUse("reference-index");
					rep.appendXml(result, idxIndent);

					StrBuffer changedIndent = new StrBuffer(idxIndent);
					changedIndent.append("  ");
					writer.appendOpenTag(result, idxIndent, "modified");
					result.append('\n');

					if (nameDiff)
					{
						TagAttribute oldAtt = new TagAttribute("oldvalue", ind.getName());
						TagAttribute newAtt = new TagAttribute("newvalue", refIndex.getName());
						writer.appendOpenTag(result, changedIndent, IndexReporter.TAG_INDEX_NAME, false, oldAtt, newAtt);
						result.append("/>\n");
					}

					if (uniqueDiff)
					{
						TagAttribute oldAtt = new TagAttribute("oldvalue", Boolean.toString(ind.isUnique()));
						TagAttribute newAtt = new TagAttribute("newvalue", Boolean.toString(refIndex.isUnique()));
						writer.appendOpenTag(result, changedIndent, IndexReporter.TAG_INDEX_UNIQUE, false, oldAtt, newAtt);
						result.append("/>\n");
					}
					if (pkDiff)
					{
						TagAttribute oldAtt = new TagAttribute("oldvalue", Boolean.toString(ind.isPrimaryKeyIndex()));
						TagAttribute newAtt = new TagAttribute("newvalue", Boolean.toString(refIndex.isPrimaryKeyIndex()));
						writer.appendOpenTag(result, changedIndent, IndexReporter.TAG_INDEX_PK, false, oldAtt, newAtt);
						result.append("/>\n");
					}
					if (typeDiff)
					{
						TagAttribute oldAtt = new TagAttribute("oldvalue", ind.getIndexType());
						TagAttribute newAtt = new TagAttribute("newvalue", refIndex.getIndexType());
						writer.appendOpenTag(result, changedIndent, IndexReporter.TAG_INDEX_TYPE, false, oldAtt, newAtt);
						result.append("/>\n");
					}
					
					writer.appendCloseTag(result, idxIndent, "modified");
					writer.appendCloseTag(result, myindent, TAG_MODIFY_INDEX);
				}
			}
		}

		for (IndexDefinition targetIndex : target)
		{
			IndexDefinition ind = this.findIndexInReference(targetIndex);
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

	private IndexDefinition findIndexInTarget(IndexDefinition toCheck)
	{
		return findIndex(target, toCheck);
	}

	private IndexDefinition findIndexInReference(IndexDefinition toCheck)
	{
		return findIndex(reference, toCheck);
	}

	private IndexDefinition findIndex(Collection<IndexDefinition> defs, IndexDefinition toCheck)
	{
		for (IndexDefinition idx : defs)
		{
			if (idx.equals(toCheck)) return idx;
		}
		return null;
	}
}
