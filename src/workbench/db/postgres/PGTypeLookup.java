/*
 * PGTypeLookup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Thomas Kellerer
 */
public class PGTypeLookup
{
	private Map<Long, PGType> oidToTypeMap;
	private Map<String, PGType> rawTypeMap;

	public PGTypeLookup(Map<Long, PGType> oidMap)
	{
		oidToTypeMap = oidMap;
		rawTypeMap = new HashMap<String, PGType>(oidToTypeMap.size());
	}

	public PGType getTypeFromOID(long oid)
	{
		return oidToTypeMap.get(Long.valueOf(oid));
	}

	public PGType getTypeEntry(String rawType)
	{
		PGType result = rawTypeMap.get(rawType);
		if (result == null)
		{
			for (PGType typ : oidToTypeMap.values())
			{
				if (typ.rawType.equals(rawType))
				{
					rawTypeMap.put(rawType, typ);
					result = typ;
					break;
				}
			}
		}
		return result;
	}
}
