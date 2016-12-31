/*
 * GenericReportObject.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
import java.util.List;

import workbench.db.BaseObjectType;
import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;
import workbench.db.DomainIdentifier;
import workbench.db.EnumIdentifier;
import workbench.db.TableConstraint;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
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

  public static final String TAG_TYPE_ATTRS = "attributes";

  private DbObject object;
  private String source;
  private String schemaNameToUse;

  public GenericReportObject(WbConnection con, DbObject dbo)
  {
    object = dbo;
    this.source = con == null ? null : con.getMetadata().getObjectSource(dbo);
  }

  public void setSchemaNameToUse(String name)
  {
    this.schemaNameToUse = name;
  }

  public void writeXml(Writer out)
    throws IOException
  {
    StringBuilder line = this.getXml();
    out.append(line);
  }

  public StringBuilder getXml()
  {
    return getXml(new StringBuilder("  "));
  }

  public StringBuilder getXml(StringBuilder indent)
  {
    StringBuilder line = new StringBuilder(500);
    StringBuilder defIndent = new StringBuilder(indent);
    defIndent.append("  ");

    String[] att = new String[2];
    String[] val = new String[2];

    TagWriter tagWriter = new TagWriter();

    att[0] = "name";
    val[0] = SqlUtil.removeObjectQuotes(this.object.getObjectName());
    att[1] = "object-type";
    val[1] = object.getObjectType();

    tagWriter.appendOpenTag(line, indent, TAG_OBJECT_DEF, att, val);

    line.append('\n');
    tagWriter.appendTag(line, defIndent, TAG_OBJECT_CATALOG, SqlUtil.removeObjectQuotes(this.object.getCatalog()));
    tagWriter.appendTag(line, defIndent, TAG_OBJECT_SCHEMA, (this.schemaNameToUse == null ? SqlUtil.removeObjectQuotes(this.object.getSchema()) : this.schemaNameToUse));
    tagWriter.appendTag(line, defIndent, TAG_OBJECT_NAME, SqlUtil.removeObjectQuotes(object.getObjectName()));
    tagWriter.appendTag(line, defIndent, TAG_OBJECT_TYPE, SqlUtil.removeObjectQuotes(object.getObjectType()));
    tagWriter.appendTag(line, defIndent, TAG_OBJECT_COMMENT, object.getComment(), true);

    StringBuilder details = new StringBuilder(defIndent);
    details.append("  ");
    StringBuilder in2 = new StringBuilder(details);
    in2.append("  ");
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
      for (String value : enumDef.getValues())
      {
        tagWriter.appendTag(line, in2, TAG_ENUM_VALUE, value);
      }
      tagWriter.appendCloseTag(line, details, TAG_ENUM_VALUES);
      tagWriter.appendCloseTag(line, defIndent, TAG_OBJECT_DETAILS);
    }
    else if (object instanceof BaseObjectType)
    {
      BaseObjectType obj = (BaseObjectType)object;
      List<ColumnIdentifier> cols = obj.getAttributes();
      if (CollectionUtil.isNonEmpty(cols))
      {
        tagWriter.appendOpenTag(line, defIndent, TAG_OBJECT_DETAILS);
        line.append('\n');
        tagWriter.appendOpenTag(line, details, TAG_TYPE_ATTRS);
        line.append('\n');
        for (ColumnIdentifier col : cols)
        {
          ReportColumn rcol = new ReportColumn(col);
          rcol.appendXml(line, in2, true);
        }
        tagWriter.appendCloseTag(line, details, TAG_TYPE_ATTRS);
        tagWriter.appendCloseTag(line, defIndent, TAG_OBJECT_DETAILS);
      }
    }
    if (StringUtil.isNonEmpty(source))
    {
      tagWriter.appendTag(line, defIndent, TAG_OBJECT_SOURCE, source, true);
    }
    tagWriter.appendCloseTag(line, indent, TAG_OBJECT_DEF);
    return line;
  }

}
