/*
 * ReportTrigger.java
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
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.TriggerDefinition;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ReportTrigger
{
	public final String TAG_TRIGGER_DEF = "trigger-def";
	public final String TAG_TRIGGER_NAME = "trigger-name";
	public final String TAG_TRIGGER_EVENT = "trigger-event";
	public final String TAG_TRIGGER_TYPE = "trigger-type";
	public final String TAG_TRIGGER_SOURCE = "trigger-source";

	private TriggerDefinition trigger;
	private TagWriter tagWriter = new TagWriter();
	private StringBuilder indent = new StringBuilder("  ");
	private StringBuilder indent2 = new StringBuilder("    ");

	private final Pattern PG_SOURCE = Pattern.compile("^---< Source for \\w+ >---$", Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);

	public ReportTrigger(TriggerDefinition def)
	{
		this.trigger = def;
	}

	public void writeXml(Writer out)
		throws IOException
	{
		StringBuilder xml = getXml();
		out.append(xml);
	}

	public TriggerDefinition getTrigger()
	{
		return trigger;
	}

	public void setIndent(StringBuilder ind)
	{
		this.indent = ind == null ? new StringBuilder(0) : ind;
		this.indent2 = new StringBuilder(indent);
		this.indent2.append("  ");
	}

	/**
	 * Return the source of the trigger.
	 * The source is "cleaned" from some additional information that is
	 * displayed in the UI but is not necessary for a Schema report or a diff
	 *
	 * @return the defined source of the trigger.
	 */
	public CharSequence getSource()
	{
		CharSequence src = trigger.getSource();
		if (StringUtil.isEmptyString(src)) return src;

		// When retrieving the source of a Postgres trigger, the
		// source code for the underlying function is also appended.
		// See TriggerSourceStatements.xml

		// For a schema report or diff this is not needed as the function will
		// also be part of the output
		Matcher m = PG_SOURCE.matcher(src);
		if (m.find())
		{
			return src.subSequence(0, m.start()).toString().trim();
		}
		return src;
	}

	public StringBuilder getXml()
	{
		StringBuilder result = new StringBuilder(500);
		tagWriter.appendOpenTag(result, indent, TAG_TRIGGER_DEF);
		result.append('\n');
		tagWriter.appendTag(result, indent2, TAG_TRIGGER_NAME, trigger.getObjectName());
		tagWriter.appendTag(result, indent2, TAG_TRIGGER_TYPE, trigger.getTriggerType());
		tagWriter.appendTag(result, indent2, TAG_TRIGGER_EVENT, trigger.getTriggerEvent());
		tagWriter.appendTag(result, indent2, TAG_TRIGGER_SOURCE, getSource(), true);
		tagWriter.appendCloseTag(result, indent, TAG_TRIGGER_DEF);
		return result;
	}
}
