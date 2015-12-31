/*
 * TriggerListDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.List;

import workbench.db.TriggerDefinition;
import workbench.db.report.ReportTrigger;
import workbench.db.report.TagWriter;

/**
 *
 * @author Thomas Kellerer
 */
public class TriggerListDiff
{
	public static final String TAG_DROP_TRIGGER = "drop-trigger";

	private List<TriggerDefinition> referenceTriggers;
	private List<TriggerDefinition> targetTriggers;

	public TriggerListDiff(List<TriggerDefinition> refTriggers, List<TriggerDefinition> toCompare)
	{
		this.referenceTriggers = refTriggers;
		this.targetTriggers = toCompare;
	}

	public boolean hasChanges()
	{
		for (TriggerDefinition cmp : targetTriggers)
		{
			TriggerDefinition ref = findTrigger(cmp.getObjectName(), referenceTriggers);
			if (ref == null)
			{
				return true;
			}
		}
		for (TriggerDefinition ref : referenceTriggers)
		{
			TriggerDefinition cmp = findTrigger(ref.getObjectName(), targetTriggers);
			ReportTrigger rref = new ReportTrigger(ref);
			ReportTrigger rcmp = (cmp == null ? null : new ReportTrigger(cmp));
			TriggerDiff trgDiff = new TriggerDiff(rref, rcmp);
			if (trgDiff.isDifferent()) return true;
		}
		return false;
	}

	private TriggerDefinition findTrigger(String name, List<TriggerDefinition> toSearch)
	{
		for (TriggerDefinition def : toSearch)
		{
			if (def.getObjectName().equals(name)) return def;
		}
		return null;
	}

	public void writeXml(StringBuilder indent, StringBuilder buffer)
	{
		TagWriter writer = new TagWriter();
		// Check triggers that need to be dropped
		for (TriggerDefinition cmp : targetTriggers)
		{
			TriggerDefinition ref = findTrigger(cmp.getObjectName(), referenceTriggers);
			if (ref == null)
			{
				writer.appendEmptyTag(buffer, indent, TAG_DROP_TRIGGER, "name", cmp.getObjectName());
				buffer.append("\n");
			}
		}

		// Check triggers that need to be added or updated
		for (TriggerDefinition ref : referenceTriggers)
		{
			TriggerDefinition cmp = findTrigger(ref.getObjectName(), targetTriggers);
			ReportTrigger rref = new ReportTrigger(ref);
			ReportTrigger rcmp = (cmp == null ? null : new ReportTrigger(cmp));
			TriggerDiff trgDiff = new TriggerDiff(rref, rcmp);
			buffer.append(trgDiff.getMigrateTargetXml(indent));
		}

	}

}
