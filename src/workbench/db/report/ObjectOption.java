/*
 * ObjectOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */

package workbench.db.report;

import workbench.util.StrBuffer;
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

	public ObjectOption(String type, String optionSource)
	{
		this.type = type;
		this.optionSource = optionSource;
	}

	public StrBuffer getXml(StrBuffer indent)
	{
		StrBuffer result = new StrBuffer(100);
		StrBuffer myindent = new StrBuffer(indent);
		myindent.append("  ");
		result.append(indent);
		result.append("<option type=\"");
		result.append(type);
		result.append("\">\n");

		result.append(myindent);
		result.append("<source>\n");
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
		result.append("</source>\n");
		result.append(indent);
		result.append("</option>\n");
		return result;
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
