/*
 * TagWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Utility class to add XML tags to a StrBuffer.
 * Handles namespaces for the tags as well.
 * @author  support@sql-workbench.net
 */
public class TagWriter
{
	public static final String CDATA_START = "<![CDATA[";
	public static final String CDATA_END = "]]>";
	
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
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, CharSequence value)
	{
		appendTag(target, indent, tag, value, false);
	}
	
	public void appendTagConditionally(StrBuffer target, StrBuffer indent, String tag, String value)
	{
		if (!StringUtil.isEmptyString(value)) appendTag(target, indent, tag, value, false);
	}
	
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, CharSequence value, String attr, String attValue)
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
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, CharSequence value, boolean checkCData)
	{
		appendOpenTag(target, indent, tag);
		boolean useCData = checkCData && needsCData(value);
		if (useCData) 
		{
			target.append(CDATA_START);
			target.append('\n');
		}
		target.append(value);
		if (useCData) 
		{
			target.append(CDATA_END);
			target.append('\n');
			target.append(indent);
		}
		appendCloseTag(target, null, tag);
	}

	/**
	 * Appends the tag and the value in one line. There will be a new line
	 * after the closing tag.
	 */
	public void appendEmptyTag(StrBuffer target, StrBuffer indent, String tag, String attribute, String attValue)
	{
		appendOpenTag(target, indent, tag, false, new TagAttribute(attribute, attValue));
		target.append("/>");
	}
	
	public void appendOpenTag(StrBuffer target, StrBuffer indent, String tag)
	{
		this.appendOpenTag(target, indent, tag, null, true);
	}
	
	public  void appendOpenTag(StrBuffer target, StrBuffer indent, String tag, String attribute, String attValue)
	{
		appendOpenTag(target, indent, tag, true, new TagAttribute(attribute, attValue));
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
		List<TagAttribute> att = null;
		if (attributes != null)
		{
			att = new ArrayList<TagAttribute>(attributes.length);
			for (int i=0; i < attributes.length; i++)
			{
				att.add(new TagAttribute(attributes[i], values[i]));
			}
		}
		appendOpenTag(target, indent, tag, att, closeTag);
	}
	
	public void appendOpenTag(StrBuffer target, StrBuffer indent, String tag, Collection<TagAttribute> attributes, boolean closeTag)		
	{
		if (indent != null) target.append(indent);
		target.append('<');
		if (this.xmlNamespace != null)
		{
			target.append(xmlNamespace);
			target.append(':');
		}
		target.append(tag);
		if (attributes != null && attributes.size() > 0)
		{
			for (TagAttribute att : attributes)
			{
				target.append(' ');
				target.append(att.getTagText());
			}
		}
		if (closeTag) target.append('>');
	}

	public void appendOpenTag(StrBuffer target, StrBuffer indent, String tag, boolean closeTag, TagAttribute ... attributes)		
	{
		if (indent != null) target.append(indent);
		target.append('<');
		if (this.xmlNamespace != null)
		{
			target.append(xmlNamespace);
			target.append(':');
		}
		target.append(tag);
		for (TagAttribute att : attributes)
		{
			target.append(' ');
			target.append(att.getTagText());
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
	
	private boolean needsCData(CharSequence value)
	{
		if (value == null) return false;
		for (int i=0; i < SPECIAL_CHARS.length; i++)
		{
			if (StringUtil.indexOf(value, SPECIAL_CHARS[i]) > -1) return true;
		}
		return false;
	}
	
}
