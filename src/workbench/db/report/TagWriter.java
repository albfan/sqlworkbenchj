/*
 * TagWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.report;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import workbench.resource.ResourceMgr;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Utility class to add XML tags to a StrBuffer.
 * Handles namespaces for the tags as well.
 * @author  Thomas Kellerer
 */
public class TagWriter
{
	public static final String CDATA_START = "<![CDATA[";
	public static final String CDATA_END = "]]>";

	public static final String TAG_GENERATED_BY = "generated-by";

	public TagWriter()
	{
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

	public void appendCDATATag(StrBuffer target, StrBuffer indent, String tag, CharSequence value, String attr, String attValue)
	{
		TagAttribute ta = new TagAttribute(attr, attValue);
		appendCDATATag(target, indent, tag, value, ta);
	}

	public void appendCDATATag(StrBuffer target, StrBuffer indent, String tag, CharSequence value, TagAttribute... attrs)
	{
		appendOpenTag(target, indent, tag, true, attrs);
		target.append('\n');
		target.append(indent);
		target.append("  ");
		target.append(CDATA_START);
		target.append(value);
		target.append(CDATA_END);
		target.append('\n');
		target.append(indent);
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
		if (StringUtil.isNonBlank(attribute))
		{
			appendOpenTag(target, indent, tag, true, new TagAttribute(attribute, attValue));
		}
		else
		{
			appendOpenTag(target, indent, tag, true);
		}
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
		target.append(tag);
		if (attributes != null && attributes.length > 0)
		{
			for (TagAttribute att : attributes)
			{
				if (att != null)
				{
					target.append(' ');
					target.append(att.getTagText());
				}
			}
		}
		if (closeTag) target.append('>');
	}

	public void appendCloseTag(StrBuffer target, StrBuffer indent, String tag)
	{
		if (indent != null) target.append(indent);
		target.append("</");
		target.append(tag);
		target.append(">\n");
	}

	private static final char[] SPECIAL_CHARS = new char[] {'<', '>', '&', '\n', '\r' };

	public static boolean needsCData(CharSequence value)
	{
		if (value == null) return false;
		for (int i=0; i < SPECIAL_CHARS.length; i++)
		{
			if (StringUtil.indexOf(value, SPECIAL_CHARS[i]) > -1) return true;
		}
		return false;
	}

	public void writeWorkbenchVersion(StrBuffer target, StrBuffer indent)
	{
		appendTag(target, indent, TAG_GENERATED_BY, ResourceMgr.TXT_PRODUCT_NAME + " " + ResourceMgr.getBuildInfo());
	}

	public static void writeWorkbenchVersion(Writer out, StrBuffer indent)
		throws IOException
	{
		indent.writeTo(out);
		out.append('<');
		out.append(TAG_GENERATED_BY);
		out.append('>');
		out.append(ResourceMgr.TXT_PRODUCT_NAME + " " + ResourceMgr.getBuildInfo());
		out.append("</");
		out.append(TAG_GENERATED_BY);
		out.append(">\n");
	}

}
