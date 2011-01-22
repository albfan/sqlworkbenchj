/*
 * TriggerDiff.java
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

import workbench.db.TriggerDefinition;
import workbench.db.report.ReportTrigger;
import workbench.db.report.TagWriter;
import workbench.util.StrBuffer;

/**
 *
 * @author Thomas Kellerer
 */
public class TriggerDiff
{
	public static final String TAG_CREATE_TRIGGER = "create-trigger";
	public static final String TAG_UPDATE_TRIGGER = "update-trigger";
	
	private ReportTrigger reference;
	private ReportTrigger target;
	
	public TriggerDiff(ReportTrigger ref, ReportTrigger tar)
	{
		reference = ref;
		target = tar;
	}

	public boolean isDifferent()
	{
		TriggerDefinition trgRef = reference.getTrigger();
		TriggerDefinition trgTarget = (target != null ? target.getTrigger() : null);

		boolean isDifferent = false;
		boolean isNew = trgTarget == null;

		if (isNew)
		{
			return true;
		}
		CharSequence refSource = trgRef.getSource();
		CharSequence targetSource = trgTarget.getSource();

		isDifferent = !(refSource != null ? refSource.equals(targetSource) : false);
		isDifferent = isDifferent || !trgRef.getTriggerEvent().equals(trgTarget.getTriggerEvent());
		isDifferent = isDifferent || !trgRef.getTriggerType().equals(trgTarget.getTriggerType());

		return isDifferent;
	}
	
	public StrBuffer getMigrateTargetXml(StrBuffer indent)
	{
		StrBuffer result = null;
		TagWriter writer = new TagWriter();
		
		boolean isDifferent = isDifferent();
		if (!isDifferent) return null;

		TriggerDefinition trgTarget = (target != null ? target.getTrigger() : null);
		boolean isNew = trgTarget == null;

		String tagToUse = (isNew ? TAG_CREATE_TRIGGER : TAG_UPDATE_TRIGGER);
		
		StrBuffer myIndent = new StrBuffer(indent);
		myIndent.append("  ");
		result = new StrBuffer();
		writer.appendOpenTag(result, indent, tagToUse);
		result.append('\n');
		reference.setIndent(myIndent);
		result.append(reference.getXml());
		writer.appendCloseTag(result, indent, tagToUse);
		
		return result;
	}	

		
}
