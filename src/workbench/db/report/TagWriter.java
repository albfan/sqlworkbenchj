/*
 * XmlUtil.java
 *
 * Created on September 9, 2004, 10:55 PM
 */

package workbench.db.report;

import workbench.util.StrBuffer;

/**
 *
 * @author  workbench@kellerer.org
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
	
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, int value)
	{
		appendTag(target, indent, tag, String.valueOf(value));
	}
	
	public void appendTag(StrBuffer target, StrBuffer indent, String tag, boolean value)
	{
		if (value)
			appendTag(target, indent, tag, "true");
		else
			appendTag(target, indent, tag, "false");
	}
	
	public  void appendTag(StrBuffer target, StrBuffer indent, String tag, String value)
	{
		appendOpenTag(target, indent, tag);
		target.append(value);
		appendCloseTag(target, null, tag);
	}
	
	public  void appendOpenTag(StrBuffer target, StrBuffer indent, String tag)
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
		target.append('>');
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
	
	
}
