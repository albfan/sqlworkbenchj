/*
 * ViewDiff.java
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
package workbench.db.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import workbench.db.IndexDefinition;
import workbench.db.report.ReportTableGrants;
import workbench.db.report.ReportView;
import workbench.db.report.TagAttribute;
import workbench.db.report.TagWriter;

import workbench.util.StringUtil;

/**
 * Compares two database views for differences in their definition.
 * The generating source of the views is compared using String.equals(),
 * so any difference in Upper/Lowercase writing (even if not important
 * for the functionality of the view) qualify the two views as
 * beeing different.
 *
 * @author Thomas Kellerer
 */
public class ViewDiff
{
  public static final String TAG_CREATE_VIEW = "create-view";
  public static final String TAG_UPDATE_VIEW = "update-view";

  private ReportView reference;
  private ReportView target;
  private final TagWriter writer = new TagWriter();
  private StringBuilder indent = StringUtil.emptyBuilder();

  public ViewDiff(ReportView ref, ReportView tar)
  {
    reference = ref;
    target = tar;
  }

  public StringBuilder getMigrateTargetXml()
  {
    StringBuilder result = new StringBuilder(500);

    StringBuilder myindent = new StringBuilder(indent);
    myindent.append("  ");
    boolean sourceDifferent = false;
    boolean indexDifferent = false;
    boolean grantDifferent = false;
    boolean createView = (target == null);

    CharSequence s = null;

    s = reference.getViewSource();
    String refSource = (s == null ? null : s.toString());
    s = (target == null ? null : target.getViewSource());
    String targetSource = (s == null ? null : s.toString());

    if (targetSource != null)
    {
      sourceDifferent = !refSource.trim().equals(targetSource.trim());
    }

    StringBuilder indexDiff = getIndexDiff();
    StringBuilder grants = getGrantDiff();
    if (indexDiff != null && indexDiff.length() > 0)
    {
      indexDifferent = true;
    }

    if (grants != null && grants.length() > 0)
    {
      grantDifferent = true;
    }

    if (!sourceDifferent && !createView && !indexDifferent && !grantDifferent) return result;

    List<TagAttribute> att = new ArrayList<>();

    String type = reference.getView().getType();
    if (!"VIEW".equals(type))
    {
      att.add(new TagAttribute("type", type));
    }

    if (indexDifferent && !sourceDifferent)
    {
      att.add(new TagAttribute("name", target.getView().getTableName()));
    }

    writer.appendOpenTag(result, this.indent, (createView ? TAG_CREATE_VIEW : TAG_UPDATE_VIEW), att, true);

    result.append('\n');
    if (createView)
    {
      result.append(reference.getXml(myindent, true));
    }
    else if (sourceDifferent)
    {
      String schema = reference.getView().getSchema();
      String cat = reference.getView().getCatalog();
      reference.getView().setSchema(target.getView().getSchema());
      reference.getView().setCatalog(target.getView().getCatalog());
      result.append(reference.getXml(myindent, indexDifferent));
      reference.getView().setSchema(schema);
      reference.getView().setCatalog(cat);
    }
    else
    {
      result.append(indexDiff);
    }

    if (grantDifferent)
    {
      result.append(grants);
    }

    writer.appendCloseTag(result, this.indent, (createView ? TAG_CREATE_VIEW : TAG_UPDATE_VIEW));

    return result;
  }

  private StringBuilder getGrantDiff()
  {
    ReportTableGrants refGrants = this.reference.getGrants();
    ReportTableGrants targetGrants = target == null ? null : this.target.getGrants();
    if (refGrants == null && targetGrants == null) return null;

    TableGrantDiff td = new TableGrantDiff(refGrants, targetGrants);
    StringBuilder diffXml = td.getMigrateTargetXml(writer, indent);
    return diffXml;
  }

  private StringBuilder getIndexDiff()
  {
    if (this.target == null) return null;

    Collection<IndexDefinition> ref = this.reference.getIndexList();
    Collection<IndexDefinition> targ = this.target.getIndexList();
    if (ref == null && targ == null) return null;
    IndexDiff id = new IndexDiff(ref, targ);
    id.setIndent(indent);
    StringBuilder diff = id.getMigrateTargetXml();
    return diff;
  }

  /**
   *  Set an indent for generating the XML
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
