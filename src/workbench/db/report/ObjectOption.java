/*
 * ObjectOption.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import workbench.util.StringUtil;

/**
 * A class to store generic options for database objects such as tables or indexes
 * @author Thomas Kellerer
 */
public class ObjectOption
	implements Comparable<ObjectOption>
{
	private String type;
	private String optionSource;
	private boolean flatXML;

	public ObjectOption(String type, String optionSource)
	{
		this.type = type;
		this.optionSource = optionSource;
	}

	public StringBuilder getXml(StringBuilder indent)
	{
		StringBuilder result = new StringBuilder(100);
		StringBuilder myindent = new StringBuilder(indent);
		myindent.append("  ");
		result.append(indent);
		result.append("<option type=\"");
		result.append(type);
		result.append("\">\n");

		if (flatXML)
		{
			result.append(myindent);
			result.append("<definition>");
			result.append(optionSource);
			result.append("</definition>\n");
		}
		else
		{
			result.append(myindent);
			result.append("<definition>\n");
			result.append(myindent);
			result.append("  ");
			result.append(TagWriter.CDATA_START);
			result.append('\n');
			result.append(myindent);
			result.append("  ");
			result.append(optionSource.replace("\n", "\n  " + myindent.toString()));
			result.append('\n');

			result.append(myindent);
			result.append("  ");
			result.append(TagWriter.CDATA_END);
			result.append('\n');
			result.append(myindent);
			result.append("</definition>\n");
		}
		result.append(indent);
		result.append("</option>\n");
		return result;
	}

	public void setWriteFlaxXML(boolean flag)
	{
		this.flatXML = flag;
	}

	public String getType()
	{
		return type;
	}

	public String getOptionSource()
	{
		return optionSource;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final ObjectOption other = (ObjectOption) obj;
		return compareTo(other) == 0;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 47 * hash + (this.type != null ? this.type.hashCode() : 0);
		hash = 47 * hash + (this.optionSource != null ? this.optionSource.hashCode() : 0);
		return hash;
	}

	@Override
	public int compareTo(ObjectOption other)
	{
		int result = StringUtil.compareStrings(this.type, other.type, false);
		if (result != 0)
		{
			return result;
		}
		return StringUtil.compareStrings(this.optionSource, other.optionSource, false);
	}

}
