/*
 * SequenceDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import workbench.db.report.ReportSequence;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

/**
 * Compares two database sequences for differences in their definition. 
 * 
 * @author Thomas Kellerer
 */
public class SequenceDiff
{
	public static final String TAG_CREATE_SEQUENCE = "create-sequence";
	public static final String TAG_UPDATE_SEQUENCE = "update-sequence";
	
	private ReportSequence reference;
	private ReportSequence target;
	private TagWriter writer;
	private StrBuffer indent;
	
	public SequenceDiff(ReportSequence ref, ReportSequence tar)
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
		boolean createSequence = (target == null);
		boolean different = (target == null || !reference.getSequence().equals(target.getSequence()));
		if (!different) return result;
		
		writer.appendOpenTag(result, this.indent, (createSequence ? TAG_CREATE_SEQUENCE : TAG_UPDATE_SEQUENCE));
		result.append('\n');
		if (different)
		{
			result.append(reference.getXml(myindent));
		}
		writer.appendCloseTag(result, this.indent, (createSequence ? TAG_CREATE_SEQUENCE : TAG_UPDATE_SEQUENCE));

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
