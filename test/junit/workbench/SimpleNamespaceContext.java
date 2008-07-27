/*
 * SimpleNamespaceContext.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;


/**
 *
 * @author support@sql-workbench.net
 */
public class SimpleNamespaceContext 
	implements NamespaceContext
{

	private Map<String, String> namespaceMap;
	
	public SimpleNamespaceContext(Map<String, String> nameMap)
	{
		namespaceMap = new HashMap<String, String>(nameMap);
		namespaceMap.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
		namespaceMap.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
	}

	public String getNamespaceURI(String prefix)
	{
		String uri = namespaceMap.get(prefix);
		return (uri == null ? XMLConstants.XML_NS_URI : uri);
	}

	public String getPrefix(String namespaceURI)
	{
		if (namespaceURI == null) return null;

		for (Entry<String, String> entry : namespaceMap.entrySet())
		{
			if (entry.getValue().equals(namespaceURI)) return entry.getKey();
		}
		return null;
	}
	
	public Iterator getPrefixes(String namespaceURI)
	{
		List<String> prefixes = new ArrayList<String>();

		for ( String prefix : namespaceMap.keySet())
		{
			if (namespaceMap.get(prefix).equals(namespaceURI))
			{
				prefixes.add( prefix);
			}
		}
		return prefixes.iterator();
	}
	
}
