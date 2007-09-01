/*
 * LnFDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import workbench.util.StringUtil;

/**
 * The definition of a pluggable look and feel. It stores the classname
 * of the Look & Feel together with the library from which the class
 * should be loaded
 * 
 * @author support@sql-workbench.net
 */
public class LnFDefinition
	implements Comparable<LnFDefinition>
{
	private String name;
	private String className;
	private String library;
	private boolean isBuiltIn;

	public LnFDefinition(String desc)
	{
		this(desc, null, null);
		this.isBuiltIn = false;
	}
	
	public LnFDefinition(String desc, String clazz)
	{
		this(desc, clazz, null);
		this.isBuiltIn = true;
 	}

	public LnFDefinition(String desc, String clazz, String libs)
	{
		this.name = desc;
		this.className = (clazz == null ? clazz : clazz.trim());
		this.library = libs;
		this.isBuiltIn = (libs == null);
	}
	
	public boolean isBuiltInLnF()
	{
		return this.isBuiltIn;
	}
	
	public boolean isComplete()
	{
		if (this.isBuiltIn) return true;
		return !StringUtil.isEmptyString(this.name) && !StringUtil.isEmptyString(this.className)
		       && !StringUtil.isEmptyString(this.library);
	}
	
	public String toString()
	{
		return getName();
	}
	
	public String getName()
	{
		return name;
	}

	public String getClassName()
	{
		return className;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setClassName(String className)
	{
		this.className = className;
	}

	public String getLibrary()
	{
		if (this.isBuiltIn) return "rt.jar";
		return library;
	}

	public void setLibrary(String library)
	{
		this.library = library;
	}

	public LnFDefinition createCopy()
	{
		return new LnFDefinition(getName(), getClassName(), getLibrary());
	}

	public int compareTo(LnFDefinition o)
	{
		String cls = o.getClassName();
		return this.className.compareTo(cls);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof LnFDefinition)
		{
			LnFDefinition other = (LnFDefinition)o;
			return this.className.equals(other.className);
		}
		if (o instanceof String)
		{
			return this.className.equals((String)o);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return this.className.hashCode();
	}

	
	
}
