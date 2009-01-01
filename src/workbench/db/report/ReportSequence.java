/*
 * ReportSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import workbench.db.SequenceDefinition;
import workbench.util.StrBuffer;

/**
 *
 * @author support@sql-workbench.net
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
	public static final String TAG_SEQ_PROPERTY = "property";
	public static final String TAG_SEQ_PROP_NAME = "name";
	public static final String TAG_SEQ_PROP_VALUE = "value";
	
	private SequenceDefinition sequence;
	private TagWriter tagWriter = new TagWriter();
	private String schemaNameToUse = null;
	
	public ReportSequence(SequenceDefinition def, String nspace)
	{
		this.sequence = def;
		this.tagWriter.setNamespace(nspace);
	}

	public SequenceDefinition getSequence()
	{
		return this.sequence;
	}
	
	public void writeXml(Writer out)
		throws IOException
	{
		StrBuffer line = this.getXml();
		line.writeTo(out);
	}	
	
	public void setSchemaNameToUse(String schema)
	{
		this.schemaNameToUse = schema;
	}
	
	public StrBuffer getXml()
	{
		return getXml(new StrBuffer("  "));
	}	
	/**
	 * Return an XML representation of this view information.
	 * The columns will be listed alphabetically not in the order
	 * they were retrieved from the database.
	 */
	public StrBuffer getXml(StrBuffer indent)
	{
		StrBuffer line = new StrBuffer(500);
		StrBuffer colindent = new StrBuffer(indent);
		colindent.append(indent);

		tagWriter.appendOpenTag(line, indent, TAG_SEQ_DEF, "name", this.sequence.getSequenceName());
		line.append('\n');
		tagWriter.appendTag(line, colindent, TAG_SEQ_SCHEMA, (this.schemaNameToUse == null ? this.sequence.getSequenceOwner() : this.schemaNameToUse));
		tagWriter.appendTag(line, colindent, TAG_SEQ_NAME, this.sequence.getSequenceName());

		writeSequenceProperties(line, colindent);
		writeSourceTag(tagWriter, line, colindent, sequence.getSource());
		
		tagWriter.appendCloseTag(line, indent, TAG_SEQ_DEF);
		return line;
	}

	public void writeSourceTag(TagWriter tagWriter, StrBuffer target, StrBuffer indent, CharSequence source)
	{
		if (source == null) return;
		tagWriter.appendOpenTag(target, indent, TAG_SEQ_SOURCE);
		target.append(TagWriter.CDATA_START);
		target.append(source);
		target.append(TagWriter.CDATA_END);
		target.append('\n');
		tagWriter.appendCloseTag(target, indent, TAG_SEQ_SOURCE);
	}	

	protected void writeSequenceProperties(StrBuffer toAppend, StrBuffer indent)
	{
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		StrBuffer propindent = new StrBuffer(myindent);
		propindent.append("  ");
		
		tagWriter.appendOpenTag(toAppend, indent, TAG_SEQ_PROPS);
		toAppend.append('\n');
		Iterator<String> itr = this.sequence.getProperties();
		while (itr.hasNext())
		{
			String propName = itr.next();
			Object value = this.sequence.getSequenceProperty(propName);
			TagAttribute name = new TagAttribute("name", propName);
			TagAttribute v = new TagAttribute("value", (value == null ? "" : value.toString()));
			tagWriter.appendOpenTag(toAppend, myindent, TAG_SEQ_PROPERTY, false, name, v);
			toAppend.append("/>\n");
		}
		tagWriter.appendCloseTag(toAppend, indent, TAG_SEQ_PROPS);
	}
	
}
