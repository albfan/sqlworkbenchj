/*
 * ReportSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;

import workbench.util.StringUtil;
import workbench.util.WbDateFormatter;

/**
 *
 * @author Thomas Kellerer
 */
public class ReportSequence
{
	public static final String TAG_SEQ_DEF = "sequence-def";
	public static final String TAG_SEQ_NAME = "sequence-name";
	public static final String TAG_SEQ_CATALOG = "sequence-catalog";
	public static final String TAG_SEQ_SCHEMA = "sequence-schema";
	public static final String TAG_SEQ_COMMENT = "sequence-comment";
	public static final String TAG_SEQ_SOURCE = "sequence-source";
	public static final String TAG_SEQ_PROPS = "sequence-properties";
	public static final String TAG_SEQ_OWNING_TABLE = "owned-by-table";
	public static final String TAG_SEQ_OWNING_COLUMN = "owned-by-column";
	public static final String TAG_SEQ_PROPERTY = "property";

	private SequenceDefinition sequence;
	private TagWriter tagWriter = new TagWriter();
	private String schemaNameToUse = null;

	public ReportSequence(SequenceDefinition def)
	{
		this.sequence = def;
	}

	public SequenceDefinition getSequence()
	{
		return this.sequence;
	}

	public void writeXml(Writer out)
		throws IOException
	{
		StringBuilder line = this.getXml();
		out.append(line);
	}

	public void setSchemaNameToUse(String schema)
	{
		this.schemaNameToUse = schema;
	}

	public StringBuilder getXml()
	{
		return getXml(new StringBuilder("  "), true);
	}
	/**
	 * Return an XML representation of this view information.
	 * The columns will be listed alphabetically not in the order
	 * they were retrieved from the database.
	 */
	public StringBuilder getXml(StringBuilder indent, boolean includeSource)
	{
		StringBuilder line = new StringBuilder(500);
		StringBuilder colindent = new StringBuilder(indent);
		colindent.append(indent);

		tagWriter.appendOpenTag(line, indent, TAG_SEQ_DEF, "name", this.sequence.getSequenceName());
		line.append('\n');
		if (StringUtil.isNonEmpty(sequence.getCatalog()))
		{
			tagWriter.appendTag(line, colindent, TAG_SEQ_CATALOG, this.sequence.getCatalog());
		}
		if (sequence.getRelatedTable() != null && sequence.getRelatedColumn() != null)
		{
			tagWriter.appendTag(line, colindent, TAG_SEQ_OWNING_TABLE, sequence.getRelatedTable().getTableName());
			tagWriter.appendTag(line, colindent, TAG_SEQ_OWNING_COLUMN, sequence.getRelatedColumn());
		}
		tagWriter.appendTag(line, colindent, TAG_SEQ_SCHEMA, (this.schemaNameToUse == null ? this.sequence.getSequenceOwner() : this.schemaNameToUse));
		tagWriter.appendTag(line, colindent, TAG_SEQ_NAME, this.sequence.getSequenceName());
		tagWriter.appendTagConditionally(line, colindent, TAG_SEQ_COMMENT, sequence.getComment());

		writeSequenceProperties(line, colindent);
		if (includeSource)
		{
			writeSourceTag(tagWriter, line, colindent, sequence.getSource());
		}
		tagWriter.appendCloseTag(line, indent, TAG_SEQ_DEF);
		return line;
	}

	public void writeSourceTag(TagWriter tagWriter, StringBuilder target, StringBuilder indent, CharSequence source)
	{
		if (source == null) return;
		tagWriter.appendOpenTag(target, indent, TAG_SEQ_SOURCE);
		target.append(TagWriter.CDATA_START);
		target.append(StringUtil.rtrim(source));
		target.append(TagWriter.CDATA_END);
		target.append('\n');
		tagWriter.appendCloseTag(target, indent, TAG_SEQ_SOURCE);
	}

	protected void writeSequenceProperties(StringBuilder toAppend, StringBuilder indent)
	{
		if (sequence.getProperties().isEmpty()) return;

		StringBuilder myindent = new StringBuilder(indent);
		myindent.append("  ");
		StringBuilder propindent = new StringBuilder(myindent);
		propindent.append("  ");

		tagWriter.appendOpenTag(toAppend, indent, TAG_SEQ_PROPS);
		toAppend.append('\n');

		for (String propName : sequence.getProperties())
		{
			if ("remarks".equalsIgnoreCase("propName")) continue;

			Object value = this.sequence.getSequenceProperty(propName);
			TagAttribute name = new TagAttribute("name", propName);
			TagAttribute val = new TagAttribute("value", (value == null ? "" : WbDateFormatter.getDisplayValue(value)));
			tagWriter.appendOpenTag(toAppend, myindent, TAG_SEQ_PROPERTY, false, name, val);
			toAppend.append("/>\n");
		}

		if (sequence.getRelatedTable() != null && sequence.getRelatedColumn() != null)
		{
			String colref = sequence.getRelatedTable().getTableName() + "." + sequence.getRelatedColumn();
			TagAttribute name = new TagAttribute("name", SequenceReader.PROP_OWNED_BY.toUpperCase());
			TagAttribute val = new TagAttribute("value", colref);
			tagWriter.appendOpenTag(toAppend, myindent, TAG_SEQ_PROPERTY, false, name, val);
			toAppend.append("/>\n");
		}

		tagWriter.appendCloseTag(toAppend, indent, TAG_SEQ_PROPS);
	}

}
