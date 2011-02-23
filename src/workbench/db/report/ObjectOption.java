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

/**
 * A class to store generic options for database objects such as tables or indexes
 * @author Thomas Kellerer
 */
public class ObjectOption
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

	public static void main(String[] args)
	{
		String source = "line 1\nline 2\nline 3";
		System.out.println(source.replace("\n", "\n   "));
	}
}
