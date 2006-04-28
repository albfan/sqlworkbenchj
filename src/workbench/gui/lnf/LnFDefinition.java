/*
 * LnFDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class LnFDefinition
{
	private String name;
	private String className;
	private String library;
	private boolean isBuiltIn;

	public LnFDefinition(String desc)
	{
		this(desc, null, null);
	}
	
	public LnFDefinition(String desc, String clazz)
	{
		this(desc, clazz, null);
		this.isBuiltIn = true;
 	}

	public LnFDefinition(String desc, String clazz, String libs)
	{
		this.name = desc;
		this.className = clazz;
		this.library = libs;
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
	
}
