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
	
	public  void appendTag(StrBuffer target, StrBuffer indent, String tag, String value)
	{
		appendOpenTag(target, indent, tag);
		target.append(value);
		appendCloseTag(target, null, tag);
	}
	
	public  void appendOpenTag(StrBuffer target, StrBuffer indent, String tag)
	{
		if (indent != null) target.append(indent);
		target.append('<');
		if (this.xmlNamespace != null)
		{
			target.append(xmlNamespace);
			target.append(':');
		}
		target.append(tag);
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
