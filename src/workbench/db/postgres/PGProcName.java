/*
 * PGProcName.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
	private String formattedName;

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
				PGType typ = typeMap.getTypeEntry(s.trim());
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

	public static List<PGType> getTypesFromOid(String oidList, PGTypeLookup typeMap)
	{
		String[] items = oidList.split(";");
		List<PGType> result = new ArrayList<PGType>(items.length);
		for (String s : items)
		{
			int oid = Integer.valueOf(s.trim());
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
			argTypes.append(Integer.toString(arguments.get(i).oid));
		}
		return argTypes.toString();
	}

	public List<PGType> getArguments()
	{
		return arguments;
	}

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
		if (formattedName == null)
		{
			StringBuilder b = new StringBuilder(procName.length() + arguments.size() * 10);
			b.append(procName);
			b.append('(');
			for (int i=0; i < arguments.size(); i++)
			{
				if (i > 0) b.append(", ");
				b.append(arguments.get(i).rawType);
			}
			b.append(')');
			formattedName = b.toString();
		}
		return formattedName;
	}

	public String toString()
	{
		return getFormattedName();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final PGProcName other = (PGProcName) obj;
		if (this.arguments != other.arguments && (this.arguments == null || !this.arguments.equals(other.arguments)))
		{
			return false;
		}
		if ((this.procName == null) ? (other.procName != null) : !this.procName.equals(other.procName))
		{
			return false;
		}
		return true;
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
