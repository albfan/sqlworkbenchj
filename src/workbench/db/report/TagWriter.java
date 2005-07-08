/*
 * TagWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Utility class to add XML tags to a StrBuffer.
 * Handles namespaces for the tags as well.
 * @author  support@sql-workbench.net
 */
public class TagWriter
{
	private String xmlNamespace = null;
	
	public TagWriter()
	{
	}
	
	public TagWriter(String ns)
	{
		this.xmlNamespace = ns;
	}
	
	/**
	 * Appends an integer value for a tag in one line. There will be a new line
	 * after the closing tag.
	 */
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, int value)
	{
		appendTag(target, indent, tag, String.valueOf(value), false);
	}
	
	/**
	 * Appends a boolean value for a tag in one line. There will be a new line
	 * after the closing tag.
	 */
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, boolean value)
	{
		if (value)
			appendTag(target, indent, tag, "true", false);
		else
			appendTag(target, indent, tag, "false", false);
	}
	
	/**
	 * Appends the tag and the value in one line. There will be a new line
	 * after the closing tag.
	 */
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, String value)
	{
		appendTag(target, indent, tag, value, false);
	}
	
	public void appendTagConditionally(StrBuffer target, StrBuffer indent, String tag, String value)
	{
		if (!StringUtil.isEmptyString(value)) appendTag(target, indent, tag, value, false);
	}
	
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, String value, String attr, String attValue)
	{
		appendOpenTag(target, indent, tag, attr, attValue);
		target.append(value);
		appendCloseTag(target, null, tag);
	}
	
	/**
	 * Appends the tag and the value in one line. There will be a new line
	 * after the closing tag. If checkCData is true, then the value 
	 * is checked for characters which require a <![CDATA[ "quoting"
	 */
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, String value, boolean checkCData)
	{
		appendOpenTag(target, indent, tag);
		boolean useCData = checkCData && needsCData(value);
		if (useCData) target.append("<![CDATA[");
		target.append(value);
		if (useCData) target.append("]]>");
		appendCloseTag(target, null, tag);
	}

	/**
	 * Appends the tag and the value in one line. There will be a new line
	 * after the closing tag.
	 */
	public void appendEmptyTag(StrBuffer target, StrBuffer indent, String tag, String attribute, String attValue)
	{
		String[] attr = new String[1];
		String[] values = new String[1];
		attr[0] = attribute;
		values[0] = attValue;
		appendOpenTag(target, indent, tag, attr, values, false);
		target.append("/>");
	}
	
	public void appendOpenTag(StrBuffer target, StrBuffer indent, String tag)
	{
		this.appendOpenTag(target, indent, tag, (String[])null, (String[])null);
	}
	
	public  void appendOpenTag(StrBuffer target, StrBuffer indent, String tag, String attribute, String attValue)
	{
		String[] attr = new String[1];
		String[] values = new String[1];
		attr[0] = attribute;
		values[0] = attValue;
		this.appendOpenTag(target, indent, tag, attr, values);
	}
	
	public void appendOpenTag(StrBuffer target, StrBuffer indent, String tag, String[] attributes, String[] values)
	{
		appendOpenTag(target, indent, tag, attributes, values, true);
	}
	
	/**
	 * Appends a opening tag to the target buffer including attributes.
	 * No new line will be written 
	 */
	public void appendOpenTag(StrBuffer target, StrBuffer indent, String tag, String[] attributes, String[] values, boolean closeTag)
	{
		if (indent != null) target.append(indent);
		target.append('<');
		if (this.xmlNamespace != null)
		{
			target.append(xmlNamespace);
			target.append(':');
		}
		target.append(tag);
		if (attributes != null && values != null)
		{
			for (int i=0; i < attributes.length; i++)
			{
				if (attributes[i] != null && values[i] != null)
				{
					target.append(' ');
					target.append(attributes[i]);
					target.append("=\"");
					target.append(values[i]);
					target.append('"');
				}
			}
		}
		if (closeTag) target.append('>');
	}

	public  void appendCloseTag(StrBuffer target, StrBuffer indent, String tag)
	{
		if (indent != null) target.append(indent);
		target.append("</");
		if (this.xmlNamespace != null)
		{
			target.append(xmlNamespace);
			target.append(':');
		}
		target.append(tag);
		target.append(">\n");
	}

	/**
	 * Getter for property namespace.
	 * @return Value of property namespace.
	 */
	public String getNamespace()
	{
		return xmlNamespace;
	}
	
	/**
	 * Setter for property namespace.
	 * @param namespace New value of property namespace.
	 */
	public void setNamespace(String namespace)
	{
		this.xmlNamespace = namespace;
	}

	private static final char[] SPECIAL_CHARS = new char[] {'<', '>', '&', '\'', '\n', '\r' };
	
	private boolean needsCData(String value)
	{
		if (value == null) return false;
		for (int i=0; i < SPECIAL_CHARS.length; i++)
		{
			if (value.indexOf(SPECIAL_CHARS[i]) > -1) return true;
		}
		return false;
	}
	
}
