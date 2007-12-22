/*
 * NamedComponentChooser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui;

import java.awt.Component;
import org.netbeans.jemmy.ComponentChooser;

/**
 *
 * @author support@sql-workbench.net
 */
public class NamedComponentChooser 
	implements ComponentChooser
{
	private String name;

	public NamedComponentChooser()
	{
	}
	
	public NamedComponentChooser(String s)
	{
		setName(s);
	}
					
	public boolean checkComponent(Component component)
	{
		String compName = component.getName();
		return this.getName().equalsIgnoreCase(compName);
	}

	public String getDescription()
	{
		return "Component: " + name;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String newName)
	{
		this.name = newName;
	}
	

}
