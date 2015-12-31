/*
 * PGProcName.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import workbench.db.ColumnIdentifier;
import workbench.db.ProcedureDefinition;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PGProcName
	implements Comparable<PGProcName>
{
	private List<PGArg> arguments;
	private String procName;

	public PGProcName(ProcedureDefinition def, PGTypeLookup typeMap)
	{
		procName = def.getProcedureName();
		List<ColumnIdentifier> parameters = def.getParameters(null);
		if (CollectionUtil.isNonEmpty(parameters))
		{
			arguments = new ArrayList<>(parameters.size());
			for (ColumnIdentifier col : parameters)
			{
				String mode = col.getArgumentMode();
				PGType typ = typeMap.getEntryByType(col.getDbmsType());
        if (typ == null)
        {
          typ = new PGType(col.getDbmsType(), -1);
        }
				PGArg arg = new PGArg(typ, mode);
				arguments.add(arg);
			}
		}
		else
		{
			initFromDisplayName(def.getDisplayName(), typeMap);
		}
	}

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
		initFromDisplayName(fullname, typeMap);
	}

	private void initFromDisplayName(String displayName, PGTypeLookup typeMap)
	{
		int pos = displayName.indexOf('(');
		if (pos > -1)
		{
			procName = displayName.substring(0, pos);
			String args = displayName.substring(pos + 1, displayName.indexOf(')'));
			String[] elements = args.split(",");
			arguments = new ArrayList<>();
			for (String s : elements)
			{
				PGType typ = typeMap.getEntryByType(s.trim());
				if (typ != null)
				{
					PGArg arg = new PGArg(typ, "in");
					arguments.add(arg);
				}
			}
		}
		else
		{
			procName = displayName;
			arguments = Collections.emptyList();
		}
	}

	public PGProcName(String name, String oidArgs, String modes, PGTypeLookup typeMap)
	{
		procName = name;
		if (StringUtil.isNonBlank(oidArgs))
		{
			arguments = getTypesFromOid(oidArgs, modes, typeMap);
		}
	}

	private List<PGArg> getTypesFromOid(String oidList, String modes, PGTypeLookup typeMap)
	{
		String[] items = oidList.split(";");
		String[] paramModes = modes.split(";");

		List<PGArg> result = new ArrayList<>(items.length);
		for (int i=0; i < items.length; i++)
		{
			String arg = items[i];
			String mode = (i < paramModes.length ? paramModes[i] : null);

			if ("t".equals(mode)) continue;

			Long oid = Long.valueOf(arg.trim());
			PGType typ = typeMap.getTypeFromOID(oid);
			if (typ != null)
			{
				PGArg parg = new PGArg(typ, mode);
				result.add(parg);
			}
		}
		return result;
	}

	public String getInputOIDs()
	{
		if (arguments == null || arguments.isEmpty()) return null;
		StringBuilder argTypes = new StringBuilder(arguments.size() * 4);
		int argCount = 0;
		for (PGArg arg : arguments)
		{
			if (arg.argMode == PGArg.ArgMode.in || arg.argMode == PGArg.ArgMode.inout)
			{
				if (argCount > 0) argTypes.append(' ');
				argTypes.append(Long.toString(arg.argType.getOid()));
				argCount ++;
			}
		}
		return argTypes.toString();
	}

	public String getOIDs()
	{
		if (arguments == null || arguments.isEmpty()) return null;

		StringBuilder argTypes = new StringBuilder(arguments.size() * 4);
		for (int i=0; i < arguments.size(); i++)
		{
			if (i > 0) argTypes.append(' ');
			argTypes.append(Long.toString(arguments.get(i).argType.getOid()));
		}
		return argTypes.toString();
	}

	List<PGArg> getArguments()
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

	public String getSignature()
	{
		if (arguments == null || arguments.isEmpty()) return procName +"()";
		StringBuilder b = new StringBuilder(procName.length() + arguments.size() * 10);
		b.append(procName);
		b.append('(');
		int argCount = 0;
		for (PGArg arg : arguments)
		{
			if (arg.argMode == PGArg.ArgMode.in || arg.argMode == PGArg.ArgMode.inout)
			{
				if (argCount > 0) b.append(", ");
				b.append(arg.argType.getTypeName());
				argCount ++;
			}
		}
		b.append(')');
		return b.toString();
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
			b.append(arguments.get(i).argType.getTypeName());
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
