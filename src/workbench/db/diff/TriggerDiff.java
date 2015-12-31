/*
 * TriggerDiff.java
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

import workbench.db.TriggerDefinition;
import workbench.db.report.ReportTrigger;
import workbench.db.report.TagWriter;

import workbench.util.StringUtil;

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

	public StringBuilder getMigrateTargetXml(StringBuilder indent)
	{
		boolean isDifferent = isDifferent();
		if (!isDifferent) return StringUtil.emptyBuilder();

		TriggerDefinition trgTarget = (target != null ? target.getTrigger() : null);
		boolean isNew = trgTarget == null;

		String tagToUse = (isNew ? TAG_CREATE_TRIGGER : TAG_UPDATE_TRIGGER);

		StringBuilder myIndent = new StringBuilder(indent);
		myIndent.append("  ");
		TagWriter writer = new TagWriter();
		StringBuilder result = new StringBuilder();
		writer.appendOpenTag(result, indent, tagToUse);
		result.append('\n');
		reference.setIndent(myIndent);
		result.append(reference.getXml());
		writer.appendCloseTag(result, indent, tagToUse);

		return result;
	}


}
