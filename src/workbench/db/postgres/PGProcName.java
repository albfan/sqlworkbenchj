/*
 * PGProcName.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.postgres;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PGProcName
	implements Comparable<PGProcName>
{
	private List<PGType> arguments;
	private String procName;

	/**
	 * Initialize a PGProcName from a "full" name that includes the
	 * procedure's name and all parameter types in brackets.
	 * <br/>
	 * e.g. my_func(int4, varchar, date)
	 *
	 * @param fullname
	 * @param typeMap
	 */
	public PGProcName(String fullname, PGTypeLookup typeMap)
	{
		int pos = fullname.indexOf('(');
		if (pos > -1)
		{
			procName = fullname.substring(0, pos);
			String args = fullname.substring(pos + 1, fullname.indexOf(')'));
			String[] elements = args.split(",");
			arguments = new ArrayList<PGType>();
			for (String s : elements)
			{
				PGType typ = typeMap.getEntryByFormattedType(s.trim());
				if (typ != null)
				{
					arguments.add(typ);
				}
			}
		}
		else
		{
			procName = fullname;
			arguments = Collections.emptyList();
		}
	}

	public PGProcName(String name, String oidArgs, PGTypeLookup typeMap)
	{
		procName = name;
		if (StringUtil.isNonBlank(oidArgs))
		{
			arguments = getTypesFromOid(oidArgs, typeMap);
		}
	}

	private List<PGType> getTypesFromOid(String oidList, PGTypeLookup typeMap)
	{
		String[] items = oidList.split(";");
		List<PGType> result = new ArrayList<PGType>(items.length);
		for (String s : items)
		{
			Long oid = Long.valueOf(s.trim());
			PGType typ = typeMap.getTypeFromOID(oid);
			if (typ != null)
			{
				result.add(typ);
			}
		}
		return result;
	}

	public String getOIDs()
	{
		if (arguments == null || arguments.isEmpty()) return null;

		StringBuilder argTypes = new StringBuilder(arguments.size() * 4);
		for (int i=0; i < arguments.size(); i++)
		{
			if (i > 0) argTypes.append(' ');
			argTypes.append(Long.toString(arguments.get(i).oid));
		}
		return argTypes.toString();
	}

	public List<PGType> getArguments()
	{
		return arguments;
	}

	@Override
	public int compareTo(PGProcName o)
	{
		return getFormattedName().compareTo(o.getFormattedName());
	}

	public String getName()
	{
		return procName;
	}

	public String getFormattedName()
	{
		if (arguments == null || arguments.isEmpty()) return procName +"()";
		StringBuilder b = new StringBuilder(procName.length() + arguments.size() * 10);
		b.append(procName);
		b.append('(');
		for (int i=0; i < arguments.size(); i++)
		{
			if (i > 0) b.append(", ");
			b.append(arguments.get(i).formattedType);
		}
		b.append(')');
		return b.toString();
	}

	@Override
	public String toString()
	{
		return getFormattedName();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof PGProcName)
		{
			final PGProcName other = (PGProcName) obj;
			String myName = getFormattedName();
			String otherName = other.getFormattedName();
			return myName.equals(otherName);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 5;
		hash = 79 * hash + (this.arguments != null ? this.arguments.hashCode() : 0);
		hash = 79 * hash + (this.procName != null ? this.procName.hashCode() : 0);
		return hash;
	}


}
