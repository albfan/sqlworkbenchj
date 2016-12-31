/*
 * LnFDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.lnf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * The definition of a pluggable look and feel. It stores the classname
 * of the Look & Feel together with the library from which the class
 * should be loaded
 *
 * @author Thomas Kellerer
 */
public class LnFDefinition
	implements Comparable<LnFDefinition>
{
	private String name;
	private String className;
	private List<String> liblist;
	private boolean isBuiltIn;
	public static final String LNF_PATH_SEPARATOR = "$|$";

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

	public LnFDefinition(String desc, String clazz, List<String> libs)
	{
		this.name = desc;
		this.className = (clazz == null ? clazz : clazz.trim());
		this.isBuiltIn = (libs == null);
		this.setLibraries(libs);
	}

	public boolean isBuiltInLnF()
	{
		return this.isBuiltIn;
	}

	public boolean isComplete()
	{
		if (this.isBuiltIn) return true;
		return !StringUtil.isEmptyString(this.name) && !StringUtil.isEmptyString(this.className)
			&& CollectionUtil.isNonEmpty(liblist);
	}

	@Override
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

	public List<String> getLibraries()
	{
		if (this.isBuiltIn) return Collections.singletonList("rt.jar");
		if (CollectionUtil.isEmpty(liblist)) return new ArrayList<String>(0);
		return Collections.unmodifiableList(liblist);
	}

	public void setLibraries(List<String> list)
	{
		if (list != null)
		{
			liblist = new ArrayList<String>(list);
		}
		else
		{
			liblist = null;
		}
	}

	public LnFDefinition createCopy()
	{
		return new LnFDefinition(getName(), getClassName(), liblist);
	}

	@Override
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
