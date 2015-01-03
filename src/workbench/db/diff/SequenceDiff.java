/*
 * SequenceDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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


import workbench.resource.Settings;

import workbench.db.SequenceDefinition;
import workbench.db.report.ReportSequence;
import workbench.db.report.TagAttribute;
import workbench.db.report.TagWriter;

import workbench.storage.RowData;

import workbench.util.StringUtil;
import workbench.util.WbDateFormatter;

/**
 * Compares two database sequences for differences in their definition.
 *
 * @author Thomas Kellerer
 */
public class SequenceDiff
{
	public static final String TAG_CREATE_SEQUENCE = "create-sequence";
	public static final String TAG_UPDATE_SEQUENCE = "update-sequence";
	public static final String TAG_ATTRIB_LIST = "modify-properties";

	private ReportSequence reference;
	private ReportSequence target;
	private final TagWriter writer = new TagWriter();
	private StringBuilder indent = StringUtil.emptyBuilder();
	private boolean includeSource;
	private String targetSchema;
	public SequenceDiff(ReportSequence ref, ReportSequence tar, String targetSchema)
	{
		reference = ref;
		target = tar;
		this.targetSchema = targetSchema;
		includeSource = Settings.getInstance().getBoolProperty("workbench.diff.sequence.include_sql", false);
	}

	public StringBuilder getMigrateTargetXml()
	{
		StringBuilder result = new StringBuilder(50);

		StringBuilder myindent = new StringBuilder(indent);
		myindent.append("  ");
		boolean createSequence = (target == null);
		boolean different = (target == null || !reference.getSequence().equals(target.getSequence()));
		if (!different) return result;

		writer.appendOpenTag(result, this.indent, (createSequence ? TAG_CREATE_SEQUENCE : TAG_UPDATE_SEQUENCE));
		result.append('\n');
		if (different)
		{
			if (reference != null && target != null)
			{
				writeChangedProperties(myindent, result, reference.getSequence(), target.getSequence());
			}
			if (createSequence)
			{
				reference.setSchemaNameToUse(targetSchema);
			}
			result.append(reference.getXml(myindent, includeSource));
			if (createSequence)
			{
				reference.setSchemaNameToUse(null);
			}
		}
		writer.appendCloseTag(result, this.indent, (createSequence ? TAG_CREATE_SEQUENCE : TAG_UPDATE_SEQUENCE));

		return result;
	}

	private void writeChangedProperties(StringBuilder ind, StringBuilder result, SequenceDefinition refSeq, SequenceDefinition targetSeq)
	{
		if (refSeq == null || targetSeq == null) return;

		StringBuilder myindent = new StringBuilder(ind);
		myindent.append("  ");

		boolean open = false;
		for (String key : refSeq.getProperties())
		{
			Object refValue = refSeq.getSequenceProperty(key);
			Object tValue = targetSeq.getSequenceProperty(key);
			if (!RowData.objectsAreEqual(refValue, tValue))
			{
				if (!open)
				{
					writer.appendOpenTag(result, ind, TAG_ATTRIB_LIST);
					result.append('\n');
					open = true;
				}
				String display = WbDateFormatter.getDisplayValue(refValue);
				TagAttribute name = new TagAttribute("name", key);
				TagAttribute val = new TagAttribute("value", display);
				writer.appendOpenTag(result, myindent, ReportSequence.TAG_SEQ_PROPERTY, false, name, val);
				result.append("/>\n");
			}
		}
		if (open)
		{
			writer.appendCloseTag(result, ind, TAG_ATTRIB_LIST);
		}
	}

	/**
	 *	Set an indent for generating the XML
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
