/*
 * NamedComponentChooser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui;

import java.awt.Component;
import org.netbeans.jemmy.ComponentChooser;

/**
 *
 * @author Thomas Kellerer
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
