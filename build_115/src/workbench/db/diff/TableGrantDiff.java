/*
 * TableGrantDiff.java
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

import java.util.Collection;
import java.util.LinkedList;
import workbench.db.TableGrant;
import workbench.db.report.ReportTableGrants;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

/**
 * @author Thomas Kellerer
 */
public class TableGrantDiff
{
	public static final String TAG_ADD_GRANTS = "add-grants";
	public static final String TAG_REVOKE_GRANTS = "revoke-grants";

	private Collection<TableGrant> referenceGrants;
	private Collection<TableGrant> targetGrants;

	public TableGrantDiff(ReportTableGrants reference, ReportTableGrants target)
	{
		if (reference != null)
		{
			this.referenceGrants = reference.getGrants();
		}

		if (target != null)
		{
			this.targetGrants = target.getGrants();
		}
	}

	public StrBuffer getMigrateTargetXml(TagWriter writer, StrBuffer indent)
	{
		Collection<TableGrant> grantsToAdd = new LinkedList<TableGrant>();
		if (this.referenceGrants != null)
		{
			grantsToAdd.addAll(this.referenceGrants);
		}
		if (this.targetGrants != null)
		{
			grantsToAdd.removeAll(targetGrants);
		}

		Collection<TableGrant> grantsToRemove = new LinkedList<TableGrant>();
		if (this.targetGrants != null)
		{
			grantsToRemove.addAll(targetGrants);
		}
		if (this.referenceGrants != null)
		{
			grantsToRemove.removeAll(referenceGrants);
		}

		if (grantsToAdd.isEmpty() && grantsToRemove.isEmpty()) return null;

		StrBuffer result = new StrBuffer(grantsToAdd.size() * 50 + grantsToRemove.size() * 50);
		StrBuffer indent2 = new StrBuffer(indent);
		indent2.append("  ");
		StrBuffer indent3 = new StrBuffer(indent2);
		indent3.append("  ");
		if (grantsToAdd.size() > 0)
		{
			ReportTableGrants report = new ReportTableGrants(grantsToAdd);
			writer.appendOpenTag(result, indent2, TAG_ADD_GRANTS);
			result.append('\n');
			report.appendXml(result, indent3);
			writer.appendCloseTag(result, indent2, TAG_ADD_GRANTS);
			result.append('\n');
		}

		if (grantsToRemove.size() > 0)
		{
			ReportTableGrants report = new ReportTableGrants(grantsToRemove);
			writer.appendOpenTag(result, indent2, TAG_REVOKE_GRANTS);
			result.append('\n');
			report.appendXml(result, indent3);
			writer.appendCloseTag(result, indent2, TAG_REVOKE_GRANTS);
			result.append('\n');
		}
		return result;
	}

}
