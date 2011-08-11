/*
 * TagAttribute.java
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

import workbench.util.HtmlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TagAttribute
{
	private final CharSequence tagText;

	public TagAttribute(String name, String value)
	{
		StringBuilder b = new StringBuilder(name.length() + value.length() + 1);
		b.append(name);
		b.append("=\"");
		b.append(HtmlUtil.escapeHTML(value));
		b.append('"');
		tagText = b;
	}

	public CharSequence getTagText()
	{
		return tagText;
	}

}
