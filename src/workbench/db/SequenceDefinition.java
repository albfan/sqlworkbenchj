/*
 * SequenceDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 *
 * @author support@sql-workbench.net
 */
public class SequenceDefinition
	implements DbObject
{
	private String sequenceName;
	private String sequenceOwner;
	private CharSequence source;
	
	private Map<String, Object> properties = new LinkedHashMap<String, Object>();
	
	public SequenceDefinition(String owner, String name)
	{
		sequenceName = name;
		sequenceOwner = owner;
	}

	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.sequenceName);
	}
	
	public String getObjectExpression(WbConnection conn)
	{
		StringBuilder expr = new StringBuilder(30);
		if (sequenceOwner != null)
		{
			expr.append(conn.getMetadata().quoteObjectname(sequenceOwner));
			expr.append('.');
		}
		expr.append(conn.getMetadata().quoteObjectname(sequenceName));
		return expr.toString();
	}
	
	public String getObjectType()
	{
		return "SEQUENCE";
	}
	
	public String getDisplayName()
	{
		return getSequenceName();
	}
	
	public String getSequenceName() 
	{
		return this.sequenceName;
	}
	
	public String getSequenceOwner()
	{
		return this.sequenceOwner;
	}
	
	/**
	 * As each Database has a different set of attributes for a sequence
	 * these attributes are stored as key/value pairs to be as generic
	 * as possible
	 */
	public void setSequenceProperty(String property, Object value)
	{
		this.properties.put(property, value);
	}
	
	public Object getSequenceProperty(String property)
	{
		return properties.get(property);
	}
	
	public CharSequence getSource()
	{
		return source;
	}
	
	public void setSource(CharSequence src)
	{
		source = src;
	}
	
	public Iterator<String> getProperties()
	{
		return this.properties.keySet().iterator();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof SequenceDefinition)
		{
			SequenceDefinition od = (SequenceDefinition)other;
			if (od.properties.size() != this.properties.size()) return false;
			Iterator<Map.Entry<String, Object>> entries = this.properties.entrySet().iterator();
			
			while (entries.hasNext())
			{
				Map.Entry entry = entries.next();
				Object ov = od.properties.get(entry.getKey());
				if (ov == null) return false;
				if (!ov.equals(entry.getValue())) return false; 
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 67 * hash + (this.properties != null ? this.properties.hashCode() : 0);
		return hash;
	}
	
}