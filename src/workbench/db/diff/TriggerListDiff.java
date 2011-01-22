/*
 * TriggerListDiff.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.util.List;
import workbench.db.TriggerDefinition;
import workbench.db.report.ReportTrigger;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

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

	public void writeXml(StrBuffer indent, StrBuffer buffer)
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
