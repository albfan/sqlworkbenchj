/*
 * GenericReportObject.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.EnumIdentifier;
import workbench.db.TableConstraint;
import workbench.db.WbConnection;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class GenericReportObject
{
	public static final String TAG_OBJECT_DEF = "object-def";
	public static final String TAG_OBJECT_NAME = "object-name";
	public static final String TAG_OBJECT_CATALOG = "object-catalog";
	public static final String TAG_OBJECT_SCHEMA = "object-schema";
	public static final String TAG_OBJECT_COMMENT = "object-comment";
	public static final String TAG_OBJECT_TYPE = "object-type";
	public static final String TAG_OBJECT_SOURCE = "object-source";
	public static final String TAG_OBJECT_DETAILS = "object-details";

	public static final String TAG_DOMAIN_TYPE = "domain-datatype";
	public static final String TAG_DOMAIN_NULLABLE = "domain-nullable";
	public static final String TAG_DOMAIN_CONSTRAINT = "domain-constraint";
	public static final String TAG_DOMAIN_DEFVALUE = "domain-defaultvalue";

	public static final String TAG_ENUM_VALUES = "enum-values";
	public static final String TAG_ENUM_VALUE = "enum-value";

	private DbObject object;
	private String source;
	private String schemaNameToUse;

	public GenericReportObject(WbConnection con, DbObject dbo)
	{
		object = dbo;
		this.source = con.getMetadata().getObjectSource(dbo);
	}

	public void setSchemaNameToUse(String name)
	{
		this.schemaNameToUse = name;
	}

	public void writeXml(Writer out)
		throws IOException
	{
		StrBuffer line = this.getXml();
		line.writeTo(out);
	}

	public StrBuffer getXml()
	{
		return getXml(new StrBuffer("  "));
	}

	public StrBuffer getXml(StrBuffer indent)
	{
		StrBuffer line = new StrBuffer(500);
		StrBuffer defIndent = new StrBuffer(indent);
		defIndent.append("  ");

		String[] att = new String[2];
		String[] val = new String[2];

		TagWriter tagWriter = new TagWriter();

		att[0] = "name";
		val[0] = StringUtil.trimQuotes(this.object.getObjectName());
		att[1] = "object-type";
		val[1] = object.getObjectType();

		tagWriter.appendOpenTag(line, indent, TAG_OBJECT_DEF, att, val);

		line.append('\n');
		tagWriter.appendTag(line, defIndent, TAG_OBJECT_CATALOG, StringUtil.trimQuotes(this.object.getCatalog()));
		tagWriter.appendTag(line, defIndent, TAG_OBJECT_SCHEMA, (this.schemaNameToUse == null ? StringUtil.trimQuotes(this.object.getSchema()) : this.schemaNameToUse));
		tagWriter.appendTag(line, defIndent, TAG_OBJECT_NAME, StringUtil.trimQuotes(object.getObjectName()));
		tagWriter.appendTag(line, defIndent, TAG_OBJECT_TYPE, StringUtil.trimQuotes(object.getObjectType()));
		tagWriter.appendTag(line, defIndent, TAG_OBJECT_COMMENT, object.getComment(), true);

		StrBuffer details = new StrBuffer(defIndent);
		details.append("  ");
		if (object instanceof DomainIdentifier)
		{
			tagWriter.appendOpenTag(line, defIndent, TAG_OBJECT_DETAILS);
			line.append('\n');
			DomainIdentifier domain = (DomainIdentifier)object;
			if (StringUtil.isNonBlank(domain.getCheckConstraint()))
			{
				tagWriter.appendOpenTag(line, details, TAG_DOMAIN_CONSTRAINT);
				line.append('\n');
				TableConstraint con = new TableConstraint(domain.getConstraintName(), domain.getCheckConstraint());
				StrBuffer in2 = new StrBuffer(details);
				in2.append("  ");
				ReportTable.writeConstraint(con, tagWriter, line, in2);
				tagWriter.appendCloseTag(line, details, TAG_DOMAIN_CONSTRAINT);
			}
			tagWriter.appendTag(line, details, TAG_DOMAIN_TYPE, domain.getDataType());
			tagWriter.appendTag(line, details, TAG_DOMAIN_NULLABLE, domain.isNullable());
			tagWriter.appendTag(line, details, TAG_DOMAIN_DEFVALUE, domain.getDefaultValue());
			tagWriter.appendCloseTag(line, defIndent, TAG_OBJECT_DETAILS);
		}
		else if (object instanceof EnumIdentifier)
		{
			tagWriter.appendOpenTag(line, defIndent, TAG_OBJECT_DETAILS);
			line.append('\n');
			EnumIdentifier enumDef = (EnumIdentifier)object;
			tagWriter.appendOpenTag(line, details, TAG_ENUM_VALUES);
			line.append('\n');
			StrBuffer in2 = new StrBuffer(details);
			in2.append("  ");
			for (String value : enumDef.getValues())
			{
				tagWriter.appendTag(line, in2, TAG_ENUM_VALUE, value);
			}
			tagWriter.appendCloseTag(line, details, TAG_ENUM_VALUES);
			tagWriter.appendCloseTag(line, defIndent, TAG_OBJECT_DETAILS);
		}
		tagWriter.appendTag(line, defIndent, TAG_OBJECT_SOURCE, source, true);
		tagWriter.appendCloseTag(line, indent, TAG_OBJECT_DEF);
		return line;
	}

}
